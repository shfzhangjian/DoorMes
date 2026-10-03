<script lang="ts" setup>
import type { GrindingConsumption } from '#/api/mes/hc/grinding-consumption';
import { getGrindingConsumptionDefault, newGrindingConsumption, validateGrindingConsumption } from '#/api/mes/hc/grinding-consumption';

import { isSampleLockDeferredToCutRound } from '../shared/sampleAbnormalLockPolicy';
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
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';
import { useRoute } from 'vue-router';

import { uploadFile } from '#/api/infra/file';
import type { MesHcRoughGrindingConsoleApi } from '#/api/mes/hc/execution/rough-grinding-console';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import {
  applyRoughGrindingConsoleFai,
  applyRoughGrindingConsoleSecondSegmentInspection,
  completeRoughGrindingConsoleWorkOrder,
  confirmRoughGrindingConsoleSecondReport,
  confirmRoughGrindingConsoleDailyCheck,
  confirmRoughGrindingConsoleMiddleProductRecord,
  deleteRoughGrindingConsoleFirstAllocation,
  deleteRoughGrindingConsoleFirstReport,
  deleteRoughGrindingConsoleSecondReport,
  getRoughGrindingConsoleAbnormalPositionList,
  getRoughGrindingConsoleBoard,
  getRoughGrindingConsoleEquipmentOptions,
  getRoughGrindingConsoleFaiSummary,
  getRoughGrindingConsoleMiddleProductRecord,
  getRoughGrindingConsoleSecondSegmentInspectionSummary,
  getOrInitRoughGrindingConsoleSegmentMiddleProductRecord,
  markRoughGrindingConsoleSecondReportPrinted,
  replaceRoughGrindingConsoleConsumable,
  reviseRoughGrindingConsoleStatisticsData,
  saveRoughGrindingConsoleDailyCheck,
  saveRoughGrindingConsoleFirstAllocation,
  saveRoughGrindingConsoleMiddleProductRecord,
  saveRoughGrindingConsoleSecondReport,
  stampRoughGrindingConsoleSegmentTiming,
  startRoughGrindingConsoleWorkOrder,
  switchRoughGrindingConsoleWorkOrderEquipment,
  updateRoughGrindingConsoleReportTime,
} from '#/api/mes/hc/execution/rough-grinding-console';
import { getRoughGrindingReportTaskList } from '#/api/mes/hc/execution/rough-grinding-report';
import {
  confirmProcessFormRecordBySigner,
  exportProcessFormRecordLayout,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  importProcessFormRecordLayout,
  updateProcessFormRecord,
} from '#/api/mes/hc/processform';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import { resolvePublishedStationForm } from '#/api/mes/hc/stationform';
import { updateToolingConsumableLedgerUsageStatus } from '#/api/mes/hc/tooling-consumable-ledger';
import {
  buildRoughMiddleProductExcelLayout,
  loadRoughMiddleProductThicknessLabels,
  resolveRoughMiddleProductColumns,
  ROUGH_MIDDLE_PRODUCT_COLUMNS,
} from '#/views/mes/hc/shared/roughGrindingFormLayout';
import RoughMiddleProductRecordSheet from '#/views/mes/hc/shared/RoughMiddleProductRecordSheet.vue';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';
import ConsumableLedgerSwitchModal from '#/views/mes/hc/base/tooling-consumable-ledger/components/ConsumableLedgerSwitchModal.vue';
import EdgeConsumableRegisterTab from '#/views/mes/hc/base/tooling-consumable-ledger/components/EdgeConsumableRegisterTab.vue';
import FaiDetailModal from '#/views/mes/quality/fai/modules/detail-modal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import {
  getActiveSampleAbnormalLock,
  type MesQmsSampleAbnormalRecheckApi,
} from '#/api/mes/quality/sample-abnormal-recheck';
import QmsSampleAbnormalLockGuard from '#/views/mes/quality/sample-abnormal-recheck/components/QmsSampleAbnormalLockGuard.vue';
import RoughGrindingFirstInspectionPrintPreviewModal from '../rough-grinding/modules/RoughGrindingFirstInspectionPrintPreviewModal.vue';
import {
  ProductionInstructionMessageTab,
  type ProductionInstructionContext,
} from '#/views/mes/hc/shared/production-instruction';
import { getProductionInstructionUnreadCount } from '#/api/mes/hc/production-instruction';
import { useExecutionFullscreenClock } from '../shared/useExecutionFullscreenClock';
import {
  buildInspectionTransferTicketPayload,
  buildTransferTicketQrValue,
  buildWorkOrderTicketHtml,
  resolveTransferTicketQrBusinessNo,
  sendTransferTicketToPrintAgent,
} from '../shared/workOrderTicketPrint';
import { applyPrintFieldTemplate } from '../shared/printFieldTemplate';

defineOptions({ name: 'MesExecutionRoughGrindingConsolePrototype' });

const middleThicknessLabels = ref<string[]>([]);
const middleThicknessColumns = computed(() => resolveRoughMiddleProductColumns(middleThicknessLabels.value));

const TRANSFER_TICKET_PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const PLAN_SCAN_MIN_LENGTH = 10;
const DEFAULT_SANDPAPER_LIMIT_DAYS = 15;
const DEFAULT_SANDPAPER_LIMIT_LENGTH = 500;
const DEFAULT_GUIDE_CLOTH_LIMIT_COUNT = 200;
const GUIDE_CLOTH_USE_INCREMENT = 2;
const CONSUMABLE_WARNING_PERCENT = 90;
const LAST_BOUND_EQUIPMENT_STORAGE_KEY = 'mes:rough-grinding-console:last-bound-equipment';
const MIDDLE_PRODUCT_PAGE_SIZE = 50;
const MIDDLE_PRODUCT_DEFAULT_ROW_COUNT = 50;
const SECOND_RANGE_EPS = 0.0001;
const SHOW_TOP_FIRST_INSPECTION = false;
const TASK_LIST_DEFAULT_PAGE_SIZE = 10;
const GLOBAL_SCANNER_MAX_GAP_MS = 80;
const GLOBAL_SCANNER_IDLE_FLUSH_MS = 160;
type GrindPass = 'FIRST' | 'SECOND';
type BoardTabKey = GrindPass | 'ABNORMAL_POSITIONS' | 'CHECK' | 'EDGE_CONSUMABLE' | 'INSPECTION_RECORDS' | 'INSTRUCTION_MESSAGES' | 'MIDDLE_RECORDS' | 'RECORDS';
type CheckType = 'CLEANING' | 'STARTUP';
type DailyCheckMode = 'confirm' | 'edit' | 'view';
type FirstInspectionStatus = 'NG' | 'OK' | 'PENDING' | 'WAITING';
type RoughConsoleConsumableType = 'GUIDE_CLOTH' | 'SANDPAPER';
type ConsumableUsageStatus = 'ACTIVE' | 'USED_UP';
type RoughConsoleConsumableLedgerSelection = MesHcToolingConsumableLedgerApi.Ledger & {
  nextUsageStatus?: ConsumableUsageStatus;
};
type RoughConsoleConsumableApply = (row: RoughConsoleConsumableLedgerSelection) => void;
type PendingConsumableUsageStatus = {
  batchNo?: string;
  ledgerId: number;
  nextUsageStatus?: ConsumableUsageStatus;
};

interface PlanInfo {
  availableLength: number;
  batchNo: string;
  endTime?: string;
  materialCode: string;
  modelCode: string;
  planId?: number;
  planOperationId?: number;
  planNo: string;
  previousEndTime?: string;
  previousOperationName: string;
  previousOperationStatus?: string;
  previousOutputLength: number;
  recorderName?: string;
  requirements: string;
  startTime?: string;
  status?: string;
}

interface FirstInspectionTask {
  feedbackRemark?: string;
  feedbackTime?: string;
  allowReportSubmit?: boolean;
  faiId?: number;
  faiJudgment?: string;
  faiNo?: string;
  faiStatus?: string;
  faiStandardNo?: string;
  faiStandardVersion?: string;
  id: string;
  motherBatchNo?: string;
  parentBatchNo?: string;
  kind?: 'FIRST_SAMPLE' | 'SECOND_SEGMENT_SAMPLE';
  planNo: string;
  productionBatchNo?: string;
  pushedAt: string;
  result?: 'NG' | 'OK';
  sampleLength?: number;
  sampleType?: string;
  sampleTypeName?: string;
  secondDetailId?: number;
  segmentLabel?: string;
  segmentMark?: string;
  sourceModule?: string;
  status: FirstInspectionStatus;
}

interface ConsumableState {
  batchNo: string;
  id?: number;
  lastReplaceTime: string;
  lifeLength: number;
  name: string;
  thresholdDays?: number;
  thresholdLength: number;
  thresholdUseCount?: number;
  useCount: number;
}

interface DailySummary {
  lossLength: number;
  middleProductGeneratedLength?: number;
  middleProductRecordId?: number;
  middleProductRecordTime?: string;
  middleProductRecorder?: string;
  middleProductStatus?: string;
  motherBatchNo?: string;
  napSampleLength: number;
  researchConsumptionLength: number;
  outputLength: number;
  parentBatchNo?: string;
  processLength: number;
}

interface SegmentSummary extends DailySummary {
  label: string;
  latestRecord?: WorkRecord;
  recordCount?: number;
  value: string;
}

interface GrindingSegmentTiming {
  endOperatorName?: string;
  endTime?: string;
  firstDetailId?: number;
  id?: number;
  passType: GrindPass;
  qtime?: MesHcRoughGrindingConsoleApi.QtimeInfo;
  secondDetailId?: number;
  segmentBatchNo?: string;
  segmentMark: 'NONE' | 'P' | 'Q' | 'R' | 'S';
  startOperatorName?: string;
  startTime?: string;
}

type SegmentTimingAction = 'START' | 'END';
type TimingSegmentMark = 'NONE' | 'P' | 'Q' | 'R' | 'S';

interface PendingSegmentTimingAction {
  action: SegmentTimingAction;
  passType: GrindPass;
  segmentMark: TimingSegmentMark;
}

type SampleLockCandidate = MesQmsSampleAbnormalRecheckApi.ActiveReqVO & {
  objectLabel: string;
  processName: string;
};

interface WorkRecord {
  consumption?: GrindingConsumption;
  afterGrindingThickness?: string;
  batchNo: string;
  confirmStatus?: string;
  confirmTime?: string;
  currentGuideClothBatchNo?: string;
  currentSandpaperBatchNo?: string;
  defectCode?: string;
  endTime: string;
  grindingMeters?: number;
  grindingThickness?: string;
  guideClothBatchNo?: string;
  id?: number;
  firstAllocationId?: number;
  lineSpeed?: string;
  lossLength: number;
  meterCounter?: string;
  middleProductGeneratedLength?: number;
  middleProductRecordId?: number;
  middleProductRecordTime?: string;
  middleProductRecorder?: string;
  middleProductStatus?: string;
  motherBatchNo?: string;
  napSampleLength: number;
  researchConsumptionLength: number;
  outputLength: number;
  passName: string;
  passType: GrindPass;
  planNo: string;
  inspectionApplyTime?: string;
  inspectionId?: number;
  inspectionNo?: string;
  inspectionRejectReason?: string;
  inspectionResult?: string;
  inspectionReturnTime?: string;
  inspectionStatus?: string;
  printCount?: number;
  printStatus?: string;
  printTime?: string;
  pressure?: string;
  processLength: number;
  productionBatchNo?: string;
  qualityThickness?: string;
  qualityWidth?: string;
  recorder?: string;
  reportDate?: string;
  recordKey: string;
  recordRole?: 'FIRST_ALLOCATION' | 'FIRST_ORIGINAL' | 'SECOND' | string;
  rotationSpeed?: string;
  sandpaperBatchNo?: string;
  segmentTimings?: GrindingSegmentTiming[];
  segmentMark: string;
  selfCheck?: string;
  startPosition?: number;
  startTime: string;
}

interface ProcessItem {
  actualValue: string;
  category: string;
  itemName: string;
  remark?: string;
  result: 'NG' | 'OK';
  standard: string;
}

interface RoughReportAbnormalPositionRow {
  abnormalLength?: number;
  clientKey: string;
  positionText: string;
  remark?: string;
  sortOrder?: number;
}

interface MiddleProductRow {
  [key: string]: any;
  batchNo: string;
  grindingPass: string;
  guideClothBatchNo: string;
  guideClothLife: number | string;
  innerThickness: string;
  inputLength: number | string;
  key: number;
  length: number | string;
  lengthMeter: number | string;
  materialCode: string;
  modelCode: string;
  outerThickness: string;
  outputLength: number | string;
  recorderName: string;
  recordDate: string;
  remark: string;
  replaceReason: string;
  result: 'NG' | 'OK';
  sandpaperBatchNo: string;
  sandpaperLife: number | string;
  seq: number;
  thickness: string;
  width: string;
}

interface MiddleProductDetailHeader {
  attachments?: MiddleProductAttachment[];
  batchNo?: string;
  displayName?: string;
  formName?: string;
  generatedLength?: number;
  materialCode?: string;
  motherBatchNo?: string;
  motherModelCode?: string;
  passName?: string;
  passType?: GrindPass;
  planNo?: string;
  processLength?: number;
  productionBatchNo?: string;
  recordDate?: string;
  recorder?: string;
  recorderTime?: string;
  segmentMark?: string;
  segmentName?: string;
  segmentTotalLength?: number;
  confirmer?: string;
  confirmerTime?: string;
  docStatus?: string;
  width?: string;
  widthMm?: string;
}

interface MiddleProductAttachment {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uid?: string;
  uploadTime?: string;
  url?: string;
}

interface WorkPrepareDetail {
  category?: string;
  item: string;
  remark: string;
  result: 'NG' | 'OK';
  seq: number;
  standard: string;
  value: string;
}

interface WorkPrepareRow {
  canConfirm?: boolean;
  canFill?: boolean;
  canView?: boolean;
  confirmer: string;
  confirmerTime: string;
  details: WorkPrepareDetail[];
  formCode?: string;
  formId?: number;
  key: CheckType;
  name: string;
  recordId?: number;
  recorder: string;
  recorderTime: string;
  result: string;
  status: 'COMPLETED' | 'FILLED' | 'PENDING';
  timing: string;
}

const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const currentDateText = computed(() => dayjs(currentDateTime.value).format('YYYY-MM-DD'));
const currentTimeText = computed(() => dayjs(currentDateTime.value).format('HH:mm:ss'));
const { showExecutionClock } = useExecutionFullscreenClock();
let timer: ReturnType<typeof setInterval> | null = null;
const qtimeNowTimestamp = ref(Date.now());
const route = useRoute();
const userStore = useUserStore();
const boardLoading = ref(false);
const firstInspectionApplying = ref(false);
const secondSegmentInspectionApplyingKey = ref('');
const dailyCheckLoaded = ref(false);
const currentTask = ref<any>();
const sourceBalances = ref<any[]>([]);
const equipmentOptions = ref<any[]>([]);

const currentPlan = reactive<PlanInfo>({
  availableLength: 0,
  batchNo: '',
  materialCode: '',
  modelCode: '',
  planNo: '',
  previousEndTime: '',
  previousOperationName: '',
  previousOutputLength: 0,
  requirements: '',
});

const productionInstructionUnreadCount = ref(0);
const productionInstructionContext = computed<ProductionInstructionContext>(() => ({
  operationCode: 'WC-GRIND',
  operationName: '磨皮',
  processCode: 'WC-GRIND',
  processName: '磨皮',
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

const sideStockLength = ref(0);
const scanPlanNo = ref('');
const planScanInputRef = ref<any>();
const recordScanConfirmInputRef = ref<any>();
const lastAutoScannedPlanNo = ref('');
let planScanTimer: ReturnType<typeof setTimeout> | null = null;
let pendingScannerPlanNo = '';
let globalScannerBuffer = '';
let globalScannerLastAt = 0;
let globalScannerTimer: ReturnType<typeof setTimeout> | null = null;
let globalScannerListenerAttached = false;
const PLAN_SCAN_SEPARATOR_REGEXP = /[，,；;]/;
const hasPlanScanDelimiter = (value?: string) => PLAN_SCAN_SEPARATOR_REGEXP.test(value || '');
const normalizePlanScanNo = (value?: string) => (value || '').trim().split(PLAN_SCAN_SEPARATOR_REGEXP)[0]?.trim() || '';
const normalizeConfirmScanCode = (value?: string) => {
  const text = String(value || '').trim();
  const separatorIndex = text.search(/[，,]/);
  return separatorIndex >= 0 ? text.slice(separatorIndex + 1).trim() || text : text;
};
const syncPlanScanNo = (value?: string) => {
  const scannerInput = hasPlanScanDelimiter(value);
  const planNo = normalizePlanScanNo(value);
  if (scannerInput) pendingScannerPlanNo = planNo;
  if ((value || '').trim() !== planNo && scanPlanNo.value !== planNo) {
    scanPlanNo.value = planNo;
  }
  return planNo;
};
const handlePlanScanInput = (value?: string) => {
  const rawValue = String(value || '');
  if (hasPlanScanDelimiter(rawValue)) {
    const planNo = normalizePlanScanNo(rawValue);
    pendingScannerPlanNo = planNo;
    scanPlanNo.value = planNo;
    return;
  }
  scanPlanNo.value = rawValue;
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
const focusRecordScanConfirmInput = () => focusInputRef(recordScanConfirmInputRef);
const isEventTargetInInputRef = (target: EventTarget | null, inputRef: { value?: any }) => {
  const element = inputRef.value?.input || inputRef.value?.$el || inputRef.value;
  return !!(element && target instanceof Node && element.contains?.(target));
};
const isEventFromScannerInput = (event: KeyboardEvent) =>
  isEventTargetInInputRef(event.target, planScanInputRef) ||
  isEventTargetInInputRef(event.target, recordScanConfirmInputRef);
const clearGlobalScannerBuffer = () => {
  globalScannerBuffer = '';
  globalScannerLastAt = 0;
  if (globalScannerTimer) {
    clearTimeout(globalScannerTimer);
    globalScannerTimer = null;
  }
};
const isGlobalScannerCandidate = (value: string) => {
  const text = value.trim();
  if (!text) return false;
  if (recordScanConfirmVisible.value) return text.length >= 3;
  return hasPlanScanDelimiter(text) || text.length >= PLAN_SCAN_MIN_LENGTH;
};
const routeGlobalScannerInput = (value: string) => {
  if (recordScanConfirmVisible.value) {
    recordScanConfirmForm.scanCode = value.trim();
    focusRecordScanConfirmInput();
    nextTick(() => void confirmWorkRecordScan());
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
  if (event.ctrlKey || event.altKey || event.metaKey || event.isComposing || isEventFromScannerInput(event)) {
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

const materialScanCode = ref('');
const operatorId = ref<number>();
const operatorName = ref('');
const operatorNo = ref('');
const currentUserId = computed(() => userStore.userInfo?.id as number | undefined);
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '');
const currentUserNo = computed(() => userStore.userInfo?.username || (currentUserId.value ? String(currentUserId.value) : ''));
const equipmentInfo = reactive({
  code: '',
  id: undefined as number | undefined,
  lineName: '',
  name: '',
  workCenterId: undefined as number | undefined,
  workStatus: '',
});

const createDailyCheckFallbackRows = (): WorkPrepareRow[] => [
  {
    canConfirm: false,
    canFill: false,
    canView: false,
    confirmer: '-',
    confirmerTime: '-',
    details: [
      { item: '放卷气缸压力', remark: '', result: 'OK', seq: 1, standard: '0.25-0.65MPa', value: '' },
      { item: '前三轮压力', remark: '', result: 'OK', seq: 2, standard: '0.10-0.20MPa', value: '' },
      { item: '前三轮张力架', remark: '', result: 'OK', seq: 3, standard: '手动抬降无明显卡顿', value: '' },
      { item: '砂纸轮电机', remark: '', result: 'OK', seq: 4, standard: '电机运行正常,无异响', value: '' },
      { item: '毛刷轮电机', remark: '', result: 'OK', seq: 5, standard: '电机运行正常,无异响', value: '' },
      { item: '1#拍打轮压力', remark: '', result: 'OK', seq: 6, standard: '0.10-0.20MPa', value: '' },
      { item: '2#拍打轮压力', remark: '', result: 'OK', seq: 7, standard: '0.10-0.20MPa', value: '' },
      { item: '3#拍打轮压力', remark: '', result: 'OK', seq: 8, standard: '0.10-0.20MPa', value: '' },
      { item: '3#拍打轮张力架', remark: '', result: 'OK', seq: 9, standard: '手动抬降无明显卡顿', value: '' },
      { item: '收卷张力架压力', remark: '', result: 'OK', seq: 10, standard: '0.10-0.20MPa', value: '' },
      { item: '收卷气缸压力', remark: '', result: 'OK', seq: 11, standard: '0.25-0.65MPa', value: '' },
      { item: '收卷气杆', remark: '', result: 'OK', seq: 12, standard: '气杆抬降无明显卡顿', value: '' },
      { item: '校准块校准厚度计', remark: '', result: 'OK', seq: 13, standard: '1.000±0.003mm', value: '' },
      { item: '温湿度计', remark: '', result: 'OK', seq: 14, standard: '数显正常、电量≥20%', value: '' },
      { item: '计量器具', remark: '', result: 'OK', seq: 15, standard: '校准标签完好,且在有限期内', value: '' },
      { item: '塞尺', remark: '', result: 'OK', seq: 16, standard: '无形变、无锈蚀', value: '' },
      { item: '电葫芦', remark: '', result: 'OK', seq: 17, standard: '电机运行正常,无异响', value: '' },
      { item: '砂纸状况', remark: '', result: 'OK', seq: 18, standard: '寿命≤500m且≤15天；缝隙均匀(4-10mm)，无破损、无翘边', value: '' },
      { item: '导布状况', remark: '', result: 'OK', seq: 19, standard: '寿命≤200次；表面无明显片状脏污、裂口≤2cm', value: '' },
    ],
    formCode: 'ROUGH_STARTUP_CHECK',
    key: 'STARTUP',
    name: 'CMP软垫磨皮开机点检表',
    recorder: '-',
    recorderTime: '-',
    result: '未填写',
    status: 'PENDING',
    timing: '开机前',
  },
  {
    canConfirm: false,
    canFill: false,
    canView: false,
    confirmer: '-',
    confirmerTime: '-',
    details: [
      { category: '环境区', item: '温湿度计', remark: '', result: 'OK', seq: 1, standard: '表面清洁无脏污和异物', value: '' },
      { category: '放卷区', item: '放卷气杠', remark: '', result: 'OK', seq: 2, standard: '表面清洁无脏污和异物', value: '' },
      { category: '放卷区', item: '磁粉刹车', remark: '', result: 'OK', seq: 3, standard: '表面清洁无脏污和异物', value: '' },
      { category: '放卷区', item: '放卷辊轮', remark: '', result: 'OK', seq: 4, standard: '表面清洁无脏污和异物', value: '' },
      { category: '前三轮区', item: '前三轮', remark: '', result: 'OK', seq: 5, standard: '表面清洁无脏污和异物', value: '' },
      { category: '前三轮区', item: '前三轮张力架', remark: '', result: 'OK', seq: 6, standard: '表面清洁无脏污和异物', value: '' },
      { category: '主机区', item: '磨皮机操作面板', remark: '', result: 'OK', seq: 7, standard: '表面清洁无脏污和异物', value: '' },
      { category: '主机区', item: '磨皮水箱挡板', remark: '', result: 'OK', seq: 8, standard: '表面清洁无脏污和异物', value: '' },
      { category: '主机区', item: '磨皮机砂纸轮两侧', remark: '', result: 'OK', seq: 9, standard: '表面清洁无脏污和异物', value: '' },
      { category: '主机区', item: '磨辊集尘仓', remark: '', result: 'OK', seq: 10, standard: '无团簇状残留磨屑', value: '' },
      { category: '主机区', item: '磨皮机踏板', remark: '', result: 'OK', seq: 11, standard: '表面清洁无脏污和异物', value: '' },
      { category: '主机区', item: '毛刷轮电机', remark: '', result: 'OK', seq: 12, standard: '表面清洁无脏污和异物', value: '' },
      { category: '主机区', item: '毛刷轮', remark: '', result: 'OK', seq: 13, standard: '无团簇状残留导布丝', value: '' },
      { category: '收卷区', item: '后三轮', remark: '', result: 'OK', seq: 14, standard: '表面清洁无脏污和异物', value: '' },
      { category: '收卷区', item: '后三轮张力架', remark: '', result: 'OK', seq: 15, standard: '表面清洁无脏污和异物', value: '' },
      { category: '收卷区', item: '收卷气杠', remark: '', result: 'OK', seq: 16, standard: '表面清洁无脏污和异物', value: '' },
      { category: '收卷区', item: '收卷辊轮', remark: '', result: 'OK', seq: 17, standard: '表面清洁无脏污和异物', value: '' },
      { category: '收卷区', item: '产品接触辊轮', remark: '', result: 'OK', seq: 18, standard: '表面清洁无脏污和异物', value: '' },
      { category: '辅助区', item: '非直接接触部件', remark: '', result: 'OK', seq: 19, standard: '表面清洁无脏污和异物', value: '' },
      { category: '辅助区', item: '磨皮集尘箱', remark: '', result: 'OK', seq: 20, standard: '每周清洁一次', value: '' },
    ],
    formCode: 'ROUGH_CLEANING_CHECK',
    key: 'CLEANING',
    name: 'CMP软垫磨皮设备清洁点检表',
    recorder: '-',
    recorderTime: '-',
    result: '未填写',
    status: 'PENDING',
    timing: '清洁后/开机前',
  },
];

const dailyCheckRows = reactive<WorkPrepareRow[]>(createDailyCheckFallbackRows());

const dailyPreparationCompleted = computed(() => dailyCheckRows.every((item) => item.status !== 'PENDING'));

const sandpaper = reactive<ConsumableState>({
  batchNo: '-',
  lastReplaceTime: '-',
  lifeLength: 0,
  name: '砂纸',
  thresholdDays: DEFAULT_SANDPAPER_LIMIT_DAYS,
  thresholdLength: DEFAULT_SANDPAPER_LIMIT_LENGTH,
  useCount: 0,
});

const guideCloth = reactive<ConsumableState>({
  batchNo: '-',
  lastReplaceTime: '-',
  lifeLength: 0,
  name: '导布',
  thresholdLength: 0,
  thresholdUseCount: DEFAULT_GUIDE_CLOTH_LIMIT_COUNT,
  useCount: 0,
});

const dailyFirstSummary = reactive<DailySummary>({
  lossLength: 0,
  napSampleLength: 0,
  researchConsumptionLength: 0,
  outputLength: 0,
  processLength: 0,
});

const dailySecondSummary = reactive<DailySummary>({
  lossLength: 0,
  napSampleLength: 0,
  researchConsumptionLength: 0,
  outputLength: 0,
  processLength: 0,
});

const workRecords = ref<WorkRecord[]>([]);
const segmentTimingRows = ref<GrindingSegmentTiming[]>([]);
const firstAllocationRows = ref<MesHcRoughGrindingConsoleApi.FirstAllocation[]>([]);
const segmentTimingStampingKey = ref('');
const segmentTimingAuthVisible = ref(false);
const segmentTimingAuthActionName = ref('磨皮分段时间确认');
const pendingSegmentTimingAction = ref<PendingSegmentTimingAction>();
const focusedWorkRecordKey = ref<number | string>();
const selectedWorkRecordKeys = ref<(number | string)[]>([]);
const firstInspectionTasks = ref<FirstInspectionTask[]>([]);
const abnormalPositionRows = ref<MesHcRoughGrindingConsoleApi.AbnormalPositionItem[]>([]);
const abnormalPositionLoading = ref(false);
const middleProductRows = ref<MiddleProductRow[]>([]);
const middleProductDetailVisible = ref(false);
const middleProductDetailMode = ref<'edit' | 'view'>('edit');
const middleProductDetailPage = ref(1);
const middleProductActivePass = ref<GrindPass>('FIRST');
const middleProductDetailLoading = ref(false);
const middleProductDetailHeader = reactive<MiddleProductDetailHeader>({});
const middleProductOpenedFromReport = ref(false);
const middleProductReturnReportTab = ref('middle');
const middleProductDraftSaved = ref(false);
const middleProductDraftKey = ref('');
const middleProductMainWidth = ref('');
const middleProductConfirmer = ref('');
const middleProductEditingRecordId = ref<number>();
const middleProductOpenedFromSegment = ref(false);
const middleProductSegmentKey = ref('');
const middleProductImportInputRef = ref<HTMLInputElement>();
const middleProductReportMeters = ref<number | string>('');
const middleProductAuthVisible = ref(false);

const dailyCheckVisible = ref(false);
const dailyCheckMode = ref<DailyCheckMode>('edit');
const dailyCheckType = ref<CheckType>('STARTUP');
const dailyCheckListVisible = ref(false);
const dailyCheckAuthVisible = ref(false);
const dailyCheckAuthAction = ref('工作准备记录认证');
const pendingDailyCheckAction = ref<{ mode: DailyCheckMode; type: CheckType; previousEquipmentId?: number }>();
const equipmentSelectVisible = ref(false);
const equipmentSelectLoading = ref(false);
const equipmentSelectRows = ref<any[]>([]);
const equipmentSelectKeyword = ref('');
const operationAuthVisible = ref(false);
const operationAuthActionName = ref('工单开工确认');
const completionPromptKey = ref('');
const workRecordListVisible = ref(false);
const taskListVisible = ref(false);
const taskListLoading = ref(false);
const taskListRows = ref<any[]>([]);
const taskListPage = ref(1);
const taskListPageSize = ref(TASK_LIST_DEFAULT_PAGE_SIZE);
const taskListFilters = reactive({
  batchNo: '',
  modelCode: '',
  planNo: '',
  status: 'UNFINISHED',
});
const recordScanConfirmVisible = ref(false);
const recordScanConfirmForm = reactive({
  error: '',
  message: '',
  row: undefined as WorkRecord | undefined,
  scanCode: '',
});
const recordScanConfirmSubmitting = ref(false);
const consumableReplaceVisible = ref(false);
const roughConsoleConsumableSwitchModalRef = ref<InstanceType<typeof ConsumableLedgerSwitchModal>>();
const roughConsoleConsumableApply = ref<RoughConsoleConsumableApply>();
const boardConsumableUsageStatus = reactive<{
  batchNo?: string;
  ledgerId?: number;
  nextUsageStatus?: ConsumableUsageStatus;
  target?: RoughConsoleConsumableType;
}>({});
const reportConsumableUsageStatuses = reactive<
  Partial<Record<RoughConsoleConsumableType, PendingConsumableUsageStatus>>
>({});
const reportVisible = ref(false);
const reportReadonly = ref(false);
const reportCurrentRecord = ref<WorkRecord>();
const reportDeleting = ref(false);
const reportSubmitting = ref(false);
const reportTimeEditing = ref(false);
const reportTimeSaving = ref(false);
const firstAllocationQuantityRevisionVisible = ref(false);
const firstAllocationQuantityRevisionSubmitting = ref(false);
const firstAllocationQuantityRevisionForm = reactive({
  confirmedLength: null as number | null,
  lossLength: null as number | null,
  outputLength: null as number | null,
  processLength: null as number | null,
  reason: '',
});
const reportStep = ref<'PARAMS' | 'REPORT'>('PARAMS');
const activeReportTab = ref('basic');
const activeBoardTab = ref<BoardTabKey>('FIRST');
const activePass = ref<GrindPass>('FIRST');
const visualMaximized = ref(false);
const showExtendedBoardTabs = ref(false);
const reportAbnormalRows = ref<RoughReportAbnormalPositionRow[]>([]);
const roughDevProcessParamForms = ref<Partial<Record<GrindPass, MesHcStationFormApi.StationForm>>>({});
const roughDevProcessParamRecords = ref<Record<GrindPass, MesHcProcessFormApi.Record[]>>({
  FIRST: [],
  SECOND: [],
});
const roughDevProcessParamLoading = ref<Record<GrindPass, boolean>>({
  FIRST: false,
  SECOND: false,
});
const roughDevProcessParamLoadVersion = ref<Record<GrindPass, number>>({
  FIRST: 0,
  SECOND: 0,
});
const roughDevProcessParamFillOpen = ref(false);
const roughDevProcessParamFillForm = ref<MesHcStationFormApi.StationForm | null>(null);
const roughDevProcessParamFillPass = ref<GrindPass>('FIRST');
const roughDevProcessParamInitialParams = ref<Record<string, string>>({});
const roughDevProcessParamViewVisible = ref(false);
const roughDevProcessParamViewLoading = ref(false);
const roughDevProcessParamViewRecord = ref<MesHcProcessFormApi.Record | null>(null);
const roughDevProcessParamViewMode = ref<'edit' | 'view'>('view');
const roughDevProcessParamRuntimeRef = ref<any>();
const roughDevProcessParamViewHeaderData = ref<Record<string, any>>({});
const roughDevProcessParamSaving = ref(false);
const roughDevProcessParamConfirming = ref(false);
const roughDevProcessParamAuthVisible = ref(false);
const roughDevProcessParamAuthRecord = ref<MesHcProcessFormApi.Record | null>(null);

const reportForm = reactive({
  batchNo: '',
  defectCode: '',
  endTime: '',
  firstAllocationId: undefined as number | undefined,
  guideClothChanged: 'NO',
  guideClothNewBatchNo: '',
  guideClothReplaceReason: '',
  lossLength: 0,
  napSampleLength: 0,
  researchConsumptionLength: 0,
  outputLength: 0,
  planNo: '',
  processLength: null as number | null,
  sandpaperChanged: 'NO',
  sandpaperNewBatchNo: '',
  sandpaperReplaceReason: '',
  segmentMark: '',
  selfCheck: 'OK',
  startPosition: 0,
  startTime: '',
});

const reportFormLocked = computed(() => reportReadonly.value && !reportTimeEditing.value);

const middleProductDisplayName = computed(() =>
  getMiddleProductDisplayName(middleProductDetailHeader),
);

const middleProductAttachments = computed(() =>
  normalizeMiddleProductAttachments(
    middleProductDetailHeader.attachments ||
      (middleProductDetailHeader as Record<string, any>).importAttachment,
  ),
);

const middleProductConfirmed = computed(
  () => String(middleProductDetailHeader.docStatus || '').toUpperCase() === 'CONFIRMED',
);

const middleProductHeadFields = computed(() => [
  {
    key: 'modelCode',
    label: '型号',
    value: middleProductDetailHeader.motherModelCode || currentPlan.modelCode || '-',
  },
  {
    key: 'materialCode',
    label: '料号',
    value: middleProductDetailHeader.materialCode || currentPlan.materialCode || '-',
  },
  {
    key: 'productionBatchNo',
    label: middleProductActivePass.value === 'SECOND' ? '分段批号' : '母批批号',
    value: middleProductDetailHeader.productionBatchNo || reportForm.batchNo || '-',
  },
  {
    editable: true,
    key: 'processLength',
    label: '报工米数/m',
    type: 'number',
    value: middleProductReportMeters.value || middleProductDetailHeader.processLength || '-',
  },
  {
    editable: true,
    key: 'widthMm',
    label: '宽幅mm',
    type: 'input',
    value: middleProductMainWidth.value || middleProductDetailHeader.widthMm || middleProductDetailHeader.width || '-',
  },
  {
    key: 'recordDate',
    label: '生产日期',
    value: middleProductDetailHeader.recordDate || todayText(),
  },
]);

const middleProductSignatureFields = computed(() => [
  {
    key: 'recorder',
    label: '记录人',
    value: middleProductDetailHeader.recorder || operatorName.value || currentUserName.value || '-',
  },
  {
    key: 'recorderTime',
    label: '记录时间',
    value: middleProductDetailHeader.recorderTime || '-',
  },
  {
    editable: true,
    key: 'confirmer',
    label: '确认人',
    type: 'input',
    value: middleProductConfirmer.value || middleProductDetailHeader.confirmer || '-',
  },
  {
    key: 'confirmerTime',
    label: '确认时间',
    value: middleProductDetailHeader.confirmerTime || '-',
  },
]);
const middleProductMetaItems = computed(() => [
  `计划号：${middleProductDetailHeader.planNo || reportForm.planNo || currentPlan.planNo || '-'}`,
  `机台编号：${equipmentInfo.code || '-'}`,
  `磨皮阶段：${middleProductDetailHeader.passName || getGrindingPassName(middleProductActivePass.value)}`,
  `报工米数：${middleProductReportMeters.value || middleProductDetailHeader.processLength || '-'}`,
]);

const reportConsumption = ref<GrindingConsumption>(newGrindingConsumption());
const replaceConsumption = ref<GrindingConsumption>(newGrindingConsumption());
const reportConsumptionBalances = reactive<{ sandpaper?: number; guideCloth?: number }>({});
const replaceConsumptionBalance = ref<number>();
const replaceSubmitting = ref(false);

const replaceForm = reactive({
  batchNo: '',
  initialLength: 0,
  initialUseCount: 0,
  reason: '',
  replaceTime: '',
  replacer: '',
  replacerId: undefined as number | undefined,
  replacerNo: '',
  target: 'SANDPAPER' as 'GUIDE_CLOTH' | 'SANDPAPER',
});

function getConsumableTypeName(target: RoughConsoleConsumableType) {
  return target === 'SANDPAPER' ? '砂纸' : '导布';
}

function openRoughConsoleConsumableLedger(
  consumableType: RoughConsoleConsumableType,
  apply: RoughConsoleConsumableApply,
) {
  roughConsoleConsumableApply.value = apply;
  roughConsoleConsumableSwitchModalRef.value?.open({
    consumableType,
    processCode: 'ROUGH_GRINDING',
    title: consumableType === 'SANDPAPER' ? '磨皮砂纸边库台账' : '磨皮导布边库台账',
  });
}

function handleRoughConsoleConsumableSelected(row: RoughConsoleConsumableLedgerSelection) {
  roughConsoleConsumableApply.value?.(row);
}

function clearBoardConsumableSelection() {
  boardConsumableUsageStatus.batchNo = undefined;
  boardConsumableUsageStatus.ledgerId = undefined;
  boardConsumableUsageStatus.nextUsageStatus = undefined;
  boardConsumableUsageStatus.target = undefined;
}

function rememberBoardConsumableSelection(
  target: RoughConsoleConsumableType,
  row: RoughConsoleConsumableLedgerSelection,
) {
  if (!row.id) {
    clearBoardConsumableSelection();
    return;
  }
  boardConsumableUsageStatus.batchNo = row.batchNo;
  boardConsumableUsageStatus.ledgerId = row.id;
  boardConsumableUsageStatus.nextUsageStatus = row.nextUsageStatus;
  boardConsumableUsageStatus.target = target;
}

function clearReportConsumableSelection(target: RoughConsoleConsumableType) {
  const prefix = target === 'SANDPAPER' ? 'sandpaper' : 'guideCloth';
  reportConsumption.value[`${prefix}LedgerId`] = undefined;
  reportConsumption.value[`${prefix}Qty`] = undefined;
  reportConsumption.value[`${prefix}Unit`] = undefined;
  reportConsumptionBalances[prefix] = undefined;
  delete reportConsumableUsageStatuses[target];
}

function rememberReportConsumableSelection(
  target: RoughConsoleConsumableType,
  row: RoughConsoleConsumableLedgerSelection,
) {
  if (!row.id) {
    clearReportConsumableSelection(target);
    return;
  }
  reportConsumableUsageStatuses[target] = {
    batchNo: row.batchNo,
    ledgerId: row.id,
    nextUsageStatus: row.nextUsageStatus,
  };
}

async function updateBoardConsumableUsageStatusAfterReplace() {
  if (
    !boardConsumableUsageStatus.ledgerId ||
    boardConsumableUsageStatus.nextUsageStatus !== 'USED_UP'
  ) {
    clearBoardConsumableSelection();
    return;
  }
  await updateToolingConsumableLedgerUsageStatus(boardConsumableUsageStatus.ledgerId, 'USED_UP');
  clearBoardConsumableSelection();
}

async function updateReportConsumableUsageStatusesAfterSubmit() {
  const pendingEntries = Object.entries(reportConsumableUsageStatuses)
    .filter(([, item]) => item?.nextUsageStatus === 'USED_UP') as Array<
      [RoughConsoleConsumableType, PendingConsumableUsageStatus]
    >;
  if (pendingEntries.length === 0) return;
  for (const [target, item] of pendingEntries) {
    await updateToolingConsumableLedgerUsageStatus(item.ledgerId, 'USED_UP');
    clearReportConsumableSelection(target);
  }
}

function openReportConsumableLedger(target: RoughConsoleConsumableType) {
  openRoughConsoleConsumableLedger(target, (row) => {
    rememberReportConsumableSelection(target, row);
    if (target === 'SANDPAPER') {
      reportForm.sandpaperChanged = 'YES';
      reportForm.sandpaperNewBatchNo = row.batchNo || '';
      reportConsumption.value.sandpaperLedgerId = row.id;
      const defaults = getGrindingConsumptionDefault(target, row);
      reportConsumption.value.sandpaperQty = defaults.qty;
      if (defaults.warning) message.warning(defaults.warning);
      reportConsumption.value.sandpaperUnit = row.uomName || row.uomCode;
      reportConsumptionBalances.sandpaper = row.balanceQty;
      return;
    }
    reportForm.guideClothChanged = 'YES';
    reportForm.guideClothNewBatchNo = row.batchNo || '';
    reportConsumption.value.guideClothLedgerId = row.id;
    reportConsumption.value.guideClothQty = undefined;
    reportConsumption.value.guideClothUnit = row.uomName || row.uomCode;
    reportConsumptionBalances.guideCloth = row.balanceQty;
  });
}

const buildDefaultProcessItems = (pass: GrindPass = 'FIRST'): ProcessItem[] => {
  const rows: ProcessItem[] = [
    { actualValue: '', category: '生产环境', itemName: '环境温度', result: 'OK', standard: '20-28℃' },
    { actualValue: '', category: '工艺参数点检表', itemName: '气压', result: 'OK', standard: '0.45-0.60MPa' },
    { actualValue: '', category: '工艺参数点检表', itemName: '线速', result: 'OK', standard: '8-12m/min' },
    { actualValue: '', category: '工艺参数点检表', itemName: '转速', result: 'OK', standard: '120-180rpm' },
    { actualValue: '', category: '工艺参数点检表', itemName: '计米器', result: 'OK', standard: '与本次报工收卷米数一致' },
    { actualValue: '', category: '质量结果', itemName: '磨皮厚度', result: 'OK', standard: '按工艺卡' },
    { actualValue: '', category: '质量结果', itemName: '磨皮后厚度', result: 'OK', standard: '按工艺卡' },
  ];
  if (pass === 'SECOND') {
    rows.push(
      { actualValue: '', category: '磨皮后NAP层', itemName: '厚度', result: 'OK', standard: '按工艺卡' },
      { actualValue: '', category: '磨皮后NAP层', itemName: '磨后宽幅', result: 'OK', standard: '按工艺卡' },
      { actualValue: '', category: '磨皮后NAP层', itemName: '磨皮米数', result: 'OK', standard: '与本次报工收卷米数一致' },
    );
  }
  return rows;
};

const processItems = ref<ProcessItem[]>(buildDefaultProcessItems());

const secondSegmentOptions = [
  { label: 'P段', value: 'P' },
  { label: 'Q段', value: 'Q' },
  { label: 'R段', value: 'R' },
  { label: 'S段', value: 'S' },
  { label: '不分段', value: '' },
];
const timingSegmentOptions: Array<{ label: string; value: TimingSegmentMark }> = [
  { label: 'P段', value: 'P' },
  { label: 'Q段', value: 'Q' },
  { label: 'R段', value: 'R' },
  { label: 'S段', value: 'S' },
  { label: '不分段', value: 'NONE' },
];
const secondSegmentActionOptions = computed(() =>
  secondSegmentOptions.map((option) => {
    const pendingAllocation = getFirstAllocationBySegment(option.value, true);
    return {
      ...option,
      hasPendingAllocation: isFirstAllocatedMode.value && !!pendingAllocation &&
        !secondWorkRecords.value.some(
          (record) => Number(record.firstAllocationId || 0) === Number(pendingAllocation.id || 0),
        ),
      latestRecord: getSecondSegmentLatestRecord(option.value),
    };
  }),
);

const normalizeSegmentMark = (segmentMark?: string) => {
  const normalized = String(segmentMark || '').trim().toUpperCase();
  return !normalized || normalized === '-' || normalized === 'NONE' ? '' : normalized;
};
const toTimingSegmentMark = (segmentMark?: string): TimingSegmentMark =>
  (normalizeSegmentMark(segmentMark) || 'NONE') as TimingSegmentMark;
const normalizeFirstAllocationSegmentMark = (segmentMark?: string) => {
  const normalized = String(segmentMark || '').trim().toUpperCase();
  return normalized === 'NONE' ? '' : normalizeSegmentMark(normalized);
};
const toFirstAllocationSegmentMark = (segmentMark?: string) =>
  (normalizeFirstAllocationSegmentMark(segmentMark) || 'NONE') as 'NONE' | 'P' | 'Q' | 'R' | 'S';
// 一磨统一按 P/Q/R/S/不分段处理，不再提供原有母批报工模式。
const isFirstAllocatedMode = computed(() => true);
const isFirstAllocationReport = computed(
  () => activePass.value === 'FIRST' && isFirstAllocatedMode.value,
);
const getFirstAllocationsBySegment = (segmentMark?: string) => {
  const expected = toFirstAllocationSegmentMark(segmentMark);
  return firstAllocationRows.value.filter(
    (item) => toFirstAllocationSegmentMark(item.segmentMark) === expected,
  );
};
const getFirstAllocationBySegment = (segmentMark?: string, preferPendingSecond = false) => {
  const allocations = getFirstAllocationsBySegment(segmentMark);
  if (!allocations.length) return undefined;
  if (preferPendingSecond && toFirstAllocationSegmentMark(segmentMark) === 'NONE') {
    return allocations.find(
      (allocation) =>
        !secondWorkRecords.value.some(
          (record) => Number(record.firstAllocationId || 0) === Number(allocation.id || 0),
        ),
    ) || allocations[0];
  }
  return allocations[0];
};
const getFirstAllocationById = (id?: number) =>
  firstAllocationRows.value.find((item) => Number(item.id || 0) === Number(id || 0));
const isSecondAllocationUnavailable = (segmentMark?: string) =>
  isFirstAllocatedMode.value && !getFirstAllocationBySegment(segmentMark, true);
const firstAllocationActionOptions = computed(() =>
  secondSegmentOptions.map((option) => ({
    ...option,
    allocation: getFirstAllocationBySegment(option.value),
    allocationCount: getFirstAllocationsBySegment(option.value).length,
  })),
);
const getFirstAllocationSegmentTimingText = (
  segmentMark: string,
  field: 'endTime' | 'startTime',
) =>
  getSegmentTiming('FIRST', segmentMark)?.[field] ||
  getFirstAllocationBySegment(segmentMark)?.[field] ||
  '未记录';
const firstAllocatedLength = computed(() =>
  firstAllocationRows.value.reduce(
    (total, item) => total + Number(item.confirmedLength || 0),
    0,
  ),
);
const firstAllocationRemainingLength = computed(() =>
  Math.max(Number(firstReportAvailableLength.value || 0), 0),
);
const getFirstAllocationSuggestedStartPosition = () => {
  const occupiedRanges = firstAllocationRows.value
    .map((item) => ({
      end: Number(item.startPosition || 0) + Number(item.confirmedLength || 0),
      start: Number(item.startPosition || 0),
    }))
    .sort((a, b) => a.start - b.start);
  let suggested = 0;
  for (const range of occupiedRanges) {
    if (range.start - suggested > SECOND_RANGE_EPS) break;
    suggested = Math.max(suggested, range.end);
  }
  return Number(suggested.toFixed(3));
};
const getFirstGrindingOutputTotalLength = () =>
  isFirstAllocatedMode.value
    ? firstAllocatedLength.value
    : Number(dailyFirstSummary.outputLength || dailyFirstSummary.processLength || 0);

const buildSecondGrindingBatchNo = (sourceBatchNo: string, segmentMark?: string) => {
  const source = String(sourceBatchNo || '').trim();
  const segment = normalizeSegmentMark(segmentMark).trim();
  return segment ? `${source}${segment}` : source;
};

const getSecondRecordEndPosition = (record: Pick<WorkRecord, 'processLength' | 'startPosition'>) =>
  Number(record.startPosition || 0) + Number(record.processLength || 0);

const getSecondStartPosition = () =>
  Number(
    workRecords.value
      .filter((record) => record.passType === 'SECOND')
      .reduce((max, record) => Math.max(max, getSecondRecordEndPosition(record)), 0)
      .toFixed(3),
  );

const secondSegmentSummaries = computed<SegmentSummary[]>(() =>
  secondSegmentOptions.map((option) => {
    const records = workRecords.value.filter(
      (record) =>
        record.passType === 'SECOND' &&
        normalizeSegmentMark(record.segmentMark) === option.value,
    );
    const latestRecord = [...records].reverse().find(Boolean);
    return records.reduce<SegmentSummary>(
      (summary, record) => {
        summary.processLength += Number(record.processLength || 0);
        summary.lossLength += Number(record.lossLength || 0);
        summary.outputLength += Number(record.outputLength || 0);
        summary.napSampleLength += Number(record.napSampleLength || 0);
        summary.researchConsumptionLength += Number(record.researchConsumptionLength || 0);
        return summary;
      },
      {
        label: option.label,
        lossLength: 0,
        napSampleLength: 0,
        researchConsumptionLength: 0,
        outputLength: 0,
        processLength: 0,
        latestRecord,
        recordCount: records.length,
        value: option.value,
      },
    );
  }),
);

const secondClothSegments = computed(() =>
  secondSegmentSummaries.value,
);
const firstProgressPercent = computed(() => {
  const total = Number(currentPlan.previousOutputLength || currentPlan.availableLength || 0);
  if (!total) return 0;
  return Math.min(100, Math.round((dailyFirstSummary.processLength / total) * 100));
});
const firstProgressLabelPercent = computed(() => Math.max(8, firstProgressPercent.value));
const firstRemainingLength = computed(() =>
  Math.max(Number(currentPlan.previousOutputLength || 0) - dailyFirstSummary.processLength, 0),
);
const currentPlanSourceBalance = computed(() => {
  const planId = Number(currentPlan.planId || 0);
  const planNo = String(currentPlan.planNo || '').trim();
  const batchNo = String(currentPlan.batchNo || '').trim();
  const sources = sourceBalances.value.filter((item) => item.sourceType !== 'STOCK');
  return (
    sources.find((item) => planId && Number(item.sourcePlanId || 0) === planId) ||
    sources.find((item) => planNo && item.sourcePlanNo === planNo) ||
    sources.find((item) => batchNo && (item.sourceProductionBatchNo === batchNo || item.sourceBatchNo === batchNo))
  );
});
const firstReportAvailableLength = computed(() => {
  const balance = currentPlanSourceBalance.value;
  if (balance) return toNumber(balance.availableLength);
  return firstRemainingLength.value;
});
const firstAllocationSourceTotalLength = computed(() => {
  const balanceTotalLength = toNumber(currentPlanSourceBalance.value?.totalLength);
  return Math.max(balanceTotalLength, Number(currentPlan.previousOutputLength || 0), 0);
});
const firstReportStartPosition = computed(() => {
  const balance = currentPlanSourceBalance.value;
  if (balance) {
    const usedFirstLength = toNumber(balance.usedFirstLength);
    if (usedFirstLength > 0) return usedFirstLength;
    const totalLength = toNumber(balance.totalLength);
    const availableLength = toNumber(balance.availableLength);
    if (totalLength > 0 && availableLength >= 0) {
      return Math.max(0, Number((totalLength - availableLength).toFixed(3)));
    }
  }
  return Number((dailyFirstSummary.processLength || 0).toFixed(3));
});
const firstReportSourcePlanNo = computed(() => currentPlanSourceBalance.value?.sourcePlanNo || currentPlan.planNo || '');
const firstReportSourceBatchNo = computed(() =>
  currentPlanSourceBalance.value?.sourceProductionBatchNo ||
  currentPlanSourceBalance.value?.sourceBatchNo ||
  currentPlan.batchNo ||
  '',
);
const firstReportSourceTypeText = computed(() => {
  const sourceType = currentPlanSourceBalance.value?.sourceType;
  if (sourceType === 'WET_OUTPUT') return '前序湿法收卷';
  if (sourceType === 'FIRST_GRINDING') return '一次磨皮来源';
  if (sourceType === 'STOCK') return '边库余料';
  return sourceType || '计划来源';
});
function getConsumableUseDays(lastReplaceTime?: string, targetTime?: string) {
  const replaceText = String(lastReplaceTime || '').trim();
  if (!replaceText || replaceText === '-') return 0;
  const replaceDay = dayjs(replaceText);
  if (!replaceDay.isValid()) return 0;
  const targetDay = targetTime ? dayjs(targetTime) : dayjs();
  if (!targetDay.isValid()) return 0;
  // 耗材使用天数采用含首日口径：更换当天即为第 1 天，前后端与生产记录保持一致。
  return Math.max(1, targetDay.startOf('day').diff(replaceDay.startOf('day'), 'day') + 1);
}

const sandpaperUseDays = computed(() => getConsumableUseDays(sandpaper.lastReplaceTime));
function reachesConsumableWarningThreshold(value?: number, limit?: number) {
  const safeValue = Number(value || 0);
  const safeLimit = Number(limit || 0);
  return safeLimit > 0 && safeValue * 100 >= safeLimit * CONSUMABLE_WARNING_PERCENT;
}

const sandpaperNeedReplace = computed(
  () =>
    reachesConsumableWarningThreshold(sandpaper.lifeLength, sandpaper.thresholdLength) ||
    reachesConsumableWarningThreshold(
      sandpaperUseDays.value,
      sandpaper.thresholdDays || DEFAULT_SANDPAPER_LIMIT_DAYS,
    ),
);
const guideClothNeedReplace = computed(() =>
  reachesConsumableWarningThreshold(guideCloth.useCount, guideCloth.thresholdUseCount),
);
const normalizeOperationStatus = (status?: string) => {
  const raw = String(status || '').trim();
  const normalized = raw.toUpperCase();
  if (normalized === 'RUNNING' || normalized === 'IN_PROGRESS') return 'RUNNING';
  if (normalized === 'FINISHED' || normalized === 'COMPLETED') return 'FINISHED';
  if (normalized === 'RELEASED' || normalized === 'PENDING') return 'RELEASED';
  if (raw.includes('暂停')) return 'PAUSED';
  if (raw.includes('作废') || raw.includes('取消')) return 'CANCELLED';
  if (normalized === 'CANCELED' || normalized === 'VOID' || normalized === 'ABORTED') return 'CANCELLED';
  if (normalized === 'PAUSED' || normalized === 'CANCELLED') return normalized;
  return raw || '';
};
const currentOperationStatus = computed(() => normalizeOperationStatus(currentTask.value?.status || currentPlan.status));
const previousOperationStatus = computed(() =>
  normalizeOperationStatus(
    currentPlan.previousOperationStatus ||
      currentTask.value?.previousOperationStatus ||
      currentTask.value?.previousOpStatus ||
      currentTask.value?.previousStatus,
  ),
);
const roughPreviousWetEndTime = computed(() =>
  parseQtimeDateTime(
    currentPlan.previousEndTime ||
      currentTask.value?.previousEndTime ||
      currentTask.value?.wetEndTime ||
      currentTask.value?.previousFinishTime ||
      '',
    currentTask.value?.previousProductionDate || currentTask.value?.productionDate || todayText(),
  ),
);
const roughFirstStartTime = computed(() => {
  let firstSegmentStartTime: ReturnType<typeof dayjs> | null = null;
  for (const timing of segmentTimingRows.value) {
    if (timing.passType !== 'FIRST' || !timing.startTime) continue;
    const startTime = parseQtimeDateTime(timing.startTime, currentTask.value?.productionDate || todayText());
    if (startTime && (!firstSegmentStartTime || startTime.isBefore(firstSegmentStartTime))) {
      firstSegmentStartTime = startTime;
    }
  }
  if (firstSegmentStartTime) return firstSegmentStartTime;
  let firstReportStartTime: ReturnType<typeof dayjs> | null = null;
  for (const record of workRecords.value) {
    if (record.passType !== 'FIRST') continue;
    const startTime = parseQtimeDateTime(record.startTime, currentTask.value?.productionDate || todayText());
    if (startTime && (!firstReportStartTime || startTime.isBefore(firstReportStartTime))) {
      firstReportStartTime = startTime;
    }
  }
  if (firstReportStartTime) return firstReportStartTime;
  if (reportVisible.value && activePass.value === 'FIRST') {
    return parseQtimeDateTime(toDateTimeText(reportForm.startTime) || '', resolveReportTimeFallbackDate());
  }
  return null;
});
const roughQtimeInfo = computed(() => {
  const backendQtime = selectWorstRoughQtimeInfo(activePass.value === 'SECOND' ? 'SECOND' : 'FIRST');
  if (backendQtime) {
    return {
      durationText: roughBackendQtimeDurationText(backendQtime),
      levelClass: resolveRoughBackendQtimeLevelClass(backendQtime),
    };
  }
  const previousEndTime = roughPreviousWetEndTime.value;
  const targetTime = roughFirstStartTime.value || dayjs(qtimeNowTimestamp.value);
  const qtimeMinutes = previousEndTime ? targetTime.diff(previousEndTime, 'minute') : null;
  return {
    durationText: formatQtimeDuration(qtimeMinutes),
    levelClass: resolveRoughQtimeLevelClass(qtimeMinutes),
  };
});
const shouldBlockByPreviousOperation = computed(
  () => !!currentPlan.previousOperationName && !!previousOperationStatus.value && previousOperationStatus.value !== 'FINISHED',
);
const isWorkOrderRunning = computed(() => currentOperationStatus.value === 'RUNNING');
const isWorkOrderFinished = computed(() => currentOperationStatus.value === 'FINISHED');
const isWorkOrderPaused = computed(() => currentOperationStatus.value === 'PAUSED');
const isWorkOrderCancelled = computed(() => currentOperationStatus.value === 'CANCELLED');
const canStartWorkOrder = computed(
  () =>
    !!currentPlan.planOperationId &&
    !isWorkOrderRunning.value &&
    !isWorkOrderFinished.value &&
    !isWorkOrderPaused.value &&
    !isWorkOrderCancelled.value,
);
const canCompleteWorkOrder = computed(() => !!currentPlan.planOperationId && isWorkOrderRunning.value);
const workbenchBlockedReason = computed(() => {
  const batchNo =
    currentPlan.batchNo ||
    currentTask.value?.productionBatchNo ||
    currentTask.value?.batchNo ||
    currentTask.value?.sourceProductionBatchNo ||
    currentTask.value?.sourceBatchNo ||
    '-';
  if (isWorkOrderPaused.value) {
    return `当前磨皮工序已暂停，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 暂不能继续报工或提交操作，请等待生产计划复工指令。`;
  }
  if (isWorkOrderCancelled.value) {
    return `当前磨皮工序已作废取消，计划 ${currentPlan.planNo || '-'}，批号 ${batchNo} 不能继续报工或提交操作。`;
  }
  return '';
});
const workbenchBlockedTitle = computed(() =>
  isWorkOrderCancelled.value ? '当前磨皮工序已作废取消' : '当前磨皮工序已暂停',
);
const hasBoundEquipment = computed(() => !!equipmentInfo.id);
const selectedBoardEquipmentCode = computed(() => equipmentInfo.code || '');
const selectedBoardEquipmentName = computed(() => equipmentInfo.name || '');
const selectedBoardWorkCenterName = computed(() => equipmentInfo.lineName || '-');
const roughTaskListTitle = computed(
  () => `磨皮工作列表 - ${selectedBoardEquipmentCode.value || '未绑定'} / ${selectedBoardEquipmentName.value || '-'}`,
);
const currentFirstInspectionTask = computed(() => {
  const planNo = currentPlan.planNo;
  if (!planNo) return undefined;
  return firstInspectionTasks.value.find((task) => task.planNo === planNo && (!task.kind || task.kind === 'FIRST_SAMPLE'));
});
const currentDailyCheckRow = computed(() => dailyCheckRows.find((item) => item.key === dailyCheckType.value));
const firstWorkRecords = computed(() => workRecords.value.filter((record) => record.passType === 'FIRST'));
const secondWorkRecords = computed(() => workRecords.value.filter((record) => record.passType === 'SECOND'));
const secondInspectionRecords = computed(() =>
  firstInspectionTasks.value
    .filter((task) => task.kind === 'SECOND_SEGMENT_SAMPLE' && task.planNo === currentPlan.planNo)
    .sort((a, b) => dayjs(b.pushedAt || 0).valueOf() - dayjs(a.pushedAt || 0).valueOf()),
);
const secondInspectionSummary = computed(() => ({
  abnormal: secondInspectionRecords.value.filter(
    (task) => task.status === 'NG' || isSecondSegmentInspectionReapplyAllowed(task),
  ).length,
  ok: secondInspectionRecords.value.filter((task) => task.status === 'OK').length,
  pending: secondInspectionRecords.value.filter((task) => task.status === 'PENDING' || task.status === 'WAITING').length,
  total: secondInspectionRecords.value.length,
}));
const abnormalPositionSummary = computed(() => ({
  first: abnormalPositionRows.value.filter((row) => row.processStage === 'FIRST_GRINDING').length,
  second: abnormalPositionRows.value.filter((row) => row.processStage === 'SECOND_GRINDING').length,
  total: abnormalPositionRows.value.length,
  wet: abnormalPositionRows.value.filter((row) => !row.processStage || row.processStage === 'WET').length,
}));
const selectedSecondWorkRecords = computed(() =>
  secondWorkRecords.value.filter((record) => selectedWorkRecordKeys.value.includes(record.recordKey)),
);
const selectedPrintableWorkRecords = computed(() =>
  selectedSecondWorkRecords.value.filter((record) => !!(record.productionBatchNo || record.batchNo)),
);
const unprintedSecondWorkRecords = computed(() =>
  secondWorkRecords.value.filter(
    (record) => record.printStatus !== '已打印' && !!(record.productionBatchNo || record.batchNo),
  ),
);
const unconfirmedSecondWorkRecords = computed(() =>
  secondWorkRecords.value.filter((record) => record.confirmStatus !== '已确认' && record.confirmStatus !== 'CONFIRMED'),
);
const getWorkRecordRowClassName = (record: WorkRecord) =>
  record.recordKey === focusedWorkRecordKey.value ? 'rough-record-row--focused' : '';
const getSecondReportEndPosition = (record: Pick<WorkRecord, 'processLength' | 'startPosition'>) =>
  getSecondRecordEndPosition(record);
const isRangeOverlap = (aStart: number, aEnd: number, bStart: number, bEnd: number) =>
  aStart < bEnd - SECOND_RANGE_EPS && aEnd > bStart + SECOND_RANGE_EPS;
const secondReportedRanges = computed(() =>
  secondWorkRecords.value
    .map((record) => ({
      batchNo: record.productionBatchNo || record.batchNo || '-',
      end: getSecondReportEndPosition(record),
      key: record.recordKey,
      segment: record.segmentMark || '不分段',
      start: Number(record.startPosition || 0),
    }))
    .filter((item) => item.end - item.start > SECOND_RANGE_EPS),
);
const currentSecondReportRange = computed(() => {
  const start = Number(reportForm.startPosition || 0);
  const end = start + Number(reportForm.processLength || 0);
  return { end, start };
});
const currentSecondFirstAllocationRange = computed(() => {
  if (activePass.value !== 'SECOND' || !isFirstAllocatedMode.value) return undefined;
  const allocation = getFirstAllocationById(reportForm.firstAllocationId) ||
    getFirstAllocationBySegment(reportForm.segmentMark, true);
  if (!allocation) return undefined;
  const start = Number(allocation.startPosition || 0);
  const length = Number(allocation.confirmedLength || 0);
  return {
    end: start + length,
    length,
    start,
  };
});
const secondReportOverlapRanges = computed(() => {
  if (activePass.value !== 'SECOND' || Number(reportForm.processLength || 0) <= 0) {
    return [];
  }
  const current = currentSecondReportRange.value;
  return secondReportedRanges.value.filter((item) => isRangeOverlap(current.start, current.end, item.start, item.end));
});
const hasSecondReportOverlap = computed(() => secondReportOverlapRanges.value.length > 0);
const hasFirstGrindingReport = computed(() => getFirstGrindingOutputTotalLength() > SECOND_RANGE_EPS || firstWorkRecords.value.length > 0);
const secondReportRangeTotal = computed(() => {
  const reportedMax = secondReportedRanges.value.reduce((max, item) => Math.max(max, item.end), 0);
  const currentMax = activePass.value === 'SECOND' ? currentSecondReportRange.value.end : 0;
  return Math.max(getFirstGrindingOutputTotalLength(), reportedMax, currentMax, 1);
});
const secondReportRangeSegments = computed(() => {
  const total = secondReportRangeTotal.value || 1;
  const clampPercent = (value: number) => Math.max(0, Math.min(100, value));
  const segments = secondReportedRanges.value.map((item) => ({
    ...item,
    left: clampPercent((item.start / total) * 100),
    type: 'reported',
    width: clampPercent(((item.end - item.start) / total) * 100),
  }));
  if (activePass.value === 'SECOND' && Number(reportForm.processLength || 0) > 0) {
    const current = currentSecondReportRange.value;
    segments.push({
      batchNo: reportForm.batchNo || '当前报工',
      end: current.end,
      key: 'CURRENT',
      left: clampPercent((current.start / total) * 100),
      segment: reportForm.segmentMark || '不分段',
      start: current.start,
      type: hasSecondReportOverlap.value ? 'overlap' : 'current',
      width: Math.max(1, clampPercent(((current.end - current.start) / total) * 100)),
    });
  }
  return segments;
});
const secondReportRangeText = computed(() =>
  `${Number(currentSecondReportRange.value.start || 0).toFixed(3)}-${Number(currentSecondReportRange.value.end || 0).toFixed(3)} m`,
);
const currentSecondFirstAllocationRangeText = computed(() => {
  const range = currentSecondFirstAllocationRange.value;
  return range
    ? `${Number(range.start || 0).toFixed(3)}-${Number(range.end || 0).toFixed(3)} m`
    : '-';
});
const firstAllocationReportedRanges = computed(() =>
  firstAllocationRows.value
    .map((item) => {
      const start = Number(item.startPosition || 0);
      const end = start + Number(item.confirmedLength || 0);
      return {
        end,
        key: item.id || item.segmentMark,
        segment: item.segmentMark || '不分段',
        start,
      };
    })
    .filter((item) => item.end - item.start > SECOND_RANGE_EPS),
);
const currentFirstAllocationReportRange = computed(() => {
  const start = Number(reportForm.startPosition || 0);
  const end = start + Number(reportForm.processLength || 0);
  return { end, start };
});
const firstAllocationReportOverlapRanges = computed(() => {
  if (!isFirstAllocationReport.value || Number(reportForm.processLength || 0) <= 0) {
    return [];
  }
  const current = currentFirstAllocationReportRange.value;
  return firstAllocationReportedRanges.value.filter((range) =>
    isRangeOverlap(current.start, current.end, range.start, range.end),
  );
});
const hasFirstAllocationReportOverlap = computed(
  () => firstAllocationReportOverlapRanges.value.length > 0,
);
const firstAllocationReportRangeTotal = computed(() => {
  const reportedMax = firstAllocationReportedRanges.value.reduce((max, item) => Math.max(max, item.end), 0);
  const currentMax = isFirstAllocationReport.value ? currentFirstAllocationReportRange.value.end : 0;
  return Math.max(firstAllocationSourceTotalLength.value, reportedMax, currentMax, 1);
});
const firstAllocationReportRangeSegments = computed(() => {
  const total = firstAllocationReportRangeTotal.value || 1;
  const clampPercent = (value: number) => Math.max(0, Math.min(100, value));
  const segments = firstAllocationReportedRanges.value.map((item) => ({
    ...item,
    left: clampPercent((item.start / total) * 100),
    type: 'reported',
    width: clampPercent(((item.end - item.start) / total) * 100),
  }));
  if (isFirstAllocationReport.value && Number(reportForm.processLength || 0) > 0) {
    const current = currentFirstAllocationReportRange.value;
    segments.push({
      end: current.end,
      key: 'CURRENT',
      left: clampPercent((current.start / total) * 100),
      segment: reportForm.segmentMark || '不分段',
      start: current.start,
      type: 'current',
      width: Math.max(1, clampPercent(((current.end - current.start) / total) * 100)),
    });
  }
  return segments;
});
const firstAllocationReportRangeText = computed(() =>
  `${Number(currentFirstAllocationReportRange.value.start || 0).toFixed(3)}-${Number(currentFirstAllocationReportRange.value.end || 0).toFixed(3)} m`,
);
const workRecordRowSelection = computed(() => ({
  fixed: true,
  getCheckboxProps: (record: WorkRecord) => ({
    disabled: record.passType !== 'SECOND',
  }),
  onChange: (keys: (number | string)[]) => {
    selectedWorkRecordKeys.value = keys;
  },
  selectedRowKeys: selectedWorkRecordKeys.value,
}));
const reportProcessLengthValue = computed(() => Number(reportForm.processLength || 0));
const reportLossLengthValue = computed(() =>
  activePass.value === 'FIRST' ? 0 : Number(reportForm.lossLength || 0),
);
const reportNapSampleLengthValue = computed(() =>
  activePass.value === 'FIRST' ? 0 : Number(reportForm.napSampleLength || 0),
);
const reportResearchConsumptionLengthValue = computed(() =>
  activePass.value === 'SECOND' ? Number(reportForm.researchConsumptionLength || 0) : 0,
);
const reportAbnormalTotalLengthValue = computed(() => {
  let totalLength = 0;
  for (const row of reportAbnormalRows.value) {
    const abnormalLength = Number(row.abnormalLength || 0);
    if (Number.isFinite(abnormalLength) && abnormalLength > 0) {
      totalLength += abnormalLength;
    }
  }
  return totalLength;
});
const reportOutputLengthValue = computed(() =>
  Math.max(
    Number(
      (
        reportProcessLengthValue.value -
        reportLossLengthValue.value -
        reportNapSampleLengthValue.value -
        reportResearchConsumptionLengthValue.value -
        (activePass.value === 'SECOND'
          ? reportAbnormalTotalLengthValue.value
          : 0)
      ).toFixed(3),
    ),
    0,
  ),
);
const submittedMiddleProductRecords = computed(() =>
  workRecords.value
    .filter((record) => !!record.middleProductRecordId)
    .map((record) => ({
      ...record,
      generatedLength: record.middleProductGeneratedLength || 0,
      name: '磨皮中间品记录表',
      processLength: record.processLength || 0,
      recordId: record.middleProductRecordId,
      recorder: record.middleProductRecorder || '-',
      recorderTime: record.middleProductRecordTime || '-',
      status: record.middleProductStatus || 'RECORDED',
    })),
);
const reportConsumableLengthValue = computed(() =>
  activePass.value === 'SECOND'
    ? Math.max(reportProcessLengthValue.value - reportLossLengthValue.value, 0)
    : reportProcessLengthValue.value + reportLossLengthValue.value,
);
const reportSandpaperAfterLength = computed(() => sandpaper.lifeLength + reportConsumableLengthValue.value);
const reportSandpaperAfterUseDays = computed(() =>
  getConsumableUseDays(sandpaper.lastReplaceTime, String(reportForm.endTime || '')),
);
const reportSandpaperAfterNeedReplace = computed(
  () =>
    reachesConsumableWarningThreshold(reportSandpaperAfterLength.value, sandpaper.thresholdLength) ||
    reachesConsumableWarningThreshold(
      reportSandpaperAfterUseDays.value,
      sandpaper.thresholdDays || DEFAULT_SANDPAPER_LIMIT_DAYS,
    ),
);
const reportGuideClothAfterUseCount = computed(() => {
  const increment = reportProcessLengthValue.value > 0 ? GUIDE_CLOTH_USE_INCREMENT : 0;
  return reportForm.guideClothChanged === 'YES'
    ? increment
    : guideCloth.useCount + increment;
});
const reportGuideClothAfterNeedReplace = computed(() =>
  reachesConsumableWarningThreshold(reportGuideClothAfterUseCount.value, guideCloth.thresholdUseCount),
);
const reportAbnormalSummary = computed(() => {
  const rows = reportAbnormalRows.value.filter(
    (row) =>
      String(row.positionText || '').trim() ||
      row.abnormalLength !== undefined ||
      Boolean(row.remark),
  );
  if (!rows.length) return '未登记';
  const totalLength = reportAbnormalTotalLengthValue.value;
  return totalLength > 0 ? `${rows.length} 条 / ${formatNumber(totalLength)} m` : `${rows.length} 条`;
});

const checkColumns = [
  { dataIndex: 'name', title: '表单名称', width: 260 },
  { dataIndex: 'timing', title: '执行时机', width: 130 },
  { dataIndex: 'status', title: '单据状态', width: 120 },
  { dataIndex: 'result', title: '执行结果', width: 120 },
  { dataIndex: 'recorderInfo', title: '记录人/时间', width: 180 },
  { dataIndex: 'confirmerInfo', title: '确认人/确认时间', width: 180 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 180 },
];

const renderLossLengthTitle = () =>
  h('span', [
    '固定损耗-',
    h('span', { class: 'report-loss-sample-emphasis' }, '包含小样条'),
    '（米）',
  ]);

const recordColumns = [
  { dataIndex: 'passName', title: '类型', width: 110 },
  { dataIndex: 'planNo', title: '计划号', width: 140 },
  { dataIndex: 'batchNo', title: '母批/分段批号', width: 140 },
  { dataIndex: 'segmentMark', title: '分段', width: 80 },
  { dataIndex: 'startPosition', title: '起位置(m)', width: 110 },
  { dataIndex: 'processLength', title: '投入米数(m)', width: 120 },
  { dataIndex: 'lossLength', title: renderLossLengthTitle, width: 170 },
  { dataIndex: 'outputLength', title: '产出米(m)', width: 110 },
  { dataIndex: 'napSampleLength', title: 'NAP留样(m)', width: 120 },
  { dataIndex: 'researchConsumptionLength', title: '研发消耗(m)', width: 120 },
  { dataIndex: 'startTime', title: '开始时间', width: 170 },
  { dataIndex: 'endTime', title: '结束时间', width: 170 },
  { dataIndex: 'printCount', fixed: 'right' as const, title: '打印次数', width: 92 },
  { dataIndex: 'printStatus', fixed: 'right' as const, title: '打印标记', width: 112 },
  { dataIndex: 'confirmStatus', fixed: 'right' as const, title: '确认状态', width: 112 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 90 },
];

const secondInspectionRecordColumns = [
  { dataIndex: 'segmentLabel', title: '分段', width: 90 },
  { dataIndex: 'faiNo', title: '送检单号', width: 170 },
  { dataIndex: 'productionBatchNo', title: '分段批号', width: 180 },
  { dataIndex: 'sampleTypeName', title: '样品类型', width: 130 },
  { dataIndex: 'sampleLength', title: '送检米数(m)', width: 120 },
  { dataIndex: 'pushedAt', title: '送检时间', width: 170 },
  { dataIndex: 'status', title: '送检记录单状态', width: 150 },
  { dataIndex: 'result', title: '检验结果', width: 120 },
  { dataIndex: 'feedbackTime', title: '反馈时间', width: 170 },
  { dataIndex: 'feedbackRemark', title: '反馈备注', width: 220 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 220 },
];

const abnormalPositionColumns = [
  { dataIndex: 'processStage', title: '来源工序', width: 120 },
  { dataIndex: 'planNo', title: '计划号', width: 160 },
  { dataIndex: 'batchNo', title: '母批批号', width: 180 },
  { dataIndex: 'productionBatchNo', title: '分段批号', width: 180 },
  { dataIndex: 'positionText', title: '异常位置', width: 200 },
  { dataIndex: 'abnormalLength', title: '异常米数', width: 110 },
  { dataIndex: 'remark', title: '备注', width: 220 },
  { dataIndex: 'createTime', title: '登记时间', width: 170 },
];

const taskListColumns = [
  { dataIndex: 'planNo', title: '计划号', width: 170 },
  { dataIndex: 'motherModelCode', title: '产品型号', width: 150 },
  { dataIndex: 'batchNo', title: '母批批号', width: 160 },
  { dataIndex: 'productionDate', title: '生产日期', width: 120 },
  { dataIndex: 'motherLength', title: '收卷（m）', width: 110 },
  { dataIndex: 'firstGrindingProcessLength', title: '已完成1次磨皮量（m）', width: 160 },
  { dataIndex: 'secondGrindingProcessLength', title: '已完成2次磨皮量（m）', width: 160 },
  { dataIndex: 'status', title: '状态', width: 120 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 100 },
];

const equipmentSelectColumns = [
  { dataIndex: 'code', title: '设备编码', width: 150 },
  { dataIndex: 'name', title: '设备名称', width: 180 },
  { dataIndex: 'workCenterName', title: '工作中心', width: 160 },
  { dataIndex: 'workStatus', title: '运行状态', width: 120 },
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

const processColumns = [
  { dataIndex: 'category', title: '类别', width: 120 },
  { dataIndex: 'itemName', title: '工艺参数点检表项目', width: 170 },
  { dataIndex: 'standard', title: '标准', width: 180 },
  { dataIndex: 'actualValue', title: '实测值', width: 200 },
  { dataIndex: 'remark', title: '异常备注', width: 180 },
];
const roughDevProcessParamColumns = [
  { dataIndex: 'passName', title: '磨皮阶段', width: 110 },
  { dataIndex: 'planNo', title: '计划号', width: 150 },
  { dataIndex: 'batchNo', title: '母批/分段批号', width: 150 },
  { dataIndex: 'recordStatus', title: '状态', width: 100 },
  { dataIndex: 'fillInfo', title: '填写人/时间', width: 220 },
  { dataIndex: 'confirmResult', title: '确认结果', width: 100 },
  { dataIndex: 'confirmInfo', title: '确认人/时间', width: 220 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 190 },
];

const getDetailCategoryRowSpan = (rows: WorkPrepareDetail[], index: number) => {
  const category = rows[index]?.category;
  if (!category) return 1;
  if (index > 0 && rows[index - 1]?.category === category) return 0;
  let count = 1;
  for (let i = index + 1; i < rows.length; i += 1) {
    if (rows[i]?.category !== category) break;
    count += 1;
  }
  return count;
};

const reportedMiddleProductRecordColumns = [
  { dataIndex: 'name', title: '表单名称', width: 300 },
  { dataIndex: 'passName', title: '磨皮阶段', width: 110 },
  { dataIndex: 'planNo', title: '计划号', width: 150 },
  { dataIndex: 'batchNo', title: '母批/分段批号', width: 170 },
  { dataIndex: 'outputLength', title: '产出米数(m)', width: 120 },
  { dataIndex: 'processLength', title: '报工米数(m)', width: 120 },
  { dataIndex: 'status', title: '单据状态', width: 110 },
  { dataIndex: 'recorderInfo', title: '记录人/时间', width: 220 },
  { dataIndex: 'action', fixed: 'right' as const, title: '操作', width: 170 },
];

const formatNumber = (value: number) => `${Number(value || 0).toFixed(3)} m`;
const toNumber = (value: any) => Number(value ?? 0) || 0;
const todayText = () => dayjs().format('YYYY-MM-DD');
const firstText = (...values: unknown[]) => {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
};
const toDateTimeText = (value: any) => {
  if (!value) return undefined;
  if (dayjs.isDayjs(value)) return value.format('YYYY-MM-DD HH:mm:ss');
  if (value instanceof Date) return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
  if (typeof value === 'string') return value.trim();
  if (typeof value === 'number') return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
  const innerDate = value?.$d || value?.date || value?.value;
  if (innerDate) {
    const parsedInner = dayjs(innerDate);
    if (parsedInner.isValid()) return parsedInner.format('YYYY-MM-DD HH:mm:ss');
  }
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : String(value);
};

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function normalizeReportDateTimeText(value: any, fallbackDate = todayText()) {
  const text = toDateTimeText(value);
  if (!text || text === '-') return '';
  const normalized = normalizeBizDateTime(fallbackDate, text);
  if (!normalized || isTimeOnlyText(normalized)) return '';
  const parsed = dayjs(normalized);
  if (!parsed.isValid() || isPlaceholderDateTime(parsed)) return '';
  return parsed.format('YYYY-MM-DD HH:mm:ss');
}

function reportDateFromDateTime(value: any, fallbackDate = todayText()) {
  const normalized = normalizeReportDateTimeText(value, fallbackDate);
  return normalized ? dayjs(normalized).format('YYYY-MM-DD') : fallbackDate;
}

function readPickerDateText(value: any, dateString?: string | string[]) {
  if (Array.isArray(dateString)) return dateString.find(Boolean);
  return dateString || value;
}

function resolveReportTimeFallbackDate(record?: WorkRecord) {
  const candidates = [
    record?.reportDate,
    record?.startTime,
    record?.endTime,
    currentTask.value?.productionDate,
    currentPlan.startTime,
    todayText(),
  ];
  for (const candidate of candidates) {
    const text = String(candidate || '').trim();
    if (!text || text === '-' || isTimeOnlyText(text)) continue;
    const parsed = dayjs(text);
    if (parsed.isValid() && !isPlaceholderDateTime(parsed)) {
      return parsed.format('YYYY-MM-DD');
    }
  }
  return todayText();
}

function normalizeReportFormTimeField(field: 'startTime' | 'endTime', value?: any, record = reportCurrentRecord.value) {
  const fallbackDate = resolveReportTimeFallbackDate(record);
  const normalized = normalizeReportDateTimeText(value ?? reportForm[field], fallbackDate);
  if (normalized) {
    reportForm[field] = normalized;
  }
  return normalized;
}

const handleReportStartTimeChange = (value: any, dateString?: string | string[]) => {
  normalizeReportFormTimeField('startTime', readPickerDateText(value, dateString));
};

const handleReportEndTimeChange = (value: any, dateString?: string | string[]) => {
  normalizeReportFormTimeField('endTime', readPickerDateText(value, dateString));
};

function parseQtimeDateTime(value?: string | null, productionDate?: string) {
  const normalized = normalizeBizDateTime(productionDate || todayText(), value || undefined);
  if (!normalized || isTimeOnlyText(normalized)) return null;
  const date = dayjs(normalized);
  return date.isValid() ? date : null;
}

function formatQtimeDuration(totalMinutes?: number | null) {
  if (totalMinutes === null || totalMinutes === undefined || !Number.isFinite(totalMinutes)) return '-';
  const minutes = Math.max(0, Math.floor(totalMinutes));
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  const remainMinutes = minutes % 60;
  if (days > 0) {
    return `${days}天${hours}小时${remainMinutes}分钟`;
  }
  if (hours > 0) {
    return `${hours}小时${remainMinutes}分钟`;
  }
  return `${minutes}分钟`;
}

function resolveRoughQtimeLevelClass(totalMinutes?: number | null) {
  if (totalMinutes === null || totalMinutes === undefined || !Number.isFinite(totalMinutes)) {
    return 'rough-qtime-bar__qtime-value--empty';
  }
  if (totalMinutes >= 1440) return 'rough-qtime-bar__qtime-value--danger';
  if (totalMinutes >= 60) return 'rough-qtime-bar__qtime-value--warning';
  return 'rough-qtime-bar__qtime-value--normal';
}

function roughBackendQtimeElapsedMinutes(qtime?: MesHcRoughGrindingConsoleApi.QtimeInfo | null) {
  if (!qtime?.sourceEndTime) return qtime?.elapsedMinutes;
  const sourceTime = parseQtimeDateTime(qtime.sourceEndTime);
  if (!sourceTime) return qtime.elapsedMinutes;
  const targetTime = qtime.targetStartTime
    ? parseQtimeDateTime(qtime.targetStartTime)
    : dayjs(qtimeNowTimestamp.value);
  return targetTime ? targetTime.diff(sourceTime, 'minute') : qtime.elapsedMinutes;
}

function isRoughBackendQtimeTimeout(qtime?: MesHcRoughGrindingConsoleApi.QtimeInfo | null) {
  const elapsedMinutes = roughBackendQtimeElapsedMinutes(qtime);
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

function resolveRoughBackendQtimeLevelClass(qtime?: MesHcRoughGrindingConsoleApi.QtimeInfo | null) {
  if (!qtime || qtime.status === 'MISSING_SOURCE_TIME') {
    return 'rough-qtime-bar__qtime-value--empty';
  }
  if (isRoughBackendQtimeTimeout(qtime)) {
    return 'rough-qtime-bar__qtime-value--danger';
  }
  if (qtime.status === 'NORMAL') {
    return 'rough-qtime-bar__qtime-value--normal';
  }
  return 'rough-qtime-bar__qtime-value--warning';
}

function roughBackendQtimeDurationText(qtime?: MesHcRoughGrindingConsoleApi.QtimeInfo | null) {
  if (!qtime) return '-';
  const elapsedMinutes = roughBackendQtimeElapsedMinutes(qtime);
  if (
    elapsedMinutes !== undefined &&
    elapsedMinutes !== null &&
    qtime.standardMinutes !== undefined &&
    qtime.standardMinutes !== null
  ) {
    const stateText = qtime.targetStarted ? '实际磨皮开工' : '当前磨皮开工';
    const elapsedText = formatQtimeDuration(elapsedMinutes);
    const standardText = formatQtimeDuration(qtime.standardMinutes);
    return isRoughBackendQtimeTimeout(qtime)
      ? `${stateText}间隔 ${elapsedText}，已超出额定 ${standardText}`
      : `${stateText}间隔 ${elapsedText}，额定 ${standardText} 内`;
  }
  if (qtime.message) return qtime.message;
  if (qtime.status === 'MISSING_SOURCE_TIME') return '湿法未完工';
  if (qtime.status === 'NO_RULE') return '未配置额定 QTIME';
  return '-';
}

function roughQtimeSeverity(qtime?: MesHcRoughGrindingConsoleApi.QtimeInfo | null) {
  if (!qtime || qtime.status === 'MISSING_SOURCE_TIME') return 0;
  if (isRoughBackendQtimeTimeout(qtime)) return 3;
  if (qtime.status === 'NO_RULE') return 2;
  return 1;
}

function selectWorstRoughQtimeInfo(passType: GrindPass) {
  return segmentTimingRows.value
    .filter((item) => item.passType === passType && item.qtime)
    .map((item) => item.qtime as MesHcRoughGrindingConsoleApi.QtimeInfo)
    .sort((left, right) => roughQtimeSeverity(right) - roughQtimeSeverity(left))[0];
}

function getGrindingPassName(pass: GrindPass) {
  return pass === 'FIRST' ? '一次磨皮' : '二次磨皮';
}

function getMiddleProductProcessLengthFromReport() {
  const length = activePass.value === 'SECOND' ? reportOutputLengthValue.value : reportProcessLengthValue.value;
  return Number(Number(length || 0).toFixed(3));
}

function getMiddleProductRowCount() {
  return getMiddleProductProcessLengthFromReport() > 0 ? MIDDLE_PRODUCT_DEFAULT_ROW_COUNT : 0;
}

function getMiddleProductStartPosition() {
  return Number(reportForm.startPosition || 0);
}

function getMiddleProductDraftKey() {
  return [
    activePass.value,
    reportForm.batchNo || '',
    getMiddleProductProcessLengthFromReport(),
    getMiddleProductStartPosition(),
  ].join('|');
}

function createMiddleProductRow(seq: number, batchNo = ''): MiddleProductRow {
  return {
    batchNo,
    grindingPass: middleProductDetailHeader.passName || getGrindingPassName(activePass.value),
    guideClothBatchNo: guideCloth.batchNo === '-' ? '' : guideCloth.batchNo,
    guideClothLife: guideCloth.useCount || '',
    innerThickness: '',
    inputLength: '',
    key: seq,
    length: '',
    lengthMeter: '',
    materialCode: currentPlan.materialCode || '',
    modelCode: currentPlan.modelCode || '',
    outerThickness: '',
    outputLength: '',
    recorderName: operatorName.value || currentUserName.value || '',
    recordDate: todayText(),
    remark: '',
    replaceReason: '',
    result: 'OK' as const,
    sandpaperBatchNo: sandpaper.batchNo === '-' ? '' : sandpaper.batchNo,
    sandpaperLife: sandpaper.lifeLength || '',
    seq,
    thickness: '',
    width: '',
  };
}

function buildMiddleProductRows(
  batchNo = '',
  processLength: number | string = 0,
  rowCount = Number(processLength || 0) > 0 ? MIDDLE_PRODUCT_DEFAULT_ROW_COUNT : 0,
  _startPosition = 0,
): MiddleProductRow[] {
  const totalLength = Number(processLength || 0);
  const count = Math.max(0, Math.ceil(Number(rowCount || 0)));
  let remaining = totalLength;
  return Array.from({ length: count }, (_, index) => {
    const currentLength = remaining > 1 ? 1 : Math.max(remaining, 0);
    remaining = Number((remaining - currentLength).toFixed(3));
    return createMiddleProductRow(index + 1, batchNo);
  });
}

function getSecondSegmentMiddleProductKey(segmentMark = '') {
  return [
    currentPlan.planOperationId || currentPlan.planNo || 'NO_PLAN',
    normalizeSegmentMark(segmentMark) || 'NONE',
  ].join('|');
}

function buildSecondSegmentMiddleProductHeader(segmentMark = ''): MiddleProductDetailHeader {
  const segment = normalizeSegmentMark(segmentMark);
  const segmentLabel = getSegmentLabel(segment);
  const productionBatchNo = buildSecondGrindingBatchNo(currentPlan.batchNo, segment);
  return {
    batchNo: currentPlan.batchNo || '',
    displayName: `磨皮中间品记录表 - ${segmentLabel}`,
    formName: `CMP软垫（W26P0100）磨皮中间品记录表 - ${segmentLabel}`,
    generatedLength: 0,
    materialCode: currentPlan.materialCode || '',
    motherBatchNo: currentPlan.batchNo || '',
    motherModelCode: currentPlan.modelCode || '',
    passName: `第二次磨皮-${segmentLabel}`,
    passType: 'SECOND',
    planNo: currentPlan.planNo || '',
    processLength: 0,
    productionBatchNo,
    recordDate: todayText(),
    recorder: operatorName.value || currentUserName.value || '',
    recorderTime: '',
    segmentMark: segment,
    segmentName: segmentLabel,
    segmentTotalLength: 0,
    width: '',
    widthMm: '',
  };
}

function syncMiddleProductRowsFromReport(force = false) {
  const key = getMiddleProductDraftKey();
  if (!force && (middleProductDraftSaved.value || middleProductDraftKey.value === key)) {
    return;
  }
  const middleProductProcessLength = getMiddleProductProcessLengthFromReport();
  middleProductRows.value = buildMiddleProductRows(
    '',
    middleProductProcessLength,
    getMiddleProductRowCount(),
    getMiddleProductStartPosition(),
  );
  middleProductDraftKey.value = key;
  middleProductDetailPage.value = 1;
}

function resetMiddleProductDraft() {
  middleProductDraftSaved.value = false;
  middleProductDraftKey.value = '';
  middleProductRows.value = [];
  middleProductMainWidth.value = '';
  middleProductConfirmer.value = '';
  middleProductDetailPage.value = 1;
}

function closeMiddleProductDetail(force = false) {
  if (!force && middleProductDetailMode.value === 'edit') {
    AModal.confirm({
      title: '确认关闭中间品记录单',
      content: '关闭后未保存的中间品记录明细会丢失，确认关闭？',
      okText: '确认关闭',
      okType: 'danger',
      cancelText: '继续填写',
      onOk: () => {
        if (!middleProductEditingRecordId.value && !middleProductDraftSaved.value && !middleProductOpenedFromSegment.value) {
          syncMiddleProductRowsFromReport(true);
          middleProductMainWidth.value = '';
        }
        closeMiddleProductDetail(true);
      },
    });
    return;
  }
  middleProductDetailVisible.value = false;
  if (!middleProductOpenedFromReport.value) {
    middleProductEditingRecordId.value = undefined;
  }
  if (middleProductOpenedFromSegment.value) {
    middleProductOpenedFromSegment.value = false;
    middleProductSegmentKey.value = '';
  }
}

function buildMiddleProductRowsFromDetails(details: any[]): MiddleProductRow[] {
  return (details || []).map((item: any, index: number): MiddleProductRow => ({
    batchNo: item.batchNo || '',
    grindingPass: item.grindingPass || '',
    guideClothBatchNo: item.guideClothBatchNo || '',
    guideClothLife: item.guideClothLife ?? '',
    innerThickness: item.innerThickness || item.thickness || '',
    inputLength: item.inputLength ?? item.length ?? '',
    key: index + 1,
    length: item.length ?? '',
    lengthMeter: item.lengthMeter ?? item.length ?? item.inputLength ?? '',
    materialCode: item.materialCode || '',
    modelCode: item.modelCode || '',
    outerThickness: item.outerThickness || item.width || '',
    outputLength: item.outputLength ?? '',
    recorderName: item.recorderName || '',
    recordDate: item.recordDate || todayText(),
    remark: '',
    replaceReason: item.replaceReason || '',
    result: item.result === 'NG' ? 'NG' : 'OK',
    sandpaperBatchNo: item.sandpaperBatchNo || '',
    sandpaperLife: item.sandpaperLife ?? '',
    seq: index + 1,
    thickness: item.thickness || item.innerThickness || '',
    width: item.width || item.outerThickness || '',
  }));
}

function resetMiddleProductDetailHeader() {
  Object.keys(middleProductDetailHeader).forEach((key) => {
    delete middleProductDetailHeader[key as keyof MiddleProductDetailHeader];
  });
  middleProductReportMeters.value = '';
}

function buildMiddleProductHeaderData(pass: GrindPass) {
  const now = buildNowText();
  const processLength = Number(
    middleProductReportMeters.value ||
      middleProductDetailHeader.processLength ||
      getMiddleProductProcessLengthFromReport() ||
      0,
  );
  return {
    batchNo: reportForm.batchNo || currentPlan.batchNo || '',
    displayName: '磨皮中间品记录表',
    formName: 'CMP软垫（W26P0100）磨皮中间品记录表',
    generatedLength: middleProductRows.value.length || getMiddleProductRowCount(),
    materialCode: currentPlan.materialCode || '',
    motherBatchNo: currentPlan.batchNo || reportForm.batchNo || '',
    motherModelCode: currentPlan.modelCode || '',
    passName: getGrindingPassName(pass),
    passType: pass,
    planNo: reportForm.planNo || currentPlan.planNo || '',
    processLength,
    productionBatchNo: reportForm.batchNo || '',
    recordDate: todayText(),
    recorder: operatorName.value || currentUserName.value || '',
    recorderTime: middleProductDetailHeader.recorderTime || now,
    segmentTotalLength: processLength,
    confirmer: middleProductConfirmer.value || '',
    confirmerTime: middleProductDetailHeader.confirmerTime || '',
    width: middleProductMainWidth.value || '',
    widthMm: middleProductMainWidth.value || '',
  };
}

function buildMiddleProductHeaderPayload(pass: GrindPass) {
  const processLength = Number(
    middleProductReportMeters.value ||
      middleProductDetailHeader.processLength ||
      getMiddleProductProcessLengthFromReport() ||
      0,
  );
  return JSON.stringify({
    ...buildMiddleProductHeaderData(pass),
    ...middleProductDetailHeader,
    displayName: getMiddleProductDisplayName(middleProductDetailHeader),
    generatedLength: middleProductRows.value.length || getMiddleProductRowCount(),
    processLength,
    confirmer: middleProductConfirmer.value || middleProductDetailHeader.confirmer || '',
    confirmerTime: middleProductDetailHeader.confirmerTime || '',
    width: middleProductMainWidth.value || middleProductDetailHeader.width || '',
    widthMm: middleProductMainWidth.value || middleProductDetailHeader.widthMm || '',
  });
}

function buildMiddleProductPayloadRows() {
  return middleProductRows.value.map((row) => ({
    lengthMeter: row.lengthMeter,
    innerThickness: row.innerThickness,
    outerThickness: row.outerThickness,
    length: row.length,
    batchNo: row.batchNo,
    grindingPass: row.grindingPass,
    guideClothBatchNo: row.guideClothBatchNo,
    guideClothLife: row.guideClothLife,
    inputLength: row.inputLength,
    materialCode: row.materialCode,
    modelCode: row.modelCode,
    outputLength: row.outputLength,
    recorderName: row.recorderName,
    recordDate: row.recordDate,
    remark: row.remark,
    replaceReason: row.replaceReason,
    result: row.result,
    sandpaperBatchNo: row.sandpaperBatchNo,
    sandpaperLife: row.sandpaperLife,
    seq: row.seq,
    thickness: row.innerThickness || row.thickness,
    width: row.outerThickness || row.width,
  }));
}

function getProcessActualValue(itemName: string) {
  return processItems.value.find((item) => item.itemName === itemName)?.actualValue || '';
}

function syncProcessItemsWithReportLength() {
  const meterCounter = processItems.value.find((item) => item.itemName === '计米器');
  if (meterCounter && !meterCounter.actualValue) {
    meterCounter.actualValue = Number(reportForm.processLength || 0).toFixed(3);
  }
  const napGrindingMeters = processItems.value.find((item) => item.category === '磨皮后NAP层' && item.itemName === '磨皮米数');
  if (napGrindingMeters) {
    napGrindingMeters.actualValue = Number(reportForm.processLength || 0).toFixed(3);
  }
}

function buildProcessPayloadFields(
  dynamicDetails: Array<{ actualValue?: string; item?: string }> = [],
) {
  const getActualValue = (names: string[]) => {
    const dynamicValue = dynamicDetails.find((item) => names.includes(firstText(item.item)))?.actualValue;
    return firstText(dynamicValue, getProcessActualValue(names[0]));
  };
  const napGrindingMeters = getActualValue(['磨皮米数']);
  const napGrindingMetersNumber = Number(napGrindingMeters || 0);
  return {
    afterGrindingThickness: getActualValue(['磨皮后厚度']),
    grindingMeters: activePass.value === 'SECOND' && napGrindingMetersNumber > 0 ? napGrindingMetersNumber : Number(reportForm.processLength || 0),
    grindingThickness: getActualValue(['磨皮厚度']),
    lineSpeed: getActualValue(['线速']),
    meterCounter: getActualValue(['计米器']),
    pressure: getActualValue(['气压']),
    qualityThickness: activePass.value === 'SECOND' ? getActualValue(['厚度', 'NAP厚度']) : undefined,
    qualityWidth: activePass.value === 'SECOND' ? getActualValue(['磨后宽幅', '宽幅']) : undefined,
    rotationSpeed: getActualValue(['转速']),
  };
}

function buildProcessCheckHeaderPayload() {
  return JSON.stringify({
    batchNo: reportForm.batchNo || currentPlan.batchNo || '',
    formName: `${activePass.value === 'FIRST' ? '第一次磨皮' : '第二次磨皮'}工艺参数点检表`,
    passName: getGrindingPassName(activePass.value),
    passType: activePass.value,
    planNo: reportForm.planNo || currentPlan.planNo || '',
    processLength: Number(reportForm.processLength || 0),
    recordDate: todayText(),
    recorder: operatorName.value || currentUserName.value || '',
    segmentMark: activePass.value === 'SECOND' || isFirstAllocationReport.value
      ? reportForm.segmentMark || 'NONE'
      : '',
  });
}

function mapMiddleProductRows(details: any[], fallbackBatchNo = '') {
  const rows = buildMiddleProductRowsFromDetails(details || []);
  (details || []).forEach((item: any, index: number) => {
    const seq = Number(item.seq || index + 1);
    const target = rows[seq - 1];
    if (!target) return;
    target.batchNo = item.batchNo || fallbackBatchNo;
    target.grindingPass = item.grindingPass || target.grindingPass;
    target.guideClothBatchNo = item.guideClothBatchNo || '';
    target.guideClothLife = item.guideClothLife ?? '';
    target.innerThickness = item.innerThickness || item.thickness || '';
    target.inputLength = item.inputLength ?? item.length ?? '';
    target.length = item.length ?? '';
    target.lengthMeter = item.lengthMeter ?? item.length ?? item.inputLength ?? '';
    target.materialCode = item.materialCode || '';
    target.modelCode = item.modelCode || '';
    target.outerThickness = item.outerThickness || item.width || '';
    target.outputLength = item.outputLength ?? '';
    target.recorderName = item.recorderName || '';
    target.recordDate = item.recordDate || todayText();
    target.width = item.width || item.outerThickness || '';
    target.thickness = item.thickness || item.innerThickness || '';
    target.result = item.result === 'NG' ? 'NG' : 'OK';
    target.replaceReason = item.replaceReason || '';
    target.sandpaperBatchNo = item.sandpaperBatchNo || '';
    target.sandpaperLife = item.sandpaperLife ?? '';
    target.remark = item.remark || '';
  });
  return rows;
}

function parseJsonObject(value?: string) {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function getRoughDevProcessParamFormCode(pass: GrindPass = activePass.value) {
  return firstText(roughDevProcessParamForms.value[pass]?.formCode);
}

function getRoughDevProcessParamRecords(pass: GrindPass = activePass.value) {
  return roughDevProcessParamRecords.value[pass] || [];
}

function getRoughDevRecordTemplateCode(record: MesHcProcessFormApi.Record) {
  const context = parseJsonObject(record.contextJson) as Record<string, any>;
  return firstText(
    record.templateCode,
    record.template?.templateCode,
    context.formCode,
    context.runtimeSchema?.formCode,
  );
}

function getRoughDevRecordPassName(record?: MesHcProcessFormApi.Record | null) {
  if (!record) return '';
  const header = parseJsonObject(record.headerDataJson) as Record<string, any>;
  const context = parseJsonObject(record.contextJson) as Record<string, any>;
  return firstText(
    header.passName,
    context.businessParams?.passName,
    context.businessParams?.grindingPass === 'SECOND' ? '二次磨皮' : '',
    context.businessParams?.grindingPass === 'FIRST' ? '一次磨皮' : '',
  );
}

function getRoughDevProcessParamName(pass: GrindPass = activePass.value) {
  return `${getGrindingPassName(pass)}工艺参数点检表`;
}

function getCurrentLoginUserName() {
  return firstText(currentUserName.value, userStore.userInfo?.nickname, userStore.userInfo?.username, currentUserNo.value);
}

function applyRoughDevCurrentLoginUser(
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

function getRoughDevRecordHeader(record?: MesHcProcessFormApi.Record | null) {
  return parseJsonObject(record?.headerDataJson) as Record<string, any>;
}

function normalizeRoughDevRecordStatus(status?: string) {
  return String(status || 'DRAFT').trim().toUpperCase();
}

function getRoughDevRecordStatusMeta(status?: string) {
  const normalized = normalizeRoughDevRecordStatus(status);
  if (normalized === 'CONFIRMED') return { color: 'green', text: '已确认' };
  if (normalized === 'SUBMITTED') return { color: 'processing', text: '已提交' };
  if (normalized === 'DRAFT') return { color: 'blue', text: '草稿' };
  if (normalized === 'CANCELLED') return { color: 'default', text: '已作废' };
  return { color: 'default', text: status || '草稿' };
}

function isRoughDevProcessParamDraft(record?: MesHcProcessFormApi.Record | null) {
  return normalizeRoughDevRecordStatus(record?.recordStatus) === 'DRAFT';
}

function isRoughDevProcessParamConfirmed(record?: MesHcProcessFormApi.Record | null) {
  return normalizeRoughDevRecordStatus(record?.recordStatus) === 'CONFIRMED';
}

function getRoughDevConfirmResult(record?: MesHcProcessFormApi.Record | null) {
  const header = getRoughDevRecordHeader(record);
  return firstText(header.inspectionResult, header.confirmResult, record?.resultStatus, '-');
}

function getRoughDevConfirmUser(record?: MesHcProcessFormApi.Record | null) {
  const header = getRoughDevRecordHeader(record);
  return firstText(record?.confirmUserName, header.confirmer, header.confirmUserName, '-');
}

function getRoughDevConfirmTime(record?: MesHcProcessFormApi.Record | null) {
  const header = getRoughDevRecordHeader(record);
  return firstText(record?.confirmTime, header.confirmerTime, header.confirmTime, '-');
}

function getRoughDevExpectedPlanNo(pass: GrindPass = activePass.value) {
  return pass === 'FIRST'
    ? firstText(reportForm.planNo, firstReportSourcePlanNo.value, currentPlan.planNo)
    : firstText(reportForm.planNo, currentPlan.planNo);
}

function getRoughDevExpectedBatchNo(pass: GrindPass = activePass.value) {
  if (pass === 'FIRST') {
    return firstText(reportForm.batchNo, firstReportSourceBatchNo.value, currentPlan.batchNo);
  }
  return firstText(reportForm.batchNo, currentPlan.batchNo);
}

function getRoughDevExpectedSourceRowId(pass: GrindPass = activePass.value) {
  if (pass === 'SECOND' || (pass === 'FIRST' && isFirstAllocatedMode.value)) {
    return firstText(reportForm.segmentMark || 'NONE');
  }
  return firstText(reportForm.batchNo, currentPlan.batchNo);
}

function getRoughDevProcessContextKey(pass: GrindPass = activePass.value) {
  return [
    pass,
    currentPlan.planId || '',
    currentPlan.planOperationId || '',
    getRoughDevExpectedPlanNo(pass),
    getRoughDevExpectedBatchNo(pass),
    getRoughDevExpectedSourceRowId(pass),
  ].join('|');
}

function matchesCurrentRoughDevProcessRecord(record: MesHcProcessFormApi.Record, pass: GrindPass) {
  const formCode = getRoughDevProcessParamFormCode(pass);
  if (getRoughDevRecordTemplateCode(record) !== formCode) {
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
  const expectedPlanNo = getRoughDevExpectedPlanNo(pass);
  if (expectedPlanNo && record.planNo && String(record.planNo).trim() !== expectedPlanNo) {
    return false;
  }
  const expectedBatchNo = getRoughDevExpectedBatchNo(pass);
  if (expectedBatchNo && record.batchNo && String(record.batchNo).trim() !== expectedBatchNo) {
    return false;
  }
  const expectedSourceRowId = getRoughDevExpectedSourceRowId(pass);
  if (pass === 'SECOND' || (pass === 'FIRST' && isFirstAllocatedMode.value)) {
    const context = parseJsonObject(record.contextJson) as Record<string, any>;
    const sourceRowId = firstText(context.businessParams?.sourceRowId);
    if (!sourceRowId || sourceRowId !== expectedSourceRowId) {
      return false;
    }
  }
  return true;
}

async function ensureRoughDevProcessParamForm(pass: GrindPass = activePass.value) {
  const form = await resolvePublishedStationForm({
    formType: 'PROCESS_PARAM',
    grindingPass: pass,
    modelCode: firstText(currentPlan.modelCode),
    processCode: 'ROUGH_GRINDING',
  });
  roughDevProcessParamForms.value = {
    ...roughDevProcessParamForms.value,
    [pass]: form,
  };
  return form;
}

async function loadRoughDevProcessParamRecords(pass: GrindPass = activePass.value) {
  const requestVersion = roughDevProcessParamLoadVersion.value[pass] + 1;
  const requestContextKey = getRoughDevProcessContextKey(pass);
  roughDevProcessParamLoadVersion.value = {
    ...roughDevProcessParamLoadVersion.value,
    [pass]: requestVersion,
  };
  roughDevProcessParamRecords.value = {
    ...roughDevProcessParamRecords.value,
    [pass]: [],
  };
  roughDevProcessParamLoading.value = {
    ...roughDevProcessParamLoading.value,
    [pass]: true,
  };
  const isCurrentRequest = () =>
    roughDevProcessParamLoadVersion.value[pass] === requestVersion &&
    getRoughDevProcessContextKey(pass) === requestContextKey;
  try {
    const form = await ensureRoughDevProcessParamForm(pass);
    if (!form?.formCode) {
      return;
    }
    const formCode = firstText(form?.formCode, getRoughDevProcessParamFormCode(pass));
    const page = await getProcessFormRecordPage({
      batchNo: getRoughDevExpectedBatchNo(pass) || undefined,
      pageNo: 1,
      pageSize: 200,
      planNo: getRoughDevExpectedPlanNo(pass) || undefined,
      templateCode: formCode,
    });
    const rows = (Array.isArray(page?.list) ? page.list : [])
      .filter((row) => matchesCurrentRoughDevProcessRecord(row, pass))
      .sort((left, right) => String(right.createTime || right.fillTime || '').localeCompare(String(left.createTime || left.fillTime || '')));
    if (!isCurrentRequest()) return;
    roughDevProcessParamRecords.value = {
      ...roughDevProcessParamRecords.value,
      [pass]: rows,
    };
  } catch (error: any) {
    if (isCurrentRequest()) {
      message.error(error?.message || '加载工艺参数点检表记录失败');
    }
  } finally {
    if (isCurrentRequest()) {
      roughDevProcessParamLoading.value = {
        ...roughDevProcessParamLoading.value,
        [pass]: false,
      };
    }
  }
}

function buildRoughDevProcessParamInitialParams(pass: GrindPass = activePass.value) {
  return applyRoughDevCurrentLoginUser({
    batchNo: getRoughDevExpectedBatchNo(pass),
    equipmentCode: firstText(equipmentInfo.code),
    equipmentId: firstText(equipmentInfo.id),
    equipmentName: firstText(equipmentInfo.name),
    grindingPass: pass,
    materialCode: firstText(currentPlan.materialCode),
    modelCode: firstText(currentPlan.modelCode),
    passName: getGrindingPassName(pass),
    planId: firstText(currentPlan.planId),
    planNo: getRoughDevExpectedPlanNo(pass),
    planOperationId: firstText(currentPlan.planOperationId),
    recordDate: todayText(),
    sourceRowId: pass === 'SECOND' || (pass === 'FIRST' && isFirstAllocatedMode.value)
      ? firstText(reportForm.segmentMark || 'NONE', reportForm.batchNo)
      : firstText(reportForm.batchNo),
  }, { confirmer: false, recorder: true });
}

async function openRoughDevProcessParamFill(pass: GrindPass = activePass.value) {
  const form = await ensureRoughDevProcessParamForm(pass);
  if (!form?.id) {
    message.warning(`未找到${getRoughDevProcessParamName(pass)}的正式已发布模板，请先在动态表单配置中发布对应型号和阶段的表单。`);
    return;
  }
  roughDevProcessParamFillPass.value = pass;
  roughDevProcessParamFillForm.value = form;
  roughDevProcessParamInitialParams.value = buildRoughDevProcessParamInitialParams(pass);
  roughDevProcessParamFillOpen.value = true;
}

async function handleRoughDevProcessParamSaved() {
  await loadRoughDevProcessParamRecords(roughDevProcessParamFillPass.value);
}

function buildRoughDevRecordRuntimeDetailRows(record?: MesHcProcessFormApi.Record | null) {
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

function buildRoughDevProcessParamHeaderData(record?: MesHcProcessFormApi.Record | null) {
  const header = {
    ...(parseJsonObject(record?.headerDataJson) as Record<string, any>),
  };
  if (!Array.isArray(header.previewDetails)) {
    header.previewDetails = buildRoughDevRecordRuntimeDetailRows(record);
  }
  return header;
}

async function openRoughDevProcessParamDetail(record: MesHcProcessFormApi.Record, mode: 'edit' | 'view' = 'view') {
  if (!record.id) return;
  roughDevProcessParamViewLoading.value = true;
  roughDevProcessParamViewVisible.value = true;
  roughDevProcessParamViewMode.value = mode;
  try {
    const detail = await getProcessFormRecordDetail(record.id);
    roughDevProcessParamViewRecord.value = detail;
    roughDevProcessParamViewHeaderData.value = applyRoughDevCurrentLoginUser(
      buildRoughDevProcessParamHeaderData(detail),
      {
        confirmer: false,
        recorder: mode === 'edit' && isRoughDevProcessParamDraft(detail),
      },
    );
  } catch (error: any) {
    roughDevProcessParamViewVisible.value = false;
    message.error(error?.message || '加载工艺参数点检表详情失败');
  } finally {
    roughDevProcessParamViewLoading.value = false;
  }
}

function closeRoughDevProcessParamDetail() {
  roughDevProcessParamViewVisible.value = false;
  roughDevProcessParamViewRecord.value = null;
  roughDevProcessParamViewHeaderData.value = {};
  roughDevProcessParamViewMode.value = 'view';
}

const roughDevProcessParamViewContext = computed<Record<string, any>>(
  () => parseJsonObject(roughDevProcessParamViewRecord.value?.contextJson) as Record<string, any>,
);
const roughDevProcessParamViewSchema = computed<Record<string, any>>(
  () => roughDevProcessParamViewContext.value.runtimeSchema || {},
);
const roughDevProcessParamViewItems = computed<MesHcStationFormApi.StationFormItem[]>(() =>
  buildRoughDevRecordRuntimeDetailRows(roughDevProcessParamViewRecord.value).map((row) => ({
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
const roughDevProcessParamViewMetaItems = computed(() => [
  `计划号：${roughDevProcessParamViewRecord.value?.planNo || '-'}`,
  `母批/分段批号：${roughDevProcessParamViewRecord.value?.batchNo || '-'}`,
  `磨皮阶段：${getRoughDevRecordPassName(roughDevProcessParamViewRecord.value || {}) || '-'}`,
]);

function buildRoughDevProcessParamUpdatePayload(
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

async function refreshRoughDevProcessParamRecord(id: number, pass: GrindPass = activePass.value) {
  const detail = await getProcessFormRecordDetail(id);
  roughDevProcessParamViewRecord.value = detail;
  roughDevProcessParamViewHeaderData.value = buildRoughDevProcessParamHeaderData(detail);
  await loadRoughDevProcessParamRecords(pass);
  return detail;
}

async function saveRoughDevProcessParamView(silent = false) {
  const record = roughDevProcessParamViewRecord.value;
  if (!record?.id) return undefined;
  if (!isRoughDevProcessParamDraft(record)) {
    message.warning('只有草稿状态的工艺参数点检表允许修改');
    return undefined;
  }
  roughDevProcessParamSaving.value = true;
  try {
    const runtimeHeader = roughDevProcessParamRuntimeRef.value?.getHeaderData?.() || roughDevProcessParamViewHeaderData.value;
    applyRoughDevCurrentLoginUser(runtimeHeader, { confirmer: false, recorder: true });
    const recordTime = buildNowText();
    runtimeHeader.recorderTime = recordTime;
    runtimeHeader.recordTime = recordTime;
    runtimeHeader.checkerTime = recordTime;
    const runtimeItems = roughDevProcessParamRuntimeRef.value?.buildRecordItems?.() || record.items || [];
    await updateProcessFormRecord(buildRoughDevProcessParamUpdatePayload(record, runtimeHeader, runtimeItems));
    if (!silent) {
      AModal.success({
        title: '保存成功',
        content: '工艺参数点检表已保存。',
        okText: '确认',
        zIndex: 4300,
        onOk() {
          if (roughDevProcessParamViewRecord.value?.id === record.id) {
            closeRoughDevProcessParamDetail();
          }
        },
      });
    }
    if (roughDevProcessParamViewRecord.value?.id === record.id) {
      await refreshRoughDevProcessParamRecord(record.id, activePass.value);
    } else {
      await loadRoughDevProcessParamRecords(activePass.value);
    }
    return record.id;
  } finally {
    roughDevProcessParamSaving.value = false;
  }
}

async function executeConfirmRoughDevProcessParamRecord(record: MesHcProcessFormApi.Record, userInfo: any) {
  if (!record.id) return;
  if (isRoughDevProcessParamConfirmed(record)) {
    message.info('该工艺参数点检表已确认');
    return;
  }
  const confirmedRecordId = record.id!;
  const confirmUserId = Number(userInfo?.userId);
  if (!Number.isSafeInteger(confirmUserId) || confirmUserId <= 0) {
    message.warning('未识别到认证确认人，请重新进行身份认证');
    return;
  }
  roughDevProcessParamConfirming.value = true;
  try {
    const detail = await getProcessFormRecordDetail(confirmedRecordId);
    const isCurrentEditingRecord = roughDevProcessParamViewRecord.value?.id === confirmedRecordId;
    const header = isCurrentEditingRecord
      ? (roughDevProcessParamRuntimeRef.value?.getHeaderData?.() || roughDevProcessParamViewHeaderData.value)
      : buildRoughDevProcessParamHeaderData(detail);
    const items = isCurrentEditingRecord
      ? (roughDevProcessParamRuntimeRef.value?.buildRecordItems?.() || detail.items || [])
      : (detail.items || []);
    applyRoughDevCurrentLoginUser(header, { confirmer: false, recorder: false });
    header.inspectionResult = firstText(header.inspectionResult, header.confirmResult, detail.resultStatus, 'OK');
    await updateProcessFormRecord(buildRoughDevProcessParamUpdatePayload(detail, header, items));
    await confirmProcessFormRecordBySigner({ id: confirmedRecordId, confirmUserId });
    await loadRoughDevProcessParamRecords(activePass.value);
    if (roughDevProcessParamViewRecord.value?.id === confirmedRecordId) {
      await refreshRoughDevProcessParamRecord(confirmedRecordId, activePass.value);
      roughDevProcessParamViewMode.value = 'view';
      closeRoughDevProcessParamDetail();
    }
    message.success('工艺参数点检表已保存并确认。');
  } finally {
    roughDevProcessParamConfirming.value = false;
  }
}

function confirmRoughDevProcessParamRecord(record: MesHcProcessFormApi.Record) {
  if (!record.id) return;
  if (isRoughDevProcessParamConfirmed(record)) {
    message.info('该工艺参数点检表已确认');
    return;
  }
  roughDevProcessParamAuthRecord.value = record;
  roughDevProcessParamAuthVisible.value = true;
}

async function handleRoughDevProcessParamAuthSuccess(userInfo: any) {
  const record = roughDevProcessParamAuthRecord.value;
  roughDevProcessParamAuthVisible.value = false;
  roughDevProcessParamAuthRecord.value = null;
  if (!record) return;
  await executeConfirmRoughDevProcessParamRecord(record, userInfo);
}

function hasConfirmedRoughDevProcessParamRecord(pass: GrindPass = activePass.value) {
  return getRoughDevProcessParamRecords(pass).some((record) => isRoughDevProcessParamConfirmed(record));
}

function getCurrentRoughDevProcessParamRecord(pass: GrindPass = activePass.value) {
  const records = getRoughDevProcessParamRecords(pass).filter(
    (record) => normalizeRoughDevRecordStatus(record.recordStatus) !== 'CANCELLED',
  );
  return records.find((record) => isRoughDevProcessParamConfirmed(record)) || records[0];
}

async function buildCurrentRoughDevProcessCheckPayload(pass: GrindPass = activePass.value) {
  const contextKey = getRoughDevProcessContextKey(pass);
  const record = getCurrentRoughDevProcessParamRecord(pass);
  if (!record?.id || !isRoughDevProcessParamConfirmed(record)) return undefined;
  const detail = record.items?.length ? record : await getProcessFormRecordDetail(record.id);
  if (
    contextKey !== getRoughDevProcessContextKey(pass) ||
    !detail?.id ||
    !matchesCurrentRoughDevProcessRecord(detail, pass) ||
    !detail.items?.length
  ) {
    return undefined;
  }
  const header = {
    ...parseJsonObject(buildProcessCheckHeaderPayload()),
    ...parseJsonObject(detail.headerDataJson),
    processFormRecordId: detail.id,
    processFormRecordNo: detail.recordNo || '',
    sourceRowId: getRoughDevExpectedSourceRowId(pass),
  };
  return {
    details: detail.items.map((item, index) => ({
      actualValue: item.actualValue || '',
      actualValue2: item.actualValue2 || '',
      category: item.itemCategory || '',
      item: item.fieldLabel || item.fieldKey || `点检项${index + 1}`,
      itemSeq: item.itemSeq || index + 1,
      node: item.stepNode || '',
      remark: item.abnormalRemark || '',
      standard: item.standardText || '',
      status: item.resultFlag || 'OK',
      valueMode: item.valueMode || 'TEXT',
    })),
    headerDataJson: JSON.stringify(header),
    recordId: detail.id,
  };
}

function stripMiddleProductModelPrefix(name?: string) {
  const text = String(name || '').trim();
  if (!text) return '';
  const stripped = text.replace(/^.*?[（(][^）)]*[）)]\s*/u, '').trim();
  return stripped || text;
}

function getMiddleProductDisplayName(header: Partial<MiddleProductDetailHeader>) {
  return (
    header.displayName ||
    stripMiddleProductModelPrefix(header.formName) ||
    '磨皮中间品记录表'
  );
}

function normalizeMiddleProductAttachments(value?: any): MiddleProductAttachment[] {
  if (!value) return [];
  if (typeof value === 'string') {
    return normalizeMiddleProductAttachments(parseJsonObject(value));
  }
  const source = Array.isArray(value) ? value : [value];
  return source
    .map((item, index) => {
      if (!item) return null;
      const path = String(item.path || item.filePath || '');
      const url = String(item.url || item.fileUrl || path || '');
      if (!url) return null;
      return {
        name: String(item.name || item.fileName || url.split('/').pop() || `附件${index + 1}`),
        path: path || undefined,
        size: item.size,
        type: item.type,
        uid: item.uid || `${Date.now()}-${index}`,
        uploadTime: item.uploadTime,
        url,
      } as MiddleProductAttachment;
    })
    .filter(Boolean)
    .slice(-1) as MiddleProductAttachment[];
}

function openMiddleProductAttachment(attachment: MiddleProductAttachment) {
  if (!attachment?.url) return;
  window.open(attachment.url, '_blank');
}

function removeMiddleProductAttachment(attachment: MiddleProductAttachment) {
  const attachments = normalizeMiddleProductAttachments(middleProductDetailHeader.attachments).filter(
    (item) => item.uid !== attachment.uid && item.url !== attachment.url,
  );
  middleProductDetailHeader.attachments = attachments;
  (middleProductDetailHeader as Record<string, any>).importAttachment = attachments[0];
}

function buildImportedAttachment(file: File, uploaded: any) {
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

function withImportAttachment(headerDataJson: string | undefined, attachment: Record<string, any>) {
  return JSON.stringify({
    ...parseJsonObject(headerDataJson),
    attachments: [attachment],
    importAttachment: attachment,
    importTime: attachment.uploadTime,
  });
}

function buildMiddleProductExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName = middleProductDisplayName.value;
  return buildRoughMiddleProductExcelLayout({
      thicknessLabels: middleThicknessLabels.value,
    editable: middleProductDetailMode.value !== 'view',
    filePrefix: middleProductDetailHeader.planNo || currentPlan.planNo || '磨皮',
    formName,
    headerItems: [
      { editable: false, label: '计划号', value: middleProductDetailHeader.planNo || currentPlan.planNo || '' },
      { editable: false, label: '当前工序', value: '磨皮' },
      { editable: false, label: '磨皮阶段', value: middleProductDetailHeader.passName || getGrindingPassName(middleProductActivePass.value) },
      { editable: false, label: '型号', value: middleProductDetailHeader.motherModelCode || currentPlan.modelCode || '' },
      { editable: false, label: '料号', value: middleProductDetailHeader.materialCode || currentPlan.materialCode || '' },
      { editable: false, label: middleProductActivePass.value === 'SECOND' ? '分段批号' : '母批批号', value: middleProductDetailHeader.productionBatchNo || reportForm.batchNo || '' },
      { bindField: 'headerData', bindKey: 'processLength', editable: true, label: '报工米数/m', value: String(middleProductReportMeters.value || middleProductDetailHeader.processLength || '') },
      { bindField: 'headerData', bindKey: 'widthMm', editable: true, label: '宽幅mm', value: middleProductMainWidth.value || middleProductDetailHeader.widthMm || middleProductDetailHeader.width || '' },
      { bindField: 'headerData', bindKey: 'recordDate', editable: true, label: '生产日期', value: middleProductDetailHeader.recordDate || todayText() },
      { bindField: 'headerData', bindKey: 'recorder', editable: true, label: '记录人', value: middleProductDetailHeader.recorder || operatorName.value || currentUserName.value || '' },
      { bindField: 'headerData', bindKey: 'recorderTime', editable: false, label: '记录时间', value: middleProductDetailHeader.recorderTime || '' },
      { bindField: 'headerData', bindKey: 'confirmer', editable: true, label: '确认人', value: middleProductConfirmer.value || middleProductDetailHeader.confirmer || '' },
      { bindField: 'headerData', bindKey: 'confirmerTime', editable: false, label: '确认时间', value: middleProductDetailHeader.confirmerTime || '' },
    ],
    rows: middleProductRows.value,
  });
}

function applyImportedMiddleProductHeader(key: string, rawValue: unknown) {
  const value = String(rawValue ?? '').trim();
  if (!value && value !== '0') return false;
  if (key === 'processLength') {
    middleProductReportMeters.value = value;
    middleProductDetailHeader.processLength = Number(value) || 0;
    return true;
  }
  if (key === 'widthMm') {
    middleProductMainWidth.value = value;
    middleProductDetailHeader.width = value;
    middleProductDetailHeader.widthMm = value;
    return true;
  }
  if (key === 'confirmer') {
    middleProductConfirmer.value = value;
    middleProductDetailHeader.confirmer = value;
    return true;
  }
  if (['confirmerTime', 'recordDate', 'recorder', 'recorderTime'].includes(key)) {
    (middleProductDetailHeader as Record<string, any>)[key] = value;
    return true;
  }
  return false;
}

function applyImportedMiddleProductExcel(resp: MesHcProcessFormApi.LayoutImportResp) {
  let appliedCount = 0;
  let importedProcessLength = false;
  (resp.headerValues || []).forEach((item) => {
    if (item.bindField !== 'headerData' || !item.bindKey) return;
    if (applyImportedMiddleProductHeader(item.bindKey, item.value)) {
      if (item.bindKey === 'processLength') importedProcessLength = true;
      appliedCount += 1;
    }
  });
  const rowMap = new Map<number, Partial<MiddleProductRow>>();
  (resp.cellValues || []).forEach((item) => {
    const index = Number(item.bodyRowIndex ?? item.bindKey);
    if (!Number.isInteger(index) || index < 0 || !item.bindField) return;
    if (!middleProductEditFields.includes(item.bindField as MiddleProductEditField)) return;
    const value = String(item.value ?? '').trim();
    if (!value && value !== '0') return;
    const row = rowMap.get(index) || {};
    row[item.bindField as MiddleProductEditField] = value;
    rowMap.set(index, row);
    appliedCount += 1;
  });
  if (rowMap.size > 0) {
    const indexes = [...rowMap.keys()].sort((a, b) => a - b);
    const defaults = buildMiddleProductRows(
      middleProductDetailHeader.productionBatchNo || reportForm.batchNo || currentPlan.batchNo || '',
      middleProductReportMeters.value ||
        middleProductDetailHeader.processLength ||
        getMiddleProductProcessLengthFromReport() ||
        indexes.length,
      indexes.length,
      getMiddleProductStartPosition(),
    );
    middleProductRows.value = indexes.map((sourceIndex, targetIndex) => {
      const imported = rowMap.get(sourceIndex) || {};
      const base = defaults[targetIndex] || buildMiddleProductRows('', indexes.length, 1)[0]!;
      const lengthMeter = imported.lengthMeter ?? base.lengthMeter;
      const innerThickness = imported.innerThickness ?? base.innerThickness;
      const outerThickness = imported.outerThickness ?? base.outerThickness;
      return {
        ...base,
        ...imported,
        innerThickness,
        inputLength: lengthMeter,
        key: targetIndex + 1,
        length: lengthMeter,
        lengthMeter,
        outerThickness,
        outputLength: lengthMeter,
        seq: targetIndex + 1,
        thickness: innerThickness,
        width: outerThickness,
      };
    });
    middleProductDetailHeader.generatedLength = middleProductRows.value.length;
    if (!importedProcessLength) {
      const maxLength = Math.max(
        ...middleProductRows.value.map((row) => Number(row.lengthMeter || 0)).filter((value) => Number.isFinite(value)),
        0,
      );
      const processLength = maxLength || middleProductRows.value.length;
      middleProductReportMeters.value = processLength;
      middleProductDetailHeader.processLength = processLength;
    }
  }
  return appliedCount;
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

function isTimeOnlyText(value?: string) {
  if (!value) return false;
  return /^\d{2}:\d{2}(:\d{2})?$/.test(String(value).trim());
}

function isPlaceholderDateTime(date: dayjs.Dayjs) {
  return date.isValid() && (date.year() === 1970 || date.year() === 1900);
}

function shouldResetRuntimeDateTime(value?: string) {
  if (!value || value === '-') return true;
  if (isTimeOnlyText(value)) return true;
  const date = dayjs(value);
  return !date.isValid() || isPlaceholderDateTime(date) || date.format('HH:mm:ss') === '08:00:00';
}

function normalizeBizDateTime(bizDate: string | undefined, value?: string) {
  if (!value || value === '-') return '';
  const production = bizDate ? dayjs(bizDate) : null;
  if (isTimeOnlyText(value)) {
    const timeText = String(value).trim();
    return production?.isValid()
      ? `${production.format('YYYY-MM-DD')} ${timeText.length === 5 ? `${timeText}:00` : timeText}`
      : timeText;
  }
  const date = dayjs(value);
  if (isPlaceholderDateTime(date)) {
    return production?.isValid()
      ? `${production.format('YYYY-MM-DD')} ${date.format('HH:mm:ss')}`
      : date.format('HH:mm:ss');
  }
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

const normalizeDailyDateTimeText = (value: any, recordDate = todayText()) => {
  return normalizeBizDateTime(recordDate, value) || '';
};

const toCheckStatus = (status?: string): WorkPrepareRow['status'] => {
  if (status === 'CONFIRMED') return 'COMPLETED';
  if (status === 'RECORDED') return 'FILLED';
  return 'PENDING';
};

const toCheckType = (formCode?: string): CheckType => (formCode === 'ROUGH_CLEANING_CHECK' ? 'CLEANING' : 'STARTUP');

const mapDailyCheckRow = (row: any): WorkPrepareRow => ({
  canConfirm: !!row.canConfirm,
  canFill: !!row.canFill,
  canView: row.canView !== false,
  confirmer: row.confirmer || '-',
  confirmerTime: normalizeDailyDateTimeText(row.confirmerTime, row.recordDate) || '-',
  details: (row.details || row.presetDetails || []).map((item: any) => ({
    category: item.category || '',
    item: item.item || '',
    remark: item.remark || '',
    result: item.status === 'NG' ? 'NG' : 'OK',
    seq: item.itemSeq || 0,
    standard: item.standard || '',
    value: item.actualValue || '',
  })),
  formCode: row.formCode,
  formId: row.formId,
  key: toCheckType(row.formCode),
  name: row.name || row.formCode,
  recordId: row.recordId,
  recorder: row.recorder || '-',
  recorderTime: normalizeDailyDateTimeText(row.recorderTime, row.recordDate) || '-',
  result: row.result || '未检查',
  status: toCheckStatus(row.status),
  timing: row.timing || '',
});

const toPrintStatusText = (status?: string, passType?: GrindPass) => {
  if (passType === 'FIRST') return '-';
  return status === 'PRINTED' || status === '已打印' ? '已打印' : '未打印';
};

const toConfirmStatusText = (status?: string, passType?: GrindPass) => {
  if (passType === 'FIRST') return '-';
  return status === 'CONFIRMED' || status === '已确认' ? '已确认' : '未确认';
};

const mapReportRecord = (row: any, passName: string): WorkRecord => {
  const passType: GrindPass = passName === '第二次磨皮' ? 'SECOND' : 'FIRST';
  const recordRole = row.recordRole || (passType === 'FIRST' ? 'FIRST_ORIGINAL' : 'SECOND');
  const recordKey = `${recordRole}-${row.id || row.rowUid || row.productionBatchNo || row.motherBatchNo || row.startTime || 'tmp'}`;
  return {
    consumption: row.consumption,
    afterGrindingThickness: row.afterGrindingThickness || '',
    batchNo: row.productionBatchNo || row.motherBatchNo || '-',
    confirmStatus: toConfirmStatusText(row.confirmStatus, passType),
    confirmTime: normalizeBizDateTime(row.reportDate || todayText(), row.confirmTime) || '',
    currentGuideClothBatchNo: row.currentGuideClothBatchNo || '',
    currentSandpaperBatchNo: row.currentSandpaperBatchNo || '',
    defectCode: row.defectCode || '',
    endTime: normalizeBizDateTime(row.reportDate || todayText(), row.endTime) || '-',
    grindingMeters: toNumber(row.grindingMeters),
    grindingThickness: row.grindingThickness || '',
    guideClothBatchNo: row.guideClothBatchNo || '',
    id: row.id,
    firstAllocationId: row.firstAllocationId || (row.recordRole === 'FIRST_ALLOCATION' ? row.id : undefined),
    inspectionApplyTime: normalizeBizDateTime(row.reportDate || todayText(), row.inspectionApplyTime) || '',
    inspectionId: row.inspectionId,
    inspectionNo: row.inspectionNo || '',
    inspectionRejectReason: row.inspectionRejectReason || '',
    inspectionResult: row.inspectionResult || '',
    inspectionReturnTime: normalizeBizDateTime(row.reportDate || todayText(), row.inspectionReturnTime) || '',
    inspectionStatus: row.inspectionStatus || '',
    lineSpeed: row.lineSpeed || '',
    lossLength: toNumber(row.lossLength),
    meterCounter: row.meterCounter || '',
    middleProductGeneratedLength: row.middleProductGeneratedLength,
    middleProductRecordId: row.middleProductRecordId,
    middleProductRecordTime: normalizeBizDateTime(row.reportDate || todayText(), row.middleProductRecordTime) || '',
    middleProductRecorder: row.middleProductRecorder || '',
    middleProductStatus: row.middleProductStatus || '',
    motherBatchNo: row.motherBatchNo || row.parentProductionBatchNo || row.sourceProductionBatchNo || '',
    napSampleLength: toNumber(row.napSampleLength),
    researchConsumptionLength: passType === 'SECOND' ? toNumber(row.researchConsumptionLength) : 0,
    outputLength: toNumber(row.outputLength),
    parentBatchNo: row.motherBatchNo || row.parentProductionBatchNo || row.sourceProductionBatchNo || '',
    passName,
    passType,
    planNo: row.planNo || currentPlan.planNo,
    printCount: Number(row.printCount || 0),
    printStatus: toPrintStatusText(row.printStatus, passType),
    printTime: normalizeBizDateTime(row.reportDate || todayText(), row.lastPrintTime || row.printTime) || '',
    pressure: row.pressure || '',
    processLength: toNumber(row.processLength),
    productionBatchNo: row.productionBatchNo || '',
    qualityThickness: row.qualityThickness || '',
    qualityWidth: row.qualityWidth || '',
    recorder: row.operatorName || row.recorderName || row.recorder || '',
    reportDate: row.reportDate || '',
    recordKey,
    recordRole,
    rotationSpeed: row.rotationSpeed || '',
    sandpaperBatchNo: row.sandpaperBatchNo || '',
    segmentTimings: (row.segmentTimings || []).map((item: any) => ({
      endOperatorName: item.endOperatorName || '',
      endTime: normalizeBizDateTime(row.reportDate || todayText(), item.endTime) || '',
      firstDetailId: item.firstDetailId,
      id: item.id,
      passType: item.passType === 'SECOND' ? 'SECOND' : 'FIRST',
      secondDetailId: item.secondDetailId,
      segmentBatchNo: item.segmentBatchNo || '',
      segmentMark: item.segmentMark,
      startOperatorName: item.startOperatorName || '',
      startTime: normalizeBizDateTime(row.reportDate || todayText(), item.startTime) || '',
    })),
    segmentMark: row.segmentMark || '-',
    selfCheck: row.selfCheck || 'OK',
    startPosition: passType === 'FIRST'
      ? toNumber(row.remainStartMeter)
      : row.startPosition == null ? undefined : toNumber(row.startPosition),
    startTime: normalizeBizDateTime(row.reportDate || todayText(), row.startTime) || '-',
  };
};

const mapFirstAllocationRecord = (
  row: MesHcRoughGrindingConsoleApi.FirstAllocation,
): WorkRecord =>
  mapReportRecord(
    {
      ...row,
      firstAllocationId: row.id,
      id: row.firstDetailId,
      outputLength: row.confirmedLength,
      processLength: row.confirmedLength,
      recordRole: 'FIRST_ALLOCATION',
      remainStartMeter: row.startPosition,
      segmentMark: normalizeFirstAllocationSegmentMark(row.segmentMark) || '-',
    },
    '第一次磨皮',
  );

const resetPlanState = () => {
  currentTask.value = undefined;
  sourceBalances.value = [];
  firstAllocationRows.value = [];
  Object.assign(currentPlan, {
    availableLength: 0,
    batchNo: '',
    endTime: '',
    materialCode: '',
    modelCode: '',
    planId: undefined,
    planNo: '',
    planOperationId: undefined,
    previousEndTime: '',
    previousOperationName: '',
    previousOperationStatus: '',
    previousOutputLength: 0,
    recorderName: '',
    requirements: '',
    startTime: '',
    status: '',
  });
  materialScanCode.value = '';
  sideStockLength.value = 0;
  completionPromptKey.value = '';
  dailyFirstSummary.processLength = 0;
  dailyFirstSummary.lossLength = 0;
  dailyFirstSummary.outputLength = 0;
  dailyFirstSummary.napSampleLength = 0;
  dailyFirstSummary.researchConsumptionLength = 0;
  dailySecondSummary.processLength = 0;
  dailySecondSummary.lossLength = 0;
  dailySecondSummary.outputLength = 0;
  dailySecondSummary.napSampleLength = 0;
  dailySecondSummary.researchConsumptionLength = 0;
  workRecords.value = [];
  segmentTimingRows.value = [];
  roughDevProcessParamRecords.value = { FIRST: [], SECOND: [] };
  roughDevProcessParamLoadVersion.value = {
    FIRST: roughDevProcessParamLoadVersion.value.FIRST + 1,
    SECOND: roughDevProcessParamLoadVersion.value.SECOND + 1,
  };
  selectedWorkRecordKeys.value = [];
  abnormalPositionRows.value = [];
};

const resetConsumableState = () => {
  Object.assign(sandpaper, {
    batchNo: '-',
    id: undefined,
    lastReplaceTime: '-',
    lifeLength: 0,
    name: '砂纸',
    thresholdDays: DEFAULT_SANDPAPER_LIMIT_DAYS,
    thresholdLength: DEFAULT_SANDPAPER_LIMIT_LENGTH,
    thresholdUseCount: undefined,
    useCount: 0,
  });
  Object.assign(guideCloth, {
    batchNo: '-',
    id: undefined,
    lastReplaceTime: '-',
    lifeLength: 0,
    name: '导布',
    thresholdLength: 0,
    thresholdUseCount: DEFAULT_GUIDE_CLOTH_LIMIT_COUNT,
    useCount: 0,
  });
};

const readLastBoundEquipment = () => {
  if (typeof window === 'undefined') return undefined;
  const raw = window.localStorage.getItem(LAST_BOUND_EQUIPMENT_STORAGE_KEY);
  if (!raw) return undefined;
  try {
    const parsed = JSON.parse(raw);
    const id = Number(parsed?.id ?? parsed?.value ?? 0);
    return id
      ? {
          code: parsed?.code || '',
          id,
          name: parsed?.name || '',
          workCenterId: Number(parsed?.workCenterId || 0) || undefined,
          workCenterName: parsed?.workCenterName || parsed?.lineName || '',
          workStatus: parsed?.workStatus || '',
        }
      : undefined;
  } catch {
    const id = Number(raw);
    return id ? { code: '', id, name: '', workCenterName: '', workStatus: '' } : undefined;
  }
};

const saveLastBoundEquipment = (
  equipment?: {
    code?: string;
    id?: number;
    name?: string;
    value?: number;
    workCenterId?: number;
    workCenterName?: string;
    workStatus?: string;
  },
) => {
  const id = Number(equipment?.id ?? equipment?.value ?? 0);
  if (!id || typeof window === 'undefined') return;
  window.localStorage.setItem(
    LAST_BOUND_EQUIPMENT_STORAGE_KEY,
    JSON.stringify({
      code: equipment?.code || '',
      id,
      name: equipment?.name || '',
      workCenterId: equipment?.workCenterId,
      workCenterName: equipment?.workCenterName || '',
      workStatus: equipment?.workStatus || '',
    }),
  );
};

const findEquipmentOption = (equipmentId?: number) => {
  if (!equipmentId) return undefined;
  return equipmentOptions.value.find((item: any) => Number(item.value) === Number(equipmentId));
};

const applyEquipmentOption = (option: any, persist = true) => {
  if (!option?.value) return false;
  equipmentInfo.id = Number(option.value);
  equipmentInfo.code = option.code || '';
  equipmentInfo.name = option.name || '';
  equipmentInfo.workCenterId = option.workCenterId || equipmentInfo.workCenterId;
  equipmentInfo.lineName = option.workCenterName || equipmentInfo.lineName || '';
  equipmentInfo.workStatus = option.workStatus || equipmentInfo.workStatus || '';
  if (persist) saveLastBoundEquipment(option);
  return true;
};

const applyStoredEquipment = () => {
  const stored = readLastBoundEquipment();
  if (!stored?.id) return false;
  const option = findEquipmentOption(stored.id);
  if (option) return applyEquipmentOption(option);
  equipmentInfo.id = stored.id;
  equipmentInfo.code = stored.code || equipmentInfo.code || '';
  equipmentInfo.name = stored.name || equipmentInfo.name || '';
  equipmentInfo.workCenterId = stored.workCenterId || equipmentInfo.workCenterId;
  equipmentInfo.lineName = stored.workCenterName || equipmentInfo.lineName || '';
  equipmentInfo.workStatus = stored.workStatus || equipmentInfo.workStatus || '';
  return true;
};

const persistCurrentEquipment = () => {
  if (!equipmentInfo.id) return;
  saveLastBoundEquipment({
    code: equipmentInfo.code,
    id: equipmentInfo.id,
    name: equipmentInfo.name,
    workCenterId: equipmentInfo.workCenterId,
    workCenterName: equipmentInfo.lineName,
    workStatus: equipmentInfo.workStatus,
  });
};

const loadEquipmentOptions = async () => {
  const list = await getRoughGrindingConsoleEquipmentOptions({
    planOperationId: currentPlan.planOperationId || currentTask.value?.planOperationId || Number(route.query.planOperationId || 0) || undefined,
    workCenterId: currentTask.value?.workCenterId,
  });
  equipmentOptions.value = (list || []).map((item: any) => ({
    code: item.code,
    label: item.label || (item.code && item.name ? `${item.code} / ${item.name}` : item.code || item.name),
    name: item.name,
    value: item.value,
    workCenterId: item.workCenterId,
    workCenterName: item.workCenterName,
    workStatus: item.workStatus || item.status,
  }));
  const storedEquipment = readLastBoundEquipment();
  const preferredEquipmentId =
    currentTask.value?.equipmentId ||
    Number(route.query.equipmentId || 0) ||
    equipmentInfo.id ||
    storedEquipment?.id;
  const targetOption = findEquipmentOption(preferredEquipmentId);
  if (targetOption) {
    applyEquipmentOption(targetOption);
  } else if (!equipmentInfo.id && storedEquipment?.id) {
    applyStoredEquipment();
  }
};

const applyEquipmentFromAuth = (payload: any) => {
  if (!payload?.equipmentId) return;
  const option = findEquipmentOption(payload.equipmentId);
  equipmentInfo.id = payload.equipmentId;
  equipmentInfo.code = payload.equipmentCode || option?.code || equipmentInfo.code || '';
  equipmentInfo.name = payload.equipmentName || option?.name || equipmentInfo.name || '';
  equipmentInfo.workCenterId = option?.workCenterId || equipmentInfo.workCenterId;
  equipmentInfo.lineName = option?.workCenterName || payload.workCenterName || equipmentInfo.lineName || '';
  equipmentInfo.workStatus = option?.workStatus || equipmentInfo.workStatus || '';
  persistCurrentEquipment();
};

const applyBoardData = (board: any) => {
  const task = board?.task || {};
  if (currentPlan.planOperationId && task.planOperationId && currentPlan.planOperationId !== task.planOperationId) {
    completionPromptKey.value = '';
  }
  currentTask.value = task;
  sourceBalances.value = board?.sourceBalances || [];

  currentPlan.planId = task.planId;
  currentPlan.planOperationId = task.planOperationId;
  currentPlan.planNo = task.planNo || '';
  currentPlan.batchNo = task.productionBatchNo || task.parentProductionBatchNo || task.batchNo || '';
  currentPlan.materialCode = task.motherMaterialCode || task.materialCode || '';
  currentPlan.modelCode = task.motherModelCode || task.modelCode || '';
  currentPlan.previousEndTime = task.previousEndTime || task.wetEndTime || task.previousFinishTime || '';
  currentPlan.previousOperationName = task.previousOperationName || '';
  currentPlan.previousOperationStatus = normalizeOperationStatus(
    task.previousOperationStatus || task.previousOpStatus || task.previousStatus,
  );
  currentPlan.previousOutputLength = toNumber(task.previousGoodQty ?? task.motherLength);
  currentPlan.status = normalizeOperationStatus(task.status);
  currentPlan.startTime = task.startTime || '';
  currentPlan.endTime = task.endTime || '';
  currentPlan.recorderName = task.recorderName || '';
  currentPlan.availableLength = sourceBalances.value.reduce((total, item) => total + toNumber(item.availableLength), 0);
  currentPlan.requirements = task.requirements || task.executeRequirement || task.requirement || task.instructionText || '';
  sideStockLength.value = sourceBalances.value
    .filter((item) => item.sourceType === 'STOCK')
    .reduce((total, item) => total + toNumber(item.availableLength), 0);

  if (task.equipmentId) {
    const option = findEquipmentOption(task.equipmentId);
    equipmentInfo.id = task.equipmentId;
    equipmentInfo.code = task.equipmentCode || option?.code || equipmentInfo.code || '';
    equipmentInfo.name = task.equipmentName || option?.name || equipmentInfo.name || '';
    equipmentInfo.workCenterId = option?.workCenterId || task.workCenterId || equipmentInfo.workCenterId;
    equipmentInfo.lineName = option?.workCenterName || task.workCenterName || equipmentInfo.lineName || '';
    equipmentInfo.workStatus = option?.workStatus || equipmentInfo.workStatus || '';
    persistCurrentEquipment();
  } else if (!equipmentInfo.id) {
    applyStoredEquipment();
  }

  const backendChecks = (board?.dailyChecks || []).map(mapDailyCheckRow);
  dailyCheckLoaded.value = Array.isArray(board?.dailyChecks);
  const fallbackChecks = createDailyCheckFallbackRows();
  const checks = fallbackChecks.map((fallback) => backendChecks.find((item: WorkPrepareRow) => item.key === fallback.key) || fallback);
  dailyCheckRows.splice(0, dailyCheckRows.length, ...checks);

  resetConsumableState();
  const sandpaperState = (board?.consumables || []).find((item: any) => item.consumableType === 'SANDPAPER');
  if (sandpaperState) {
    sandpaper.id = sandpaperState.id;
    sandpaper.batchNo = sandpaperState.batchNo || '-';
    sandpaper.lastReplaceTime = normalizeBizDateTime(todayText(), sandpaperState.lastReplaceTime) || '-';
    sandpaper.lifeLength = toNumber(sandpaperState.usedLength);
    sandpaper.thresholdLength = toNumber(sandpaperState.limitLength) || DEFAULT_SANDPAPER_LIMIT_LENGTH;
    sandpaper.thresholdDays = DEFAULT_SANDPAPER_LIMIT_DAYS;
    sandpaper.thresholdUseCount = undefined;
    sandpaper.useCount = sandpaperState.useCount || 0;
  }
  const guideState = (board?.consumables || []).find((item: any) => item.consumableType === 'GUIDE_CLOTH');
  if (guideState) {
    guideCloth.id = guideState.id;
    guideCloth.batchNo = guideState.batchNo || '-';
    guideCloth.lastReplaceTime = normalizeBizDateTime(todayText(), guideState.lastReplaceTime) || '-';
    guideCloth.lifeLength = toNumber(guideState.usedLength);
    guideCloth.thresholdLength = 0;
    guideCloth.thresholdUseCount = guideState.limitCount || DEFAULT_GUIDE_CLOTH_LIMIT_COUNT;
    guideCloth.useCount = guideState.useCount || 0;
  }

  firstAllocationRows.value = Array.isArray(board?.firstAllocations)
    ? board.firstAllocations
    : [];
  const allocatedLength = firstAllocationRows.value.reduce(
    (total, item) => total + toNumber(item.confirmedLength),
    0,
  );
  dailyFirstSummary.processLength = isFirstAllocatedMode.value
    ? allocatedLength
    : toNumber(board?.firstProcessLength);
  dailyFirstSummary.lossLength = isFirstAllocatedMode.value
    ? 0
    : toNumber(board?.firstLossLength);
  dailyFirstSummary.outputLength = isFirstAllocatedMode.value
    ? allocatedLength
    : toNumber(board?.firstOutputLength);
  dailyFirstSummary.napSampleLength = isFirstAllocatedMode.value
    ? 0
    : toNumber(board?.firstNapSampleLength);
  dailySecondSummary.processLength = toNumber(board?.secondProcessLength);
  dailySecondSummary.lossLength = toNumber(board?.secondLossLength);
  dailySecondSummary.outputLength = toNumber(board?.secondOutputLength);
  dailySecondSummary.napSampleLength = toNumber(board?.secondNapSampleLength);
  dailySecondSummary.researchConsumptionLength = toNumber(board?.secondResearchConsumptionLength);

  segmentTimingRows.value = [
    ...(board?.firstSegmentTimings || []).map((item: any) => ({
      ...item,
      endTime: normalizeBizDateTime(todayText(), item.endTime) || '',
      passType: 'FIRST' as const,
      startTime: normalizeBizDateTime(todayText(), item.startTime) || '',
    })),
    ...(board?.secondSegmentTimings || []).map((item: any) => ({
      ...item,
      endTime: normalizeBizDateTime(todayText(), item.endTime) || '',
      passType: 'SECOND' as const,
      startTime: normalizeBizDateTime(todayText(), item.startTime) || '',
    })),
  ];
  workRecords.value = [
    ...(board?.secondReports || []).map((item: any) => mapReportRecord(item, '第二次磨皮')),
    ...firstAllocationRows.value.map(mapFirstAllocationRecord),
    ...(board?.firstReports || []).map((item: any) => mapReportRecord(item, '第一次磨皮')),
  ];
  syncSecondSegmentInspectionTasksFromBoard();
  selectedWorkRecordKeys.value = selectedWorkRecordKeys.value.filter((key) =>
    workRecords.value.some((record) => record.recordKey === key && record.passType === 'SECOND'),
  );
};

const loadBoard = async (query?: { equipmentId?: number; planId?: number; planOperationId?: number }) => {
  const planOperationId = query?.planOperationId || Number(route.query.planOperationId || 0);
  const equipmentId = query?.equipmentId || Number(route.query.equipmentId || 0) || equipmentInfo.id;
  if (!planOperationId && !equipmentId) return;
  boardLoading.value = true;
  dailyCheckLoaded.value = false;
  try {
    const board = await getRoughGrindingConsoleBoard({
      equipmentId: equipmentId || undefined,
      planId: query?.planId || Number(route.query.planId || 0) || undefined,
      planOperationId: planOperationId || undefined,
      recordDate: todayText(),
    });
    applyBoardData(board);
    await loadRoughFaiSummary();
    await loadRoughAbnormalPositionList();
  } finally {
    boardLoading.value = false;
  }
};

const loadInitialBoard = async () => {
  const routePlanId = Number(route.query.planId || 0) || undefined;
  const routePlanOperationId = Number(route.query.planOperationId || 0) || undefined;
  const routeEquipmentId = Number(route.query.equipmentId || 0) || undefined;
  if (routePlanOperationId) {
    await loadBoard({
      equipmentId: routeEquipmentId,
      planId: routePlanId,
      planOperationId: routePlanOperationId,
    });
    await loadEquipmentOptions();
    if (!equipmentInfo.id) {
      applyStoredEquipment();
    }
    if (!routeEquipmentId && equipmentInfo.id && !currentTask.value?.equipmentId) {
      await loadBoard({
        equipmentId: equipmentInfo.id,
        planId: routePlanId,
        planOperationId: routePlanOperationId,
      });
    }
    return;
  }
  await loadEquipmentOptions();
  if (equipmentInfo.id) {
    await loadBoard({
      equipmentId: equipmentInfo.id,
      planId: currentPlan.planId || routePlanId,
      planOperationId: currentPlan.planOperationId || routePlanOperationId,
    });
    return;
  }
  await loadBoard();
};

const applyTaskToBoard = async (task: any, successMessage = '已带出计划和母批信息', autoStart = false) => {
  if (!task?.planOperationId) {
    resetPlanState();
    scanPlanNo.value = '';
    lastAutoScannedPlanNo.value = '';
    pendingScannerPlanNo = '';
    AModal.warning({
      content: '未找到可用磨皮工单，请确认计划号或工序任务是否已下发。扫码计划框已清空，可重新扫码。',
      okText: '知道了',
      onOk: focusPlanScanInput,
      title: '未找到磨皮工单',
    });
    return;
  }
  const taskStatus = normalizeOperationStatus(task?.status);
  if (taskStatus !== 'FINISHED' && taskStatus !== 'COMPLETED') {
    const unlocked = await ensureRoughSampleCandidatesUnlocked(
      getRoughTaskSampleLockCandidates(task),
      autoStart ? '开工' : '加载计划',
    );
    if (!unlocked) return;
  }
  await loadBoard({
    equipmentId: task.equipmentId,
    planId: task.planId,
    planOperationId: task.planOperationId,
  });
  await loadEquipmentOptions();
  scanPlanNo.value = '';
  lastAutoScannedPlanNo.value = '';
  pendingScannerPlanNo = '';
  materialScanCode.value = currentPlan.batchNo;
  void successMessage;
  // message.success(successMessage);
  if (autoStart && canStartWorkOrder.value) {
    void requestOperationAuth('START');
  }
};

const consumePlanScan = async (silent = false) => {
  const planNo = syncPlanScanNo(scanPlanNo.value);
  if (!planNo) {
    if (!silent) {
      message.warning('请先输入或扫码计划号');
      focusPlanScanInput();
    }
    return;
  }
  scanPlanNo.value = planNo;
  const rows = await getRoughGrindingReportTaskList(buildTaskListQuery({ taskKeyword: planNo, taskStatus: 'ALL' }));
  const list = Array.isArray(rows) ? rows : [];
  const task = list.find((item: any) => String(item.planNo || '').trim() === planNo);
  if (!task) {
    resetPlanState();
    scanPlanNo.value = '';
    lastAutoScannedPlanNo.value = '';
    pendingScannerPlanNo = '';
    AModal.warning({
      content: `未找到计划号 ${planNo} 对应的磨皮工单，扫码计划框已清空，请确认后重新扫码。`,
      okText: '知道了',
      onOk: focusPlanScanInput,
      title: '扫码计划未找到',
    });
    return;
  }
  await applyTaskToBoard(task, silent ? '已自动识别计划并带出母批信息' : '已带出计划和母批信息');
};

const schedulePlanScan = (value: string) => {
  const planNo = syncPlanScanNo(value);
  const scannerInput = hasPlanScanDelimiter(value) || pendingScannerPlanNo === planNo;
  if (planScanTimer) {
    clearTimeout(planScanTimer);
    planScanTimer = null;
  }
  if (!planNo || (!scannerInput && planNo.length < PLAN_SCAN_MIN_LENGTH) || planNo === lastAutoScannedPlanNo.value) {
    return;
  }
  planScanTimer = setTimeout(() => {
    if (planNo !== normalizePlanScanNo(scanPlanNo.value)) {
      if (pendingScannerPlanNo === planNo) pendingScannerPlanNo = '';
      return;
    }
    if (pendingScannerPlanNo === planNo) pendingScannerPlanNo = '';
    void consumePlanScan(true);
  }, 180);
};

const buildTaskListQuery = (extra: Record<string, any> = {}) => ({
  equipmentCode: equipmentInfo.id ? undefined : equipmentInfo.code || undefined,
  equipmentId: equipmentInfo.id,
  ...extra,
});

const loadTaskList = async () => {
  taskListLoading.value = true;
  try {
    const rows = await getRoughGrindingReportTaskList(buildTaskListQuery({ taskStatus: 'ALL' }));
    taskListRows.value = Array.isArray(rows) ? rows : [];
    taskListPage.value = 1;
  } finally {
    taskListLoading.value = false;
  }
};

const resetTaskListFilters = () => {
  taskListFilters.planNo = '';
  taskListFilters.modelCode = '';
  taskListFilters.batchNo = '';
  taskListFilters.status = 'UNFINISHED';
  taskListPage.value = 1;
};

const handleTaskListSearch = async () => {
  taskListPage.value = 1;
  await loadTaskList();
};

const openTaskList = async () => {
  taskListVisible.value = true;
  await loadTaskList();
};

const selectTaskFromList = async (task: any) => {
  const taskStatus = normalizeOperationStatus(task?.status);
  const shouldAutoStart = taskStatus !== 'PAUSED' && taskStatus !== 'CANCELLED';
  await applyTaskToBoard(task, '已从工作列表开工并加载计划', shouldAutoStart);
  taskListVisible.value = false;
};

const getCheckStatusMeta = (status: WorkPrepareRow['status']) => {
  if (status === 'COMPLETED') return { color: 'green', text: '已确认' };
  if (status === 'FILLED') return { color: 'blue', text: '待确认' };
  return { color: 'orange', text: '未填写' };
};

const getBackendCheckStatusText = (record: Partial<WorkPrepareRow>) => {
  if (!dailyCheckLoaded.value) return '加载中';
  return getCheckStatusMeta(record.status || 'PENDING').text;
};

const canFillDailyCheck = (record: Partial<WorkPrepareRow>) => dailyCheckLoaded.value && record.canFill === true;
const canConfirmDailyCheck = (record: Partial<WorkPrepareRow>) => dailyCheckLoaded.value && record.canConfirm === true;
const canViewDailyCheck = (record: Partial<WorkPrepareRow>) => dailyCheckLoaded.value && record.canView !== false;

const focusBySelector = (selector: string) => {
  nextTick(() => {
    const target =
      document.querySelector<HTMLElement>(selector) ||
      document.querySelector<HTMLElement>(selector.replace(/\s+input$/, ''));
    target?.focus?.();
    if (target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement) {
      target.select?.();
    }
  });
};

const focusDailyCheckCell = (rowIndex: number, field: 'remark' | 'value') => {
  focusBySelector(`[data-daily-check-cell="${rowIndex}-${field}"] input`);
};

const moveDailyCheckFocus = (rowIndex: number, field: 'remark' | 'value', direction: 'down' | 'left' | 'next' | 'right' | 'up') => {
  const details = currentDailyCheckRow.value?.details || [];
  if (!details.length) return;
  let nextRow = rowIndex;
  let nextField: 'remark' | 'value' = field;
  if (direction === 'down') {
    nextRow = Math.min(details.length - 1, rowIndex + 1);
  } else if (direction === 'up') {
    nextRow = Math.max(0, rowIndex - 1);
  } else if (direction === 'right' || direction === 'next') {
    if (field === 'value') {
      nextField = 'remark';
    } else {
      nextRow = Math.min(details.length - 1, rowIndex + 1);
      nextField = 'value';
    }
  } else if (direction === 'left') {
    if (field === 'remark') {
      nextField = 'value';
    } else {
      nextRow = Math.max(0, rowIndex - 1);
      nextField = 'remark';
    }
  }
  focusDailyCheckCell(nextRow, nextField);
};

const handleDailyCheckInputKeydown = (event: KeyboardEvent, rowIndex: number, field: 'remark' | 'value') => {
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
  moveDailyCheckFocus(rowIndex, field, direction);
};

type MiddleProductEditField =
  | 'innerThickness'
  | 'lengthMeter'
  | 'outerThickness'
  | 'remark';

const middleProductEditFields: MiddleProductEditField[] = ROUGH_MIDDLE_PRODUCT_COLUMNS.map((column) => column.key);

const focusMiddleProductCell = (rowIndex: number, field: MiddleProductEditField) => {
  const targetPage = Math.floor(rowIndex / MIDDLE_PRODUCT_PAGE_SIZE) + 1;
  if (middleProductDetailPage.value !== targetPage) {
    middleProductDetailPage.value = targetPage;
  }
  const row = middleProductRows.value[rowIndex];
  if (!row) return;
  focusBySelector(`[data-middle-product-cell="${row.seq}-${field}"] input`);
};

const moveMiddleProductFocus = (
  rowIndex: number,
  field: MiddleProductEditField,
  direction: 'down' | 'left' | 'next' | 'right' | 'up',
) => {
  const rows = middleProductRows.value;
  if (!rows.length) return;
  let nextRow = rowIndex;
  let nextField = field;
  const fieldIndex = middleProductEditFields.indexOf(field);
  if (direction === 'down') {
    nextRow = Math.min(rows.length - 1, rowIndex + 1);
  } else if (direction === 'up') {
    nextRow = Math.max(0, rowIndex - 1);
  } else if (direction === 'right' || direction === 'next') {
    if (fieldIndex < middleProductEditFields.length - 1) {
      nextField = middleProductEditFields[fieldIndex + 1]!;
    } else {
      nextRow = Math.min(rows.length - 1, rowIndex + 1);
      nextField = middleProductEditFields[0]!;
    }
  } else if (direction === 'left') {
    if (fieldIndex > 0) {
      nextField = middleProductEditFields[fieldIndex - 1]!;
    } else {
      nextRow = Math.max(0, rowIndex - 1);
      nextField = middleProductEditFields[middleProductEditFields.length - 1]!;
    }
  }
  focusMiddleProductCell(nextRow, nextField);
};

const handleMiddleProductInputKeydown = (
  event: KeyboardEvent,
  row: any,
  field: MiddleProductEditField | string,
) => {
  if (middleProductDetailMode.value === 'view') return;
  if (!middleProductEditFields.includes(field as MiddleProductEditField)) return;
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
  const rowIndex = middleProductRows.value.findIndex((item) => item.key === row.key);
  if (rowIndex < 0) return;
  moveMiddleProductFocus(rowIndex, field as MiddleProductEditField, direction);
};

const addMiddleProductRow = () => {
  if (middleProductDetailMode.value === 'view') return;
  const maxSeq = Math.max(
    0,
    ...middleProductRows.value
      .map((row) => Number(row.seq || row.key || 0))
      .filter((value) => Number.isFinite(value)),
  );
  const batchNo = middleProductDetailHeader.productionBatchNo || reportForm.batchNo || currentPlan.batchNo || '';
  middleProductRows.value.push(createMiddleProductRow(maxSeq + 1, batchNo));
  middleProductDetailHeader.generatedLength = middleProductRows.value.length;
  nextTick(() => focusMiddleProductCell(middleProductRows.value.length - 1, 'lengthMeter'));
};

function handleMiddleProductHeadFieldUpdate(key: string, value: any) {
  if (key === 'processLength') {
    middleProductReportMeters.value = value;
    middleProductDetailHeader.processLength = Number(value) || 0;
    return;
  }
  if (key === 'widthMm') {
    middleProductMainWidth.value = value;
    middleProductDetailHeader.width = value;
    middleProductDetailHeader.widthMm = value;
    return;
  }
  (middleProductDetailHeader as Record<string, any>)[key] = value;
}

function handleMiddleProductSignatureFieldUpdate(key: string, value: any) {
  if (key === 'confirmer') {
    middleProductConfirmer.value = value;
    middleProductDetailHeader.confirmer = value;
    return;
  }
  (middleProductDetailHeader as Record<string, any>)[key] = value;
}

const focusProcessCell = (rowIndex: number) => {
  focusBySelector(`[data-process-row="${rowIndex}"] input`);
};

const focusProcessRemarkCell = (rowIndex: number) => {
  focusBySelector(`[data-process-remark-row="${rowIndex}"] input`);
};

const handleProcessInputKeydown = (event: KeyboardEvent, rowIndex: number, field: 'actualValue' | 'remark' = 'actualValue') => {
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
  const max = processItems.value.length - 1;
  let nextRow = rowIndex;
  let nextField = field;
  if (direction === 'next' || direction === 'right') {
    if (field === 'actualValue') {
      nextField = 'remark';
    } else {
      nextRow = Math.min(max, rowIndex + 1);
      nextField = 'actualValue';
    }
  } else if (direction === 'left') {
    if (field === 'remark') {
      nextField = 'actualValue';
    } else {
      nextRow = Math.max(0, rowIndex - 1);
      nextField = 'remark';
    }
  } else {
    nextRow = direction === 'up' ? Math.max(0, rowIndex - 1) : Math.min(max, rowIndex + 1);
  }
  if (nextField === 'remark') {
    focusProcessRemarkCell(nextRow);
  } else {
    focusProcessCell(nextRow);
  }
};

const focusReportField = (field: string) => {
  focusBySelector(`[data-report-field="${field}"] input`);
};

const focusPreferredReportField = () => {
  if (reportReadonly.value) {
    return;
  }
  if (reportStep.value !== 'REPORT') {
    focusReportField('batchNo');
    return;
  }
  if (reportForm.sandpaperChanged === 'YES') {
    focusReportField('sandpaperNewBatchNo');
    return;
  }
  if (reportForm.guideClothChanged === 'YES') {
    focusReportField('guideClothNewBatchNo');
    return;
  }
  focusReportField('processLength');
};

const openDailyCheck = (type: CheckType, mode: DailyCheckMode = 'edit') => {
  dailyCheckType.value = type;
  dailyCheckMode.value = mode;
  dailyCheckVisible.value = true;
};

const requestDailyCheckAuth = async (type: CheckType, mode: DailyCheckMode) => {
  if (mode === 'view') {
    openDailyCheck(type, mode);
    return;
  }
  const row = dailyCheckRows.find((item) => item.key === type);
  if (mode === 'edit' && (!row || !canFillDailyCheck(row))) {
    message.warning('后台状态不允许填写当前记录');
    return;
  }
  if (mode === 'confirm' && (!row || !canConfirmDailyCheck(row))) {
    message.warning('后台状态不允许确认当前记录，请先填写保存');
    return;
  }
  openDailyCheck(type, mode);
};

const handleDailyCheckAuthSuccess = async (payload: any) => {
  const pending = pendingDailyCheckAction.value;
  operatorId.value = payload?.userId || payload?.id || operatorId.value;
  operatorName.value = payload?.empName || payload?.username || operatorName.value;
  operatorNo.value = payload?.empNo || payload?.username || operatorNo.value;
  applyEquipmentFromAuth(payload);
  pendingDailyCheckAction.value = undefined;
  if (pending) {
    await submitDailyCheck(pending, payload);
    return;
  }
  if (payload?.equipmentId) {
    await loadBoard({
      equipmentId: payload.equipmentId,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
  }
};

const handleDailyCheckAuthCancel = () => {
  pendingDailyCheckAction.value = undefined;
};

const formatOperationStatusText = (status?: string) => {
  const normalized = normalizeOperationStatus(status);
  if (normalized === 'RUNNING') return '生产中';
  if (normalized === 'FINISHED') return '已完工';
  if (normalized === 'RELEASED') return '待开工';
  if (normalized === 'PAUSED') return '已暂停';
  if (normalized === 'CANCELLED') return '已作废';
  return normalized || '-';
};

const normalizeTaskListStatusValue = (status?: string) => {
  const normalized = normalizeOperationStatus(status);
  if (normalized === 'RUNNING') return 'IN_PROGRESS';
  if (normalized === 'FINISHED') return 'COMPLETED';
  if (normalized === 'RELEASED') return 'PENDING';
  if (normalized === 'PAUSED') return 'PAUSED';
  if (normalized === 'CANCELLED') return 'CANCELLED';
  if (status === 'IN_PROGRESS' || status === 'COMPLETED' || status === 'PENDING') return status;
  return status || '';
};

const getTaskListStatusMeta = (status?: string) => {
  const normalized = normalizeTaskListStatusValue(status);
  if (normalized === 'COMPLETED') return { color: 'success', text: '已完工' };
  if (normalized === 'IN_PROGRESS') return { color: 'processing', text: '生产中' };
  if (normalized === 'PENDING') return { color: 'default', text: '待开工' };
  if (normalized === 'PAUSED') return { color: 'warning', text: '已暂停' };
  if (normalized === 'CANCELLED') return { color: 'red', text: '已作废' };
  return { color: 'default', text: formatOperationStatusText(status) };
};

function getEquipmentWorkStatusMeta(status?: string) {
  const normalized = String(status || '').toUpperCase();
  if (['PRODUCING', 'RUNNING', 'IN_PROGRESS'].includes(normalized)) return { color: 'blue', text: '生产中' };
  if (['MAINTENANCE', 'REPAIR'].includes(normalized)) return { color: 'orange', text: '维护中' };
  if (['DISABLED', 'STOPPED'].includes(normalized)) return { color: 'red', text: '停用' };
  if (['IDLE', 'FREE'].includes(normalized)) return { color: 'green', text: '空闲' };
  return { color: 'default', text: status || '未记录' };
}

const getTaskListBatchText = (record: any) =>
  record?.batchNo ||
  record?.motherBatchNo ||
  record?.productionBatchNo ||
  record?.sourceProductionBatchNo ||
  record?.parentProductionBatchNo ||
  '-';

const containsTaskFilterText = (values: unknown[], keyword: string) => {
  const text = String(keyword || '').trim().toLowerCase();
  if (!text) return true;
  return values.some((value) => String(value ?? '').toLowerCase().includes(text));
};

const taskListFilteredRows = computed(() => {
  const statusFilter = taskListFilters.status;
  return taskListRows.value.filter((row) => {
    if (toNumber(row?.motherLength) <= 0) return false;
    const status = normalizeTaskListStatusValue(row?.status);
    if (statusFilter === 'UNFINISHED' && ['COMPLETED', 'CANCELLED'].includes(status)) return false;
    if (statusFilter && statusFilter !== 'ALL' && statusFilter !== 'UNFINISHED' && status !== statusFilter) return false;
    if (!containsTaskFilterText([row?.planNo, row?.id, row?.erpOrderNo], taskListFilters.planNo)) return false;
    if (
      !containsTaskFilterText(
        [row?.motherModelCode, row?.motherModelName, row?.modelCode, row?.modelName, row?.productModel],
        taskListFilters.modelCode,
      )
    ) {
      return false;
    }
    return containsTaskFilterText(
      [
        row?.batchNo,
        row?.motherBatchNo,
        row?.productionBatchNo,
        row?.sourceProductionBatchNo,
        row?.parentProductionBatchNo,
      ],
      taskListFilters.batchNo,
    );
  });
});

const taskListPagedRows = computed(() => {
  const start = (taskListPage.value - 1) * taskListPageSize.value;
  return taskListFilteredRows.value.slice(start, start + taskListPageSize.value);
});

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

const getFirstInspectionMeta = (status?: FirstInspectionStatus) => {
  if (status === 'WAITING') return { color: 'default', stampClass: 'waiting', stampText: '待反馈', text: '等待质检中' };
  if (status === 'PENDING') return { color: 'default', stampClass: 'waiting', stampText: '待反馈', text: '已提交待检' };
  if (status === 'OK') return { color: 'success', stampClass: 'ok', stampText: '首检合格', text: '检验通过' };
  if (status === 'NG') return { color: 'error', stampClass: 'ng', stampText: '首检异常', text: '首检异常' };
  return { color: 'default', stampClass: 'empty', stampText: '未送检', text: '未送检' };
};

const mapRoughFaiSummaryStatus = (summary: MesHcRoughGrindingConsoleApi.FaiSummary): FirstInspectionStatus => {
  if (summary.faiStatus === 'COMPLETED' && summary.faiJudgment === 'OK') return 'OK';
  if (summary.faiStatus === 'REJECTED' || summary.faiJudgment === 'NG') return 'NG';
  if (summary.faiStatus === 'PENDING') return 'PENDING';
  return 'WAITING';
};

const isFirstInspectionReapplyAllowed = (task?: FirstInspectionTask) =>
  task?.faiStatus === 'REJECTED' || task?.faiStatus === 'CANCELED';

const getTopFirstInspectionActionText = () => {
  const task = currentFirstInspectionTask.value;
  if (!task) return '首样送检';
  return isFirstInspectionReapplyAllowed(task) ? '重新提交' : '首检报告';
};

const handleTopFirstInspectionAction = () => {
  const task = currentFirstInspectionTask.value;
  if (task && !isFirstInspectionReapplyAllowed(task)) {
    viewFirstInspectionDetail();
    return;
  }
  submitFirstSampleInspection();
};

const applyRoughFaiSummary = (summary?: MesHcRoughGrindingConsoleApi.FaiSummary | null) => {
  if (!currentPlan.planNo) return;
  const isCurrentFirstSample = (task: FirstInspectionTask) =>
    task.planNo === currentPlan.planNo && (!task.kind || task.kind === 'FIRST_SAMPLE');
  if (!summary?.faiId) {
    firstInspectionTasks.value = firstInspectionTasks.value.filter((task) => !isCurrentFirstSample(task));
    return;
  }
  const status = mapRoughFaiSummaryStatus(summary);
  const task: FirstInspectionTask = {
    allowReportSubmit: summary.allowReportSubmit,
    feedbackRemark: summary.faiRejectReason,
    feedbackTime: summary.faiReturnTime,
    faiId: summary.faiId,
    faiJudgment: summary.faiJudgment,
    faiNo: summary.faiNo,
    faiStandardNo: summary.faiStandardNo,
    faiStandardVersion: summary.faiStandardVersion,
    faiStatus: summary.faiStatus,
    id: summary.faiNo || `FAI-${currentPlan.planNo}`,
    kind: 'FIRST_SAMPLE',
    planNo: currentPlan.planNo,
    pushedAt: summary.faiApplyTime || buildNowText(),
    result: status === 'OK' ? 'OK' : status === 'NG' ? 'NG' : undefined,
    status,
  };
  firstInspectionTasks.value = [task, ...firstInspectionTasks.value.filter((item) => !isCurrentFirstSample(item))];
};

const loadRoughFaiSummary = async () => {
  if (!currentPlan.planId || !currentPlan.planOperationId) return undefined;
  const summary = await getRoughGrindingConsoleFaiSummary({
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
  });
  applyRoughFaiSummary(summary);
  return summary;
};

const getAbnormalQueryMotherBatchNo = () =>
  String(currentPlan.batchNo || currentTask.value?.parentProductionBatchNo || currentTask.value?.batchNo || '').trim();

const loadRoughAbnormalPositionList = async () => {
  const motherBatchNo = getAbnormalQueryMotherBatchNo();
  if (!motherBatchNo) {
    abnormalPositionRows.value = [];
    return;
  }
  abnormalPositionLoading.value = true;
  try {
    abnormalPositionRows.value = await getRoughGrindingConsoleAbnormalPositionList({ motherBatchNo });
  } finally {
    abnormalPositionLoading.value = false;
  }
};

const formatStampDateTime = (value?: string) => {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('MM-DD HH:mm') : value;
};

const getFirstInspectionStampTime = (task?: FirstInspectionTask) => {
  if (!task) return '点击送检';
  if (isFirstInspectionReapplyAllowed(task)) return '点击重提';
  const isFeedback = task.status === 'OK' || task.status === 'NG';
  return `${isFeedback ? '反馈' : '推送'} ${formatStampDateTime(isFeedback ? task.feedbackTime : task.pushedAt)}`;
};

const getSegmentLabel = (segmentMark?: string) =>
  secondSegmentOptions.find((item) => item.value === normalizeSegmentMark(segmentMark))?.label || '不分段';

const getSegmentTiming = (passType: GrindPass, segmentMark: string) =>
  segmentTimingRows.value.find(
    (item) => item.passType === passType && toTimingSegmentMark(item.segmentMark) === toTimingSegmentMark(segmentMark),
  );

const getSegmentQtime = (passType: GrindPass, segmentMark: string) =>
  getSegmentTiming(passType, segmentMark)?.qtime;

const getSegmentQtimeText = (passType: GrindPass, segmentMark: string) =>
  roughBackendQtimeDurationText(getSegmentQtime(passType, segmentMark));

const getSegmentQtimeLevelClass = (passType: GrindPass, segmentMark: string) =>
  resolveRoughBackendQtimeLevelClass(getSegmentQtime(passType, segmentMark));

const isSegmentTimingStamping = (passType: GrindPass, segmentMark: string, action: SegmentTimingAction) =>
  segmentTimingStampingKey.value === `${passType}-${toTimingSegmentMark(segmentMark)}-${action}`;

const validateSegmentTimingAction = (passType: GrindPass, segmentMark: string, action: SegmentTimingAction) => {
  const normalizedSegmentMark = toTimingSegmentMark(segmentMark);
  if (!timingSegmentOptions.some((segment) => segment.value === normalizedSegmentMark)) {
    message.warning('仅支持记录 P/Q/R/S 段或不分段的开工和完工时间');
    return undefined;
  }
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码带出真实磨皮计划');
    return undefined;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('当前磨皮工序尚未开工');
    return undefined;
  }
  const timing = getSegmentTiming(passType, normalizedSegmentMark);
  const timingDisplayName = `${getGrindingPassName(passType)}${getSegmentLabel(normalizedSegmentMark)}`;
  if (action === 'START' && timing?.startTime) {
    message.warning(`${timingDisplayName}已记录开工时间`);
    return undefined;
  }
  if (action === 'END' && (!timing?.startTime || timing.endTime)) {
    message.warning(timing?.endTime
      ? `${timingDisplayName}已记录完工时间`
      : `请先记录${timingDisplayName}开工时间`);
    return undefined;
  }
  return normalizedSegmentMark;
};

const requestSegmentTimingStamp = (passType: GrindPass, segmentMark: string, action: SegmentTimingAction) => {
  const normalizedSegmentMark = validateSegmentTimingAction(passType, segmentMark, action);
  if (!normalizedSegmentMark) return;
  pendingSegmentTimingAction.value = { action, passType, segmentMark: normalizedSegmentMark };
  segmentTimingAuthActionName.value = `${getGrindingPassName(passType)}${getSegmentLabel(normalizedSegmentMark)}${action === 'START' ? '开工' : '完工'}时间确认`;
  segmentTimingAuthVisible.value = true;
};

const stampSegmentTiming = async (
  passType: GrindPass,
  segmentMark: TimingSegmentMark,
  action: SegmentTimingAction,
  authPayload: any,
) => {
  const normalizedSegmentMark = validateSegmentTimingAction(passType, segmentMark, action);
  if (!normalizedSegmentMark) return;
  const operatorId = authPayload?.userId ?? authPayload?.id;
  const operatorName = authPayload?.empName || authPayload?.nickname || authPayload?.username || authPayload?.empNo;
  if (!operatorId || !operatorName) {
    message.warning('请先完成操作人员身份确认后再记录时间');
    return;
  }
  const key = `${passType}-${normalizedSegmentMark}-${action}`;
  segmentTimingStampingKey.value = key;
  try {
    await stampRoughGrindingConsoleSegmentTiming({
      action,
      operatorId,
      operatorName,
      passType,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      segmentMark: normalizedSegmentMark,
    });
    await loadBoard({
      equipmentId: equipmentInfo.id || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    message.success(`${getGrindingPassName(passType)}${getSegmentLabel(normalizedSegmentMark)}${action === 'START' ? '开工' : '完工'}时间已记录`);
  } finally {
    if (segmentTimingStampingKey.value === key) {
      segmentTimingStampingKey.value = '';
    }
  }
};

const handleSegmentTimingAuthSuccess = async (payload: any) => {
  const pending = pendingSegmentTimingAction.value;
  pendingSegmentTimingAction.value = undefined;
  if (!pending) return;
  await stampSegmentTiming(pending.passType, pending.segmentMark, pending.action, payload);
};

const handleSegmentTimingAuthCancel = () => {
  pendingSegmentTimingAction.value = undefined;
};

const openFirstAllocationVisualAction = async (segmentMark = '') => {
  const allocation = getFirstAllocationBySegment(segmentMark);
  if (allocation && toFirstAllocationSegmentMark(segmentMark) !== 'NONE') {
    openSubmittedReportDetail(mapFirstAllocationRecord(allocation));
    return;
  }
  focusedWorkRecordKey.value = undefined;
  await openReport('FIRST', normalizeFirstAllocationSegmentMark(segmentMark));
};

const getAbnormalProcessStageMeta = (processStage?: string) => {
  if (processStage === 'WET' || !processStage) return { color: 'blue', text: '湿法' };
  if (processStage === 'FIRST_GRINDING') return { color: 'processing', text: '一次磨皮' };
  if (processStage === 'SECOND_GRINDING') return { color: 'purple', text: '二次磨皮' };
  return { color: 'default', text: processStage };
};

const getSecondSegmentSampleTask = (segmentMark?: string) => {
  const segment = normalizeSegmentMark(segmentMark);
  return firstInspectionTasks.value.find(
    (task) =>
      task.kind === 'SECOND_SEGMENT_SAMPLE' &&
      task.planNo === currentPlan.planNo &&
      normalizeSegmentMark(task.segmentMark) === segment,
  );
};

const getSecondSegmentLatestRecord = (segmentMark?: string) => {
  const segment = normalizeSegmentMark(segmentMark);
  return [...secondWorkRecords.value]
    .reverse()
    .find((record) => normalizeSegmentMark(record.segmentMark) === segment);
};

const getSecondSegmentSampleLockObjectNo = (segmentMark?: string) => {
  const latestRecord = getSecondSegmentLatestRecord(segmentMark);
  const latestBatchNo = latestRecord?.productionBatchNo || latestRecord?.batchNo || '';
  if (latestBatchNo) return latestBatchNo;
  const motherBatchNo = String(currentPlan.batchNo || '').trim();
  const segment = normalizeSegmentMark(segmentMark);
  if (!motherBatchNo || !segment) return '';
  return buildSecondGrindingBatchNo(motherBatchNo, segment);
};

function normalizeRoughSampleLockBatchNo(value?: string | null) {
  return String(value || '').trim();
}

function pushRoughSampleLockCandidate(
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

function getRoughSampleLockMotherBatchNo(fallbackBatchNo?: string) {
  return normalizeRoughSampleLockBatchNo(
    currentPlan.batchNo ||
      firstReportSourceBatchNo.value ||
      currentTask.value?.sourceProductionBatchNo ||
      currentTask.value?.batchNo ||
      fallbackBatchNo,
  );
}

function removeRoughTrailingSegmentMark(batchNo?: string) {
  const normalized = normalizeRoughSampleLockBatchNo(batchNo);
  return /[PQRS]$/i.test(normalized) ? normalized.slice(0, -1) : '';
}

function getRoughWetMotherObjectNos(motherBatchNo?: string, segmentBatchNo?: string) {
  return [
    normalizeRoughSampleLockBatchNo(motherBatchNo),
    normalizeRoughSampleLockBatchNo(segmentBatchNo),
    removeRoughTrailingSegmentMark(motherBatchNo),
    removeRoughTrailingSegmentMark(segmentBatchNo),
  ].filter(Boolean);
}

function getRoughSampleLockCandidates(pass: GrindPass = activePass.value, segmentMark?: string, motherBatchNo?: string) {
  const candidates: SampleLockCandidate[] = [];
  const seen = new Set<string>();
  const resolvedMotherBatchNo = normalizeRoughSampleLockBatchNo(motherBatchNo) || getRoughSampleLockMotherBatchNo();
  const resolvedSegmentBatchNo = pass === 'SECOND' ? getSecondSegmentSampleLockObjectNo(segmentMark) : '';
  getRoughWetMotherObjectNos(resolvedMotherBatchNo, resolvedSegmentBatchNo).forEach((objectNo) => {
    pushRoughSampleLockCandidate(candidates, seen, {
      qualificationObjectNo: resolvedSegmentBatchNo || resolvedMotherBatchNo,
      objectLabel: '母卷',
      objectNo,
      objectType: 'MOTHER_ROLL',
      processName: '湿法',
      sourceProcessCode: 'WET',
    });
  });
  if (pass === 'SECOND') {
    pushRoughSampleLockCandidate(candidates, seen, {
      objectLabel: '分段',
      objectNo: resolvedSegmentBatchNo,
      objectType: 'SEGMENT',
      processName: '磨皮',
      sourceProcessCode: 'ROUGH_GRINDING',
    });
  }
  return candidates;
}

function getRoughTaskSampleLockCandidates(task: any) {
  const motherBatchNo = normalizeRoughSampleLockBatchNo(
    task?.batchNo || task?.productionBatchNo || task?.sourceProductionBatchNo || task?.sourceBatchNo,
  );
  return getRoughSampleLockCandidates('FIRST', undefined, motherBatchNo);
}

async function ensureRoughSampleCandidatesUnlocked(candidates: SampleLockCandidate[], actionName = '报工') {
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
        `当前${objectLabel} ${lock.objectNo || candidate.objectNo} 因 ${lock.abnormalFeedbackTime || '-'}，${processName} 留样送检NG异常，锁定不允许继续${actionName}，等待复检确认后继续。`,
      title: '留样异常锁定',
    });
    return false;
  }
  return true;
}

async function ensureRoughSampleAbnormalUnlocked(
  segmentMark?: string,
  actionName = '报工',
  pass: GrindPass = activePass.value,
) {
  return ensureRoughSampleCandidatesUnlocked(getRoughSampleLockCandidates(pass, segmentMark), actionName);
}

const requireSecondSegmentLatestRecord = (segmentMark?: string) => {
  const record = getSecondSegmentLatestRecord(segmentMark);
  if (!record?.id && !(record?.productionBatchNo || record?.batchNo)) {
    message.warning(`请先完成${getSegmentLabel(segmentMark)}第二次磨皮报工`);
    return undefined;
  }
  return record;
};

const applySecondSegmentSampleLengthToDraft = (segmentMark: string | undefined, sampleLength: number) => {
  const segment = normalizeSegmentMark(segmentMark);
  const target = secondWorkRecords.value.find((record) => normalizeSegmentMark(record.segmentMark) === segment);
  if (target) {
    target.outputLength = Number(Math.max(
      Number(target.outputLength || 0) + Number(target.napSampleLength || 0) - sampleLength,
      0,
    ).toFixed(3));
    target.napSampleLength = sampleLength;
  }
  if (activePass.value === 'SECOND' && normalizeSegmentMark(reportForm.segmentMark) === segment) {
    reportForm.napSampleLength = sampleLength;
  }
};

const getSecondSegmentSampleMaxLength = (record?: WorkRecord) =>
  Number(Math.max(Number(record?.outputLength || 0) + Number(record?.napSampleLength || 0), 0).toFixed(3));

const requestSecondSegmentSampleLengthConfirm = (record: WorkRecord, segmentLabel: string) =>
  new Promise<number | undefined>((resolve) => {
    const maxSampleLength = getSecondSegmentSampleMaxLength(record);
    let sampleLengthValue = Number(record.napSampleLength || 0) || undefined;
    AModal.confirm({
      cancelText: '取消',
      content: h('div', { class: 'second-sample-length-confirm' }, [
        h(
          'p',
          `${segmentLabel}留样送检前请确认本次留样米数；确认后会反写到该报工记录。`,
        ),
        h('p', `本段可留样上限：${formatNumber(maxSampleLength)}`),
        h(InputNumber, {
          addonAfter: 'm',
          defaultValue: sampleLengthValue,
          max: maxSampleLength > 0 ? maxSampleLength : undefined,
          min: 0.001,
          placeholder: '请输入留样送检米数',
          precision: 3,
          step: 0.1,
          style: { width: '100%' },
          onChange: (value: any) => {
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
            content: '请填写大于 0 的留样送检米数。',
            title: '留样米数不能为空',
          });
          return Promise.reject(new Error('留样米数不能为空'));
        }
        if (maxSampleLength > 0 && sampleLength - maxSampleLength > SECOND_RANGE_EPS) {
          AModal.warning({
            content: `留样送检米数不能大于本段可留样上限 ${formatNumber(maxSampleLength)}。`,
            title: '留样米数超出范围',
          });
          return Promise.reject(new Error('留样米数超出范围'));
        }
        resolve(Number(sampleLength.toFixed(3)));
      },
      title: '确认留样米数',
    });
  });

const mapSecondSegmentInspectionStatus = (
  summary: MesHcRoughGrindingConsoleApi.SecondSegmentInspectionSummary,
): FirstInspectionStatus => {
  if (summary.inspectionStatus === 'COMPLETED' && summary.inspectionResult === 'OK') return 'OK';
  if (
    summary.inspectionStatus === 'REJECTED' ||
    summary.inspectionStatus === 'CANCELED' ||
    summary.inspectionResult === 'NG'
  ) {
    return 'NG';
  }
  if (summary.inspectionStatus === 'PENDING') return 'PENDING';
  return summary.inspectionId ? 'WAITING' : 'WAITING';
};

const isSecondSegmentInspectionReapplyAllowed = (task?: FirstInspectionTask) =>
  task?.faiStatus === 'REJECTED' || task?.faiStatus === 'CANCELED' || task?.faiJudgment === 'NG';

const getSecondSegmentSampleButtonStatus = (segmentMark?: string) => {
  const task = getSecondSegmentSampleTask(segmentMark);
  if (!task) return 'empty';
  if (isSecondSegmentInspectionReapplyAllowed(task)) return 'ng';
  if (task.status === 'OK') return 'ok';
  if (task.status === 'NG') return 'ng';
  return 'waiting';
};

const getSecondSegmentSampleButtonText = (segmentMark?: string) => {
  const task = getSecondSegmentSampleTask(segmentMark);
  if (!task) return '留样送检';
  if (isSecondSegmentInspectionReapplyAllowed(task)) return '异常/重提';
  if (task.status === 'OK') return '检验合格';
  if (task.status === 'NG') return '检验异常';
  if (task.status === 'PENDING') return '已送检';
  return '待反馈';
};

const getSecondInspectionRecordStatusMeta = (task: FirstInspectionTask) => {
  if (isSecondSegmentInspectionReapplyAllowed(task) || task.status === 'NG') {
    return { color: 'error', text: '异常退回' };
  }
  if (task.status === 'OK') return { color: 'success', text: '已反馈' };
  if (task.status === 'PENDING') return { color: 'processing', text: '已送检' };
  if (task.status === 'WAITING') return { color: 'warning', text: '待反馈' };
  return { color: 'default', text: '未送检' };
};

const getSecondInspectionResultMeta = (task: FirstInspectionTask) => {
  if (task.result === 'OK' || task.faiJudgment === 'OK' || task.status === 'OK') {
    return { color: 'success', text: '合格' };
  }
  if (task.result === 'NG' || task.faiJudgment === 'NG' || task.status === 'NG') {
    return { color: 'error', text: '不合格' };
  }
  return { color: 'default', text: '-' };
};

const applySecondSegmentInspectionSummary = (
  summary?: MesHcRoughGrindingConsoleApi.SecondSegmentInspectionSummary | null,
) => {
  if (!summary?.secondDetailId || !currentPlan.planNo) return undefined;
  const segment = normalizeSegmentMark(summary.segmentMark);
  const segmentLabel = getSegmentLabel(segment);
  const matchedRecord = secondWorkRecords.value.find((record) => record.id === summary.secondDetailId);
  if (matchedRecord) {
    matchedRecord.inspectionId = summary.inspectionId;
    matchedRecord.inspectionNo = summary.inspectionNo || '';
    matchedRecord.inspectionStatus = summary.inspectionStatus || '';
    matchedRecord.inspectionResult = summary.inspectionResult || '';
    matchedRecord.inspectionApplyTime = summary.inspectionApplyTime || '';
    matchedRecord.inspectionReturnTime = summary.inspectionReturnTime || '';
    matchedRecord.inspectionRejectReason = summary.inspectionRejectReason || '';
    if (summary.sampleLength !== undefined && summary.sampleLength !== null) {
      matchedRecord.outputLength = Number(Math.max(
        Number(matchedRecord.outputLength || 0) + Number(matchedRecord.napSampleLength || 0) -
          Number(summary.sampleLength || 0),
        0,
      ).toFixed(3));
      matchedRecord.napSampleLength = Number(summary.sampleLength || 0);
    }
  }
  const hasInspection = !!summary.inspectionId || !!summary.inspectionNo || !!summary.inspectionStatus;
  const isCurrentSecondSegment = (task: FirstInspectionTask) =>
    task.kind === 'SECOND_SEGMENT_SAMPLE' &&
    task.planNo === currentPlan.planNo &&
    task.secondDetailId === summary.secondDetailId;
  firstInspectionTasks.value = firstInspectionTasks.value.filter((task) => !isCurrentSecondSegment(task));
  if (!hasInspection) return undefined;
  const status = mapSecondSegmentInspectionStatus(summary);
  const task: FirstInspectionTask = {
    feedbackRemark: summary.inspectionRejectReason,
    feedbackTime: summary.inspectionReturnTime,
    faiId: summary.inspectionId,
    faiJudgment: summary.inspectionResult,
    faiNo: summary.inspectionNo,
    faiStatus: summary.inspectionStatus,
    id: summary.inspectionNo || `SECOND-${summary.secondDetailId}`,
    kind: 'SECOND_SEGMENT_SAMPLE',
    motherBatchNo: summary.motherBatchNo,
    parentBatchNo: summary.parentBatchNo || summary.motherBatchNo,
    planNo: currentPlan.planNo,
    productionBatchNo: summary.productionBatchNo || summary.productBatchNo,
    pushedAt: summary.inspectionApplyTime || buildNowText(),
    result: status === 'OK' ? 'OK' : status === 'NG' ? 'NG' : undefined,
    sampleLength: Number(summary.sampleLength || matchedRecord?.napSampleLength || 0) || undefined,
    sampleType: summary.sampleType,
    sampleTypeName: summary.sampleTypeName || '二磨分段留样',
    secondDetailId: summary.secondDetailId,
    segmentLabel,
    segmentMark: segment,
    sourceModule: summary.sourceModule,
    status,
  };
  firstInspectionTasks.value = [task, ...firstInspectionTasks.value];
  return task;
};

const syncSecondSegmentInspectionTasksFromBoard = () => {
  if (!currentPlan.planNo) return;
  secondWorkRecords.value.forEach((record) => {
    if (!record.id || (!record.inspectionId && !record.inspectionNo && !record.inspectionStatus)) return;
    applySecondSegmentInspectionSummary({
      inspectionApplyTime: record.inspectionApplyTime,
      inspectionId: record.inspectionId,
      inspectionNo: record.inspectionNo,
      inspectionRejectReason: record.inspectionRejectReason,
      inspectionResult: record.inspectionResult,
      inspectionReturnTime: record.inspectionReturnTime,
      inspectionStatus: record.inspectionStatus,
      motherBatchNo: record.motherBatchNo || record.parentBatchNo,
      parentBatchNo: record.parentBatchNo || record.motherBatchNo,
      productBatchNo: record.productionBatchNo,
      productionBatchNo: record.productionBatchNo,
      sampleLength: record.napSampleLength,
      sampleType: 'SECOND_SEGMENT_SAMPLE',
      sampleTypeName: '二磨分段留样',
      secondDetailId: record.id,
      segmentMark: normalizeSegmentMark(record.segmentMark),
      sourceModule: 'ROUGH_GRINDING_SECOND_SEGMENT',
    });
  });
};

const buildSecondSegmentInspectionTransferTicketPayload = async (task: FirstInspectionTask) => {
  const segment = normalizeSegmentMark(task.segmentMark);
  const segmentLabel = task.segmentLabel || getSegmentLabel(segment);
  const relatedRecord = getSecondSegmentLatestRecord(segment);
  const inspectionNo = task.faiNo || task.id;
  const motherBatchNo =
    task.motherBatchNo ||
    task.parentBatchNo ||
    relatedRecord?.motherBatchNo ||
    relatedRecord?.parentBatchNo ||
    currentPlan.batchNo;
  const productionBatchNo =
    task.productionBatchNo ||
    relatedRecord?.productionBatchNo ||
    relatedRecord?.batchNo ||
    buildSecondGrindingBatchNo(currentPlan.batchNo, segment);
  const sampleLength = Number(task.sampleLength || relatedRecord?.napSampleLength || 0);
  const sampleLengthText = sampleLength > 0 ? formatNumber(sampleLength).replace(/\s*m$/, '') : '-';
  const operatorText = operatorName.value || currentUserName.value;
  const fallbackFields = [
    { label: '型号', value: currentPlan.modelCode },
    { label: '分段批号', value: productionBatchNo },
    { label: '当前工序', value: '磨皮' },
    { label: '送检时间', value: task.pushedAt },
    { label: '送检人员', value: operatorText },
    { label: '送检分段', value: segmentLabel },
    { label: '留样米(m)', value: sampleLengthText },
  ];
  const fields = await applyPrintFieldTemplate('ROUGH_GRINDING_INSPECTION', fallbackFields, {
    modelCode: currentPlan.modelCode,
    operatorName: operatorText,
    processName: '磨皮',
    productionBatchNo,
    pushedAt: task.pushedAt,
    sampleLength: sampleLengthText,
    segmentLabel,
  });
  return buildInspectionTransferTicketPayload({
    applicantName: operatorText,
    applyTime: task.pushedAt,
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    materialCode: currentPlan.materialCode,
    modelCode: currentPlan.modelCode,
    planNo: task.planNo || currentPlan.planNo,
    processName: '磨皮',
    productionBatchNo,
  });
};

const printSecondSegmentInspectionTransferTicket = async (task?: FirstInspectionTask) => {
  if (!task) return;
  const inspectionNo = String(task.faiNo || task.id || '').trim();
  if (!inspectionNo) {
    AModal.warning({
      content: '当前二磨送检记录没有送检单号，无法生成检验流转单二维码。请先完成送检后再打印。',
      title: '无法打印检验流转单',
    });
    return;
  }
  try {
    const result = await sendTransferTicketToPrintAgent(
      await buildSecondSegmentInspectionTransferTicketPayload(task),
      TRANSFER_TICKET_PRINT_AGENT_URL,
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
};

const viewSecondSegmentInspectionTaskDetail = (task?: FirstInspectionTask) => {
  if (!task) return;
  let detailDialog: ReturnType<typeof AModal.confirm> | undefined;
  const openFaiDetailFromInspectionDialog = () => {
    if (!task.faiId) return;
    detailDialog?.destroy();
    nextTick(() => {
      faiDetailModalApi.setData({ id: task.faiId }).open();
    });
  };
  detailDialog = AModal.confirm({
    cancelText: '关闭',
    content: h('div', { class: 'first-inspection-detail' }, [
      h('p', `计划号：${task.planNo}`),
      h('p', `送检分段：${task.segmentLabel || getSegmentLabel(task.segmentMark)}`),
      h('p', `送检单号：${task.faiNo || task.id}`),
      h('p', `母批批号：${task.motherBatchNo || task.parentBatchNo || '-'}`),
      h('p', `分段批号：${task.productionBatchNo || '-'}`),
      h('p', `样品类型：${task.sampleTypeName || '二磨分段留样'}`),
      h('p', `送检米数：${task.sampleLength ? formatNumber(task.sampleLength) : '-'}`),
      h('p', `推送时间：${task.pushedAt}`),
      h('p', `当前状态：${getFirstInspectionMeta(task.status).text}`),
      h('p', `反馈备注：${task.feedbackRemark || '-'}`),
      h('div', { class: 'first-inspection-detail__actions' }, [
        task.faiId
          ? h(Button, {
              size: 'small',
              onClick: openFaiDetailFromInspectionDialog,
            }, () => '查看检验明细')
          : null,
        h(Button, {
          size: 'small',
          onClick: () => printSecondSegmentInspectionTransferTicket(task),
        }, () => '打印检验流转单'),
      ]),
    ]),
    okButtonProps: { style: { display: 'none' } },
    title: '留样送检明细',
  });
};

const viewSecondSegmentSampleInspectionDetail = (segmentMark?: string) => {
  viewSecondSegmentInspectionTaskDetail(getSecondSegmentSampleTask(segmentMark));
};

const submitSecondSegmentSampleInspection = async (segmentMark?: string) => {
  if (!currentPlan.planId || !currentPlan.planNo) {
    message.warning('请先扫码或从工作列表选择磨皮工单');
    return;
  }
  const segment = normalizeSegmentMark(segmentMark);
  const segmentLabel = getSegmentLabel(segment);
  const latestRecord = getSecondSegmentLatestRecord(segment);
  if (!latestRecord?.id) {
    message.warning('请先完成该分段二次磨皮报工后再送检');
    return;
  }
  if (!(await ensureRoughSampleAbnormalUnlocked(segment, '留样送检', 'SECOND'))) {
    return;
  }
  const applyingKey = `${latestRecord.id}`;
  if (secondSegmentInspectionApplyingKey.value === applyingKey) return;
  secondSegmentInspectionApplyingKey.value = applyingKey;
  try {
    const summary = await getRoughGrindingConsoleSecondSegmentInspectionSummary({
      secondDetailId: latestRecord.id,
    });
    const existedTask = applySecondSegmentInspectionSummary(summary);
    if (existedTask && !isSecondSegmentInspectionReapplyAllowed(existedTask)) {
      message.info(`${segmentLabel}已送检，可查看留样送检明细`);
      viewSecondSegmentSampleInspectionDetail(segment);
      return;
    }
    const confirmedSampleLength = await requestSecondSegmentSampleLengthConfirm(latestRecord, segmentLabel);
    if (!confirmedSampleLength) return;
    const sampleLength = confirmedSampleLength;
    const applied = await applyRoughGrindingConsoleSecondSegmentInspection({
      remark: `磨皮二次${segmentLabel}留样送检`,
      sampleLength,
      secondDetailId: latestRecord.id,
      submitterName: operatorName.value || currentUserName.value || undefined,
    });
    applySecondSegmentSampleLengthToDraft(segment, sampleLength);
    const task = applySecondSegmentInspectionSummary(applied);
    await loadBoard({
      equipmentId: equipmentInfo.id,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    if (task) {
      viewSecondSegmentInspectionTaskDetail(task);
      message.success(`${segmentLabel}留样送检已提交，送检单号：${task.faiNo || task.id}`);
    }
  } catch (error) {
    AModal.warning({
      content: getRequestErrorMessage(error) || '提交二磨分段留样送检失败，请确认该分段已完成报工且分段批号完整。',
      title: '提交失败',
    });
  } finally {
    secondSegmentInspectionApplyingKey.value = '';
  }
};

const viewFirstInspectionDetail = () => {
  const task = currentFirstInspectionTask.value;
  if (!task) {
    AModal.info({ content: '当前计划暂无首样送检任务。', title: '首检任务详情' });
    return;
  }
  if (task.faiId) {
    faiDetailModalApi.setData({ id: task.faiId }).open();
    return;
  }
  AModal.info({
    content: h('div', { class: 'first-inspection-detail' }, [
      h('p', `计划号：${task.planNo}`),
      h('p', `任务号：${task.id}`),
      h('p', `推送时间：${task.pushedAt}`),
      h('p', `当前状态：${getFirstInspectionMeta(task.status).text}`),
      h('p', `反馈时间：${task.feedbackTime || '-'}`),
      h('p', `反馈备注：${task.feedbackRemark || '-'}`),
    ]),
    okText: '关闭',
    title: '首检任务详情',
  });
};

const [FirstInspectionPrintPreviewModal, firstInspectionPrintPreviewModalApi] = useVbenModal({
  connectedComponent: RoughGrindingFirstInspectionPrintPreviewModal,
});

const [FaiDetailPreviewModal, faiDetailModalApi] = useVbenModal({
  connectedComponent: FaiDetailModal,
});

const getRequestErrorMessage = (error: unknown) => {
  const data = (error as any)?.response?.data || (error as any)?.data || {};
  return String(data.msg || data.message || (error as any)?.msg || (error as any)?.message || '');
};

const openFirstInspectionPrintPreview = (summary: MesHcRoughGrindingConsoleApi.FaiSummary) => {
  const applyTime = summary.faiApplyTime || buildNowText();
  firstInspectionPrintPreviewModalApi
    .setData({
      firstInspection: {
        applicant: operatorName.value || currentUserName.value,
        applyTime,
        faiNo: summary.faiNo,
        inspectionDesc: `磨皮首样送检，首检单号：${summary.faiNo || '-'}`,
        inspectionType: '量产发货',
        result: summary.displayText || '已提交 FAI 首件检验，请将样品送至品质部。',
        sampleType: '首样送样',
        status: summary.faiStatus || 'PENDING',
      },
      task: {
        batchNo: currentPlan.batchNo,
        faiNo: summary.faiNo,
        id: summary.faiNo || `FAI-${currentPlan.planNo}`,
        materialCode: currentPlan.materialCode,
        modelCode: currentPlan.modelCode,
        motherMaterialCode: currentPlan.materialCode,
        planNo: currentPlan.planNo,
        process: '磨皮',
        recorderName: operatorName.value || currentUserName.value,
        recorderTime: applyTime,
      },
    })
    .open();
};

const submitFirstSampleInspection = async () => {
  if (!currentPlan.planId || !currentPlan.planOperationId || !currentPlan.planNo) {
    message.warning('请先扫码或从工作列表选择磨皮工单');
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.warning({
      content: '磨皮工序必须先开工后才能提交首样送检。',
      title: '请先执行开工确认',
    });
    return;
  }
  if (firstInspectionApplying.value) return;
  const existedTask = currentFirstInspectionTask.value;
  const isReapply = isFirstInspectionReapplyAllowed(existedTask);
  if (existedTask && !isReapply) {
    viewFirstInspectionDetail();
    return;
  }
  firstInspectionApplying.value = true;
  try {
    const summary = await applyRoughGrindingConsoleFai({
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
      remark: isReapply ? '磨皮首检驳回后重新提交' : '磨皮操作看板首检申请',
      standardMatchMode: 'PRODUCT_MODEL_PROCESS',
      submitterName: operatorName.value || currentUserName.value || undefined,
      triggerReason: isReapply ? 'REWORK_RECHECK' : 'NEW_ORDER',
    });
    applyRoughFaiSummary(summary);
    openFirstInspectionPrintPreview(summary);
    message.success(`${isReapply ? '重新提交首检申请' : '首检申请已提交'}，FAI 首检单号：${summary.faiNo || '-'}`);
  } catch (error) {
    AModal.warning({
      content: getRequestErrorMessage(error) || '提交首检申请失败，请检查开工记录、机台和检验标准后重试。',
      title: '提交失败',
    });
  } finally {
    firstInspectionApplying.value = false;
  }
};

const requestOperationAuth = async (action: 'START') => {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从工作列表选择磨皮工单');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前工单此工序已完工，不能再操作！');
    return;
  }
  if (isWorkOrderRunning.value) {
    message.info('当前工单此工序已开工');
    return;
  }
  if (shouldBlockByPreviousOperation.value) {
    AModal.warning({
      content: h('div', { class: 'previous-operation-block' }, [
        h('p', `前工序：${currentPlan.previousOperationName}`),
        h('p', `当前状态：${formatOperationStatusText(previousOperationStatus.value)}`),
        h('p', '前工序尚未完成报工，不能执行当前磨皮工序开工确认。'),
      ]),
      title: '前工序尚未完成',
    });
    return;
  }
  if (!(await ensureRoughSampleAbnormalUnlocked(undefined, '开工', 'FIRST'))) {
    return;
  }
  await loadEquipmentOptions();
  if (equipmentOptions.value.length === 0) {
    message.warning('当前磨皮工序未配置可用机台，请先在计划工序或工作中心设备中维护。');
    return;
  }
  operationAuthActionName.value = '工单开工确认';
  operationAuthVisible.value = true;
};

const loadEquipmentSelectRows = async () => {
  equipmentSelectLoading.value = true;
  try {
    await loadEquipmentOptions();
    const keyword = equipmentSelectKeyword.value.trim().toLowerCase();
    equipmentSelectRows.value = keyword
      ? equipmentOptions.value.filter((item: any) =>
          [item.code, item.name, item.workCenterName].some((value) => String(value || '').toLowerCase().includes(keyword)),
        )
      : [...equipmentOptions.value];
  } finally {
    equipmentSelectLoading.value = false;
  }
};

const openEquipmentSelect = async () => {
  equipmentSelectVisible.value = true;
  await loadEquipmentSelectRows();
};

const selectBoardEquipment = async (equipment: any) => {
  if (!equipment?.value) {
    message.warning('请选择要切换的磨皮机台');
    return;
  }
  const targetEquipmentId = Number(equipment.value);
  const shouldSwitchRunningTask =
    currentPlan.planId &&
    currentPlan.planOperationId &&
    isWorkOrderRunning.value &&
    targetEquipmentId !== equipmentInfo.id;
  equipmentSelectLoading.value = true;
  try {
    if (shouldSwitchRunningTask) {
      await switchRoughGrindingConsoleWorkOrderEquipment({
        equipmentCode: equipment.code || undefined,
        equipmentId: targetEquipmentId,
        equipmentName: equipment.name || undefined,
        planId: currentPlan.planId!,
        planOperationId: currentPlan.planOperationId!,
      });
    }
    applyEquipmentOption(equipment);
    await loadBoard({
      equipmentId: equipmentInfo.id,
      planId: currentPlan.planId || Number(route.query.planId || 0) || undefined,
      planOperationId: currentPlan.planOperationId || Number(route.query.planOperationId || 0) || undefined,
    });
    if (taskListVisible.value) {
      await loadTaskList();
    }
    equipmentSelectVisible.value = false;
    // message.success(shouldSwitchRunningTask ? '磨皮机台已切换，点检清洁状态已重新加载' : '已切换看板机台，点检清洁状态已重新加载');
  } finally {
    equipmentSelectLoading.value = false;
  }
};

const handleOperationAuthSuccess = async (payload: any) => {
  operatorId.value = payload?.userId || payload?.id || operatorId.value;
  operatorName.value = payload?.empName || payload?.username || operatorName.value;
  operatorNo.value = payload?.empNo || payload?.username || operatorNo.value;
  applyEquipmentFromAuth(payload);
  const selectedEquipmentId = payload?.equipmentId || equipmentInfo.id;
  const selectedEquipmentCode = payload?.equipmentCode || equipmentInfo.code;
  const selectedEquipmentName = payload?.equipmentName || equipmentInfo.name;
  if (!selectedEquipmentId) {
    message.warning('请选择机台编号后再确认');
    return;
  }
  const now = buildNowText();
  await startRoughGrindingConsoleWorkOrder({
    equipmentCode: selectedEquipmentCode,
    equipmentId: selectedEquipmentId,
    equipmentName: selectedEquipmentName,
    planId: currentPlan.planId!,
    planOperationId: currentPlan.planOperationId!,
    recorderName: operatorName.value,
    recorderTime: now,
    reportDate: todayText(),
    startTime: now,
  });
  message.success('工单磨皮工序已开工');
  await loadBoard({
    equipmentId: selectedEquipmentId,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
  });
};

const handleOperationAuthCancel = () => {
  operationAuthVisible.value = false;
};

const openConsumableReplace = (target: 'GUIDE_CLOTH' | 'SANDPAPER') => {
  replaceForm.target = target;
  replaceConsumption.value = newGrindingConsumption();
  replaceConsumptionBalance.value = undefined;
  replaceForm.batchNo = '';
  replaceForm.initialLength = 0;
  replaceForm.initialUseCount = 0;
  replaceForm.reason = '';
  replaceForm.replaceTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
  replaceForm.replacer = operatorName.value || currentUserName.value;
  replaceForm.replacerId = operatorId.value || currentUserId.value;
  replaceForm.replacerNo = operatorNo.value || currentUserNo.value;
  clearBoardConsumableSelection();
  openRoughConsoleConsumableLedger(
    target,
    (row) => {
      replaceForm.batchNo = row.batchNo || '';
      const prefix = target === 'SANDPAPER' ? 'sandpaper' : 'guideCloth';
      replaceConsumption.value[`${prefix}LedgerId`] = row.id;
      const defaults = getGrindingConsumptionDefault(target, row);
      replaceConsumption.value[`${prefix}Qty`] = defaults.qty;
      if (defaults.warning) message.warning(defaults.warning);
      replaceConsumption.value[`${prefix}Unit`] = row.uomName || row.uomCode;
      replaceConsumptionBalance.value = row.balanceQty;
      rememberBoardConsumableSelection(target, row);
      consumableReplaceVisible.value = true;
      nextTick(() => focusBySelector('[data-replace-field="reason"] input'));
    },
  );
};

const persistConsumableReplace = async () => {
  if (!replaceForm.batchNo) {
    message.warning('请填写新的批号');
    return;
  }
  if (!replaceForm.reason) {
    message.warning('请填写更换原因');
    return;
  }
  const consumptionError = validateGrindingConsumption(replaceConsumption.value, replaceForm.target === 'SANDPAPER', replaceForm.target === 'GUIDE_CLOTH');
  if (consumptionError) { message.warning(consumptionError); return; }
  if (equipmentInfo.id) {
    await replaceRoughGrindingConsoleConsumable({
      consumption: replaceConsumption.value,
      batchNo: replaceForm.batchNo,
      consumableType: replaceForm.target,
      equipmentCode: equipmentInfo.code,
      equipmentId: equipmentInfo.id,
      equipmentName: equipmentInfo.name,
      initialUseCount: Number(replaceForm.initialUseCount || 0),
      initialUsedLength: Number(replaceForm.initialLength || 0),
      limitCount: replaceForm.target === 'SANDPAPER' ? undefined : DEFAULT_GUIDE_CLOTH_LIMIT_COUNT,
      limitLength: replaceForm.target === 'SANDPAPER' ? DEFAULT_SANDPAPER_LIMIT_LENGTH : undefined,
      operationName: currentTask.value?.process,
      operatorId: replaceForm.replacerId,
      operatorName: replaceForm.replacer,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      replaceReason: replaceForm.reason,
      replaceTime: replaceForm.replaceTime,
      stateId: replaceForm.target === 'SANDPAPER' ? sandpaper.id : guideCloth.id,
      workCenterId: currentTask.value?.workCenterId,
    });
    try {
      await updateBoardConsumableUsageStatusAfterReplace();
    } catch {
      message.warning('更换信息已保存，但边库耗材使用状态更新失败，请在边库台账中核对处理');
    }
    await loadBoard({
      equipmentId: equipmentInfo.id,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    consumableReplaceVisible.value = false;
    message.success('更换信息已保存');
    return;
  }
  message.warning('请先绑定当前磨皮看板机台，再登记砂纸/导布更换信息');
  void openEquipmentSelect();
};

const saveConsumableReplace = async () => {
  if (replaceSubmitting.value) return;
  replaceSubmitting.value = true;
  try { await persistConsumableReplace(); } finally { replaceSubmitting.value = false; }
};

const submitDailyCheck = async (
  action: { mode: DailyCheckMode; previousEquipmentId?: number; type: CheckType },
  authPayload: any,
) => {
  const row = dailyCheckRows.find((item) => item.key === action.type);
  if (row) {
    const selectedEquipmentId = authPayload?.equipmentId || equipmentInfo.id;
    const selectedEquipmentCode = authPayload?.equipmentCode || equipmentInfo.code;
    const selectedEquipmentName = authPayload?.equipmentName || equipmentInfo.name;
    const operator = authPayload?.empName || authPayload?.username || operatorName.value;
    if (!selectedEquipmentId) {
      message.warning('请选择机台编号后再提交工作准备记录');
      return;
    }
    if (!operator) {
      message.warning('请先完成记录人/确认人认证');
      return;
    }
    const now = buildNowText();
    const result = row.details.some((item) => item.result === 'NG') ? 'NG' : 'OK';
    const recorder = action.mode === 'edit'
      ? operator
      : row.recorder === '-' ? operator : row.recorder;
    const recorderTime = action.mode === 'edit' || shouldResetRuntimeDateTime(row.recorderTime)
      ? now
      : row.recorderTime;
    const confirmer = action.mode === 'confirm'
      ? operator
      : row.confirmer === '-' ? undefined : row.confirmer;
    const confirmerTime = action.mode === 'confirm'
      ? now
      : shouldResetRuntimeDateTime(row.confirmerTime) ? undefined : row.confirmerTime;
    const payload = {
      confirmer,
      confirmerTime,
      details: row.details.map((item) => ({
        actualValue: item.value,
        category: item.category,
        item: item.item,
        itemSeq: item.seq,
        remark: item.remark,
        standard: item.standard,
        status: item.result,
      })),
      equipmentCode: selectedEquipmentCode,
      equipmentId: selectedEquipmentId,
      equipmentName: selectedEquipmentName,
      formCode: row.formCode || (row.key === 'CLEANING' ? 'ROUGH_CLEANING_CHECK' : 'ROUGH_STARTUP_CHECK'),
      operationName: currentTask.value?.process,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      recordDate: todayText(),
      recorder,
      recorderTime,
      recordId: action.previousEquipmentId === selectedEquipmentId ? row.recordId : undefined,
      result,
      workCenterId: currentTask.value?.workCenterId,
    };
    if (action.mode === 'confirm') {
      await confirmRoughGrindingConsoleDailyCheck(payload);
    } else {
      await saveRoughGrindingConsoleDailyCheck(payload);
    }
    await loadBoard({
      equipmentId: selectedEquipmentId,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
  }
  dailyCheckVisible.value = false;
};

const saveDailyCheck = async () => {
  const row = dailyCheckRows.find((item) => item.key === dailyCheckType.value);
  if (!row) return;
  if (dailyCheckMode.value === 'edit' && !canFillDailyCheck(row)) {
    message.warning('后台状态不允许填写当前记录');
    return;
  }
  if (dailyCheckMode.value === 'confirm' && !canConfirmDailyCheck(row)) {
    message.warning('后台状态不允许确认当前记录，请先填写保存');
    return;
  }
  await loadEquipmentOptions();
  if (equipmentOptions.value.length === 0) {
    message.warning('当前磨皮工序未配置可用机台，请先在计划工序或工作中心设备中维护。');
    return;
  }
  pendingDailyCheckAction.value = {
    mode: dailyCheckMode.value,
    previousEquipmentId: equipmentInfo.id,
    type: dailyCheckType.value,
  };
  dailyCheckAuthAction.value = `${dailyCheckMode.value === 'confirm' ? '确认' : '保存'}${row.name || '工作准备记录'}`;
  dailyCheckAuthVisible.value = true;
};

watch(
  () => dailyCheckVisible.value,
  (visible) => {
    if (visible && dailyCheckMode.value !== 'view') {
      focusDailyCheckCell(0, 'value');
    }
  },
);

const toggleExtendedBoardTabs = () => {
  showExtendedBoardTabs.value = !showExtendedBoardTabs.value;
  if (!showExtendedBoardTabs.value && !['FIRST', 'SECOND', 'INSPECTION_RECORDS', 'ABNORMAL_POSITIONS', 'EDGE_CONSUMABLE', 'INSTRUCTION_MESSAGES'].includes(activeBoardTab.value)) {
    activeBoardTab.value = 'FIRST';
  }
  message.info(showExtendedBoardTabs.value ? '已显示点检/清洁、今日磨皮记录和中间品记录单' : '已隐藏扩展记录页签');
};

watch(showExtendedBoardTabs, (visible) => {
  if (!visible && !['FIRST', 'SECOND', 'INSPECTION_RECORDS', 'ABNORMAL_POSITIONS', 'EDGE_CONSUMABLE', 'INSTRUCTION_MESSAGES'].includes(activeBoardTab.value)) {
    activeBoardTab.value = 'FIRST';
  }
});

watch(activeBoardTab, () => {
  if (visualMaximized.value) {
    nextTick(() => window.dispatchEvent(new Event('resize')));
  }
});

watch(scanPlanNo, (value) => schedulePlanScan(value));

watch(
  () => [activeReportTab.value, reportVisible.value, reportStep.value],
  () => {
    if (reportVisible.value) {
      nextTick(focusPreferredReportField);
    }
  },
);

watch(
  () => [activeReportTab.value, reportVisible.value, activePass.value],
  ([tab, visible, pass]) => {
    if (visible && tab === 'dev-process-param') {
      void loadRoughDevProcessParamRecords(pass as GrindPass);
    }
  },
);

watch(
  reportOutputLengthValue,
  (value) => {
    if (reportReadonly.value && reportCurrentRecord.value) {
      return;
    }
    reportForm.outputLength = value;
  },
  { immediate: true },
);

watch(
  () => [
    reportForm.batchNo,
    reportForm.processLength,
    reportForm.lossLength,
    reportForm.napSampleLength,
    reportForm.researchConsumptionLength,
    reportAbnormalTotalLengthValue.value,
    reportForm.startPosition,
    activePass.value,
  ],
  () => {
    if (!reportVisible.value || middleProductDetailVisible.value || activePass.value !== 'SECOND') {
      return;
    }
    const draftKey = getMiddleProductDraftKey();
    if (middleProductDraftSaved.value && middleProductDraftKey.value !== draftKey) {
      middleProductDraftSaved.value = false;
    }
    if (!middleProductDraftSaved.value) {
      syncMiddleProductRowsFromReport();
    }
  },
);

watch(
  () => middleProductDetailVisible.value,
  (visible) => {
    if (!visible && middleProductOpenedFromReport.value) {
      middleProductOpenedFromReport.value = false;
      nextTick(() => {
        activeReportTab.value = middleProductReturnReportTab.value || 'middle';
        reportVisible.value = true;
      });
    }
  },
);

const normalizeAbnormalLength = (value: unknown) => {
  if (value === undefined || value === null || value === '') return undefined;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
};

const createReportAbnormalRow = (row?: Partial<RoughReportAbnormalPositionRow>): RoughReportAbnormalPositionRow => ({
  abnormalLength: normalizeAbnormalLength(row?.abnormalLength),
  clientKey: row?.clientKey || `rough-console-abnormal-${Date.now()}-${Math.random()}`,
  positionText: String(row?.positionText || ''),
  remark: String(row?.remark || ''),
  sortOrder: row?.sortOrder,
});

const addReportAbnormalRow = () => {
  reportAbnormalRows.value.push(createReportAbnormalRow());
};

const removeReportAbnormalRow = (index: number) => {
  reportAbnormalRows.value.splice(index, 1);
};

const openReportAbnormalTab = () => {
  if (!reportAbnormalRows.value.length) {
    addReportAbnormalRow();
  }
  activeReportTab.value = 'abnormal-position';
};

const buildReportAbnormalPositions = () => {
  const rows = reportAbnormalRows.value
    .map((row, index) => ({
      abnormalLength: normalizeAbnormalLength(row.abnormalLength),
      positionText: String(row.positionText || '').trim(),
      remark: String(row.remark || '').trim(),
      sortOrder: index + 1,
    }))
    .filter(
      (row) =>
        row.positionText ||
        row.abnormalLength !== undefined ||
        Boolean(row.remark),
    );
  for (const row of rows) {
    if (!row.positionText) {
      message.warning('异常位置不能为空');
      activeReportTab.value = 'abnormal-position';
      return null;
    }
    if (
      row.abnormalLength === undefined ||
      !Number.isFinite(row.abnormalLength) ||
      row.abnormalLength <= 0
    ) {
      message.warning('异常位置米数必须大于 0');
      activeReportTab.value = 'abnormal-position';
      return null;
    }
  }
  return rows;
};

const validateSecondReportOutputBalance = () => {
  if (activePass.value !== 'SECOND') return true;
  if (!Number.isFinite(reportResearchConsumptionLengthValue.value) || reportResearchConsumptionLengthValue.value < 0) {
    message.warning('研发消耗米数必须为非负数');
    activeReportTab.value = 'basic';
    return false;
  }
  const deductedLength =
    reportLossLengthValue.value +
    reportNapSampleLengthValue.value +
    reportResearchConsumptionLengthValue.value +
    reportAbnormalTotalLengthValue.value;
  if (deductedLength - reportProcessLengthValue.value <= SECOND_RANGE_EPS) {
    return true;
  }
  message.warning(
    `固定损耗、NAP留样、研发消耗和异常米数合计 ${formatNumber(deductedLength)}m，不能超过投入米数 ${formatNumber(reportProcessLengthValue.value)}m`,
  );
  activeReportTab.value =
    reportAbnormalTotalLengthValue.value > 0 ? 'abnormal-position' : 'basic';
  return false;
};

const resetReportForm = () => {
  reportConsumption.value = newGrindingConsumption();
  const firstAvailableLength = Number(firstReportAvailableLength.value || 0);
  reportAbnormalRows.value = [];
  reportForm.batchNo = activePass.value === 'FIRST'
    ? firstReportSourceBatchNo.value
    : materialScanCode.value || currentPlan.batchNo;
  reportForm.defectCode = '';
  reportForm.endTime = '';
  reportForm.firstAllocationId = undefined;
  reportForm.guideClothChanged = guideClothNeedReplace.value ? 'YES' : 'NO';
  reportForm.guideClothNewBatchNo = '';
  reportForm.guideClothReplaceReason = '';
  clearReportConsumableSelection('GUIDE_CLOTH');
  reportForm.lossLength = 0;
  reportForm.napSampleLength = 0;
  reportForm.researchConsumptionLength = 0;
  reportForm.outputLength = 0;
  reportForm.planNo = activePass.value === 'FIRST' ? firstReportSourcePlanNo.value : currentPlan.planNo;
  reportForm.processLength = activePass.value === 'FIRST'
    ? Number(firstAvailableLength.toFixed(3))
    : null;
  reportForm.sandpaperChanged = sandpaperNeedReplace.value ? 'YES' : 'NO';
  reportForm.sandpaperNewBatchNo = '';
  reportForm.sandpaperReplaceReason = '';
  clearReportConsumableSelection('SANDPAPER');
  reportForm.segmentMark = '';
  reportForm.selfCheck = 'OK';
  reportForm.startPosition = activePass.value === 'FIRST' ? firstReportStartPosition.value : 0;
  reportForm.startTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
  processItems.value = buildDefaultProcessItems(activePass.value);
  resetMiddleProductDraft();
  resetMiddleProductDetailHeader();
};

const requestCloseReport = () => {
  if (reportSubmitting.value) {
    message.info('报工正在提交，请等待处理完成');
    return;
  }
  if (reportTimeEditing.value) {
    if (reportCurrentRecord.value) {
      reportForm.startTime = reportCurrentRecord.value.startTime || '';
      reportForm.endTime = reportCurrentRecord.value.endTime || '';
    }
    reportTimeEditing.value = false;
    return;
  }
  if (reportReadonly.value) {
    reportVisible.value = false;
    reportReadonly.value = false;
    reportCurrentRecord.value = undefined;
    reportTimeEditing.value = false;
    return;
  }
  AModal.confirm({
    title: '确认关闭报工登记',
    content: '当前报工登记尚未提交，关闭后本次扫码、工艺参数点检表和报工收卷数据会丢失，确认关闭？',
    okText: '确认关闭',
    okType: 'danger',
    cancelText: '继续填写',
    onOk: () => {
      reportVisible.value = false;
      reportCurrentRecord.value = undefined;
      reportTimeEditing.value = false;
    },
  });
};

const openMiddleProductDetail = async (mode: 'edit' | 'view' = 'edit', returnTab?: string) => {
  if (activePass.value !== 'SECOND') {
    message.warning('第一次磨皮不产生中间品记录单');
    return;
  }
  middleProductOpenedFromSegment.value = false;
  middleProductSegmentKey.value = '';
  if (mode === 'edit') {
    if (reportStep.value !== 'REPORT') {
      message.warning('请先完成来源扫码和工艺参数点检表，再进入报工收卷填写中间品记录');
      activeReportTab.value = reportStep.value === 'PARAMS' ? 'process' : 'report';
      return;
    }
    if (Number(reportForm.processLength || 0) <= 0) {
      message.warning('请先在报工收卷中填写投入米数，且投入米数必须大于 0');
      activeReportTab.value = 'report';
      focusReportField('processLength');
      return;
    }
  }
  middleThicknessLabels.value = [];
  try { middleThicknessLabels.value = await loadRoughMiddleProductThicknessLabels(); }
  catch { message.error('厚度列配置加载失败，请重试'); return; }
  middleProductActivePass.value = activePass.value;
  middleProductDetailMode.value = mode;
  middleProductDetailPage.value = 1;
  syncMiddleProductRowsFromReport();
  resetMiddleProductDetailHeader();
  Object.assign(middleProductDetailHeader, buildMiddleProductHeaderData(activePass.value));
  middleProductDetailHeader.generatedLength = middleProductRows.value.length;
  middleProductReportMeters.value = middleProductDetailHeader.processLength || reportForm.processLength || '';
  middleProductMainWidth.value = middleProductDetailHeader.width || middleProductMainWidth.value || '';
  middleProductConfirmer.value = middleProductDetailHeader.confirmer || middleProductConfirmer.value || '';
  middleProductOpenedFromReport.value = true;
  middleProductReturnReportTab.value = returnTab || activeReportTab.value || 'middle';
  reportVisible.value = false;
  nextTick(() => {
    middleProductDetailVisible.value = true;
    if (mode === 'edit') {
      focusMiddleProductCell(0, 'lengthMeter');
    }
  });
};

const openSecondSegmentMiddleProductRecord = async (segmentMark = '') => {
  if (!currentPlan.planOperationId || !currentPlan.planNo) {
    message.warning('请先扫码带出真实磨皮计划');
    return;
  }
  const segment = normalizeSegmentMark(segmentMark);
  const latestRecord = getSecondSegmentLatestRecord(segment);
  if (!latestRecord?.id) {
    message.warning(`请先完成${getSegmentLabel(segment)}第二次磨皮报工后再填写中间品记录单`);
    return;
  }
  const key = getSecondSegmentMiddleProductKey(segment);
  middleProductActivePass.value = 'SECOND';
  middleProductDetailMode.value = 'edit';
  middleProductEditingRecordId.value = undefined;
  middleProductOpenedFromReport.value = false;
  middleProductOpenedFromSegment.value = true;
  middleProductSegmentKey.value = key;
  middleProductDetailPage.value = 1;
  resetMiddleProductDetailHeader();
  middleProductDetailVisible.value = true;
  middleProductDetailLoading.value = true;
  try {
    middleThicknessLabels.value = [];
    middleThicknessLabels.value = await loadRoughMiddleProductThicknessLabels();
    const detail = await getOrInitRoughGrindingConsoleSegmentMiddleProductRecord({
      passType: 'SECOND',
      planOperationId: currentPlan.planOperationId,
      segmentMark: segment,
    });
    const headerData = parseJsonObject(detail.headerDataJson) as Partial<MiddleProductDetailHeader>;
    middleProductEditingRecordId.value = detail.recordId;
    Object.assign(middleProductDetailHeader, {
      ...buildSecondSegmentMiddleProductHeader(segment),
      ...headerData,
      displayName: headerData.displayName || stripMiddleProductModelPrefix(detail.formName || headerData.formName) || `磨皮中间品记录表 - ${getSegmentLabel(segment)}`,
      formName: detail.formName || 'CMP软垫（W26P0100）磨皮中间品记录表',
      generatedLength: detail.generatedLength ?? headerData.generatedLength ?? 0,
      materialCode: detail.materialCode || headerData.materialCode || currentPlan.materialCode,
      motherBatchNo: detail.motherBatchNo || currentPlan.batchNo,
      motherModelCode: detail.motherModelCode || currentPlan.modelCode,
      passName: detail.passName || headerData.passName || `第二次磨皮-${getSegmentLabel(segment)}`,
      passType: 'SECOND',
      planNo: detail.headerDataJson ? headerData.planNo || currentPlan.planNo : currentPlan.planNo,
      processLength: Number(headerData.processLength || detail.segmentTotalLength || headerData.segmentTotalLength || 0),
      productionBatchNo: detail.productionBatchNo || headerData.productionBatchNo || buildSecondGrindingBatchNo(currentPlan.batchNo, segment),
      recordDate: detail.recordDate || headerData.recordDate || todayText(),
      recorder: detail.recorder || headerData.recorder || operatorName.value || currentUserName.value || '',
      recorderTime: detail.recorderTime || headerData.recorderTime || '',
      confirmer: detail.confirmer || headerData.confirmer || '',
      confirmerTime: detail.confirmerTime || headerData.confirmerTime || '',
      docStatus: detail.docStatus || headerData.docStatus || '',
      segmentMark: detail.segmentMark ?? segment,
      segmentName: detail.segmentName || getSegmentLabel(segment),
      segmentTotalLength: Number(detail.segmentTotalLength || 0),
      width: detail.widthMm || headerData.widthMm || headerData.width || '',
      widthMm: detail.widthMm || headerData.widthMm || headerData.width || '',
    });
    middleProductReportMeters.value = middleProductDetailHeader.processLength || detail.segmentTotalLength || '';
    middleProductMainWidth.value = String(detail.widthMm || headerData.widthMm || headerData.width || '');
    middleProductConfirmer.value = String(detail.confirmer || headerData.confirmer || '');
    if (String(detail.docStatus || '').toUpperCase() === 'CONFIRMED') {
      middleProductDetailMode.value = 'view';
    }
    middleProductRows.value = mapMiddleProductRows(
      detail.details || [],
      detail.productionBatchNo || buildSecondGrindingBatchNo(currentPlan.batchNo, segment),
    );
    middleProductDetailHeader.generatedLength = detail.generatedLength || middleProductRows.value.length;
    nextTick(() => focusMiddleProductCell(0, 'lengthMeter'));
  } finally {
    middleProductDetailLoading.value = false;
  }
};

const openSubmittedMiddleProductDetail = async (record: Partial<WorkRecord>, mode: 'edit' | 'view' = 'view') => {
  if (!record.middleProductRecordId) {
    message.warning('当前报工记录未生成中间品记录单');
    return;
  }
  const passType: GrindPass = record.passType || 'SECOND';
  middleProductOpenedFromSegment.value = false;
  middleProductSegmentKey.value = '';
  middleProductActivePass.value = passType;
  middleProductDetailMode.value = mode;
  middleProductEditingRecordId.value = record.middleProductRecordId;
  middleProductOpenedFromReport.value = false;
  middleProductDetailPage.value = 1;
  resetMiddleProductDetailHeader();
  middleProductDetailVisible.value = true;
  middleProductDetailLoading.value = true;
  try {
    middleThicknessLabels.value = [];
    middleThicknessLabels.value = await loadRoughMiddleProductThicknessLabels();
    const detail = await getRoughGrindingConsoleMiddleProductRecord(record.middleProductRecordId);
    const headerData = parseJsonObject(detail.headerDataJson) as Partial<MiddleProductDetailHeader>;
    Object.assign(middleProductDetailHeader, {
      batchNo: currentPlan.batchNo,
      ...headerData,
      displayName: headerData.displayName || stripMiddleProductModelPrefix(detail.formName || headerData.formName) || '磨皮中间品记录表',
      formName: detail.formName || 'CMP软垫（W26P0100）磨皮中间品记录表',
      generatedLength: detail.generatedLength ?? headerData.generatedLength ?? 0,
      materialCode: detail.materialCode || headerData.materialCode || currentPlan.materialCode,
      motherBatchNo: detail.motherBatchNo || headerData.motherBatchNo || currentPlan.batchNo,
      motherModelCode: detail.motherModelCode || headerData.motherModelCode || currentPlan.modelCode,
      passName: detail.passName || headerData.passName || getGrindingPassName(passType),
      passType,
      planNo: headerData.planNo || record.planNo || currentPlan.planNo,
      processLength: Number(headerData.processLength || record.processLength || detail.segmentTotalLength || 0),
      productionBatchNo: headerData.productionBatchNo || record.productionBatchNo || record.batchNo,
      recordDate: detail.recordDate || headerData.recordDate || todayText(),
      recorder: detail.recorder || record.middleProductRecorder || '-',
      recorderTime: detail.recorderTime || headerData.recorderTime || record.middleProductRecordTime || '',
      confirmer: detail.confirmer || headerData.confirmer || '',
      confirmerTime: detail.confirmerTime || headerData.confirmerTime || '',
      docStatus: detail.docStatus || headerData.docStatus || '',
      segmentMark: detail.segmentMark || headerData.segmentMark,
      segmentName: detail.segmentName || headerData.segmentName,
      segmentTotalLength: Number(detail.segmentTotalLength || headerData.segmentTotalLength || 0),
      width: detail.widthMm || headerData.widthMm || headerData.width || '',
      widthMm: detail.widthMm || headerData.widthMm || headerData.width || '',
    });
    middleProductReportMeters.value = middleProductDetailHeader.processLength || record.processLength || '';
    middleProductMainWidth.value = String(detail.widthMm || headerData.widthMm || headerData.width || '');
    middleProductConfirmer.value = String(detail.confirmer || headerData.confirmer || '');
    middleProductRows.value = mapMiddleProductRows(detail.details || [], record.productionBatchNo || record.batchNo);
    middleProductDetailHeader.generatedLength = detail.generatedLength || middleProductRows.value.length;
    if (mode === 'edit') {
      nextTick(() => focusMiddleProductCell(0, 'lengthMeter'));
    }
  } finally {
    middleProductDetailLoading.value = false;
  }
};

const saveMiddleProductDetail = async () => {
  middleProductDetailHeader.generatedLength = middleProductRows.value.length;
  middleProductDetailHeader.processLength = Number(
    middleProductReportMeters.value ||
      middleProductDetailHeader.processLength ||
      getMiddleProductProcessLengthFromReport() ||
      0,
  );
  middleProductDetailHeader.width = middleProductMainWidth.value;
  middleProductDetailHeader.widthMm = middleProductMainWidth.value;
  middleProductDetailHeader.confirmer = middleProductConfirmer.value;
  if (middleProductEditingRecordId.value) {
    await saveRoughGrindingConsoleMiddleProductRecord({
      details: buildMiddleProductPayloadRows(),
      headerDataJson: buildMiddleProductHeaderPayload(middleProductActivePass.value),
      recordId: middleProductEditingRecordId.value,
    });
    await loadBoard({
      equipmentId: equipmentInfo.id,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    closeMiddleProductDetail(true);
    message.success('中间品记录单已保存');
    return;
  }
  middleProductDraftSaved.value = true;
  middleProductDraftKey.value = getMiddleProductDraftKey();
  closeMiddleProductDetail(true);
  message.success('磨皮中间品记录单已暂存');
};

const confirmMiddleProductDetail = async () => {
  if (!middleProductEditingRecordId.value) {
    message.warning('请先初始化或保存中间品记录单后再确认');
    return;
  }
  if (!middleProductRows.value.length) {
    message.warning('中间品记录单明细不能为空');
    return;
  }
  middleProductAuthVisible.value = true;
};

const handleMiddleProductAuthSuccess = async (userInfo: any) => {
  middleProductAuthVisible.value = false;
  if (!middleProductEditingRecordId.value) return;
  const now = buildNowText();
  const confirmer =
    userInfo?.empName ||
    userInfo?.nickname ||
    userInfo?.username ||
    userInfo?.empNo ||
    middleProductConfirmer.value ||
    operatorName.value ||
    currentUserName.value ||
    '';
  middleProductDetailLoading.value = true;
  try {
    middleProductConfirmer.value = confirmer;
    middleProductDetailHeader.confirmer = confirmer;
    middleProductDetailHeader.confirmerTime = now;
    await confirmRoughGrindingConsoleMiddleProductRecord({
      details: buildMiddleProductPayloadRows(),
      headerDataJson: buildMiddleProductHeaderPayload(middleProductActivePass.value),
      recordId: middleProductEditingRecordId.value,
    });
    const refreshed = await getRoughGrindingConsoleMiddleProductRecord(middleProductEditingRecordId.value);
    const headerData = parseJsonObject(refreshed.headerDataJson) as Partial<MiddleProductDetailHeader>;
    middleProductRows.value = mapMiddleProductRows(
      refreshed.details || [],
      refreshed.productionBatchNo || middleProductDetailHeader.productionBatchNo || '',
    );
    Object.assign(middleProductDetailHeader, {
      ...middleProductDetailHeader,
      ...headerData,
      confirmer: refreshed.confirmer || headerData.confirmer || confirmer,
      confirmerTime: refreshed.confirmerTime || headerData.confirmerTime || now,
      docStatus: refreshed.docStatus || 'CONFIRMED',
      recorderTime: refreshed.recorderTime || headerData.recorderTime || middleProductDetailHeader.recorderTime || '',
    });
    middleProductDetailMode.value = 'view';
    await loadBoard({
      equipmentId: equipmentInfo.id,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    message.success('中间品记录单已确认');
  } finally {
    middleProductDetailLoading.value = false;
  }
};

const handleMiddleProductExport = async () => {
  if (!middleProductEditingRecordId.value) {
    message.warning('请先初始化或保存中间品记录单后再导出');
    return;
  }
  const layout = buildMiddleProductExcelLayout();
  const data = await exportProcessFormRecordLayout(layout);
  downloadFileFromBlobPart({ fileName: layout.fileName || '磨皮中间品记录表.xlsx', source: data });
};

const handleMiddleProductImportClick = () => {
  if (!middleProductEditingRecordId.value) {
    message.warning('请先初始化中间品记录单后再导入');
    return;
  }
  middleProductImportInputRef.value?.click();
};

const handleMiddleProductImportFile = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file || !middleProductEditingRecordId.value) {
    return;
  }
  if (!/\.xls[xm]?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  middleProductDetailLoading.value = true;
  try {
    const layout = buildMiddleProductExcelLayout();
    const resp = await importProcessFormRecordLayout(file, layout);
    const appliedCount = applyImportedMiddleProductExcel(resp);
    const uploaded = await uploadFile({
      directory: 'mes/rough-grinding-middle-product',
      file,
    });
    await saveRoughGrindingConsoleMiddleProductRecord({
      details: buildMiddleProductPayloadRows(),
      headerDataJson: withImportAttachment(buildMiddleProductHeaderPayload(middleProductActivePass.value), buildImportedAttachment(file, uploaded)),
      recordId: middleProductEditingRecordId.value,
    });
    const refreshed = await getRoughGrindingConsoleMiddleProductRecord(middleProductEditingRecordId.value);
    const headerData = parseJsonObject(refreshed.headerDataJson) as Partial<MiddleProductDetailHeader>;
    middleProductRows.value = mapMiddleProductRows(refreshed.details || [], refreshed.productionBatchNo || middleProductDetailHeader.productionBatchNo || '');
    Object.assign(middleProductDetailHeader, {
      ...headerData,
      generatedLength: refreshed.generatedLength || refreshed.details?.length || middleProductRows.value.length,
      processLength: Number(headerData.processLength || middleProductReportMeters.value || refreshed.segmentTotalLength || 0),
      recorderTime: refreshed.recorderTime || headerData.recorderTime || middleProductDetailHeader.recorderTime || '',
      confirmer: refreshed.confirmer || headerData.confirmer || middleProductDetailHeader.confirmer || '',
      confirmerTime: refreshed.confirmerTime || headerData.confirmerTime || middleProductDetailHeader.confirmerTime || '',
      docStatus: refreshed.docStatus || headerData.docStatus || middleProductDetailHeader.docStatus || '',
    });
    middleProductReportMeters.value = middleProductDetailHeader.processLength || middleProductReportMeters.value || '';
    message.success(`已导入 ${appliedCount} 个有值单元格，并挂接原始附件`);
  } finally {
    middleProductDetailLoading.value = false;
  }
};

const showDailyCheckGuide = () => {
  AModal.confirm({
    title: '报工前置向导',
    content: '今天必须先填写开机点检和设备清洁点检，保存后即可进行第一次/第二次磨皮报工，确认可后续补签。',
    okText: '去填写工作准备',
    cancelText: '稍后处理',
    onOk: () => {
      showExtendedBoardTabs.value = true;
      dailyCheckListVisible.value = true;
    },
  });
};

const ensureFirstGrindingReportedBeforeSecond = () => {
  if (hasFirstGrindingReport.value) {
    return true;
  }
  message.warning('请先完成第一次磨皮报工后，再进行第二次磨皮报工');
  return false;
};

const openWorkRecordList = () => {
  focusedWorkRecordKey.value = undefined;
  workRecordListVisible.value = true;
};

const setProcessItemsFromWorkRecord = (record: WorkRecord) => {
  processItems.value = buildDefaultProcessItems(record.passType);
  const valueMap: Record<string, string | undefined> = {
    厚度: record.qualityThickness,
    气压: record.pressure,
    计米器: record.meterCounter,
    磨后宽幅: record.qualityWidth,
    磨皮厚度: record.grindingThickness,
    磨皮后厚度: record.afterGrindingThickness,
    磨皮米数: record.grindingMeters == null ? undefined : String(record.grindingMeters),
    线速: record.lineSpeed,
    转速: record.rotationSpeed,
  };
  processItems.value.forEach((item) => {
    item.actualValue = valueMap[item.itemName] || item.actualValue || '';
  });
};

const isConfirmedWorkRecord = (record?: WorkRecord) =>
  record?.confirmStatus === '已确认' || record?.confirmStatus === 'CONFIRMED';

const canDeleteReadonlyReport = computed(() => {
  const record = reportCurrentRecord.value;
  if (!reportReadonly.value || !record?.id) {
    return false;
  }
  if (record.passType === 'SECOND') {
    return !isConfirmedWorkRecord(record);
  }
  return record.passType === 'FIRST';
});

const canEditReadonlyReportTime = computed(
  () =>
    reportReadonly.value &&
    !!reportCurrentRecord.value?.id,
);
const statisticsRevisionRole = computed(() => {
  const record = reportCurrentRecord.value;
  if (record?.recordRole) return record.recordRole;
  return record?.passType === 'SECOND' ? 'SECOND' : 'FIRST_ORIGINAL';
});
const isStatisticsRevisionFirstAllocation = computed(
  () => statisticsRevisionRole.value === 'FIRST_ALLOCATION',
);
const statisticsRevisionProcessLabel = computed(() =>
  statisticsRevisionRole.value === 'SECOND' ? '报工米数(m)' : '投入米数(m)',
);
const canReviseStatisticsData = computed(() => {
  if (!reportReadonly.value) return false;
  if (statisticsRevisionRole.value === 'FIRST_ALLOCATION') {
    return !!(reportForm.firstAllocationId || reportCurrentRecord.value?.firstAllocationId || reportCurrentRecord.value?.id);
  }
  return !!reportCurrentRecord.value?.id;
});

const startEditReadonlyReportTime = () => {
  if (!canEditReadonlyReportTime.value) {
    message.warning('未找到可修改时间的报工记录');
    return;
  }
  reportTimeEditing.value = true;
  activeReportTab.value = 'report';
};

const saveReadonlyReportTime = async () => {
  const record = reportCurrentRecord.value;
  if (!record?.id) {
    message.warning('未找到可保存的报工记录');
    return;
  }
  const fallbackDate = resolveReportTimeFallbackDate(record);
  const startTime = normalizeReportFormTimeField('startTime', reportForm.startTime, record);
  const endTime = normalizeReportFormTimeField('endTime', reportForm.endTime, record);
  const reportDate = reportDateFromDateTime(startTime || endTime, fallbackDate);
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
    console.info('[磨皮报工时间保存] submit', {
      endTime,
      id: record.id,
      passType: record.passType,
      fallbackDate,
      rawEndTime: toDateTimeText(reportForm.endTime),
      rawStartTime: toDateTimeText(reportForm.startTime),
      reportDate,
      startTime,
    });
    await updateRoughGrindingConsoleReportTime({
      endTime,
      id: record.id,
      passType: record.passType,
      reportDate,
      startTime,
    });
    await loadBoard({
      equipmentId: equipmentInfo.id || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    reportVisible.value = false;
    reportReadonly.value = false;
    reportTimeEditing.value = false;
    reportCurrentRecord.value = undefined;
    message.success('报工开始/结束时间已保存');
  } finally {
    reportTimeSaving.value = false;
  }
};

const getCurrentFirstAllocationConfirmedLength = () =>
  toNumber(reportForm.processLength ?? reportCurrentRecord.value?.processLength);

const openFirstAllocationQuantityRevisionModal = () => {
  if (!canReviseStatisticsData.value) {
    message.warning('未找到可修订的磨皮统计数据');
    return;
  }
  firstAllocationQuantityRevisionForm.confirmedLength =
    getCurrentFirstAllocationConfirmedLength();
  firstAllocationQuantityRevisionForm.processLength = toNumber(
    reportForm.processLength ?? reportCurrentRecord.value?.processLength,
  );
  firstAllocationQuantityRevisionForm.lossLength = toNumber(
    reportForm.lossLength ?? reportCurrentRecord.value?.lossLength,
  );
  firstAllocationQuantityRevisionForm.outputLength = toNumber(
    reportForm.outputLength ?? reportCurrentRecord.value?.outputLength,
  );
  firstAllocationQuantityRevisionForm.reason = '';
  firstAllocationQuantityRevisionVisible.value = true;
};

const handleFirstAllocationQuantityRevisionConfirm = async () => {
  const role = statisticsRevisionRole.value;
  const firstAllocationId = Number(
    reportForm.firstAllocationId || reportCurrentRecord.value?.firstAllocationId || reportCurrentRecord.value?.id || 0,
  );
  const recordId = Number(reportCurrentRecord.value?.id || 0);
  const confirmedLength = Number(firstAllocationQuantityRevisionForm.confirmedLength ?? 0);
  const processLength = Number(firstAllocationQuantityRevisionForm.processLength ?? 0);
  const lossLength = Number(firstAllocationQuantityRevisionForm.lossLength ?? 0);
  const outputLength = Number(firstAllocationQuantityRevisionForm.outputLength ?? 0);
  const reason = String(firstAllocationQuantityRevisionForm.reason || '').trim();
  if (role === 'FIRST_ALLOCATION' && !firstAllocationId) {
    message.warning('未找到一磨分段处理记录，无法保存');
    return;
  }
  if (role !== 'FIRST_ALLOCATION' && !recordId) {
    message.warning('未找到磨皮报工记录，无法保存');
    return;
  }
  if (role === 'FIRST_ALLOCATION' && (!Number.isFinite(confirmedLength) || confirmedLength <= 0)) {
    message.warning('请填写大于 0 的修正后确认加工米数');
    return;
  }
  if (
    role !== 'FIRST_ALLOCATION' &&
    ![processLength, lossLength, outputLength].every((value) => Number.isFinite(value) && value >= 0)
  ) {
    message.warning('统计数据不能为负数');
    return;
  }
  if (!reason) {
    message.warning('请填写修订原因');
    return;
  }
  const currentLength = getCurrentFirstAllocationConfirmedLength();
  if (role === 'FIRST_ALLOCATION' && Math.abs(confirmedLength - currentLength) < 0.0001) {
    message.warning('修正后确认加工米数与当前米数一致，无需更新');
    return;
  }
  if (
    role !== 'FIRST_ALLOCATION' &&
    Math.abs(processLength - toNumber(reportForm.processLength ?? reportCurrentRecord.value?.processLength)) < 0.0001 &&
    Math.abs(lossLength - toNumber(reportForm.lossLength ?? reportCurrentRecord.value?.lossLength)) < 0.0001 &&
    Math.abs(outputLength - toNumber(reportForm.outputLength ?? reportCurrentRecord.value?.outputLength)) < 0.0001
  ) {
    message.warning('修订后统计数据与当前数据一致，无需更新');
    return;
  }
  const normalizedLength = Number(confirmedLength.toFixed(3));
  const normalizedProcessLength = Number(processLength.toFixed(3));
  const normalizedLossLength = Number(lossLength.toFixed(3));
  const normalizedOutputLength = Number(outputLength.toFixed(3));
  firstAllocationQuantityRevisionSubmitting.value = true;
  try {
    await reviseRoughGrindingConsoleStatisticsData({
      confirmedLength: role === 'FIRST_ALLOCATION' ? normalizedLength : undefined,
      firstAllocationId,
      id: role === 'FIRST_ALLOCATION' ? firstAllocationId : recordId,
      lossLength: role === 'FIRST_ALLOCATION' ? undefined : normalizedLossLength,
      outputLength: role === 'FIRST_ALLOCATION' ? normalizedLength : normalizedOutputLength,
      processLength: role === 'FIRST_ALLOCATION' ? normalizedLength : normalizedProcessLength,
      reason,
      recordRole: role,
    });
    reportForm.processLength = role === 'FIRST_ALLOCATION' ? normalizedLength : normalizedProcessLength;
    reportForm.lossLength = role === 'FIRST_ALLOCATION' ? reportForm.lossLength : normalizedLossLength;
    reportForm.outputLength = role === 'FIRST_ALLOCATION' ? normalizedLength : normalizedOutputLength;
    if (reportCurrentRecord.value) {
      reportCurrentRecord.value = {
        ...reportCurrentRecord.value,
        lossLength: role === 'FIRST_ALLOCATION' ? reportCurrentRecord.value.lossLength : normalizedLossLength,
        outputLength: role === 'FIRST_ALLOCATION' ? normalizedLength : normalizedOutputLength,
        processLength: role === 'FIRST_ALLOCATION' ? normalizedLength : normalizedProcessLength,
      };
    }
    if (role === 'FIRST_ALLOCATION') {
      syncProcessItemsWithReportLength();
    }
    await loadBoard({
      equipmentId: equipmentInfo.id || undefined,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    firstAllocationQuantityRevisionVisible.value = false;
    message.success('磨皮母卷批次良品统计数据已修订');
  } finally {
    firstAllocationQuantityRevisionSubmitting.value = false;
  }
};

const handleDeleteCurrentReport = () => {
  const record = reportCurrentRecord.value;
  if (!record?.id) {
    message.warning('未找到可删除的报工记录');
    return;
  }
  if (record.passType === 'SECOND' && isConfirmedWorkRecord(record)) {
    message.warning('第二次磨皮已扫码确认，不能删除');
    return;
  }
  const isFirst = record.passType === 'FIRST';
  const isFirstAllocation = record.recordRole === 'FIRST_ALLOCATION';
  const content = isFirstAllocation
    ? '确认后将删除当前一磨分段处理、对应工艺参数点检、耗材事件和生产记录，并回滚该段米数。已有对应二磨时不能删除。确认继续吗？'
    : isFirst
    ? '删除第一次磨皮前必须先删除全部第二次磨皮报工。确认后将物理删除当前一次磨皮报工、工艺参数点检表和异常位置，并回滚来源余额与耗材使用，删除后可重新报工。确认继续删除吗？'
    : '确认后将物理删除当前第二次磨皮报工、工艺参数点检表、中间品记录和异常位置，并回滚耗材使用，删除后可重新报工。确认继续删除吗？';
  AModal.confirm({
    cancelText: '取消',
    content,
    okText: '确认删除',
    okType: 'danger',
    title: isFirstAllocation
      ? `删除一磨${getSegmentLabel(record.segmentMark)}处理`
      : `删除${isFirst ? '第一次' : '第二次'}磨皮报工`,
    async onOk() {
      reportDeleting.value = true;
      try {
        if (isFirstAllocation) {
          await deleteRoughGrindingConsoleFirstAllocation(record.firstAllocationId || record.id!);
        } else if (isFirst) {
          await deleteRoughGrindingConsoleFirstReport(record.id!);
        } else {
          await deleteRoughGrindingConsoleSecondReport(record.id!);
        }
        reportVisible.value = false;
        reportReadonly.value = false;
        reportTimeEditing.value = false;
        reportCurrentRecord.value = undefined;
        workRecordListVisible.value = false;
        await loadBoard({
          equipmentId: equipmentInfo.id || undefined,
          planId: currentPlan.planId,
          planOperationId: currentPlan.planOperationId,
        });
        message.success('报工记录已删除，相关数据已回滚，可重新报工');
      } finally {
        reportDeleting.value = false;
      }
    },
  });
};

const loadSubmittedReportAbnormalRows = async (record: WorkRecord) => {
  reportAbnormalRows.value = [];
  if (!record.id || record.recordRole === 'FIRST_ALLOCATION') {
    return true;
  }
  const recordKey = record.recordKey;
  try {
    await loadRoughAbnormalPositionList();
  } catch {
    message.warning('异常位置加载失败，请稍后重试');
    return true;
  }
  if (reportCurrentRecord.value?.recordKey !== recordKey) {
    return false;
  }
  const processStage =
    record.passType === 'FIRST' ? 'FIRST_GRINDING' : 'SECOND_GRINDING';
  reportAbnormalRows.value = abnormalPositionRows.value
    .filter(
      (row) =>
        row.sourceMenuCode === 'ROUGH_GRINDING_REPORT' &&
        row.processStage === processStage &&
        Number(row.sourceDetailId || 0) === Number(record.id),
    )
    .map((row) =>
      createReportAbnormalRow({
        abnormalLength: row.abnormalLength,
        positionText: row.positionText,
        remark: row.remark,
        sortOrder: row.sortOrder,
      }),
    );
  return true;
};

const openSubmittedReportDetail = async (record?: WorkRecord) => {
  if (!record) return;
  reportCurrentRecord.value = record;
  reportReadonly.value = true;
  reportTimeEditing.value = false;
  activePass.value = record.passType;
  resetReportForm();
  reportForm.batchNo = record.passType === 'SECOND' || record.recordRole === 'FIRST_ALLOCATION'
    ? record.productionBatchNo || record.batchNo || ''
    : record.motherBatchNo || record.batchNo || '';
  reportForm.defectCode = record.defectCode || '';
  const fallbackDate = resolveReportTimeFallbackDate(record);
  reportForm.endTime = normalizeReportDateTimeText(record.endTime, fallbackDate) || record.endTime || '';
  reportForm.firstAllocationId = record.firstAllocationId;
  reportForm.lossLength = record.lossLength || 0;
  reportForm.napSampleLength = record.napSampleLength || 0;
  reportForm.researchConsumptionLength = record.researchConsumptionLength || 0;
  reportForm.outputLength = record.outputLength || 0;
  reportForm.planNo = record.planNo || currentPlan.planNo || '';
  reportForm.processLength = record.processLength || 0;
  reportForm.segmentMark = normalizeSegmentMark(record.segmentMark);
  reportForm.selfCheck = record.selfCheck || 'OK';
  reportForm.startPosition = record.startPosition || 0;
  reportForm.startTime = normalizeReportDateTimeText(record.startTime, fallbackDate) || record.startTime || '';
  setProcessItemsFromWorkRecord(record);
  if (!(await loadSubmittedReportAbnormalRows(record))) {
    return;
  }
  reportStep.value = 'REPORT';
  activeReportTab.value = 'basic';
  workRecordListVisible.value = false;
  reportVisible.value = true;
  void loadRoughDevProcessParamRecords(record.passType);
};

const openSecondGrindingVisualAction = async (segmentMark = '') => {
  const pendingAllocation = isFirstAllocatedMode.value
    ? getFirstAllocationBySegment(segmentMark, true)
    : undefined;
  const hasPendingAllocation = !!pendingAllocation && !secondWorkRecords.value.some(
    (record) => Number(record.firstAllocationId || 0) === Number(pendingAllocation.id || 0),
  );
  if (hasPendingAllocation) {
    if (!(await ensureRoughSampleAbnormalUnlocked(segmentMark, '打开二磨报工', 'SECOND'))) {
      return;
    }
    focusedWorkRecordKey.value = undefined;
    void openReport('SECOND', segmentMark);
    return;
  }
  const latestRecord = getSecondSegmentLatestRecord(segmentMark);
  if (latestRecord) {
    openSubmittedReportDetail(latestRecord);
    return;
  }
  if (!(await ensureRoughSampleAbnormalUnlocked(segmentMark, '打开二磨报工', 'SECOND'))) {
    return;
  }
  focusedWorkRecordKey.value = undefined;
  void openReport('SECOND', segmentMark);
};

const openReport = async (pass: GrindPass, segmentMark = '') => {
  reportReadonly.value = false;
  reportTimeEditing.value = false;
  reportCurrentRecord.value = undefined;
  if (!currentPlan.planOperationId || !currentPlan.planNo) {
    message.warning('请先扫码带出真实磨皮计划');
    return;
  }
  if (!currentPlan.batchNo) {
    message.warning('当前计划未带出母批批号，无法报工');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前工单此工序已完工，不能再报工！');
    return;
  }
  const openingFirstAllocation = pass === 'FIRST' && isFirstAllocatedMode.value;
  if (!openingFirstAllocation && !(await ensureRoughSampleAbnormalUnlocked(segmentMark, '报工', pass))) {
    return;
  }
  if (!isWorkOrderRunning.value) {
    AModal.confirm({
      title: '请先执行开工确认',
      content: '当前工单磨皮工序尚未开工，必须选择机台并完成操作工确认后才能报工。',
      okText: '执行开工确认',
      cancelText: '取消',
      onOk: () => requestOperationAuth('START'),
    });
    return;
  }
  if (!dailyPreparationCompleted.value) {
    showDailyCheckGuide();
    return;
  }
  if (pass === 'SECOND' && !ensureFirstGrindingReportedBeforeSecond()) {
    return;
  }
  activePass.value = pass;
  resetReportForm();
  reportForm.segmentMark = segmentMark;
  reportForm.batchNo = pass === 'SECOND' || openingFirstAllocation
    ? buildSecondGrindingBatchNo(currentPlan.batchNo, segmentMark)
    : firstReportSourceBatchNo.value;
  reportForm.planNo = openingFirstAllocation
    ? currentPlan.planNo
    : pass === 'FIRST' ? firstReportSourcePlanNo.value : currentPlan.planNo;
  reportForm.startPosition = openingFirstAllocation
    ? getFirstAllocationSuggestedStartPosition()
    : pass === 'SECOND' ? getSecondStartPosition() : firstReportStartPosition.value;
  if (openingFirstAllocation) {
    reportForm.processLength = Number(firstAllocationRemainingLength.value.toFixed(3));
  } else if (pass === 'FIRST') {
    reportForm.processLength = Number(firstReportAvailableLength.value.toFixed(3));
  } else {
    reportForm.processLength = null;
  }
  if (pass === 'SECOND' && isFirstAllocatedMode.value) {
    const allocation = getFirstAllocationBySegment(segmentMark, true);
    if (!allocation?.id) {
      message.warning(`请先完成一磨${getSegmentLabel(segmentMark)}处理确认`);
      return;
    }
    reportForm.firstAllocationId = allocation.id;
    reportForm.startPosition = Number(allocation.startPosition || 0);
    reportForm.processLength = Number(allocation.confirmedLength || 0);
    reportForm.batchNo = allocation.productionBatchNo || buildSecondGrindingBatchNo(currentPlan.batchNo, segmentMark);
  }
  if (pass === 'FIRST') {
    syncProcessItemsWithReportLength();
  }
  if (pass === 'SECOND' || openingFirstAllocation) {
    const timing = getSegmentTiming(pass, segmentMark);
    if (timing?.startTime) {
      reportForm.startTime = timing.startTime;
    }
    if (timing?.endTime) {
      reportForm.endTime = timing.endTime;
    }
  }
  if (pass === 'SECOND') {
    syncMiddleProductRowsFromReport(true);
  }
  reportStep.value = 'PARAMS';
  activeReportTab.value = 'basic';
  reportVisible.value = true;
  void loadRoughDevProcessParamRecords(pass);
  if (pass === 'FIRST') {
    focusReportField('processLength');
  }
};

const startReportWork = async () => {
  if (!reportForm.batchNo) {
    message.warning('请先扫码或输入母批批号');
    return;
  }
  if (isFirstAllocationReport.value) {
    activeReportTab.value = 'dev-process-param';
    await loadRoughDevProcessParamRecords(activePass.value);
    return;
  }
  if (!isFirstAllocationReport.value && !(await ensureRoughSampleAbnormalUnlocked(reportForm.segmentMark, '开工报工', activePass.value))) {
    return;
  }
  if (!normalizeReportDateTimeText(reportForm.startTime, resolveReportTimeFallbackDate())) {
    reportForm.startTime = buildNowText();
  }
  normalizeReportFormTimeField('startTime', reportForm.startTime);
  activeReportTab.value = 'dev-process-param';
  await loadRoughDevProcessParamRecords(activePass.value);
};

const handleReportParamsAction = async () => {
  if (activeReportTab.value !== 'dev-process-param') {
    activeReportTab.value = 'dev-process-param';
    await loadRoughDevProcessParamRecords(activePass.value);
    return;
  }
  if (roughDevProcessParamLoading.value[activePass.value]) {
    message.info('工艺参数点检表正在加载，请稍候');
    return;
  }
  const records = getRoughDevProcessParamRecords(activePass.value);
  if (!records.length) {
    void openRoughDevProcessParamFill(activePass.value);
    return;
  }
  if (!hasConfirmedRoughDevProcessParamRecord(activePass.value)) {
    const currentRecord = records.find((record) => isRoughDevProcessParamDraft(record)) || records[0];
    if (!currentRecord) {
      return;
    }
    message.info('请先完成并确认已有的工艺参数点检表，再进入报工收卷。');
    void openRoughDevProcessParamDetail(
      currentRecord,
      isRoughDevProcessParamDraft(currentRecord) ? 'edit' : 'view',
    );
    return;
  }
  void goReportForm();
};

const getReportParamsActionText = () => {
  if (activeReportTab.value !== 'dev-process-param') return '填写工艺参数点检表';
  const records = getRoughDevProcessParamRecords(activePass.value);
  if (!records.length) return '新建工艺参数点检表';
  if (!hasConfirmedRoughDevProcessParamRecord(activePass.value)) return '继续填写并确认工艺点检表';
  return '进入报工收卷';
};

const goReportForm = async () => {
  if (!isFirstAllocationReport.value && !reportForm.startTime) {
    message.warning('请先点击开工');
    return;
  }
  if (!isFirstAllocationReport.value && !(await ensureRoughSampleAbnormalUnlocked(reportForm.segmentMark, '报工收卷', activePass.value))) {
    return;
  }
  if (roughDevProcessParamLoading.value[activePass.value]) {
    message.info('当前加工单元的工艺参数点检表正在加载，请稍候');
    return;
  }
  if (!hasConfirmedRoughDevProcessParamRecord(activePass.value)) {
    activeReportTab.value = 'dev-process-param';
    message.warning(`请先填写并确认${getRoughDevProcessParamName(activePass.value)}，再进入报工收卷。`);
    return;
  }
  syncProcessItemsWithReportLength();
  if (!isFirstAllocationReport.value && !normalizeReportDateTimeText(reportForm.endTime, resolveReportTimeFallbackDate())) {
    reportForm.endTime = buildNowText();
  }
  if (!isFirstAllocationReport.value) {
    normalizeReportFormTimeField('endTime', reportForm.endTime);
  }
  reportStep.value = 'REPORT';
  activeReportTab.value = 'report';
  nextTick(focusPreferredReportField);
};

const getWorkRecordTimeRange = () => {
  const startTimes = workRecords.value
    .map((record) => normalizeReportDateTimeText(record.startTime, resolveReportTimeFallbackDate(record)))
    .filter(Boolean)
    .sort();
  const endTimes = workRecords.value
    .map((record) => normalizeReportDateTimeText(record.endTime, resolveReportTimeFallbackDate(record)))
    .filter(Boolean)
    .sort();
  return {
    endTime: endTimes[endTimes.length - 1] || '',
    startTime: startTimes[0] || '',
  };
};

const buildWorkOrderCompletePayload = () => {
  const now = buildNowText();
  const savedRange = getWorkRecordTimeRange();
  const fallbackDate = resolveReportTimeFallbackDate();
  const startTime = firstText(
    savedRange.startTime,
    normalizeReportDateTimeText(reportForm.startTime, fallbackDate),
    normalizeReportDateTimeText(currentPlan.startTime, fallbackDate),
    now,
  );
  const endTime = firstText(
    savedRange.endTime,
    normalizeReportDateTimeText(reportForm.endTime, fallbackDate),
    now,
  );
  return {
    batchNo: currentPlan.batchNo,
    confirmerName: operatorName.value || currentUserName.value,
    confirmerTime: now,
    endTime,
    equipmentCode: equipmentInfo.code,
    equipmentId: equipmentInfo.id,
    equipmentName: equipmentInfo.name,
    extraJson: JSON.stringify({
      completeSource: 'ROUGH_CONSOLE',
      consoleSummary: {
        firstLossLength: dailyFirstSummary.lossLength,
        firstNapSampleLength: dailyFirstSummary.napSampleLength,
        firstOutputLength: dailyFirstSummary.outputLength,
        firstProcessLength: dailyFirstSummary.processLength,
        secondLossLength: dailySecondSummary.lossLength,
        secondNapSampleLength: dailySecondSummary.napSampleLength,
        secondResearchConsumptionLength: dailySecondSummary.researchConsumptionLength,
        secondOutputLength: dailySecondSummary.outputLength,
        secondProcessLength: dailySecondSummary.processLength,
      },
    }),
    firstLossLength: Number(dailyFirstSummary.lossLength || 0),
    firstNapSampleLength: Number(dailyFirstSummary.napSampleLength || 0),
    firstOutputLength: Number(dailyFirstSummary.outputLength || 0),
    firstProcessLength: Number(dailyFirstSummary.processLength || 0),
    inputLength: Number(currentPlan.previousOutputLength || 0),
    planId: currentPlan.planId!,
    planOperationId: currentPlan.planOperationId!,
    recorderName: currentPlan.recorderName || operatorName.value || currentUserName.value,
    recorderTime: startTime,
    remark: `磨皮看板完工：一次磨皮${formatNumber(dailyFirstSummary.processLength)}，二次磨皮${formatNumber(dailySecondSummary.processLength)}`,
    reportDate: reportDateFromDateTime(endTime || startTime, fallbackDate),
    reportQty: Number(dailySecondSummary.processLength || 0),
    secondLossLength: Number(dailySecondSummary.lossLength || 0),
    secondNapSampleLength: Number(dailySecondSummary.napSampleLength || 0),
    secondOutputLength: Number(dailySecondSummary.outputLength || 0),
    secondProcessLength: Number(dailySecondSummary.processLength || 0),
    startTime,
  };
};

const ensureSecondReportsConfirmedBeforeComplete = (silent = false) => {
  const unconfirmedRecords = unconfirmedSecondWorkRecords.value;
  if (!unconfirmedRecords.length) {
    return true;
  }
  if (!silent) {
    const firstUnconfirmed = unconfirmedRecords[0];
    focusedWorkRecordKey.value = firstUnconfirmed?.recordKey;
    selectedWorkRecordKeys.value = unconfirmedRecords.map((record) => record.recordKey);
    workRecordListVisible.value = true;
    message.warning(`还有 ${unconfirmedRecords.length} 条第二次磨皮报工未扫码确认，请全部扫码确认后再进行工单完工。`);
  }
  return false;
};

const ensureFirstAllocatedReportsClosedBeforeComplete = (silent = false) => {
  if (!isFirstAllocatedMode.value) return true;
  if (!firstAllocationRows.value.length) {
    if (!silent) message.warning('当前为一磨分段模式，请先完成至少一个一磨加工单元');
    return false;
  }
  if (firstAllocationRemainingLength.value > SECOND_RANGE_EPS) {
    if (!silent) {
      message.warning(
        `一磨还有 ${formatNumber(firstAllocationRemainingLength.value)}m 未确认，请继续分段或用“不分段”收口后再完工`,
      );
    }
    return false;
  }
  const invalidAllocations = firstAllocationRows.value.filter((allocation) => {
    const matchedCount = secondWorkRecords.value.filter(
      (record) => Number(record.firstAllocationId || 0) === Number(allocation.id || 0),
    ).length;
    return matchedCount !== 1;
  });
  if (invalidAllocations.length) {
    if (!silent) {
      const labels = invalidAllocations
        .map((allocation) => getSegmentLabel(normalizeFirstAllocationSegmentMark(allocation.segmentMark)))
        .join('、');
      message.warning(`以下一磨加工单元尚未形成唯一对应二磨：${labels}`);
    }
    return false;
  }
  return true;
};

const executeWorkOrderComplete = async () => {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从工作列表选择磨皮工单');
    return;
  }
  if (isWorkOrderFinished.value) {
    message.warning('当前工单此工序已完工，不能重复完工！');
    return;
  }
  if (!isWorkOrderRunning.value) {
    message.warning('当前工单此工序未开工，不能完工');
    return;
  }
  if (!equipmentInfo.id) {
    message.warning('当前工单未挂接机台，不能完工');
    return;
  }
  if (!ensureFirstAllocatedReportsClosedBeforeComplete()) {
    return;
  }
  if (!ensureSecondReportsConfirmedBeforeComplete()) {
    return;
  }
  await completeRoughGrindingConsoleWorkOrder(buildWorkOrderCompletePayload());
  await loadBoard({
    equipmentId: equipmentInfo.id,
    planId: currentPlan.planId,
    planOperationId: currentPlan.planOperationId,
  });
  message.success('工单磨皮工作已完工，后续不能再磨皮报工');
};

const requestWorkOrderComplete = async (auto = false) => {
  if (!currentPlan.planId || !currentPlan.planOperationId) {
    message.warning('请先扫码或从工作列表选择磨皮工单');
    return;
  }
  if (!ensureFirstAllocatedReportsClosedBeforeComplete(auto)) {
    return;
  }
  if (!ensureSecondReportsConfirmedBeforeComplete(auto)) {
    return;
  }
  const previousLength = Number(currentPlan.previousOutputLength || 0);
  const secondLength = Number(dailySecondSummary.processLength || 0);
  AModal.confirm({
    title: auto ? '累计投入米数已达到前序湿法报工米数' : '确认工单完工',
    content: h('div', { class: 'work-order-complete-confirm' }, [
      h('p', auto
        ? `当前二次磨皮累计 ${formatNumber(secondLength)}，已达到或超过前序湿法报工 ${formatNumber(previousLength)}。`
        : `当前二次磨皮累计 ${formatNumber(secondLength)}，前序湿法报工 ${formatNumber(previousLength)}。`),
      h('p', '执行此操作后，系统将标识该工单磨皮工作全部完成。'),
      h('p', '完工后该工单不能继续新增一磨或二磨报工记录。'),
      h('p', '请确认所有分段打印、扫码确认和中间品记录已经处理完成。'),
    ]),
    okText: '确认完工',
    okType: 'danger',
    cancelText: auto ? '继续报工' : '取消',
    onOk: executeWorkOrderComplete,
  });
};

const promptCompletionIfReached = () => {
  const previousLength = Number(currentPlan.previousOutputLength || 0);
  const secondLength = Number(dailySecondSummary.processLength || 0);
  if (!isWorkOrderRunning.value || previousLength <= 0 || secondLength + 0.001 < previousLength) {
    return;
  }
  const key = `${currentPlan.planOperationId}-${previousLength}-${secondLength}`;
  if (completionPromptKey.value === key) {
    return;
  }
  completionPromptKey.value = key;
  requestWorkOrderComplete(true);
};

const handleReportReplaceChange = (target: 'GUIDE_CLOTH' | 'SANDPAPER') => {
  if (target === 'SANDPAPER') {
    if (reportForm.sandpaperChanged === 'YES') {
      openReportConsumableLedger('SANDPAPER');
    } else {
      clearReportConsumableSelection('SANDPAPER');
      reportForm.sandpaperNewBatchNo = '';
    }
    return;
  }
  if (reportForm.guideClothChanged === 'YES') {
    openReportConsumableLedger('GUIDE_CLOTH');
  } else {
    clearReportConsumableSelection('GUIDE_CLOTH');
    reportForm.guideClothNewBatchNo = '';
  }
};

const validateSecondReportRange = () => {
  if (activePass.value !== 'SECOND') {
    return true;
  }
  if (!ensureFirstGrindingReportedBeforeSecond()) {
    focusReportField('processLength');
    return false;
  }
  const start = Number(reportForm.startPosition || 0);
  const length = Number(reportForm.processLength || 0);
  const end = start + length;
  if (length <= 0) {
    message.warning('第二次磨皮投入米数必须大于 0');
    focusReportField('processLength');
    return false;
  }
  if (isFirstAllocatedMode.value) {
    const allocationRange = currentSecondFirstAllocationRange.value;
    if (!allocationRange || allocationRange.length <= SECOND_RANGE_EPS) {
      message.warning('请先选择对应的一磨加工单元');
      focusReportField('processLength');
      return false;
    }
    if (start < allocationRange.start - SECOND_RANGE_EPS || end > allocationRange.end + SECOND_RANGE_EPS) {
      message.warning(`第二次磨皮区间 ${secondReportRangeText.value} 必须在当前一磨加工区间 ${currentSecondFirstAllocationRangeText.value} 内`);
      focusReportField('processLength');
      return false;
    }
  } else {
    const totalFirstLength = getFirstGrindingOutputTotalLength();
    if (totalFirstLength > 0 && end - totalFirstLength > SECOND_RANGE_EPS) {
      message.warning(`第二次磨皮区间 ${secondReportRangeText.value} 不能超出当前一磨产出米数 ${formatNumber(totalFirstLength)}`);
      focusReportField('processLength');
      return false;
    }
  }
  if (hasSecondReportOverlap.value) {
    const conflict = secondReportOverlapRanges.value[0]!;
    message.warning(
      `第二次磨皮区间 ${secondReportRangeText.value} 与已报区间 ${conflict.start.toFixed(3)}-${conflict.end.toFixed(3)} m 重叠`,
    );
    focusReportField('startPosition');
    return false;
  }
  return true;
};

const validateFirstAllocationReportRange = () => {
  if (!isFirstAllocationReport.value) return true;
  const start = Number(reportForm.startPosition || 0);
  const length = Number(reportForm.processLength || 0);
  const end = start + length;
  const totalLength = firstAllocationSourceTotalLength.value;
  if (!Number.isFinite(start) || start < 0) {
    message.warning('一磨分段起位置必须大于或等于 0');
    focusReportField('startPosition');
    return false;
  }
  if (!totalLength || end - totalLength > SECOND_RANGE_EPS) {
    message.warning(
      `一磨加工区间 ${firstAllocationReportRangeText.value} 不能超出母批总长度 ${formatNumber(totalLength)}m`,
    );
    focusReportField('startPosition');
    return false;
  }
  if (hasFirstAllocationReportOverlap.value) {
    const conflict = firstAllocationReportOverlapRanges.value[0]!;
    message.warning(
      `一磨加工区间 ${firstAllocationReportRangeText.value} 与已报${conflict.segment || '不分段'}区间 ${conflict.start.toFixed(3)}-${conflict.end.toFixed(3)}m 重叠`,
    );
    focusReportField('startPosition');
    return false;
  }
  return true;
};

const executeReportSubmission = async () => {
  if (!isFirstAllocationReport.value && !(await ensureRoughSampleAbnormalUnlocked(reportForm.segmentMark, '提交报工', activePass.value))) {
    return;
  }
  if (Number(reportForm.processLength || 0) <= 0) {
    message.warning('投入米数必须大于 0');
    return;
  }
  const abnormalPositions = isFirstAllocationReport.value
    ? []
    : buildReportAbnormalPositions();
  if (abnormalPositions === null || !validateSecondReportOutputBalance()) {
    return;
  }
  if (!validateFirstAllocationReportRange()) {
    return;
  }
  const processCheck = await buildCurrentRoughDevProcessCheckPayload(activePass.value);
  if (!processCheck) {
    activeReportTab.value = 'dev-process-param';
    message.warning(`请先填写并确认${getRoughDevProcessParamName(activePass.value)}，再提交报工。`);
    return;
  }
  if (activePass.value === 'FIRST') {
    const availableLength = Number(
      isFirstAllocationReport.value
        ? firstAllocationRemainingLength.value
        : firstReportAvailableLength.value || 0,
    );
    const processLength = Number(reportForm.processLength || 0);
    if (availableLength <= 0) {
      message.warning('当前扫码计划没有可加工剩余收卷数，不能提交第一次磨皮报工');
      return;
    }
    if (processLength - availableLength > 0.0001) {
      message.warning(`投入米数不能超出当前可加工数量 ${formatNumber(availableLength)}`);
      focusReportField('processLength');
      return;
    }
  }
  if (!validateSecondReportRange()) {
    return;
  }
  const consumptionError = validateGrindingConsumption(reportConsumption.value,
    reportForm.sandpaperChanged === 'YES', reportForm.guideClothChanged === 'YES');
  if (consumptionError) { message.warning(consumptionError); return; }
  if (reportForm.sandpaperChanged === 'YES' && !reportForm.sandpaperNewBatchNo) {
    message.warning('砂纸更换时必须填写新的砂纸批号');
    return;
  }
  if (reportForm.guideClothChanged === 'YES' && !reportForm.guideClothNewBatchNo) {
    message.warning('导布更换时必须填写新的导布批号');
    return;
  }
  if (activePass.value === 'SECOND') {
    syncMiddleProductRowsFromReport(true);
  }

  if (currentPlan.planId && currentPlan.planOperationId) {
    if (isFirstAllocationReport.value) {
      const startPosition = Number(reportForm.startPosition || 0);
      await saveRoughGrindingConsoleFirstAllocation({
        consumption: reportConsumption.value,
        confirmedLength: Number(reportForm.processLength || 0),
        currentGuideClothBatchNo: guideCloth.batchNo === '-' ? '' : guideCloth.batchNo,
        currentSandpaperBatchNo: sandpaper.batchNo === '-' ? '' : sandpaper.batchNo,
        equipmentId: equipmentInfo.id!,
        guideClothBatchNo: reportForm.guideClothChanged === 'YES'
          ? reportForm.guideClothNewBatchNo
          : guideCloth.batchNo,
        guideClothChanged: reportForm.guideClothChanged === 'YES',
        guideClothReplaceReason: reportForm.guideClothChanged === 'YES'
          ? reportForm.guideClothReplaceReason
          : undefined,
        motherBatchNo: currentPlan.batchNo,
        operatorId: operatorId.value,
        operatorName: operatorName.value,
        planId: currentPlan.planId,
        planNo: currentPlan.planNo,
        planOperationId: currentPlan.planOperationId,
        processFormRecordId: processCheck.recordId,
        processCheckDetails: processCheck.details,
        processCheckHeaderDataJson: processCheck.headerDataJson,
        sandpaperBatchNo: reportForm.sandpaperChanged === 'YES'
          ? reportForm.sandpaperNewBatchNo
          : sandpaper.batchNo,
        sandpaperChanged: reportForm.sandpaperChanged === 'YES',
        sandpaperReplaceReason: reportForm.sandpaperChanged === 'YES'
          ? reportForm.sandpaperReplaceReason
          : undefined,
        segmentMark: toFirstAllocationSegmentMark(reportForm.segmentMark),
        startPosition,
      });
      try {
        await updateReportConsumableUsageStatusesAfterSubmit();
      } catch {
        message.warning('一磨分段处理已保存，但边库耗材使用状态更新失败，请在边库台账中核对处理');
      }
      await loadBoard({
        equipmentId: equipmentInfo.id,
        planId: currentPlan.planId,
        planOperationId: currentPlan.planOperationId,
      });
      reportVisible.value = false;
      message.success('一磨分段处理、工艺点检及生产记录已保存');
      return;
    }
    const { grindingMeters, qualityThickness, qualityWidth, ...processPayload } = buildProcessPayloadFields(processCheck.details);
    const middleProductPayload = activePass.value === 'SECOND'
      ? {
          middleProductDetails: buildMiddleProductPayloadRows(),
          middleProductHeaderDataJson: buildMiddleProductHeaderPayload(activePass.value),
        }
      : {};
    const reportTimeFallbackDate = resolveReportTimeFallbackDate();
    const reportStartTime = normalizeReportFormTimeField('startTime', reportForm.startTime);
    const reportEndTime = normalizeReportFormTimeField('endTime', reportForm.endTime);
    const reportDate = reportDateFromDateTime(reportStartTime || reportEndTime, reportTimeFallbackDate);
    console.info('[磨皮报工收卷保存] submit', {
      activePass: activePass.value,
      reportTimeFallbackDate,
      reportDate,
      reportEndTime,
      reportStartTime,
      rawEndTime: toDateTimeText(reportForm.endTime),
      rawStartTime: toDateTimeText(reportForm.startTime),
    });
    const basePayload = {
      consumption: reportConsumption.value,
      ...processPayload,
      ...middleProductPayload,
      processFormRecordId: processCheck.recordId,
      processCheckDetails: processCheck.details,
      processCheckHeaderDataJson: processCheck.headerDataJson,
      currentGuideClothBatchNo: guideCloth.batchNo === '-' ? '' : guideCloth.batchNo,
      currentSandpaperBatchNo: sandpaper.batchNo === '-' ? '' : sandpaper.batchNo,
      defectCode: reportForm.defectCode,
      endTime: reportEndTime || undefined,
      equipmentCode: equipmentInfo.code,
      equipmentId: equipmentInfo.id,
      equipmentName: equipmentInfo.name,
      guideClothBatchNo: reportForm.guideClothChanged === 'YES' ? reportForm.guideClothNewBatchNo : guideCloth.batchNo,
      guideClothChanged: reportForm.guideClothChanged === 'YES',
      guideClothReplaceReason: reportForm.guideClothChanged === 'YES' ? reportForm.guideClothReplaceReason : undefined,
      lossLength: reportLossLengthValue.value,
      motherBatchNo: activePass.value === 'SECOND' ? currentPlan.batchNo : reportForm.batchNo,
      napSampleLength: reportNapSampleLengthValue.value,
      researchConsumptionLength: reportResearchConsumptionLengthValue.value,
      operatorId: operatorId.value,
      operatorName: operatorName.value,
      outputLength: reportOutputLengthValue.value,
      planId: currentPlan.planId,
      planNo: currentPlan.planNo,
      planOperationId: currentPlan.planOperationId,
      processLength: Number(reportForm.processLength || 0),
      remainLength: activePass.value === 'FIRST' ? Number(firstReportAvailableLength.value || 0) : undefined,
      remainStartMeter: activePass.value === 'FIRST' ? Number(reportForm.startPosition || 0) : undefined,
      reportDate,
      sandpaperBatchNo: reportForm.sandpaperChanged === 'YES' ? reportForm.sandpaperNewBatchNo : sandpaper.batchNo,
      sandpaperChanged: reportForm.sandpaperChanged === 'YES',
      sandpaperReplaceReason: reportForm.sandpaperChanged === 'YES' ? reportForm.sandpaperReplaceReason : undefined,
      selfCheck: reportForm.selfCheck,
      sourcePlanNo: activePass.value === 'FIRST' ? firstReportSourcePlanNo.value : currentPlan.planNo,
      sourceProductionBatchNo: activePass.value === 'FIRST' ? firstReportSourceBatchNo.value : currentPlan.batchNo,
      startTime: reportStartTime || undefined,
      workCenterId: currentTask.value?.workCenterId,
    };
    await saveRoughGrindingConsoleSecondReport({
      ...basePayload,
      abnormalPositions,
      motherBatchNo: currentPlan.batchNo,
      parentProductionBatchNo: currentPlan.batchNo,
      productionBatchNo: reportForm.batchNo,
      firstAllocationId: reportForm.firstAllocationId,
      grindingMeters,
      qualityThickness,
      qualityWidth,
      segmentMark: reportForm.segmentMark,
      startPosition: Number(reportForm.startPosition || 0),
    } as unknown as MesHcRoughGrindingConsoleApi.ReportSaveReq);
    try {
      await updateReportConsumableUsageStatusesAfterSubmit();
    } catch {
      message.warning('磨皮报工已保存，但边库耗材使用状态更新失败，请在边库台账中核对处理');
    }
    await loadBoard({
      equipmentId: equipmentInfo.id,
      planId: currentPlan.planId,
      planOperationId: currentPlan.planOperationId,
    });
    reportVisible.value = false;
    message.success('报工已保存到后台并刷新看板');
    if (activePass.value === 'SECOND') {
      promptCompletionIfReached();
    }
    return;
  }

  const targetSummary = activePass.value === 'FIRST' ? dailyFirstSummary : dailySecondSummary;
  targetSummary.processLength += Number(reportForm.processLength || 0);
  targetSummary.lossLength += reportLossLengthValue.value;
  targetSummary.outputLength += reportOutputLengthValue.value;
  targetSummary.napSampleLength += reportNapSampleLengthValue.value;
  targetSummary.researchConsumptionLength += reportResearchConsumptionLengthValue.value;

  if (reportForm.sandpaperChanged === 'YES') {
    const remainingLength = Math.max((sandpaper.thresholdLength || DEFAULT_SANDPAPER_LIMIT_LENGTH) - sandpaper.lifeLength, 0);
    const newBatchUseLength = Math.max(reportConsumableLengthValue.value - remainingLength, 0);
    sandpaper.batchNo = reportForm.sandpaperNewBatchNo;
    sandpaper.lastReplaceTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
    sandpaper.lifeLength = newBatchUseLength;
    sandpaper.useCount = newBatchUseLength > 0 ? 1 : 0;
  } else {
    sandpaper.useCount += 1;
    sandpaper.lifeLength += reportConsumableLengthValue.value;
  }
  if (reportForm.guideClothChanged === 'YES') {
    guideCloth.batchNo = reportForm.guideClothNewBatchNo;
    guideCloth.lastReplaceTime = dayjs().format('YYYY-MM-DD HH:mm:ss');
    guideCloth.lifeLength = 0;
    guideCloth.useCount = GUIDE_CLOTH_USE_INCREMENT;
  } else {
    guideCloth.useCount += GUIDE_CLOTH_USE_INCREMENT;
  }

  workRecords.value.unshift({
    batchNo: reportForm.batchNo,
    confirmStatus: activePass.value === 'SECOND' ? '未确认' : '-',
    endTime: reportForm.endTime,
    lossLength: reportLossLengthValue.value,
    napSampleLength: reportNapSampleLengthValue.value,
    researchConsumptionLength: reportResearchConsumptionLengthValue.value,
    outputLength: reportOutputLengthValue.value,
    passName: activePass.value === 'FIRST' ? '第一次磨皮' : '第二次磨皮',
    passType: activePass.value,
    planNo: reportForm.planNo,
    printCount: 0,
    printStatus: activePass.value === 'SECOND' ? '未打印' : '-',
    processLength: Number(reportForm.processLength || 0),
    productionBatchNo: reportForm.batchNo,
    recorder: operatorName.value || currentUserName.value,
    recordKey: `${activePass.value}-${Date.now()}`,
    segmentMark: reportForm.segmentMark || '-',
    startPosition: Number(reportForm.startPosition || 0),
    startTime: reportForm.startTime,
  });
  try {
    await updateReportConsumableUsageStatusesAfterSubmit();
  } catch {
    message.warning('原型报工已提交，但边库耗材使用状态更新失败，请在边库台账中核对处理');
  }
  reportVisible.value = false;
  message.success('原型报工已提交，并已更新看板统计');
};

const submitReport = async () => {
  if (reportSubmitting.value) return;
  reportSubmitting.value = true;
  try {
    await executeReportSubmission();
  } finally {
    reportSubmitting.value = false;
  }
};

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

function normalizeTicketText(value?: unknown) {
  const text = String(value ?? '').trim();
  return text || '-';
}

function formatTicketDateTime(value?: string) {
  const text = normalizeTicketText(value);
  if (text === '-') return text;
  const date = dayjs(text);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : text;
}

function formatRoughGrindingMeterRange(record: WorkRecord) {
  const startMeter = Number(record.startPosition || 0).toFixed(3);
  const processMeter = Number(record.processLength || 0).toFixed(3);
  return `起${startMeter} / 加工${processMeter} m`;
}

function resolveRoughGrindingSegmentBatchNo(record: WorkRecord) {
  return normalizeTicketText(record.productionBatchNo || record.batchNo);
}

async function sendRoughGrindingTransferTicketsToPrintAgent(ticketPayload: any) {
  const response = await fetch(`${TRANSFER_TICKET_PRINT_AGENT_URL}/print/transfer-tickets`, {
    body: JSON.stringify(ticketPayload),
    headers: {
      'Content-Type': 'application/json',
    },
    method: 'POST',
  });
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result?.success || !result?.accepted) {
    throw new Error(result?.message || `本机打印服务返回异常：${response.status}`);
  }
  return result;
}

async function buildRoughGrindingTransferTicketPayload(records: WorkRecord[]) {
  const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
  const materialCode = normalizeTicketText(
    (planDetail as any)?.motherMaterialCode ||
      (planDetail as any)?.materialCode ||
      currentPlan.materialCode,
  );
  const modelCode = normalizeTicketText(
    (planDetail as any)?.motherModelCode ||
      (planDetail as any)?.modelCode ||
      currentPlan.modelCode,
  );
  const processName = '磨皮';
  return {
    copies: 1,
    items: await Promise.all(records.map(async (record) => {
      const segmentBatchNo = resolveRoughGrindingSegmentBatchNo(record);
      const planNo = normalizeTicketText(record.planNo || currentPlan.planNo);
      const startTime = formatTicketDateTime(record.startTime);
      const endTime = formatTicketDateTime(record.endTime);
      const recorderName = normalizeTicketText(record.recorder || operatorName.value || currentUserName.value);
      const processLengthRange = formatRoughGrindingMeterRange(record);
      const fallbackFields = [
        { label: '料号', value: materialCode },
        { label: '型号', value: modelCode },
        { label: '分段批号', value: segmentBatchNo },
        { label: '当前工序', value: processName },
        { label: '开工时间', value: startTime },
        { label: '完工时间', value: endTime },
        { label: '记录人', value: recorderName },
        { label: '磨皮米数', value: processLengthRange },
        { label: '研发消耗(m)', value: formatNumber(record.researchConsumptionLength) },
      ];
      const fields = await applyPrintFieldTemplate('ROUGH_GRINDING_TRANSFER', fallbackFields, {
        endTime,
        materialCode,
        modelCode,
        planNo,
        processLengthRange,
        researchConsumptionLength: formatNumber(record.researchConsumptionLength),
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
        qrValue: buildTransferTicketQrValue(planNo, segmentBatchNo),
        recorderName,
        segmentMark: record.segmentMark,
        startTime,
        ticketId: record.id || record.recordKey,
      };
    })),
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    printerKey: 'roughGrinding',
    printMode: 'raw',
    processName,
    rawProtocol: 'pplb',
    title: '工艺流转单',
    waitForSpooler: true,
  };
}

async function markWorkRecordsPrinted(records: WorkRecord[], printTime: string) {
  await Promise.all(
    records.map(async (record) => {
      record.printStatus = '已打印';
      record.printTime = printTime;
      record.printCount = Number(record.printCount || 0) + 1;
      if (record.id) {
        await markRoughGrindingConsoleSecondReportPrinted({
          id: record.id,
          lastPrintTime: printTime,
          printCount: record.printCount,
          printStatus: 'PRINTED',
        });
      }
    }),
  );
  selectedWorkRecordKeys.value = selectedWorkRecordKeys.value.filter((key) =>
    !records.some((record) => record.recordKey === key),
  );
}

const openWorkRecordTickets = async (
  records: WorkRecord[],
  options: { autoPrint?: boolean; markPrinted?: boolean } = {},
) => {
  const autoPrint = options.autoPrint !== false;
  const markPrinted = options.markPrinted !== false;
  const validRows = records.filter((record) => record.passType === 'SECOND' && !!(record.productionBatchNo || record.batchNo));
  if (!validRows.length) {
    AModal.info({ title: '没有可打印分段', content: '请选择已有分段批号的第二次磨皮分段记录。' });
    return;
  }
  const printWindow = window.open('', '_blank', 'width=980,height=720');
  if (!printWindow) return;
  const now = dayjs().format('YYYY-MM-DD HH:mm:ss');
  const planDetail = currentPlan.planId ? await getPlanOrderDetail(currentPlan.planId as any) : {};
  const ticketRows = await Promise.all(
    validRows.map(async (record) => ({
      confirmer: operatorName.value || currentUserName.value,
      currentSection: '磨皮',
      endTime: record.endTime,
      materialCode: (planDetail as any)?.motherMaterialCode || (planDetail as any)?.materialCode || currentPlan.materialCode,
      metricValue: record.processLength,
      modelCode: (planDetail as any)?.motherModelCode || (planDetail as any)?.modelCode || currentPlan.modelCode,
      parentBatchNo: record.batchNo,
      planNo: record.planNo || currentPlan.planNo,
      productionBatchNo: record.productionBatchNo || record.batchNo,
      qrDataUrl: await createPrintQrDataUrl(
        buildTransferTicketQrValue(record.planNo || currentPlan.planNo, record.productionBatchNo || record.batchNo),
      ),
      recorder: operatorName.value || currentUserName.value,
      remark: [
        `母批批号:${record.batchNo || '-'}`,
        `分段:${record.segmentMark || '-'}`,
        `固定损耗-包含小样条:${formatNumber(record.lossLength) || '-'}`,
        `产出:${formatNumber(record.outputLength) || '-'}`,
        `NAP:${formatNumber(record.napSampleLength) || '-'}`,
        `研发消耗:${formatNumber(record.researchConsumptionLength)}m`,
      ].join(' '),
      segmentMark: record.segmentMark,
      startTime: record.startTime,
    })),
  );
  printWindow.document.write(
    buildWorkOrderTicketHtml(ticketRows, {
      materialCode: (planDetail as any)?.motherMaterialCode || (planDetail as any)?.materialCode || currentPlan.materialCode,
      modelCode: (planDetail as any)?.motherModelCode || (planDetail as any)?.modelCode || currentPlan.modelCode,
      operations: resolvePlanPrintOperations(planDetail as Record<string, any>),
      planNo: currentPlan.planNo,
      printTime: now,
    }),
  );
  printWindow.document.close();
  printWindow.focus();
  if (autoPrint) {
    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 300);
  } else {
    message.success(`已打开${validRows.length}条当前分段工单预览`);
  }

  if (!markPrinted) return;
  await Promise.all(
    validRows.map(async (record) => {
      record.printStatus = '已打印';
      record.printTime = now;
      record.printCount = Number(record.printCount || 0) + 1;
      if (record.id) {
        await markRoughGrindingConsoleSecondReportPrinted({
          id: record.id,
          lastPrintTime: now,
          printCount: record.printCount,
          printStatus: 'PRINTED',
        });
      }
    }),
  );
  selectedWorkRecordKeys.value = selectedWorkRecordKeys.value.filter((key) =>
    !validRows.some((record) => record.recordKey === key),
  );
};

const printWorkRecords = async (records: WorkRecord[]) => {
  const validRows = records.filter((record) => record.passType === 'SECOND' && !!(record.productionBatchNo || record.batchNo));
  if (!validRows.length) {
    AModal.info({ title: '没有可打印分段', content: '请选择已有分段批号的第二次磨皮分段记录。' });
    return;
  }
  try {
    const payload = await buildRoughGrindingTransferTicketPayload(validRows);
    const result = await sendRoughGrindingTransferTicketsToPrintAgent(payload);
    const now = dayjs().format('YYYY-MM-DD HH:mm:ss');
    try {
      await markWorkRecordsPrinted(validRows, now);
      message.success(`磨皮流转单已确认发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || validRows.length}。`);
    } catch (markError: any) {
      AModal.warning({
        content: `Python 打印服务已确认接收，但已打印状态回写失败：${markError?.message || markError}。请刷新后确认记录状态。`,
        title: '打印已发送，状态回写失败',
      });
    }
  } catch (error: any) {
    AModal.warning({
      content: `未能连接或确认本机 Python 打印服务：${error?.message || error}。请先启动工序流转单打印服务后重试。`,
      title: '打印流转单失败',
    });
  }
};

const exportWorkRecords = async (records: WorkRecord[]) => {
  await openWorkRecordTickets(records, { autoPrint: false, markPrinted: false });
};

const printSelectedWorkRecords = () => {
  void printWorkRecords(selectedPrintableWorkRecords.value);
};

const printAllSecondWorkRecords = () => {
  void printWorkRecords(unprintedSecondWorkRecords.value);
};

const exportSecondSegmentWorkRecord = (segmentMark?: string) => {
  const record = requireSecondSegmentLatestRecord(segmentMark);
  if (!record) return;
  void exportWorkRecords([record]);
};

const printSecondSegmentWorkRecord = (segmentMark?: string) => {
  const record = requireSecondSegmentLatestRecord(segmentMark);
  if (!record) return;
  void printWorkRecords([record]);
};

const openRecordScanConfirm = (record: WorkRecord) => {
  if (record.passType !== 'SECOND') {
    message.info('当前仅二次磨皮分段需要扫码确认');
    return;
  }
  recordScanConfirmForm.row = record;
  recordScanConfirmForm.scanCode = '';
  recordScanConfirmForm.error = '';
  recordScanConfirmForm.message = '';
  recordScanConfirmSubmitting.value = false;
  recordScanConfirmVisible.value = true;
  focusRecordScanConfirmInput();
};

const openSelectedRecordScanConfirm = () => {
  if (!secondWorkRecords.value.length) {
    AModal.info({ title: '暂无记录', content: '请先追加第二次磨皮记录后再扫码确认。' });
    return;
  }
  const selectedRow =
    selectedSecondWorkRecords.value.length === 1 && selectedSecondWorkRecords.value[0]?.confirmStatus !== '已确认'
      ? selectedSecondWorkRecords.value[0]
      : undefined;
  const row = selectedRow || secondWorkRecords.value.find((record) => record.confirmStatus !== '已确认');
  if (!row) {
    AModal.info({ title: '已全部确认', content: '第二次磨皮分段均已扫码确认。' });
    return;
  }
  openRecordScanConfirm(row);
};

const getSecondWorkRecordScanBatchNo = (record?: WorkRecord) => {
  const productionBatchNo = String(record?.productionBatchNo || '').trim();
  if (productionBatchNo) return productionBatchNo;
  const fallbackBatchNo = String(record?.batchNo || '').trim();
  const motherBatchNo = String(currentPlan.batchNo || '').trim();
  if (fallbackBatchNo && fallbackBatchNo !== motherBatchNo) return fallbackBatchNo;
  const segmentBatchNo = buildSecondGrindingBatchNo(motherBatchNo, record?.segmentMark);
  return segmentBatchNo !== motherBatchNo ? segmentBatchNo : '';
};

const findSecondWorkRecordByScannedBatch = (scannedBatchNo: string) => {
  const scanned = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(scannedBatchNo));
  const matches = secondWorkRecords.value.filter((record) => getSecondWorkRecordScanBatchNo(record) === scanned);
  return matches.find((record) => record.confirmStatus !== '已确认') || matches[0];
};

const confirmWorkRecordScan = async () => {
  if (recordScanConfirmSubmitting.value) {
    return;
  }
  const scanned = resolveTransferTicketQrBusinessNo(normalizeConfirmScanCode(recordScanConfirmForm.scanCode));
  recordScanConfirmForm.error = '';
  recordScanConfirmForm.message = '';
  if (!scanned) {
    recordScanConfirmForm.error = '请扫描或输入流转单分段批号。';
    focusRecordScanConfirmInput();
    return;
  }
  const matchedRecord = findSecondWorkRecordByScannedBatch(scanned);
  if (!matchedRecord) {
    recordScanConfirmForm.error = `未在当前母批批号 ${currentPlan.batchNo || '-'} 的二次磨皮分段中找到分段批号 ${scanned}`;
    recordScanConfirmForm.scanCode = '';
    focusRecordScanConfirmInput();
    return;
  }
  if (matchedRecord.confirmStatus === '已确认') {
    recordScanConfirmForm.error = `分段批号 ${scanned} 已扫码确认，无需重复确认。`;
    recordScanConfirmForm.row = matchedRecord;
    recordScanConfirmForm.scanCode = '';
    focusRecordScanConfirmInput();
    return;
  }
  if (!matchedRecord.id) {
    recordScanConfirmForm.error = '匹配到的二次磨皮记录尚未保存，不能扫码确认。';
    recordScanConfirmForm.row = matchedRecord;
    focusRecordScanConfirmInput();
    return;
  }
  if (!(await ensureRoughSampleAbnormalUnlocked(matchedRecord.segmentMark, '扫码确认', 'SECOND'))) {
    recordScanConfirmForm.scanCode = '';
    focusRecordScanConfirmInput();
    return;
  }
  const now = dayjs().format('YYYY-MM-DD HH:mm:ss');
  recordScanConfirmSubmitting.value = true;
  try {
    await confirmRoughGrindingConsoleSecondReport({
      confirmerName: operatorName.value,
      confirmerTime: now,
      id: matchedRecord.id,
      scannedBatchNo: scanned,
    });
    matchedRecord.confirmStatus = '已确认';
    matchedRecord.confirmTime = now;
    recordScanConfirmForm.row = matchedRecord;
    selectedWorkRecordKeys.value = selectedWorkRecordKeys.value.filter((key) => key !== matchedRecord.recordKey);
    recordScanConfirmForm.message = `批次 ${scanned} 已确认入账。`;
    recordScanConfirmForm.scanCode = '';
  } finally {
    recordScanConfirmSubmitting.value = false;
    focusRecordScanConfirmInput();
  }
};

onActivated(() => {
  attachGlobalScannerListener();
});

onMounted(() => {
  attachGlobalScannerListener();
  timer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
    qtimeNowTimestamp.value = Date.now();
  }, 1000);
  focusPlanScanInput();
  void loadInitialBoard().finally(focusPlanScanInput);
});

onDeactivated(() => {
  detachGlobalScannerListener();
});

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
  if (planScanTimer) clearTimeout(planScanTimer);
  detachGlobalScannerListener();
});
</script>

<template>
  <Page auto-content-height :loading="boardLoading">
    <div class="rough-console" :class="{ 'is-source-maximized': visualMaximized }">
      <div class="prototype-banner shrink-0 bg-white rounded-xl shadow-sm border border-slate-200 flex p-3 relative overflow-hidden">
        <div class="flex flex-1 items-center gap-4 min-w-0 pl-1">
          <div
            class="console-main-icon w-[60px] h-[60px] bg-gradient-to-br from-indigo-500 to-indigo-600 rounded-xl shadow-md flex items-center justify-center shrink-0 text-white"
            title="双击显示/隐藏点检清洁、今日磨皮记录和中间品记录单"
            @dblclick="toggleExtendedBoardTabs"
          >
            <IconifyIcon icon="lucide:monitor-play" class="text-[32px]" />
          </div>

          <div class="console-title-block">
            <div class="console-title-row">
              <span class="console-title-text">磨皮操作看板</span>
              <Tag color="processing" class="console-title-tag">
                磨皮
              </Tag>
            </div>

            <div class="console-meta-row">
              <div class="console-meta-item console-meta-item--machine" :class="{ 'is-unbound': !hasBoundEquipment }">
                <span class="console-meta-label">机台</span>
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

        <div v-if="SHOW_TOP_FIRST_INSPECTION" class="inspection-stamp-slot">
          <button
            class="inspection-stamp-side"
            :class="`inspection-stamp-side--${getFirstInspectionMeta(currentFirstInspectionTask?.status).stampClass}`"
            :disabled="firstInspectionApplying"
            type="button"
            @click="handleTopFirstInspectionAction"
          >
            <span class="inspection-stamp-content">
              <span>首样检验</span>
              <strong>{{ getFirstInspectionMeta(currentFirstInspectionTask?.status).stampText }}</strong>
              <em>{{ getFirstInspectionStampTime(currentFirstInspectionTask) }}</em>
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
            @click="dailyCheckListVisible = true"
          >
            <IconifyIcon icon="lucide:clipboard-check" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">点检清洁</span>
          </div>

          <div
            class="w-[64px] h-[64px] bg-slate-50 border border-slate-300 text-slate-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-slate-100 hover:shadow-md transition-all active:scale-95"
            @click="openTaskList"
          >
            <IconifyIcon icon="lucide:list-todo" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">待加工</span>
          </div>

          <div
            v-if="canStartWorkOrder"
            class="w-[64px] h-[64px] bg-amber-50 border border-amber-300 text-amber-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-amber-100 hover:shadow-md transition-all active:scale-95"
            @click="requestOperationAuth('START')"
          >
            <IconifyIcon icon="lucide:play-circle" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">开工确认</span>
          </div>

          <div
            v-if="canCompleteWorkOrder"
            class="w-[64px] h-[64px] bg-rose-50 border border-rose-300 text-rose-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-rose-100 hover:shadow-md transition-all active:scale-95"
            @click="requestWorkOrderComplete(false)"
          >
            <IconifyIcon icon="lucide:badge-check" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">工单完工</span>
          </div>

          <div
            v-if="SHOW_TOP_FIRST_INSPECTION && !isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-lime-50 border border-lime-300 text-lime-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-lime-100 hover:shadow-md transition-all active:scale-95"
            :class="{ 'pointer-events-none opacity-60': firstInspectionApplying }"
            @click="handleTopFirstInspectionAction"
          >
            <IconifyIcon icon="lucide:clipboard-signature" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">{{ getTopFirstInspectionActionText() }}</span>
          </div>

          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-sky-50 border border-sky-200 text-sky-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-sky-100 hover:shadow-md transition-all active:scale-95"
            @click="openSelectedRecordScanConfirm"
          >
            <IconifyIcon icon="lucide:scan-line" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">扫码确认</span>
          </div>

          <div
            v-if="!isWorkOrderPaused && !isWorkOrderCancelled"
            class="w-[64px] h-[64px] bg-teal-50 border border-teal-200 text-teal-700 rounded-xl flex flex-col items-center justify-center cursor-pointer hover:bg-teal-100 hover:shadow-md transition-all active:scale-95"
            @click="openWorkRecordList"
          >
            <IconifyIcon icon="lucide:list-checks" class="text-[22px] mb-1 opacity-80" />
            <span class="text-[11px] font-bold">磨皮记录</span>
          </div>
        </div>
      </div>

      <section v-if="!visualMaximized" class="erp-card plan-scan-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:scan-line" />
          当前计划与扫码
        </div>
        <div class="erp-form-grid">
          <label>扫码计划</label>
          <div class="erp-input-line">
            <Input
              ref="planScanInputRef"
              :value="scanPlanNo"
              allow-clear
              class="scan-input"
              placeholder="请扫码或输入计划号"
              @press-enter="() => consumePlanScan()"
              @update:value="handlePlanScanInput"
            />
          </div>
          <label>计划号</label><strong>{{ currentPlan.planNo || '请扫码计划号' }}</strong>
          <label>产品型号</label><strong>{{ currentPlan.modelCode || '-' }}</strong>
          <label>母批批号</label><strong>{{ currentPlan.batchNo || '-' }}</strong>
          <label>状态</label><strong>{{ formatOperationStatusText(currentOperationStatus) }}</strong>
          <label>执行要求</label><strong class="erp-full-value">{{ currentPlan.requirements || '暂无执行要求' }}</strong>
        </div>
      </section>

      <section v-if="!visualMaximized" class="erp-card consumable-card full-consumable-card">
        <div class="erp-card-title">
          <IconifyIcon icon="lucide:disc-3" />
          砂纸/导布
          <span class="consumable-limit-note">
            90%提醒：砂纸 {{ formatNumber(sandpaper.thresholdLength) }} / {{ sandpaper.thresholdDays }} 天；导布 {{ guideCloth.thresholdUseCount }} 次
          </span>
          <Tag :color="sandpaperNeedReplace || guideClothNeedReplace ? 'red' : 'green'">
            {{ sandpaperNeedReplace || guideClothNeedReplace ? '需要更换' : '正常' }}
          </Tag>
          <Button size="small" type="primary" class="consumable-replace-btn" @click="openConsumableReplace('SANDPAPER')">更换砂纸</Button>
          <Button size="small" danger class="consumable-replace-btn" @click="openConsumableReplace('GUIDE_CLOTH')">更换导布</Button>
        </div>
        <div class="consumable-compact-grid">
          <button class="consumable-compact-cell" :class="{ warning: sandpaperNeedReplace }" type="button" @click="openConsumableReplace('SANDPAPER')">
            <label>砂纸批号</label>
            <span>{{ sandpaper.batchNo }}</span>
          </button>
          <button class="consumable-compact-cell consumable-compact-cell--split" :class="{ warning: sandpaperNeedReplace }" type="button" @click="openConsumableReplace('SANDPAPER')">
            <div class="consumable-split-field">
              <label>砂纸累计寿命</label>
              <strong>{{ formatNumber(sandpaper.lifeLength) }}</strong>
            </div>
            <div class="consumable-split-field">
              <label>砂纸累计天数</label>
              <strong>{{ sandpaperUseDays }} 天</strong>
            </div>
          </button>
          <button class="consumable-compact-cell" :class="{ warning: guideClothNeedReplace }" type="button" @click="openConsumableReplace('GUIDE_CLOTH')">
            <label>导布批号</label>
            <span>{{ guideCloth.batchNo }}</span>
          </button>
          <button class="consumable-compact-cell" :class="{ warning: guideClothNeedReplace }" type="button" @click="openConsumableReplace('GUIDE_CLOTH')">
            <label>导布累计寿命</label>
            <span>{{ guideCloth.useCount }} 次</span>
          </button>
        </div>
      </section>

      <Tabs v-model:active-key="activeBoardTab" class="console-tabs">
        <template #rightExtra>
          <Button class="tab-maximize-button" size="small" @click="visualMaximized = !visualMaximized">
            <IconifyIcon :icon="visualMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
            {{ visualMaximized ? '还原' : '最大化' }}
          </Button>
        </template>
        <TabPane key="FIRST" tab="第一次磨皮">
          <div
            class="tab-erp-content cloth-tab-content rough-workbench-panel"
            :class="{ 'rough-workbench-panel--blocked': workbenchBlockedReason }"
          >
            <QmsSampleAbnormalLockGuard
                  defer-to-cut-round
              class="cloth-board rough-first-lock-guard"
              :class="{
                'rough-first-cloth-board--allocated': isFirstAllocatedMode,
              }"
              action-source-process-code="ROUGH_GRINDING"
              :candidates="[]"
              object-label="母卷"
              :object-no="getRoughSampleLockMotherBatchNo()"
              process-name="磨皮"
            >
              <div class="second-sample-toolbar first-allocation-toolbar">
                <div class="second-sample-segment-strip">
                  <div
                    v-for="segment in firstAllocationActionOptions"
                    :key="`first-action-${segment.value || 'NONE'}`"
                    class="second-sample-segment-cell"
                    :class="{ 'no-segment': !segment.value }"
                  >
                    <button
                      class="second-sample-action-button second-sample-action-button--timing second-sample-action-button--timing-start"
                      :class="{ disabled: !!getSegmentTiming('FIRST', segment.value)?.startTime || !!segment.allocation }"
                      :disabled="!!getSegmentTiming('FIRST', segment.value)?.startTime || !!segment.allocation || isSegmentTimingStamping('FIRST', segment.value, 'START')"
                      type="button"
                      @click="requestSegmentTimingStamp('FIRST', segment.value, 'START')"
                    >
                      <IconifyIcon icon="lucide:circle-play" class="second-sample-action-button__icon" />
                      <span>{{ getSegmentTiming('FIRST', segment.value)?.startTime || segment.allocation ? '已开工' : '开工' }}</span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--timing second-sample-action-button--timing-end"
                      :class="{ disabled: !getSegmentTiming('FIRST', segment.value)?.startTime || !!getSegmentTiming('FIRST', segment.value)?.endTime || !!segment.allocation }"
                      :disabled="!getSegmentTiming('FIRST', segment.value)?.startTime || !!getSegmentTiming('FIRST', segment.value)?.endTime || !!segment.allocation || isSegmentTimingStamping('FIRST', segment.value, 'END')"
                      type="button"
                      @click="requestSegmentTimingStamp('FIRST', segment.value, 'END')"
                    >
                      <IconifyIcon icon="lucide:circle-stop" class="second-sample-action-button__icon" />
                      <span>{{ getSegmentTiming('FIRST', segment.value)?.endTime || segment.allocation ? '已完工' : '完工' }}</span>
                    </button>
                  </div>
                </div>
              </div>
              <div class="cloth-strip wafer-pad-strip segmented-cloth-strip first-allocation-cloth-strip">
                <span class="cloth-meter-ruler"></span>
                <button
                  v-for="segment in firstAllocationActionOptions"
                  :key="`first-allocation-visual-${segment.value || 'NONE'}`"
                  class="cloth-segment first-allocation-cloth-segment"
                  :class="{
                    reported: !!segment.allocationCount,
                    'no-segment': !segment.value,
                    'timing-started': !!getSegmentTiming('FIRST', segment.value)?.startTime,
                  }"
                  type="button"
                  @click="openFirstAllocationVisualAction(segment.value)"
                >
                  <div class="cloth-segment-topbar">
                    <span class="cloth-segment-count">{{ segment.allocationCount || 0 }} 条</span>
                  </div>
                  <strong>{{ segment.label }}</strong>
                  <em
                    class="cloth-segment-scan-status"
                    :class="{
                      confirmed: !!segment.allocationCount,
                      empty: !segment.allocationCount,
                    }"
                  >
                    {{ segment.allocationCount ? '已确认加工' : '待处理' }}
                  </em>
                  <span>开工 {{ getFirstAllocationSegmentTimingText(segment.value, 'startTime') }}</span>
                  <span>完工 {{ getFirstAllocationSegmentTimingText(segment.value, 'endTime') }}</span>
                  <span
                    class="cloth-segment-qtime"
                    :class="getSegmentQtimeLevelClass('FIRST', segment.value)"
                  >
                    QTIME {{ getSegmentQtimeText('FIRST', segment.value) }}
                  </span>
                  <span v-if="segment.value && segment.allocation">
                    起 {{ formatNumber(segment.allocation.startPosition) }}m / 加工 {{ formatNumber(segment.allocation.confirmedLength) }}m
                  </span>
                  <span v-else-if="segment.allocationCount">点击查看报工</span>
                  <span v-else>点击进入处理</span>
                </button>
              </div>
              <div class="cloth-summary-row first-allocation-summary-row">
                <div><span>已确认加工</span><strong>{{ formatNumber(firstAllocatedLength) }}</strong></div>
                <div><span>已处理单元</span><strong>{{ firstAllocationRows.length }}</strong></div>
                <div><span>可选单元</span><strong>P/Q/R/S/不分段</strong></div>
                <div class="first-allocation-summary-row__qtime">
                  <span>间隔时间</span>
                  <strong :class="['rough-qtime-bar__qtime-value', roughQtimeInfo.levelClass]">
                    {{ roughQtimeInfo.durationText }}
                  </strong>
                </div>
                <div><span>剩余</span><strong>{{ formatNumber(firstAllocationRemainingLength) }}</strong></div>
              </div>
            </QmsSampleAbnormalLockGuard>
            <div v-if="workbenchBlockedReason" class="workbench-blocked-mask">
              <div class="workbench-blocked-mask__content">
                <IconifyIcon icon="lucide:octagon-alert" />
                <strong>{{ workbenchBlockedTitle }}</strong>
                <span>{{ workbenchBlockedReason }}</span>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="SECOND" tab="第二次磨皮">
          <div
            class="tab-erp-content cloth-tab-content rough-workbench-panel"
            :class="{ 'rough-workbench-panel--blocked': workbenchBlockedReason }"
          >
            <div class="cloth-board second-cloth-board">
              <div class="second-sample-toolbar">
                <div class="second-sample-segment-strip">
                  <div
                    v-for="segment in secondSegmentActionOptions"
                    :key="`sample-${segment.value || 'NONE'}`"
                    class="second-sample-segment-cell"
                    :class="{ 'no-segment': segment.value === '' }"
                  >
                    <button
                      class="second-sample-action-button second-sample-action-button--timing second-sample-action-button--timing-start"
                      :disabled="isSecondAllocationUnavailable(segment.value) || !!getSegmentTiming('SECOND', segment.value)?.startTime || isSegmentTimingStamping('SECOND', segment.value, 'START')"
                      :class="{ disabled: isSecondAllocationUnavailable(segment.value) || !!getSegmentTiming('SECOND', segment.value)?.startTime }"
                      type="button"
                      @click="requestSegmentTimingStamp('SECOND', segment.value, 'START')"
                    >
                      <IconifyIcon icon="lucide:circle-play" class="second-sample-action-button__icon" />
                      <span>{{ getSegmentTiming('SECOND', segment.value)?.startTime ? '已开工' : '开工' }}</span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--timing second-sample-action-button--timing-end"
                      :disabled="isSecondAllocationUnavailable(segment.value) || !getSegmentTiming('SECOND', segment.value)?.startTime || !!getSegmentTiming('SECOND', segment.value)?.endTime || isSegmentTimingStamping('SECOND', segment.value, 'END')"
                      :class="{ disabled: isSecondAllocationUnavailable(segment.value) || !getSegmentTiming('SECOND', segment.value)?.startTime || !!getSegmentTiming('SECOND', segment.value)?.endTime }"
                      type="button"
                      @click="requestSegmentTimingStamp('SECOND', segment.value, 'END')"
                    >
                      <IconifyIcon icon="lucide:circle-stop" class="second-sample-action-button__icon" />
                      <span>{{ getSegmentTiming('SECOND', segment.value)?.endTime ? '已完工' : '完工' }}</span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--report"
                      :class="{
                        disabled: isSecondAllocationUnavailable(segment.value),
                        'second-sample-action-button--reported': !!segment.latestRecord && !segment.hasPendingAllocation,
                      }"
                      :disabled="isSecondAllocationUnavailable(segment.value)"
                      type="button"
                      @click="openSecondGrindingVisualAction(segment.value)"
                    >
                      <IconifyIcon
                        :icon="segment.latestRecord && !segment.hasPendingAllocation ? 'lucide:eye' : 'lucide:play-circle'"
                        class="second-sample-action-button__icon"
                      />
                      <span>{{ segment.latestRecord && !segment.hasPendingAllocation ? '查看' : '报工' }}</span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--sample"
                      :class="{
                        disabled: isSecondAllocationUnavailable(segment.value),
                        sent: !!getSecondSegmentSampleTask(segment.value),
                        'sample-status-ng': getSecondSegmentSampleButtonStatus(segment.value) === 'ng',
                        'sample-status-ok': getSecondSegmentSampleButtonStatus(segment.value) === 'ok',
                        'sample-status-waiting': getSecondSegmentSampleButtonStatus(segment.value) === 'waiting',
                        'no-segment': segment.value === '',
                        'pointer-events-none opacity-60':
                          !!secondSegmentInspectionApplyingKey &&
                          secondSegmentInspectionApplyingKey === `${getSecondSegmentLatestRecord(segment.value)?.id || ''}`,
                      }"
                      :disabled="isSecondAllocationUnavailable(segment.value)"
                      type="button"
                      @click="submitSecondSegmentSampleInspection(segment.value)"
                    >
                      <IconifyIcon icon="lucide:send" class="second-sample-action-button__icon" />
                      <span class="second-sample-segment-button__text">
                        {{ getSecondSegmentSampleButtonText(segment.value) }}
                      </span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--print"
                      :class="{ disabled: !segment.latestRecord }"
                      :disabled="!segment.latestRecord"
                      type="button"
                      @click="printSecondSegmentWorkRecord(segment.value)"
                    >
                      <IconifyIcon icon="lucide:printer" class="second-sample-action-button__icon" />
                      <span>打印</span>
                    </button>
                    <button
                      class="second-sample-action-button second-sample-action-button--middle"
                      :class="{ disabled: !segment.latestRecord }"
                      :disabled="!segment.latestRecord"
                      type="button"
                      @click="openSecondSegmentMiddleProductRecord(segment.value)"
                    >
                      <IconifyIcon icon="lucide:table-2" class="second-sample-action-button__icon" />
                      <span>中间品</span>
                    </button>
                  </div>
                </div>
              </div>
              <div class="cloth-strip wafer-pad-strip segmented-cloth-strip">
                <span class="cloth-meter-ruler"></span>
                <QmsSampleAbnormalLockGuard
                  defer-to-cut-round
                  v-for="segment in secondClothSegments"
                  :key="segment.value || 'NONE'"
                  class="cloth-segment rough-cloth-segment-lock-guard"
                  :class="{
                    'inspection-ok': getSecondSegmentSampleButtonStatus(segment.value) === 'ok',
                    'no-segment': segment.value === '',
                  }"
                  object-label="分段"
                  :object-no="getSecondSegmentSampleLockObjectNo(segment.value)"
                  action-source-process-code="ROUGH_GRINDING"
                  :candidates="getRoughSampleLockCandidates('SECOND', segment.value)"
                  process-name="磨皮"
                  role="button"
                  tabindex="0"
                  @click="openSecondGrindingVisualAction(segment.value)"
                  @keydown.enter.prevent="openSecondGrindingVisualAction(segment.value)"
                  @keydown.space.prevent="openSecondGrindingVisualAction(segment.value)"
                >
                  <div class="cloth-segment-topbar">
                    <span class="cloth-segment-count">{{ segment.recordCount || 0 }} 条</span>
                    <button
                      class="cloth-segment-export"
                      :class="{ disabled: !segment.recordCount }"
                      :disabled="!segment.recordCount"
                      title="导出当前段工单"
                      type="button"
                      @click.stop="exportSecondSegmentWorkRecord(segment.value)"
                    >
                      <IconifyIcon icon="lucide:file-down" />
                    </button>
                  </div>
                  <strong>{{ segment.label }}</strong>
                  <em v-if="getSecondSegmentSampleButtonStatus(segment.value) === 'ok'" class="cloth-segment-inspection-ok">
                    检验合格
                  </em>
                  <em
                    class="cloth-segment-scan-status"
                    :class="{
                      confirmed: segment.latestRecord?.confirmStatus === '已确认',
                      pending: !!segment.latestRecord && segment.latestRecord?.confirmStatus !== '已确认',
                      empty: !segment.latestRecord,
                    }"
                  >
                    {{
                      segment.latestRecord
                        ? segment.latestRecord.confirmStatus === '已确认'
                          ? '扫码已确认'
                          : '扫码未确认'
                        : '暂无报工'
                    }}
                  </em>
                  <span>加工 {{ formatNumber(segment.processLength) }}</span>
                  <span>固定损耗-<em class="report-loss-sample-emphasis">包含小样条</em> {{ formatNumber(segment.lossLength) }}</span>
                  <span>产出 {{ formatNumber(segment.outputLength) }}</span>
                  <span>留样 {{ formatNumber(segment.napSampleLength) }}</span>
                  <span>研发消耗 {{ formatNumber(segment.researchConsumptionLength) }}</span>
                  <span
                    class="cloth-segment-qtime"
                    :class="getSegmentQtimeLevelClass('SECOND', segment.value)"
                  >
                    QTIME {{ getSegmentQtimeText('SECOND', segment.value) }}
                  </span>
                </QmsSampleAbnormalLockGuard>
              </div>
            </div>
            <div v-if="workbenchBlockedReason" class="workbench-blocked-mask">
              <div class="workbench-blocked-mask__content">
                <IconifyIcon icon="lucide:octagon-alert" />
                <strong>{{ workbenchBlockedTitle }}</strong>
                <span>{{ workbenchBlockedReason }}</span>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="INSPECTION_RECORDS" tab="检验记录">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">二次磨皮检验记录</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">共 {{ secondInspectionSummary.total }} 张</span>
                </div>
              </div>
              <div class="second-inspection-summary">
                <div class="second-inspection-summary__item">
                  <span>待反馈</span>
                  <strong>{{ secondInspectionSummary.pending }}</strong>
                </div>
                <div class="second-inspection-summary__item ok">
                  <span>合格</span>
                  <strong>{{ secondInspectionSummary.ok }}</strong>
                </div>
                <div class="second-inspection-summary__item abnormal">
                  <span>异常</span>
                  <strong>{{ secondInspectionSummary.abnormal }}</strong>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="secondInspectionRecordColumns"
                  :data-source="secondInspectionRecords"
                  :pagination="false"
                  :scroll="{ x: 1650, y: 156 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'segmentLabel'">
                      {{ record.segmentLabel || getSegmentLabel(record.segmentMark) }}
                    </template>
                    <template v-if="column.dataIndex === 'faiNo'">
                      {{ record.faiNo || record.id || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'productionBatchNo'">
                      {{ record.productionBatchNo || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'sampleTypeName'">
                      {{ record.sampleTypeName || '二磨分段留样' }}
                    </template>
                    <template v-if="column.dataIndex === 'sampleLength'">
                      {{ record.sampleLength ? formatNumber(record.sampleLength) : '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'status'">
                      <Tag :color="getSecondInspectionRecordStatusMeta(record).color" class="!m-0">
                        {{ getSecondInspectionRecordStatusMeta(record).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'result'">
                      <Tag :color="getSecondInspectionResultMeta(record).color" class="!m-0">
                        {{ getSecondInspectionResultMeta(record).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'feedbackTime'">
                      {{ record.feedbackTime || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'feedbackRemark'">
                      {{ record.feedbackRemark || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <div class="table-action-stack">
                        <Button size="small" type="link" @click="viewSecondSegmentInspectionTaskDetail(record)">查看明细</Button>
                        <Button size="small" type="link" @click="printSecondSegmentInspectionTransferTicket(record)">流转单</Button>
                      </div>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane key="ABNORMAL_POSITIONS" tab="异常位置">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">异常位置列表</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">
                    当前母批批号 {{ getAbnormalQueryMotherBatchNo() || '-' }} / 共 {{ abnormalPositionSummary.total }} 条
                  </span>
                  <Button size="small" :loading="abnormalPositionLoading" @click="loadRoughAbnormalPositionList">刷新</Button>
                </div>
              </div>
              <div class="second-inspection-summary">
                <div class="second-inspection-summary__item">
                  <span>湿法</span>
                  <strong>{{ abnormalPositionSummary.wet }}</strong>
                </div>
                <div class="second-inspection-summary__item">
                  <span>一磨</span>
                  <strong>{{ abnormalPositionSummary.first }}</strong>
                </div>
                <div class="second-inspection-summary__item abnormal">
                  <span>二磨</span>
                  <strong>{{ abnormalPositionSummary.second }}</strong>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="abnormalPositionColumns"
                  :data-source="abnormalPositionRows"
                  :loading="abnormalPositionLoading"
                  :pagination="false"
                  :scroll="{ x: 1340, y: 156 }"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'processStage'">
                      <Tag :color="getAbnormalProcessStageMeta(record.processStage).color" class="!m-0">
                        {{ getAbnormalProcessStageMeta(record.processStage).text }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'batchNo'">
                      {{ record.batchNo || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'productionBatchNo'">
                      {{ record.productionBatchNo || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'abnormalLength'">
                      {{ record.abnormalLength == null ? '-' : formatNumber(record.abnormalLength) }}
                    </template>
                    <template v-if="column.dataIndex === 'remark'">
                      {{ record.remark || '-' }}
                    </template>
                    <template v-if="column.dataIndex === 'createTime'">
                      {{ record.createTime || '-' }}
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
              :columns="checkColumns"
              :data-source="dailyCheckRows"
              :pagination="false"
              :scroll="{ x: 1250, y: 176 }"
              row-key="key"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'status'">
                  <Tag :color="dailyCheckLoaded ? getCheckStatusMeta(record.status).color : 'default'">{{ getBackendCheckStatusText(record) }}</Tag>
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
                    <Button size="small" type="link" :disabled="!canFillDailyCheck(record)" @click="requestDailyCheckAuth(record.key, 'edit')">
                      填写
                    </Button>
                    <Button size="small" type="link" :disabled="!canConfirmDailyCheck(record)" @click="requestDailyCheckAuth(record.key, 'confirm')">
                      确认
                    </Button>
                    <Button size="small" type="link" :disabled="!canViewDailyCheck(record)" @click="openDailyCheck(record.key, 'view')">查看</Button>
                  </div>
                </template>
              </template>
            </ATable>
          </div>
        </TabPane>
        <TabPane v-if="showExtendedBoardTabs" key="RECORDS" tab="今日磨皮记录">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">今日磨皮记录</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">一磨 {{ firstWorkRecords.length }} 条 / 二磨 {{ secondWorkRecords.length }} 条 / 已选 {{ selectedSecondWorkRecords.length }} 条</span>
                  <Button size="small" :disabled="!secondWorkRecords.length" @click="openSelectedRecordScanConfirm">扫码确认</Button>
                  <Button size="small" :disabled="!selectedPrintableWorkRecords.length" @click="printSelectedWorkRecords">选择打印</Button>
                  <Button size="small" :disabled="!unprintedSecondWorkRecords.length" @click="printAllSecondWorkRecords">打印全部新报工</Button>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="recordColumns"
                  :data-source="workRecords"
                  :pagination="false"
                  :row-selection="workRecordRowSelection"
                  :row-class-name="getWorkRecordRowClassName"
                  :scroll="{ x: 1750, y: 176 }"
                  row-key="recordKey"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'printStatus'">
                      <Tag v-if="record.passType === 'SECOND'" :color="record.printStatus === '已打印' ? 'success' : 'default'" class="!m-0">
                        {{ record.printStatus || '未打印' }}
                      </Tag>
                      <span v-else>-</span>
                    </template>
                    <template v-if="column.dataIndex === 'printCount'">
                      <span v-if="record.passType === 'SECOND'">{{ Number(record.printCount || 0) }}</span>
                      <span v-else>-</span>
                    </template>
                    <template v-if="column.dataIndex === 'confirmStatus'">
                      <Tag v-if="record.passType === 'SECOND'" :color="record.confirmStatus === '已确认' ? 'success' : 'warning'" class="!m-0">
                        {{ record.confirmStatus || '未确认' }}
                      </Tag>
                      <span v-else>-</span>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click.stop="openSubmittedReportDetail(record)">查看</Button>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane v-if="showExtendedBoardTabs" key="MIDDLE_RECORDS" tab="中间品记录单">
          <div class="tab-table-content">
            <div class="console-table-shell">
              <div class="rough-grid-toolbar console-record-toolbar">
                <div class="rough-grid-toolbar__title">中间品记录单</div>
                <div class="pass-work-actions rough-grid-toolbar__actions">
                  <span class="console-table-count">共 {{ submittedMiddleProductRecords.length }} 张</span>
                </div>
              </div>
              <div class="console-table-body">
                <ATable
                  class="rough-check-table console-record-table"
                  :columns="reportedMiddleProductRecordColumns"
                  :data-source="submittedMiddleProductRecords"
                  :pagination="false"
                  :scroll="{ x: 1390, y: 176 }"
                  row-key="recordKey"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'status'">
                      <Tag :color="record.status === 'RECORDED' ? 'processing' : 'success'" class="!m-0">
                        {{ record.status === 'RECORDED' ? '已记录' : record.status || '-' }}
                      </Tag>
                    </template>
                    <template v-if="column.dataIndex === 'recorderInfo'">
                      <div class="record-info-cell">
                        <strong>{{ record.recorder || '-' }}</strong>
                        <span>{{ record.recorderTime || '-' }}</span>
                      </div>
                    </template>
                    <template v-if="column.dataIndex === 'action'">
                      <div class="table-action-stack">
                        <Button size="small" type="link" @click="openSubmittedMiddleProductDetail(record, 'view')">查看明细</Button>
                        <Button size="small" type="link" @click="openSubmittedMiddleProductDetail(record, 'edit')">编辑修改</Button>
                      </div>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane v-if="false" key="EDGE_CONSUMABLE" tab="边库耗材登记">
          <div class="tab-table-content">
            <EdgeConsumableRegisterTab
              :plan-no="currentPlan.planNo"
              process-code="ROUGH_GRINDING"
              :production-batch-no="currentPlan.batchNo"
              :readonly="isWorkOrderFinished || isWorkOrderPaused || isWorkOrderCancelled"
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
            title="磨皮指令消息"
            @unread-change="handleProductionInstructionUnreadChange"
          />
        </TabPane>
      </Tabs>

      <AModal v-model:open="dailyCheckListVisible" :footer="null" :width="720" class="rough-prototype-modal" title="今日点检/清洁记录">
        <div class="daily-check-card-grid">
          <div
            v-for="record in dailyCheckRows"
            :key="record.key"
            class="daily-check-card"
          >
            <span class="daily-check-card__icon">
              <IconifyIcon :icon="record.key === 'STARTUP' ? 'lucide:clipboard-check' : 'lucide:sparkles'" />
            </span>
            <span class="daily-check-card__content">
              <strong>{{ record.key === 'STARTUP' ? '开机点检表' : '清洁点检表' }}</strong>
              <em>{{ record.name }}</em>
              <span>{{ record.timing }} / {{ getBackendCheckStatusText(record) }}</span>
            </span>
            <Tag :color="dailyCheckLoaded ? getCheckStatusMeta(record.status).color : 'default'" class="daily-check-card__tag">
              {{ getBackendCheckStatusText(record) }}
            </Tag>
            <span class="daily-check-card__actions">
              <Button size="small" type="primary" :disabled="!canFillDailyCheck(record)" @click="requestDailyCheckAuth(record.key, 'edit')">填写</Button>
              <Button size="small" danger :disabled="!canConfirmDailyCheck(record)" @click="requestDailyCheckAuth(record.key, 'confirm')">确认</Button>
              <Button size="small" :disabled="!canViewDailyCheck(record)" @click="openDailyCheck(record.key, 'view')">查看</Button>
            </span>
          </div>
        </div>
        <div class="modal-footer daily-check-close-footer">
          <Button type="primary" @click="dailyCheckListVisible = false">已检关闭</Button>
        </div>
      </AModal>

      <AModal v-model:open="equipmentSelectVisible" :footer="null" :width="860" class="rough-prototype-modal" title="切换磨皮机台">
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
            选中机台后，今日点检/清洁记录按该设备和当天日期加载，砂纸/导布寿命同步按该设备刷新。
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
              row-key="value"
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

      <AModal v-model:open="taskListVisible" :footer="null" :width="1280" class="rough-prototype-modal" :title="roughTaskListTitle">
        <div class="console-table-shell console-table-shell--modal task-list-table-shell">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">{{ roughTaskListTitle }}</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">筛选 {{ taskListFilteredRows.length }} 条 / 共 {{ taskListRows.length }} 条</span>
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
              <label>母批批号</label>
              <Input
                v-model:value="taskListFilters.batchNo"
                allow-clear
                placeholder="请输入母批批号"
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
              class="rough-check-table console-record-table"
              :columns="taskListColumns"
              :data-source="taskListPagedRows"
              :loading="taskListLoading"
              :pagination="false"
              :scroll="{ x: 1200, y: 366 }"
              row-key="id"
              size="small"
              :custom-row="(record) => ({ onDblclick: () => selectTaskFromList(record) })"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'batchNo'">
                  {{ getTaskListBatchText(record) }}
                </template>
                <template v-if="column.dataIndex === 'status'">
                  <Tag :color="getTaskListStatusMeta(record.status).color" class="!m-0">
                    {{ getTaskListStatusMeta(record.status).text }}
                  </Tag>
                </template>
                <template v-if="['motherLength', 'remainingLength', 'firstGrindingProcessLength', 'secondGrindingProcessLength'].includes(String(column.dataIndex))">
                  {{ formatNumber(record[String(column.dataIndex)]) }}
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button size="small" type="link" @click="selectTaskFromList(record)">开工</Button>
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

      <AModal v-model:open="workRecordListVisible" :footer="null" :width="1180" title="今日磨皮记录">
        <div class="console-table-shell console-table-shell--modal">
          <div class="rough-grid-toolbar console-record-toolbar">
            <div class="rough-grid-toolbar__title">今日磨皮记录</div>
            <div class="pass-work-actions rough-grid-toolbar__actions">
              <span class="console-table-count">一磨 {{ firstWorkRecords.length }} 条 / 二磨 {{ secondWorkRecords.length }} 条 / 已选 {{ selectedSecondWorkRecords.length }} 条</span>
              <Button size="small" :disabled="!secondWorkRecords.length" @click="openSelectedRecordScanConfirm">扫码确认</Button>
              <Button size="small" :disabled="!selectedPrintableWorkRecords.length" @click="printSelectedWorkRecords">选择打印</Button>
              <Button size="small" :disabled="!unprintedSecondWorkRecords.length" @click="printAllSecondWorkRecords">打印全部新报工</Button>
            </div>
          </div>
          <div class="console-table-body">
            <ATable
              class="rough-check-table console-record-table"
              :columns="recordColumns"
              :data-source="workRecords"
              :pagination="false"
              :row-selection="workRecordRowSelection"
              :row-class-name="getWorkRecordRowClassName"
              :scroll="{ x: 1750, y: 420 }"
              row-key="recordKey"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'printStatus'">
                  <Tag v-if="record.passType === 'SECOND'" :color="record.printStatus === '已打印' ? 'success' : 'default'" class="!m-0">
                    {{ record.printStatus || '未打印' }}
                  </Tag>
                  <span v-else>-</span>
                </template>
                <template v-if="column.dataIndex === 'printCount'">
                  <span v-if="record.passType === 'SECOND'">{{ Number(record.printCount || 0) }}</span>
                  <span v-else>-</span>
                </template>
                <template v-if="column.dataIndex === 'confirmStatus'">
                  <Tag v-if="record.passType === 'SECOND'" :color="record.confirmStatus === '已确认' ? 'success' : 'warning'" class="!m-0">
                    {{ record.confirmStatus || '未确认' }}
                  </Tag>
                  <span v-else>-</span>
                </template>
                <template v-if="column.dataIndex === 'action'">
                  <Button size="small" type="link" @click.stop="openSubmittedReportDetail(record)">查看</Button>
                </template>
              </template>
            </ATable>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="recordScanConfirmVisible"
        :footer="null"
        :keyboard="false"
        :mask-closable="false"
        :width="560"
        class="rough-prototype-modal"
        title="二次磨皮扫码确认"
      >
        <div class="record-scan-confirm">
          <div class="record-scan-confirm__hero">
            <IconifyIcon icon="lucide:scan-line" />
            <div>
              <strong>请扫描流转单贴纸分段批号</strong>
              <span>系统会在当前母批批号下匹配对应分段批号，命中后自动更新该分段状态。</span>
            </div>
          </div>
          <div class="record-scan-confirm__expected">
            当前母批批号：{{ currentPlan.batchNo || '-' }}
          </div>
          <Input
            ref="recordScanConfirmInputRef"
            v-model:value="recordScanConfirmForm.scanCode"
            autofocus
            :disabled="recordScanConfirmSubmitting"
            placeholder="扫码枪扫描或人工输入分段批号"
            size="large"
            @press-enter="confirmWorkRecordScan"
          />
          <div v-if="recordScanConfirmForm.error" class="record-scan-confirm__error">{{ recordScanConfirmForm.error }}</div>
          <div v-if="recordScanConfirmForm.message" class="record-scan-confirm__message">{{ recordScanConfirmForm.message }}</div>
          <div class="modal-footer">
            <Button :disabled="recordScanConfirmSubmitting" @click="recordScanConfirmVisible = false">关闭</Button>
            <Button :loading="recordScanConfirmSubmitting" type="primary" @click="confirmWorkRecordScan">确认扫码</Button>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="consumableReplaceVisible"
        :footer="null"
        :width="520"
        class="rough-prototype-modal"
        :title="`确认更换${getConsumableTypeName(replaceForm.target)}`"
      >
        <div class="board-consumable-confirm">
          <div class="board-consumable-confirm__summary">
            <span>耗材类型</span>
            <strong>{{ getConsumableTypeName(replaceForm.target) }}</strong>
            <span>边库批号</span>
            <strong>
              <Tag color="blue">{{ replaceForm.batchNo || '-' }}</Tag>
            </strong>
            <span>更换人</span>
            <strong>{{ replaceForm.replacer || '-' }}</strong>
            <span>更换时间</span>
            <strong>{{ replaceForm.replaceTime || '-' }}</strong>
          </div>
          <Form layout="vertical">
            <FormItem :label="`本次消耗量（${replaceForm.target === 'SANDPAPER' ? replaceConsumption.sandpaperUnit || '台账单位' : replaceConsumption.guideClothUnit || '台账单位'}）`" required>
              <InputNumber v-if="replaceForm.target === 'SANDPAPER'" v-model:value="replaceConsumption.sandpaperQty" :min="0.001" :precision="3" :disabled="replaceSubmitting" />
              <InputNumber v-else v-model:value="replaceConsumption.guideClothQty" :min="0.001" :precision="3" :disabled="replaceSubmitting" />
              <span class="ml-2">可用余额：{{ replaceConsumptionBalance ?? '-' }}</span>
            </FormItem>
            <FormItem label="更换原因">
              <Input
                v-model:value="replaceForm.reason"
                data-replace-field="reason"
                placeholder="请填写更换原因"
              />
            </FormItem>
          </Form>
        </div>
        <div class="modal-footer">
          <Button @click="consumableReplaceVisible = false">取消</Button>
          <Button type="primary" :loading="replaceSubmitting" @click="saveConsumableReplace">确认更换</Button>
        </div>
      </AModal>
    </div>

    <AModal
      v-model:open="dailyCheckVisible"
      :footer="null"
      :title="null"
      width="100vw"
      wrap-class-name="hc-pass-work-modal rough-prep-work-modal"
    >
      <div class="pp-plan-modal">
        <div class="pp-plan-toolbar">
          <div class="pp-plan-toolbar__title pass-work-dialog-title">
            <span class="pp-plan-toolbar__main">
              {{ dailyCheckMode === 'edit' ? '填写' : dailyCheckMode === 'confirm' ? '确认' : '查看' }}{{ currentDailyCheckRow?.name || '工作准备记录' }}
            </span>
            <div class="toolbar-meta">
              <span>机台：{{ equipmentInfo.code }} / {{ equipmentInfo.name }}</span>
              <span>记录日期：{{ dayjs().format('YYYY-MM-DD') }}</span>
              <span>执行时机：{{ currentDailyCheckRow?.timing || '-' }}</span>
              <span>状态：{{ currentDailyCheckRow ? getBackendCheckStatusText(currentDailyCheckRow) : '-' }}</span>
            </div>
          </div>
          <div class="pp-plan-toolbar__actions">
            <Button v-if="dailyCheckMode === 'edit'" size="small" type="primary" :disabled="!currentDailyCheckRow || !canFillDailyCheck(currentDailyCheckRow)" @click="saveDailyCheck">保存</Button>
            <Button v-if="dailyCheckMode === 'confirm'" size="small" type="primary" danger :disabled="!currentDailyCheckRow || !canConfirmDailyCheck(currentDailyCheckRow)" @click="saveDailyCheck">确认</Button>
            <Button size="small" @click="dailyCheckVisible = false">关闭</Button>
          </div>
        </div>

        <div class="pp-plan-body">
          <fieldset class="pp-fieldset">
            <legend>表单信息</legend>
            <div class="pp-form-grid pass-work-form-grid">
              <div class="head-item">
                <span class="head-item__label">表单名称：</span>
                <span class="head-item__value">{{ currentDailyCheckRow?.name || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">执行时机：</span>
                <span class="head-item__value">{{ currentDailyCheckRow?.timing || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">记录人：</span>
                <span class="head-item__value">{{ currentDailyCheckRow?.recorder || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">记录时间：</span>
                <span class="head-item__value">{{ currentDailyCheckRow?.recorderTime || '-' }}</span>
              </div>
              <div class="head-item">
                <span class="head-item__label">确认人：</span>
                <span class="head-item__value">{{ currentDailyCheckRow?.confirmer || '-' }}</span>
              </div>
            </div>
          </fieldset>

            <div class="pp-panel pass-work-detail-panel">
              <div class="pp-panel__header">
                <span>明细项目</span>
              </div>
              <div class="pp-table-wrap pass-work-detail-wrap">
                <div class="pass-work-detail-table-body">
                <div class="daily-check-result-tip">开机、清洁保养时遇到问题请记录在备注列说明情况。</div>
                <table class="pp-grid rough-detail-grid">
                  <thead>
                    <tr v-if="dailyCheckType === 'CLEANING'">
                      <th width="120">工序</th>
                      <th width="220">点检项目</th>
                      <th width="420">检查标准</th>
                      <th width="120">OK/NG</th>
                      <th width="260">备注</th>
                    </tr>
                    <tr v-else>
                      <th width="90">序号</th>
                      <th width="260">点检项目</th>
                      <th width="360">标准</th>
                      <th width="120">OK/NG</th>
                      <th width="260">备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in currentDailyCheckRow?.details || []" :key="`${currentDailyCheckRow?.key}-${row.seq}`">
                      <template v-if="dailyCheckType === 'CLEANING'">
                        <td v-if="getDetailCategoryRowSpan(currentDailyCheckRow?.details || [], index) > 0" :rowspan="getDetailCategoryRowSpan(currentDailyCheckRow?.details || [], index)">
                          {{ row.category || '-' }}
                        </td>
                        <td>{{ row.item || '-' }}</td>
                        <td>{{ row.standard || '-' }}</td>
                      </template>
                      <template v-else>
                        <td align="center">{{ row.seq }}</td>
                        <td>{{ row.item || '-' }}</td>
                        <td>{{ row.standard || '-' }}</td>
                      </template>
                      <td>
                        <RadioGroup v-if="dailyCheckMode !== 'view'" v-model:value="row.result" class="pp-radio-group" size="small">
                          <Radio value="OK">OK</Radio>
                          <Radio value="NG">NG</Radio>
                        </RadioGroup>
                        <span v-else>{{ row.result || '-' }}</span>
                      </td>
                      <td>
                        <Input
                          v-if="dailyCheckMode !== 'view'"
                          v-model:value="row.remark"
                          :data-daily-check-cell="`${index}-remark`"
                          size="small"
                          @keydown="handleDailyCheckInputKeydown($event, index, 'remark')"
                        />
                        <span v-else>{{ row.remark || '-' }}</span>
                      </td>
                    </tr>
                    <tr v-if="!(currentDailyCheckRow?.details || []).length">
                      <td colspan="5" class="pp-empty-cell" align="center">暂无明细数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AModal>

    <AuthModal
      v-model:visible="dailyCheckAuthVisible"
      :actionName="dailyCheckAuthAction"
      authMode="username"
      :equipment-id="equipmentInfo.id"
      :equipment-options="equipmentOptions"
      equipment-label="机台编号"
      @cancel="handleDailyCheckAuthCancel"
      @success="handleDailyCheckAuthSuccess"
    />

    <AuthModal
      v-model:visible="operationAuthVisible"
      :actionName="operationAuthActionName"
      authMode="username"
      :equipment-id="equipmentInfo.id"
      :equipment-options="equipmentOptions"
      equipment-label="机台编号"
      @cancel="handleOperationAuthCancel"
      @success="handleOperationAuthSuccess"
    />
    <AuthModal
      v-model:visible="segmentTimingAuthVisible"
      :actionName="segmentTimingAuthActionName"
      authMode="username"
      :equipment-id="equipmentInfo.id"
      :equipment-options="equipmentOptions"
      equipment-label="机台编号"
      @cancel="handleSegmentTimingAuthCancel"
      @success="handleSegmentTimingAuthSuccess"
    />
    <AuthModal
      v-model:visible="roughDevProcessParamAuthVisible"
      action-name="确认工艺参数点检表"
      auth-mode="username"
      :equipment-id="equipmentInfo.id"
      :equipment-options="equipmentOptions"
      equipment-label="机台编号"
      @success="handleRoughDevProcessParamAuthSuccess"
    />
    <AuthModal
      v-model:visible="middleProductAuthVisible"
      action-name="确认中间品记录表"
      auth-mode="username"
      :equipment-id="equipmentInfo.id"
      :equipment-options="equipmentOptions"
      equipment-label="机台编号"
      @success="handleMiddleProductAuthSuccess"
    />

    <ConsumableLedgerSwitchModal
      ref="roughConsoleConsumableSwitchModalRef"
      @selected="handleRoughConsoleConsumableSelected"
    />

    <FirstInspectionPrintPreviewModal />
    <FaiDetailPreviewModal />

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
      <div
        class="report-modal-body"
        :class="{
          'report-modal-body--readonly': reportFormLocked,
          'report-modal-body--time-edit': reportTimeEditing,
        }"
      >
        <fieldset class="erp-fieldset report-modal-toolbar">
          <legend>
            {{ reportReadonly ? '查看' : '' }}{{ isFirstAllocationReport ? '第一次磨皮分段处理' : activePass === 'FIRST' ? '第一次磨皮' : '第二次磨皮' }}工艺参数点检表登记
          </legend>
          <div class="report-toolbar-form">
            <template v-if="activePass === 'SECOND' || isFirstAllocationReport">
              <label>分段批号</label>
              <strong class="report-toolbar-value report-toolbar-value--batch">{{ reportForm.batchNo || '-' }}</strong>
            </template>
            <label>砂纸本次后</label>
            <strong :class="['report-toolbar-value', { warning: reportSandpaperAfterNeedReplace }]">
              {{ formatNumber(reportSandpaperAfterLength) }} / {{ formatNumber(sandpaper.thresholdLength) }}，
              {{ reportSandpaperAfterUseDays }} / {{ sandpaper.thresholdDays }} 天
              <Tag :color="reportSandpaperAfterNeedReplace ? 'red' : 'green'">
                {{ reportSandpaperAfterNeedReplace ? '需要更换' : '正常' }}
              </Tag>
            </strong>
            <label>导布本次后</label>
            <strong :class="['report-toolbar-value', { warning: reportGuideClothAfterNeedReplace }]">
              {{ reportGuideClothAfterUseCount }} / {{ guideCloth.thresholdUseCount }} 次
              <Tag :color="reportGuideClothAfterNeedReplace ? 'red' : 'green'">
                {{ reportGuideClothAfterNeedReplace ? '需要更换' : '正常' }}
              </Tag>
            </strong>
            <template v-if="activePass === 'FIRST'">
              <label>当前可加工</label>
              <strong class="report-toolbar-value report-toolbar-value--strong">
                {{ formatNumber(isFirstAllocationReport ? firstAllocationRemainingLength : firstReportAvailableLength) }}
                <Tag color="orange">可报工</Tag>
              </strong>
            </template>
          </div>
        </fieldset>

        <Tabs v-model:active-key="activeReportTab" class="rough-report-tabs">
          <TabPane key="basic" tab="来源扫码">
            <fieldset class="erp-fieldset">
              <legend>来源信息</legend>
              <div v-if="activePass === 'FIRST' && !isFirstAllocationReport" class="report-source-summary">
                <label>当前可加工数量</label>
                <strong class="report-source-summary__value report-source-summary__value--strong">
                  {{ formatNumber(firstReportAvailableLength) }}
                </strong>
                <label>来源类型</label>
                <strong class="report-source-summary__value">{{ firstReportSourceTypeText }}</strong>
                <label>计划号</label>
                <strong class="report-source-summary__value">{{ firstReportSourcePlanNo || '-' }}</strong>
                <label>母批批号</label>
                <strong class="report-source-summary__value">{{ firstReportSourceBatchNo || '-' }}</strong>
                <label>母批批号</label>
                <strong class="report-source-summary__value">{{ reportForm.batchNo || '-' }}</strong>
                <label>当前起位置(m)</label>
                <strong class="report-source-summary__value report-source-summary__value--control">
                  <InputNumber
                    v-model:value="reportForm.startPosition"
                    :disabled="reportReadonly || isFirstAllocatedMode"
                    :min="0"
                    class="full-input"
                  />
                </strong>
                <label>开工时间</label>
                <strong class="report-source-summary__value report-source-summary__value--control">
                  <DatePicker
                    v-model:value="reportForm.startTime"
                    :disabled="!reportTimeEditing"
                    class="report-time-edit-field"
                    format="YYYY-MM-DD HH:mm:ss"
                    :show-time="{ format: 'HH:mm:ss' }"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    @change="handleReportStartTimeChange"
                  />
                </strong>
                <label>来源状态</label>
                <strong class="report-source-summary__value">已锁定</strong>
              </div>
              <div v-if="isFirstAllocationReport" class="report-source-summary">
                <label>来源类型</label>
                <strong class="report-source-summary__value">一磨分段处理</strong>
                <label>计划号</label>
                <strong class="report-source-summary__value">{{ currentPlan.planNo || '-' }}</strong>
                <label>母批批号</label>
                <strong class="report-source-summary__value">{{ currentPlan.batchNo || '-' }}</strong>
                <label>加工单元</label>
                <strong class="report-source-summary__value">{{ getSegmentLabel(reportForm.segmentMark) }}</strong>
                <label>分段批号</label>
                <strong class="report-source-summary__value">{{ reportForm.batchNo || '-' }}</strong>
                <label>处理内容</label>
                <strong class="report-source-summary__value">米数确认、砂纸/导布、工艺参数点检</strong>
              </div>
              <div v-if="activePass === 'SECOND'" class="report-source-summary">
                <label>来源类型</label>
                <strong class="report-source-summary__value">第一次磨皮</strong>
                <label>计划号</label>
                <strong class="report-source-summary__value">{{ reportForm.planNo || '-' }}</strong>
                <label>母批批号</label>
                <strong class="report-source-summary__value">{{ currentPlan.batchNo || '-' }}</strong>
                <label>分段批号</label>
                <strong class="report-source-summary__value">{{ reportForm.batchNo || '-' }}</strong>
                <label>分段标记</label>
                <strong class="report-source-summary__value">{{ reportForm.segmentMark || '不分段' }}</strong>
                <label>当前起位置(m)</label>
                <strong class="report-source-summary__value report-source-summary__value--control">
                  <InputNumber
                    v-model:value="reportForm.startPosition"
                    :disabled="reportReadonly || isFirstAllocatedMode"
                    :min="0"
                    class="full-input"
                  />
                </strong>
                <label>开工时间</label>
                <strong class="report-source-summary__value report-source-summary__value--control">
                  <DatePicker
                    v-model:value="reportForm.startTime"
                    :disabled="!reportTimeEditing"
                    class="report-time-edit-field"
                    format="YYYY-MM-DD HH:mm:ss"
                    :show-time="{ format: 'HH:mm:ss' }"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    @change="handleReportStartTimeChange"
                  />
                </strong>
              </div>
            </fieldset>
          </TabPane>
          <TabPane key="dev-process-param" tab="工艺参数点检表">
            <div class="report-tab-stack">
              <fieldset class="erp-fieldset rough-dev-param-fieldset">
                <legend>{{ getRoughDevProcessParamName(activePass) }}</legend>
                <div class="rough-dev-param-toolbar">
                  <div class="rough-dev-param-toolbar__meta">
                    <span>计划号：{{ getRoughDevExpectedPlanNo(activePass) || '-' }}</span>
                    <span>{{ activePass === 'SECOND' || isFirstAllocationReport ? '分段批号' : '母批批号' }}：{{ getRoughDevExpectedBatchNo(activePass) || '-' }}</span>
                  </div>
                  <div class="rough-dev-param-toolbar__actions">
                    <Button size="small" @click="loadRoughDevProcessParamRecords(activePass)">
                      <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
                      刷新
                    </Button>
                    <Button size="small" type="primary" @click="openRoughDevProcessParamFill(activePass)">
                      <IconifyIcon icon="lucide:plus" class="mr-1" />
                      新建上报
                    </Button>
                  </div>
                </div>
                <ATable
                  class="rough-check-table rough-dev-param-table"
                  :columns="roughDevProcessParamColumns"
                  :data-source="getRoughDevProcessParamRecords(activePass)"
                  :loading="roughDevProcessParamLoading[activePass]"
                  :pagination="false"
                  row-key="id"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.dataIndex === 'passName'">
                      {{ getRoughDevRecordPassName(record) || getGrindingPassName(activePass) }}
                    </template>
                    <template v-else-if="column.dataIndex === 'recordStatus'">
                      <Tag :color="getRoughDevRecordStatusMeta(record.recordStatus).color">
                        {{ getRoughDevRecordStatusMeta(record.recordStatus).text }}
                      </Tag>
                    </template>
                    <template v-else-if="column.dataIndex === 'fillInfo'">
                      {{ record.fillUserName || record.creator || '-' }}
                      <span class="rough-dev-param-fill-time">{{ record.fillTime || record.createTime || '' }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'confirmResult'">
                      {{ getRoughDevConfirmResult(record) }}
                    </template>
                    <template v-else-if="column.dataIndex === 'confirmInfo'">
                      {{ getRoughDevConfirmUser(record) }}
                      <span class="rough-dev-param-fill-time">{{ getRoughDevConfirmTime(record) }}</span>
                    </template>
                    <template v-else-if="column.dataIndex === 'action'">
                      <Button size="small" type="link" @click="openRoughDevProcessParamDetail(record)">查看</Button>
                      <Button
                        v-if="isRoughDevProcessParamDraft(record)"
                        size="small"
                        type="link"
                        @click="openRoughDevProcessParamDetail(record, 'edit')"
                      >
                        修改
                      </Button>
                      <Button
                        v-if="!isRoughDevProcessParamConfirmed(record)"
                        size="small"
                        type="link"
                        :loading="roughDevProcessParamConfirming"
                        @click="confirmRoughDevProcessParamRecord(record)"
                      >
                        确认
                      </Button>
                    </template>
                  </template>
                </ATable>
                <div v-if="!roughDevProcessParamLoading[activePass] && !getRoughDevProcessParamRecords(activePass).length" class="rough-dev-param-empty">
                  暂无{{ getRoughDevProcessParamName(activePass) }}记录，可点击“新建上报”填写动态表单。
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
                <FormItem :label="activePass === 'SECOND' || isFirstAllocationReport ? '分段批号' : '母批批号'">
                  <Input v-model:value="reportForm.batchNo" disabled data-report-field="batchNo" />
                </FormItem>
                <FormItem
                  label="当前磨皮起位置(m)"
                  :required="isFirstAllocationReport"
                >
                  <InputNumber
                    v-model:value="reportForm.startPosition"
                    :disabled="reportReadonly || (activePass === 'SECOND' && isFirstAllocatedMode)"
                    :max="isFirstAllocationReport ? firstAllocationSourceTotalLength : undefined"
                    :min="0"
                    :precision="3"
                    class="full-input"
                    data-report-field="startPosition"
                  />
                </FormItem>
                <FormItem required>
                  <template #label>
                    <span class="report-required-label">{{ isFirstAllocationReport ? '确认加工米数(m)' : '投入米数(m)' }}</span>
                  </template>
                  <InputNumber
                    v-model:value="reportForm.processLength"
                    :disabled="reportReadonly || (activePass === 'SECOND' && isFirstAllocatedMode)"
                    :max="!reportReadonly && activePass === 'FIRST' ? (isFirstAllocationReport ? firstAllocationRemainingLength : firstReportAvailableLength) : undefined"
                    :min="0"
                    class="full-input"
                    data-report-field="processLength"
                    @change="syncProcessItemsWithReportLength"
                  />
                </FormItem>
                <FormItem v-if="activePass !== 'FIRST'">
                  <template #label>
                    固定损耗-<span class="report-loss-sample-emphasis">包含小样条</span>（米）
                  </template>
                  <InputNumber v-model:value="reportForm.lossLength" :min="0" class="full-input" />
                </FormItem>
                <FormItem v-if="!isFirstAllocationReport">
                  <template #label>
                    <span :class="{ 'report-required-label': activePass === 'SECOND' }">异常位置</span>
                  </template>
                  <div class="report-abnormal-entry">
                    <Button size="small" @click="openReportAbnormalTab">
                      <IconifyIcon icon="lucide:map-pin" class="mr-1" />
                      增加异常位置
                    </Button>
                    <span>{{ reportAbnormalSummary }}</span>
                  </div>
                </FormItem>
                <FormItem v-if="activePass !== 'FIRST'">
                  <template #label>
                    <span class="report-required-label">NAP留样米数(m)</span>
                  </template>
                  <InputNumber v-model:value="reportForm.napSampleLength" :min="0" class="full-input" />
                </FormItem>
                <FormItem v-if="activePass === 'SECOND'" label="研发消耗（米）">
                  <InputNumber v-model:value="reportForm.researchConsumptionLength" :min="0" :precision="3" :step="0.001" class="full-input" />
                </FormItem>
                <FormItem v-if="activePass !== 'FIRST'" label="产出米数(m)">
                  <InputNumber v-model:value="reportForm.outputLength" :min="0" class="full-input" disabled />
                </FormItem>
                <FormItem v-if="isFirstAllocationReport" required class="report-time-item" label="开始时间">
                  <DatePicker
                    v-model:value="reportForm.startTime"
                    :disabled="!reportTimeEditing"
                    class="report-time-edit-field"
                    format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请先在外层点击开工"
                    :show-time="{ format: 'HH:mm:ss' }"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    @change="handleReportStartTimeChange"
                  />
                </FormItem>
                <FormItem v-if="isFirstAllocationReport" required class="report-time-item" label="结束时间">
                  <DatePicker
                    v-model:value="reportForm.endTime"
                    :disabled="!reportTimeEditing"
                    class="report-time-edit-field"
                    format="YYYY-MM-DD HH:mm:ss"
                    placeholder="请先在外层点击完工"
                    :show-time="{ format: 'HH:mm:ss' }"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    @change="handleReportEndTimeChange"
                  />
                </FormItem>
                <FormItem v-if="!isFirstAllocationReport" class="report-time-item" label="开始时间">
                  <DatePicker
                    v-model:value="reportForm.startTime"
                    :disabled="!reportTimeEditing"
                    class="report-time-edit-field"
                    format="YYYY-MM-DD HH:mm:ss"
                    :show-time="{ format: 'HH:mm:ss' }"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    @change="handleReportStartTimeChange"
                  />
                </FormItem>
                <FormItem v-if="!isFirstAllocationReport" class="report-time-item" label="结束时间">
                  <DatePicker
                    v-model:value="reportForm.endTime"
                    :disabled="!reportTimeEditing"
                    class="report-time-edit-field"
                    format="YYYY-MM-DD HH:mm:ss"
                    :show-time="{ format: 'HH:mm:ss' }"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    @change="handleReportEndTimeChange"
                  />
                </FormItem>
                <FormItem v-if="!isFirstAllocationReport" label="自检">
                  <RadioGroup v-model:value="reportForm.selfCheck">
                    <Radio value="OK">OK</Radio>
                    <Radio value="NG">NG</Radio>
                  </RadioGroup>
                </FormItem>
                <FormItem v-if="!isFirstAllocationReport" label="不良代码">
                  <Select
                    v-model:value="reportForm.defectCode"
                    allow-clear
                    :options="[
                      { label: '厚度异常', value: 'THICKNESS' },
                      { label: '宽幅异常', value: 'WIDTH' },
                      { label: '表面异常', value: 'SURFACE' },
                    ]"
                  />
                </FormItem>
              </Form>
              <div v-if="activePass !== 'FIRST'" class="report-output-hint">
                产出米数 = 投入米数 - 固定损耗-<span class="report-loss-sample-emphasis">包含小样条</span>（米） - NAP留样米数 - 研发消耗米数 - 异常米数
              </div>
                </fieldset>

                <div v-if="reportReadonly && reportCurrentRecord?.consumption" class="report-output-hint">
                  本次砂纸消耗：{{ reportCurrentRecord.consumption.sandpaperQty ?? '-' }} {{ reportCurrentRecord.consumption.sandpaperUnit }}；
                  本次导布消耗：{{ reportCurrentRecord.consumption.guideClothQty ?? '-' }} {{ reportCurrentRecord.consumption.guideClothUnit }}
                </div>
                <div class="report-replace-row">
                  <fieldset class="erp-fieldset report-replace-fieldset">
                    <legend>砂纸更换</legend>
                    <Form layout="vertical" class="report-replace-form-grid">
                      <FormItem class="report-current-batch-item" label="当前砂纸批号">
                        <Input :value="sandpaper.batchNo || '-'" disabled />
                      </FormItem>
                      <FormItem label="是否更换">
                        <RadioGroup v-model:value="reportForm.sandpaperChanged" @change="handleReportReplaceChange('SANDPAPER')">
                          <Radio value="NO">否</Radio>
                          <Radio value="YES">是</Radio>
                        </RadioGroup>
                      </FormItem>
                      <FormItem label="新砂纸批号">
                        <Input
                          v-model:value="reportForm.sandpaperNewBatchNo"
                          data-report-field="sandpaperNewBatchNo"
                          :disabled="reportForm.sandpaperChanged !== 'YES'"
                          @update:value="clearReportConsumableSelection('SANDPAPER')"
                        >
                          <template #suffix>
                            <Tooltip title="选择磨皮砂纸边库批次">
                              <Button
                                class="rough-consumable-picker-button"
                                size="small"
                                type="text"
                                :disabled="reportForm.sandpaperChanged !== 'YES'"
                                @click.stop="openReportConsumableLedger('SANDPAPER')"
                              >
                                <IconifyIcon icon="lucide:package-search" />
                              </Button>
                            </Tooltip>
                          </template>
                        </Input>
                      </FormItem>
                      <FormItem v-if="reportForm.sandpaperChanged === 'YES'" :label="`本次消耗量（${reportConsumption.sandpaperUnit || '台账单位'}）`" required>
                        <InputNumber v-model:value="reportConsumption.sandpaperQty" :min="0.001" :precision="3" />
                        <span>可用余额：{{ reportConsumptionBalances.sandpaper ?? '-' }}</span>
                      </FormItem>
                      <FormItem label="更换原因">
                        <Input v-model:value="reportForm.sandpaperReplaceReason" :disabled="reportForm.sandpaperChanged !== 'YES'" />
                      </FormItem>
                    </Form>
                  </fieldset>

                <fieldset class="erp-fieldset report-replace-fieldset">
                    <legend>导布更换</legend>
                    <Form layout="vertical" class="report-replace-form-grid">
                      <FormItem class="report-current-batch-item" label="当前导布批号">
                        <Input :value="guideCloth.batchNo || '-'" disabled />
                      </FormItem>
                      <FormItem label="是否更换">
                        <RadioGroup v-model:value="reportForm.guideClothChanged" @change="handleReportReplaceChange('GUIDE_CLOTH')">
                          <Radio value="NO">否</Radio>
                          <Radio value="YES">是</Radio>
                        </RadioGroup>
                      </FormItem>
                      <FormItem label="新导布批号">
                        <Input
                          v-model:value="reportForm.guideClothNewBatchNo"
                          data-report-field="guideClothNewBatchNo"
                          :disabled="reportForm.guideClothChanged !== 'YES'"
                          @update:value="clearReportConsumableSelection('GUIDE_CLOTH')"
                        >
                          <template #suffix>
                            <Tooltip title="选择磨皮导布边库批次">
                              <Button
                                class="rough-consumable-picker-button"
                                size="small"
                                type="text"
                                :disabled="reportForm.guideClothChanged !== 'YES'"
                                @click.stop="openReportConsumableLedger('GUIDE_CLOTH')"
                              >
                                <IconifyIcon icon="lucide:package-search" />
                              </Button>
                            </Tooltip>
                          </template>
                        </Input>
                      </FormItem>
                      <FormItem v-if="reportForm.guideClothChanged === 'YES'" :label="`本次消耗量（${reportConsumption.guideClothUnit || '台账单位'}）`" required>
                        <InputNumber v-model:value="reportConsumption.guideClothQty" :min="0.001" :precision="3" />
                        <span>可用余额：{{ reportConsumptionBalances.guideCloth ?? '-' }}</span>
                      </FormItem>
                      <FormItem label="更换原因">
                        <Input v-model:value="reportForm.guideClothReplaceReason" :disabled="reportForm.guideClothChanged !== 'YES'" />
                      </FormItem>
                    </Form>
                  </fieldset>
                </div>
                <div v-if="activePass === 'SECOND'" class="second-range-visual">
                  <div class="second-range-visual__head">
                    <strong>二磨区间占用</strong>
                    <span>当前区间：{{ secondReportRangeText }}</span>
                    <span v-if="isFirstAllocatedMode">对应一磨区间：{{ currentSecondFirstAllocationRangeText }}</span>
                    <span v-else>一磨产出米数：{{ formatNumber(getFirstGrindingOutputTotalLength()) }}</span>
                    <em v-if="hasSecondReportOverlap">
                      与已报区间重叠：{{ secondReportOverlapRanges[0]?.start.toFixed(3) }}-{{ secondReportOverlapRanges[0]?.end.toFixed(3) }} m
                    </em>
                  </div>
                  <div class="second-range-bar">
                    <span class="second-range-bar__empty">未报区间</span>
                    <span
                      v-for="segment in secondReportRangeSegments"
                      :key="segment.key"
                      class="second-range-segment"
                      :class="`second-range-segment--${segment.type}`"
                      :style="{ left: `${segment.left}%`, width: `${segment.width}%` }"
                    >
                      {{ segment.key === 'CURRENT' ? '当前' : segment.segment }}
                    </span>
                  </div>
                  <div class="second-range-legend">
                    <span><i class="is-empty"></i>未报</span>
                    <span><i class="is-reported"></i>已报</span>
                    <span><i class="is-current"></i>当前</span>
                    <span><i class="is-overlap"></i>重叠</span>
                  </div>
                </div>
                <div v-else-if="isFirstAllocationReport" class="second-range-visual">
                  <div class="second-range-visual__head">
                    <strong>一磨区间占用</strong>
                    <span>当前区间：{{ firstAllocationReportRangeText }}</span>
                    <span>湿法产出米数：{{ formatNumber(firstAllocationSourceTotalLength) }}</span>
                    <em v-if="hasFirstAllocationReportOverlap">
                      与已确认区间重叠：{{ firstAllocationReportOverlapRanges[0]?.start.toFixed(3) }}-{{ firstAllocationReportOverlapRanges[0]?.end.toFixed(3) }} m
                    </em>
                  </div>
                  <div class="second-range-bar">
                    <span class="second-range-bar__empty">未报区间</span>
                    <span
                      v-for="segment in firstAllocationReportRangeSegments"
                      :key="segment.key"
                      class="second-range-segment"
                      :class="`second-range-segment--${segment.type}`"
                      :style="{ left: `${segment.left}%`, width: `${segment.width}%` }"
                    >
                      {{ segment.key === 'CURRENT' ? '当前' : segment.segment }}
                    </span>
                  </div>
                  <div class="second-range-legend">
                    <span><i class="is-empty"></i>未报</span>
                    <span><i class="is-reported"></i>已确认</span>
                    <span><i class="is-current"></i>当前</span>
                  </div>
                </div>
              </div>
            </div>
          </TabPane>
          <TabPane v-if="!isFirstAllocationReport" key="abnormal-position" tab="异常位置">
            <div class="report-tab-stack">
              <fieldset class="erp-fieldset">
                <legend>{{ activePass === 'FIRST' ? '第一次磨皮' : '第二次磨皮' }}异常位置</legend>
                <div class="report-abnormal-toolbar">
                  <span>
                    计划号：{{ activePass === 'FIRST' ? (firstReportSourcePlanNo || currentPlan.planNo || '-') : (reportForm.planNo || currentPlan.planNo || '-') }}
                    {{ activePass === 'FIRST' ? '母批批号' : '分段批号' }}：{{ activePass === 'FIRST' ? (firstReportSourceBatchNo || reportForm.batchNo || '-') : (reportForm.batchNo || currentPlan.batchNo || '-') }}
                    <template v-if="activePass === 'SECOND'">　分段：{{ reportForm.segmentMark || '不分段' }}</template>
                  </span>
                  <Button size="small" type="primary" @click="addReportAbnormalRow">
                    <IconifyIcon icon="lucide:plus" class="mr-1" />
                    新增
                  </Button>
                </div>
                <table class="pp-grid report-abnormal-table">
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
                        <Input v-model:value="row.positionText" placeholder="请输入异常位置" />
                      </td>
                      <td>
                        <InputNumber v-model:value="row.abnormalLength" :min="0" :precision="3" class="full-input" />
                      </td>
                      <td>
                        <Input v-model:value="row.remark" placeholder="备注" />
                      </td>
                      <td align="center">
                        <Button danger size="small" type="link" @click="removeReportAbnormalRow(index)">删除</Button>
                      </td>
                    </tr>
                    <tr v-if="!reportAbnormalRows.length">
                      <td colspan="5" class="pp-empty-cell" align="center">暂无异常位置明细</td>
                    </tr>
                  </tbody>
                </table>
              </fieldset>
            </div>
          </TabPane>
        </Tabs>
      </div>
        <div class="modal-footer">
        <Button v-if="canDeleteReadonlyReport" danger :loading="reportDeleting" @click="handleDeleteCurrentReport">删除报工</Button>
        <Button
          v-if="canReviseStatisticsData"
          v-access:code="['mes:sfc:rough-grinding-console-prototype:statistics-data:revise']"
          type="primary"
          @click="openFirstAllocationQuantityRevisionModal"
        >
          <IconifyIcon icon="lucide:chart-no-axes-column" class="mr-1" /> 修订统计数据
        </Button>
        <Button
          v-if="canEditReadonlyReportTime && !reportTimeEditing"
          v-access:code="[activePass === 'FIRST' ? 'mes:sfc:rough-grinding-console-prototype:first-report-time:update' : 'mes:sfc:rough-grinding-console-prototype:second-report-time:update']"
          type="primary"
          @click="startEditReadonlyReportTime"
        >
          修订开工完工时间
        </Button>
        <Button v-if="reportTimeEditing" type="primary" :loading="reportTimeSaving" @click="saveReadonlyReportTime">
          保存时间
        </Button>
        <Button v-if="!reportReadonly && !reportForm.startTime && !isFirstAllocationReport" type="primary" @click="startReportWork">开工</Button>
        <Button
          v-if="!reportReadonly && reportStep === 'PARAMS' && reportForm.startTime"
          type="primary"
          :loading="roughDevProcessParamLoading[activePass]"
          @click="handleReportParamsAction"
        >
          {{ getReportParamsActionText() }}
        </Button>
        <Button
          v-if="!reportReadonly && reportStep === 'REPORT'"
          type="primary"
          :disabled="reportSubmitting"
          :loading="reportSubmitting"
          @click="submitReport"
        >
          {{ isFirstAllocationReport ? '提交分段处理确认' : '提交报工收卷' }}
        </Button>
        <Button @click="requestCloseReport">{{ reportTimeEditing ? '取消修改' : reportReadonly ? '关闭' : '取消' }}</Button>
      </div>
    </AModal>

    <AModal
      v-model:open="firstAllocationQuantityRevisionVisible"
      title="修订磨皮统计数据"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="firstAllocationQuantityRevisionSubmitting"
      @ok="handleFirstAllocationQuantityRevisionConfirm"
    >
      <Form layout="vertical">
        <template v-if="isStatisticsRevisionFirstAllocation">
          <FormItem label="当前确认加工米数(m)">
            <InputNumber
              :value="getCurrentFirstAllocationConfirmedLength()"
              class="w-full"
              disabled
              :precision="3"
            />
          </FormItem>
          <FormItem label="确认加工米数(m)" required>
            <InputNumber
              v-model:value="firstAllocationQuantityRevisionForm.confirmedLength"
              class="w-full"
              :min="0.001"
              :precision="3"
            />
          </FormItem>
        </template>
        <template v-else>
          <FormItem :label="statisticsRevisionProcessLabel" required>
            <InputNumber
              v-model:value="firstAllocationQuantityRevisionForm.processLength"
              class="w-full"
              :min="0"
              :precision="3"
            />
          </FormItem>
          <FormItem label="固定损耗(m)" required>
            <InputNumber
              v-model:value="firstAllocationQuantityRevisionForm.lossLength"
              class="w-full"
              :min="0"
              :precision="3"
            />
          </FormItem>
          <FormItem label="产出米数(m)" required>
            <InputNumber
              v-model:value="firstAllocationQuantityRevisionForm.outputLength"
              class="w-full"
              :min="0"
              :precision="3"
            />
          </FormItem>
        </template>
        <FormItem label="修订原因" required>
          <Input.TextArea
            v-model:value="firstAllocationQuantityRevisionForm.reason"
            :maxlength="200"
            :rows="3"
            show-count
          />
        </FormItem>
      </Form>
    </AModal>

    <StationFormRuntimeFillModal
      v-model:open="roughDevProcessParamFillOpen"
      :form="roughDevProcessParamFillForm"
      :initial-params="roughDevProcessParamInitialParams"
      skip-business-param-step
      @success="handleRoughDevProcessParamSaved"
    />

    <AModal
      :open="roughDevProcessParamViewVisible"
      :footer="null"
      :title="null"
      :z-index="3220"
      width="100vw"
      wrap-class-name="hc-pass-work-modal rough-dev-process-param-view-modal"
      @cancel="closeRoughDevProcessParamDetail"
    >
      <div class="rough-dev-param-runtime-view">
        <div class="rough-dev-param-runtime-view__header">
          <strong>{{ roughDevProcessParamViewMode === 'edit' ? '修改' : '查看' }}{{ getRoughDevProcessParamName(activePass) }}</strong>
          <span v-if="roughDevProcessParamViewRecord" class="rough-dev-param-runtime-view__status">
            {{ getRoughDevRecordStatusMeta(roughDevProcessParamViewRecord.recordStatus).text }}
          </span>
          <Button
            v-if="roughDevProcessParamViewMode === 'edit' && roughDevProcessParamViewRecord && isRoughDevProcessParamDraft(roughDevProcessParamViewRecord)"
            :loading="roughDevProcessParamSaving"
            type="primary"
            @click="saveRoughDevProcessParamView()"
          >
            保存
          </Button>
          <Button
            v-if="roughDevProcessParamViewRecord && !isRoughDevProcessParamConfirmed(roughDevProcessParamViewRecord)"
            :loading="roughDevProcessParamConfirming"
            type="primary"
            danger
            @click="confirmRoughDevProcessParamRecord(roughDevProcessParamViewRecord)"
          >
            确认
          </Button>
          <Button @click="closeRoughDevProcessParamDetail">关闭</Button>
        </div>
        <div class="rough-dev-param-runtime-view__body">
          <StationFormRuntimeRenderer
            v-if="roughDevProcessParamViewRecord && !roughDevProcessParamViewLoading"
            ref="roughDevProcessParamRuntimeRef"
            v-model:header-data="roughDevProcessParamViewHeaderData"
            :form-name="getRoughDevProcessParamName(activePass)"
            :items="roughDevProcessParamViewItems"
            :record-meta="roughDevProcessParamViewMetaItems"
            :schema="roughDevProcessParamViewSchema"
            :readonly="roughDevProcessParamViewMode !== 'edit' || !isRoughDevProcessParamDraft(roughDevProcessParamViewRecord)"
          />
          <div v-else class="rough-dev-param-runtime-loading">正在加载工艺参数点检表详情...</div>
        </div>
      </div>
    </AModal>

    <AModal
      :open="middleProductDetailVisible"
      :footer="null"
      :title="null"
      width="100vw"
      wrap-class-name="hc-pass-work-modal rough-middle-product-modal"
      @cancel="closeMiddleProductDetail()"
    >
      <RoughMiddleProductRecordSheet
        v-model:page="middleProductDetailPage"
        :allow-remove-attachment="middleProductDetailMode !== 'view'"
        :attachments="middleProductAttachments"
        :columns="middleThicknessColumns"
        detail-title="中间品记录明细"
        :editable="middleProductDetailMode !== 'view'"
        :head-fields="middleProductHeadFields"
        :loading="middleProductDetailLoading"
        :meta-items="middleProductMetaItems"
        :page-size="MIDDLE_PRODUCT_PAGE_SIZE"
        :rows="middleProductRows"
        :signature-fields="middleProductSignatureFields"
        :title="`${middleProductDetailMode === 'edit' ? '填写' : '查看'}${middleProductDisplayName}`"
        @open-attachment="openMiddleProductAttachment"
        @remove-attachment="removeMiddleProductAttachment"
        @row-keydown="handleMiddleProductInputKeydown"
        @update-head-field="handleMiddleProductHeadFieldUpdate"
        @update-signature-field="handleMiddleProductSignatureFieldUpdate"
      >
        <template #actions>
          <input
            ref="middleProductImportInputRef"
            accept=".xls,.xlsx,.xlsm"
            class="hidden-file-input"
            type="file"
            @change="handleMiddleProductImportFile"
          />
          <Button size="small" @click="handleMiddleProductExport">导出Excel</Button>
          <Button v-if="middleProductDetailMode === 'edit'" size="small" @click="addMiddleProductRow">
            <IconifyIcon icon="lucide:plus" class="mr-1" />
            增加行
          </Button>
          <Button v-if="middleProductDetailMode === 'edit'" size="small" @click="handleMiddleProductImportClick">导入Excel</Button>
          <Button v-if="middleProductDetailMode === 'edit'" size="small" type="primary" @click="saveMiddleProductDetail">保存</Button>
          <Button
            v-if="middleProductDetailMode === 'edit' && middleProductEditingRecordId && !middleProductConfirmed"
            size="small"
            type="primary"
            @click="confirmMiddleProductDetail"
          >
            确认
          </Button>
          <Button size="small" @click="closeMiddleProductDetail()">关闭</Button>
        </template>
      </RoughMiddleProductRecordSheet>
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

.rough-console {
  --industrial-accent: #0ea5e9;
  --industrial-border: #5f6b7a;
  --industrial-card: #f4f7fa;
  --industrial-card-deep: #e5ebf2;
  --industrial-dark: #1f2937;

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

.rough-console.is-source-maximized {
  grid-template-rows:
    max-content
    minmax(0, 1fr);
}

.hidden-file-input {
  display: none;
}

.erp-card {
  border: 1px solid var(--industrial-border);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 2px 0 rgba(15, 23, 42, 0.1);
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

.erp-card {
  padding: 8px 10px;
  min-height: 0;
  margin-bottom: 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.72), rgba(230, 236, 244, 0.76)),
    var(--industrial-card);
}

.plan-scan-card {
  display: flex;
  flex-direction: column;
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

.rough-console :deep(.ant-btn-primary) {
  background: linear-gradient(180deg, #0ea5e9 0%, #0369a1 100%);
  border-color: #075985;
  border-radius: 2px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.rough-console :deep(.ant-btn) {
  border-radius: 2px;
}

.rough-console :deep(.ant-btn[disabled]),
.rough-console :deep(.ant-btn.ant-btn-disabled) {
  color: #64748b !important;
  text-shadow: none !important;
  background: #e2e8f0 !important;
  border-color: #cbd5e1 !important;
  box-shadow: none !important;
  opacity: 1;
}

.rough-console :deep(.ant-btn[disabled] *),
.rough-console :deep(.ant-btn.ant-btn-disabled *) {
  color: #64748b !important;
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

.erp-form-grid {
  display: grid;
  grid-template-columns: 7fr 18fr 7fr 18fr 7fr 18fr 7fr 18fr;
  gap: 6px 8px;
  align-items: stretch;
}

.plan-scan-card .erp-form-grid {
  flex: 0 0 auto;
  grid-template-columns: 7fr 18fr 7fr 18fr 7fr 18fr 7fr 18fr;
  grid-template-rows: repeat(2, minmax(32px, auto));
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

.rough-qtime-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 12px;
  margin-top: 6px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-left: 4px solid #f97316;
}

.rough-qtime-bar--qtime-only {
  justify-content: center;
  padding: 6px 12px;
}

.rough-qtime-bar--segment-timing {
  justify-content: space-between;
  gap: 12px;
  padding: 8px;
}

.first-segment-timing-actions {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(4, minmax(0, 1fr)) minmax(0, 0.86fr);
  gap: 6px;
  min-width: 0;
}

.first-segment-timing-action {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3px 5px;
  padding: 5px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid #fdba74;
}

.first-segment-timing-action > strong {
  grid-column: 1 / -1;
  color: #9a3412;
  font-size: 12px;
}

.first-segment-timing-action > span {
  grid-column: 1 / -1;
  overflow: hidden;
  color: #475569;
  font-family: var(--vben-font-family-mono);
  font-size: 11px;
  line-height: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.first-segment-timing-action :deep(.ant-btn) {
  min-width: 0;
  height: 24px;
  padding: 0 4px;
  font-size: 12px;
}

.first-segment-timing-action.no-segment {
  background:
    linear-gradient(180deg, rgba(180, 83, 9, 0.2), rgba(245, 158, 11, 0.14)),
    rgba(255, 255, 255, 0.76);
}

.rough-qtime-bar__qtime-summary {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  gap: 2px;
  align-items: center;
  min-width: 88px;
}

.first-segment-timing-fieldset {
  margin-top: 10px;
}

.mother-batch-timing-report-card {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.mother-batch-timing-report-card > label {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.mother-batch-timing-report-card > strong {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-family: var(--vben-font-family-mono);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mother-batch-timing-report-action {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px;
  align-items: center;
  min-width: 0;
}

.mother-batch-timing-report-action > strong {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-family: var(--vben-font-family-mono);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mother-batch-timing-report-action :deep(.ant-btn) {
  height: 26px;
  padding: 0 8px;
  font-size: 12px;
}

.mother-batch-timing-report-card > span {
  grid-column: 1 / -1;
  color: #64748b;
  font-size: 12px;
}

.rough-qtime-bar__content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px 20px;
}

.rough-qtime-bar__content--qtime-only {
  flex: 0 0 auto;
  justify-content: center;
}

.rough-qtime-bar__item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.rough-qtime-bar__item--qtime-only {
  gap: 8px;
}

.rough-qtime-bar__label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.rough-qtime-bar__qtime-value {
  font-family: var(--vben-font-family-mono);
  font-size: 18px;
  font-weight: 800;
  line-height: 22px;
}

.rough-qtime-bar__qtime-value--normal {
  color: #15803d;
}

.rough-qtime-bar__qtime-value--warning {
  color: #d97706;
}

.rough-qtime-bar__qtime-value--danger {
  color: #dc2626;
}

.rough-qtime-bar__qtime-value--empty {
  color: #64748b;
}

.plan-value--danger {
  color: #dc2626 !important;
  font-size: 14px !important;
  font-weight: 900 !important;
}

.plan-value--stock {
  color: #0369a1 !important;
  font-size: 14px !important;
  font-weight: 900 !important;
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

.inspection-stamp-slot {
  display: flex;
  align-self: stretch;
  flex: 0 0 214px;
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
  width: 184px;
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
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.18), rgba(15, 23, 42, 0.04));
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

.inspection-stamp-side--empty::before {
  border-right-style: dashed;
  border-left-style: dashed;
  opacity: 0.74;
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

.console-meta-item--machine.is-unbound .console-meta-value {
  color: #b45309;
}

.console-meta-item--machine.is-unbound .console-meta-sub {
  color: #9a3412;
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

.equipment-select-filter {
  width: 220px;
}

.equipment-select-tip {
  padding: 8px 10px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #8794a4;
  border-top: 0;
}

.first-inspection-status-chip {
  cursor: pointer;
}

.first-inspection-status-chip:hover .console-meta-label {
  color: #0369a1;
}

.first-inspection-status-chip.is-empty .console-meta-value {
  color: #b45309;
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
  max-width: none;
}

.erp-row {
  display: grid;
  grid-template-columns: 31fr 69fr;
  gap: 8px;
}

.plan-fields {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.plan-fields div,
.metric-inline div,
.plan-fields span,
.metric-inline span,
.operation-metrics span,
.consumable-fields span {
  display: block;
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-fields strong,
.metric-inline strong,
.operation-metrics strong,
.consumable-fields strong {
  display: block;
  overflow: hidden;
  color: #1f2937;
  font-size: 14px;
  line-height: 19px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.erp-stat-grid {
  display: grid;
  gap: 6px;
}

.erp-stat-grid.three {
  grid-template-columns: repeat(3, 1fr);
}

.erp-stat-grid.four {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.erp-stat-grid div {
  padding: 7px 8px;
  background: linear-gradient(180deg, #f8fafc 0%, #dfe7f0 100%);
  border: 1px solid #94a3b8;
  box-shadow: inset 3px 0 0 rgba(14, 165, 233, 0.7);
}

.erp-stat-grid span {
  display: block;
  color: #6b7280;
  font-size: 12px;
}

.erp-stat-grid strong {
  display: block;
  overflow: hidden;
  color: #0369a1;
  font-size: 23px;
  line-height: 30px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.consumable-erp-grid {
  display: grid;
  grid-template-columns: 14fr 36fr 16fr 34fr;
  gap: 6px 8px;
}

.consumable-card {
  display: flex;
  flex-direction: column;
  min-height: auto;
  padding: 6px 10px;
}

.full-consumable-card {
  margin-bottom: 0;
}

.consumable-card .erp-card-title {
  height: 26px;
  min-height: 26px;
  margin-bottom: 6px;
  font-size: 15px;
}

.consumable-card .erp-card-title :deep(.ant-tag) {
  padding: 2px 10px;
  font-size: 13px;
  font-weight: 900;
}

.consumable-limit-note {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  padding: 0 8px;
  overflow: hidden;
  color: #475569;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-overflow: ellipsis;
  text-transform: none;
  white-space: nowrap;
}

.consumable-replace-btn {
  height: 24px;
  padding: 0 12px;
  font-weight: 900;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.18);
}

.consumable-inline-layout {
  display: grid;
  gap: 8px;
  align-items: stretch;
  flex: 0 0 auto;
}

.consumable-inline-layout--plain {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  height: auto;
}

.consumable-compact-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 6px;
  min-height: 0;
}

.consumable-compact-cell {
  display: grid;
  grid-template-columns: minmax(72px, 0.4fr) minmax(0, 1fr);
  min-width: 0;
  min-height: 30px;
  padding: 0;
  overflow: hidden;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.consumable-compact-cell label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  padding: 4px 6px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  text-align: right;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.consumable-compact-cell span {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 4px 7px;
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #a2adba;
  border-left: 0;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.07);
}

.consumable-compact-cell--split {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.consumable-split-field {
  display: grid;
  grid-template-columns: minmax(80px, 0.58fr) minmax(0, 1fr);
  min-width: 0;
  min-height: 30px;
}

.consumable-compact-cell--split label {
  padding: 4px 5px;
  font-size: 11px;
  line-height: 18px;
  white-space: nowrap;
}

.consumable-compact-cell--split strong {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 4px 7px;
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #a2adba;
  border-left: 0;
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.07);
}

.consumable-compact-cell.warning label,
.consumable-compact-cell.warning span,
.consumable-compact-cell.warning strong {
  color: #9f1239;
  border-color: #f43f5e;
}

.consumable-status-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: auto;
  height: auto;
  padding: 0;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.consumable-status-card.warning {
  background: transparent;
}

.consumable-status-grid {
  display: grid;
  grid-template-columns: 7fr 18fr 7fr 18fr;
  grid-template-rows: repeat(2, minmax(32px, auto));
  flex: 0 0 auto;
  gap: 6px 8px;
  min-height: auto;
}

.consumable-status-grid label {
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

.consumable-status-grid span {
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

.consumable-status-card.warning .consumable-status-grid label,
.consumable-status-card.warning .consumable-status-grid span {
  border-color: #f43f5e;
}

.consumable-card .consumable-erp-grid {
  flex: 0 0 34%;
  grid-template-columns: 15fr 38fr 16fr 31fr;
  gap: 6px 8px;
  min-width: 360px;
}

.consumable-card .consumable-erp-grid label {
  padding: 5px 8px;
  font-size: 13px;
}

.consumable-card .consumable-erp-grid strong {
  min-height: 30px;
  padding: 5px 9px;
  font-size: 14px;
  line-height: 20px;
}

.consumable-life-grid {
  display: grid;
  flex: 1 1 auto;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  min-width: 0;
  min-height: 0;
}

.consumable-life-item {
  width: 100%;
  min-height: 0;
  overflow: hidden;
  padding: 7px 9px;
  text-align: left;
  cursor: pointer;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1px, transparent 1px) 0 0 / 16px 16px,
    linear-gradient(180deg, #eef3f8 0%, #d9e2ec 100%);
  border: 1px solid #64748b;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    inset 0 -1px 0 rgba(15, 23, 42, 0.12);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.consumable-life-item:hover {
  border-color: #0284c7;
  box-shadow:
    0 2px 10px rgba(2, 132, 199, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.life-row-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.life-row-head span {
  color: #1e293b;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.life-row-head strong {
  margin-left: auto;
  color: #075985;
  font-size: 20px;
  font-family: Consolas, 'Courier New', monospace;
}

.life-svg {
  display: block;
  width: 100%;
  height: auto;
  min-height: 52px;
  max-height: 56px;
  margin-top: -2px;
  overflow: visible;
}

.svg-paper-sheet {
  fill: #f2c887;
  stroke: #17202a;
  stroke-linejoin: round;
  stroke-width: 2.4;
}

.svg-paper-sheet.guide-sheet {
  fill: #d8e6f2;
}

.svg-paper-roll .roll-body {
  fill: #d4944b;
  stroke: #17202a;
  stroke-linejoin: round;
  stroke-width: 2.4;
}

.svg-paper-roll .roll-top,
.svg-paper-roll .roll-core {
  fill: #f2c887;
  stroke: #17202a;
  stroke-width: 2.4;
}

.svg-paper-roll.guide-roll .roll-body,
.svg-paper-roll.guide-roll .roll-top,
.svg-paper-roll.guide-roll .roll-core {
  fill: #c7d9e8;
}

.svg-paper-roll.guide-roll .roll-top,
.svg-paper-roll.guide-roll .roll-core {
  fill: #e7f0f8;
}

.svg-paper-roll .roll-dash {
  fill: none;
  stroke: #17202a;
  stroke-dasharray: 7 6;
  stroke-linecap: round;
  stroke-width: 2.4;
}

.svg-used-layer {
  mix-blend-mode: multiply;
}

.svg-limit-line {
  stroke: #dc2626;
  stroke-linecap: square;
  stroke-width: 3;
}

.consumable-life-item.warning .svg-used-layer {
  opacity: 0.95;
}

.consumable-life-item.warning .svg-limit-line {
  stroke: #b91c1c;
  stroke-width: 3;
}

.consumable-life-item.warning .life-row-head strong {
  color: #dc2626;
}

.consumable-life-item p {
  margin: 2px 0 0;
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
  line-height: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.operation-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.operation-head p {
  margin: -3px 0 8px;
  color: #6b7280;
  font-size: 12px;
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

.console-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #075985;
}

.console-tabs :deep(.ant-tabs-ink-bar) {
  height: 3px;
  background: #0ea5e9;
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

.console-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
  width: 100%;
  max-width: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.console-tabs :deep(.ant-tabs-content),
.console-tabs :deep(.ant-tabs-tabpane) {
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.tab-erp-content {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 10px;
  height: calc(100% - 8px);
}

.cloth-tab-content {
  grid-template-rows: 1fr;
}

.rough-workbench-panel {
  position: relative;
}

.rough-workbench-panel--blocked .cloth-board {
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

.cloth-board {
  display: grid;
  grid-template-rows: minmax(30px, 0.45fr) minmax(72px, 2.1fr) minmax(44px, 0.7fr);
  gap: 10px;
  height: 100%;
  min-height: 0;
  padding: 10px;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.035) 1px, transparent 1px) 0 0 / 22px 22px,
    linear-gradient(180deg, #f5f8fb 0%, #e1e9f1 100%);
  border: 1px solid #7d8b9b;
}

.rough-first-cloth-board--allocated {
  grid-template-rows: minmax(48px, auto) minmax(96px, 1fr) minmax(58px, auto);
}

.first-allocation-mode-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  min-height: 40px;
  padding: 5px 10px;
  background: #f8fafc;
  border: 1px solid #94a3b8;
}

.first-allocation-mode-bar > strong {
  color: #0f172a;
  font-size: 14px;
}

.first-allocation-mode-bar__spacer {
  flex: 1 1 auto;
  min-width: 0;
}

.first-allocation-mode-bar__qtime {
  align-self: stretch;
  min-width: 104px;
  padding: 1px 8px;
  background: #fff7ed;
  border: 1px solid #fdba74;
}

.first-allocation-mode-bar__qtime .rough-qtime-bar__label {
  color: #9a3412;
}

.first-allocation-mode-bar__qtime .rough-qtime-bar__qtime-value {
  font-size: 16px;
}

.first-allocation-visual-strip {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 0.86fr;
  min-width: 0;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.14) 1px, transparent 1px) 0 0 / 22px 100%,
    linear-gradient(180deg, #e0edf8 0%, #bfdbef 100%);
  border: 1px solid #52718c;
}

.first-allocation-visual-segment {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 23px 10px 20px;
  overflow: hidden;
  color: #f8fafc;
  text-align: left;
  cursor: pointer;
  background: rgba(15, 23, 42, 0.12);
  border: 0;
  border-right: 2px solid rgba(30, 41, 59, 0.74);
}

.first-allocation-visual-segment:last-child {
  border-right: 0;
}

.first-allocation-visual-segment:hover,
.first-allocation-visual-segment.reported {
  background: rgba(2, 132, 199, 0.3);
}

.first-allocation-visual-segment.timing-started {
  background:
    linear-gradient(180deg, rgba(21, 128, 61, 0.4), rgba(14, 116, 144, 0.22)),
    rgba(15, 23, 42, 0.12);
}

.first-allocation-visual-segment.no-segment {
  background:
    linear-gradient(180deg, rgba(180, 83, 9, 0.38), rgba(245, 158, 11, 0.24)),
    rgba(15, 23, 42, 0.12);
}

.first-allocation-visual-segment::after {
  position: absolute;
  right: 14px;
  bottom: 8px;
  left: 14px;
  height: 5px;
  content: '';
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.78), rgba(14, 165, 233, 0.4));
  border-radius: 999px;
}

.first-allocation-visual-segment__status {
  position: absolute;
  top: 6px;
  right: 8px;
  max-width: calc(100% - 16px);
  padding: 1px 6px !important;
  color: #e0f2fe !important;
  font-size: 11px !important;
  font-weight: 900;
  background: rgba(15, 23, 42, 0.78) !important;
  border: 1px solid rgba(226, 232, 240, 0.28);
  border-radius: 999px;
}

.first-allocation-visual-segment strong,
.first-allocation-visual-segment span:not(.first-allocation-visual-segment__status) {
  width: fit-content;
  max-width: 100%;
  padding: 1px 6px;
  overflow: hidden;
  color: #f8fafc;
  font-size: 11px;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(15, 23, 42, 0.74);
}

.first-allocation-visual-segment strong {
  margin-bottom: 4px;
  font-size: 16px;
  line-height: 22px;
}

.second-cloth-board {
  grid-template-rows: minmax(86px, auto) minmax(96px, 1fr);
}

.first-allocation-toolbar {
  min-height: 48px;
}

.first-allocation-toolbar .second-sample-segment-strip {
  min-height: 48px;
}

.first-allocation-toolbar .second-sample-segment-cell {
  grid-template-rows: minmax(32px, 1fr);
}

.first-allocation-cloth-segment.reported {
  background:
    linear-gradient(180deg, rgba(21, 128, 61, 0.34), rgba(14, 165, 233, 0.16)),
    rgba(226, 232, 240, 0.24);
}

.first-allocation-cloth-segment.timing-started {
  background:
    linear-gradient(180deg, rgba(21, 128, 61, 0.28), rgba(14, 116, 144, 0.14)),
    rgba(226, 232, 240, 0.24);
}

.tab-source-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr 2.8fr;
  gap: 8px;
  align-items: stretch;
}

.first-source-metrics {
  display: block;
  height: 100%;
  min-height: 0;
}

.first-source-metrics .rough-qtime-bar {
  min-height: 0;
  margin-top: 0;
}

.first-source-metrics .rough-qtime-bar--segment-timing {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  height: 100%;
  padding: 7px;
  overflow: hidden;
}

.first-source-metrics .first-segment-timing-actions {
  grid-template-columns: repeat(4, minmax(0, 1fr)) minmax(0, 0.86fr);
  gap: 8px;
  height: 100%;
  padding: 0;
  overflow: visible;
  background: transparent;
  border: 0;
}

.mother-batch-timing-actions {
  display: grid;
  grid-template-columns: minmax(150px, 0.9fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
  min-width: 0;
}

.mother-batch-timing-title,
.mother-batch-timing-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 7px 9px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid #fdba74;
}

.mother-batch-timing-title > strong,
.mother-batch-timing-item > strong,
.mother-batch-timing-title > span,
.mother-batch-timing-item > span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mother-batch-timing-title > strong {
  color: #9a3412;
  font-size: 14px;
}

.mother-batch-timing-title > span,
.mother-batch-timing-item > span {
  color: #64748b;
  font-size: 12px;
}

.mother-batch-timing-item > strong {
  color: #0f172a;
  font-family: var(--vben-font-family-mono);
  font-size: 12px;
}

.mother-batch-timing-item :deep(.mother-batch-timing-item__button) {
  width: 100%;
  height: 24px;
  min-height: 0;
  margin-top: 4px;
  padding: 0 4px;
  font-size: 12px;
}

.first-source-metrics .first-segment-timing-action {
  grid-template-areas:
    'title title'
    'start start-action'
    'end end-action';
  grid-template-columns: minmax(0, 1fr) 48px;
  grid-template-rows: 17px minmax(0, 1fr) minmax(0, 1fr);
  gap: 3px 5px;
  box-sizing: border-box;
  height: 100%;
  min-width: 0;
  padding: 5px 6px;
}

.first-source-metrics .first-segment-timing-action > strong {
  grid-area: title;
  display: block;
  padding: 0;
  color: #9a3412;
  font-size: 13px;
  line-height: 17px;
  background: transparent;
  border: 0;
}

.first-source-metrics .first-segment-timing-action > span {
  grid-column: auto;
  display: block;
  padding: 0;
  overflow: hidden;
  color: #475569;
  font-family: var(--vben-font-family-mono);
  font-size: 11px;
  font-weight: 600;
  line-height: 15px;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
}

.first-source-metrics .first-segment-timing-action > span:nth-of-type(1) {
  grid-area: start;
}

.first-source-metrics .first-segment-timing-action > span:nth-of-type(2) {
  grid-area: end;
}

.first-source-metrics .first-segment-timing-action :deep(.ant-btn) {
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 0 4px;
  font-size: 12px;
}

.first-source-metrics .first-segment-timing-action :deep(.first-segment-timing-action__button--start) {
  grid-area: start-action;
}

.first-source-metrics .first-segment-timing-action :deep(.first-segment-timing-action__button--end) {
  grid-area: end-action;
}

.tab-source-metrics div {
  min-height: 0;
  padding: 0;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fafc 0%, #e5edf5 100%);
  border: 1px solid #8794a4;
}

.tab-source-metrics span,
.tab-source-metrics strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tab-source-metrics span {
  padding: 5px 9px;
  color: #075985;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.03em;
  background: linear-gradient(90deg, #d7dee7, #eef3f8);
  border-bottom: 1px solid #94a3b8;
}

.tab-source-metrics strong {
  padding: 5px 9px;
  color: #111827;
  font-size: 18px;
  line-height: 25px;
}

.tab-source-metrics p {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 0;
  padding: 7px 10px;
  margin: 0;
  color: #263445;
  font-size: 12px;
  font-weight: 700;
  line-height: 17px;
  background: linear-gradient(180deg, #f8fafc 0%, #e3ebf4 100%);
  border: 1px dashed #7dd3fc;
  border-left: 4px solid #0284c7;
}

.second-source-metrics {
  grid-template-columns: 1fr 1fr 2.8fr;
}

.second-compact-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr)) 1.8fr;
  gap: 6px;
  align-items: stretch;
}

.second-compact-metrics div,
.second-compact-metrics p {
  min-height: 0;
  margin: 0;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fafc 0%, #e5edf5 100%);
  border: 1px solid #8794a4;
}

.second-compact-metrics div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  min-width: 0;
  min-height: 28px;
}

.second-compact-metrics span,
.second-compact-metrics strong {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.second-compact-metrics span {
  flex: 0 0 auto;
  height: 100%;
  padding: 0 7px;
  color: #075985;
  font-size: 11px;
  font-weight: 800;
  background: linear-gradient(90deg, #d7dee7, #eef3f8);
  border-right: 1px solid #94a3b8;
}

.second-compact-metrics strong {
  flex: 1 1 auto;
  justify-content: flex-end;
  padding: 0 7px 0 0;
  color: #111827;
  font-size: 14px;
  line-height: 28px;
}

.second-compact-metrics p {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  color: #263445;
  font-size: 12px;
  font-weight: 700;
  line-height: 14px;
  border-left: 4px solid #0284c7;
}

.second-sample-toolbar {
  display: grid;
  grid-template-rows: 1fr;
  gap: 0;
  align-items: stretch;
  min-height: 86px;
}

.second-sample-toolbar__hint {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 28px;
  padding: 3px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fafc 0%, #e3ebf4 100%);
  border: 1px dashed #7dd3fc;
  border-left: 4px solid #0284c7;
}

.second-sample-toolbar__hint strong {
  color: #075985;
  font-size: 13px;
  font-weight: 900;
  line-height: 16px;
}

.second-sample-toolbar__hint span {
  overflow: hidden;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  line-height: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.second-sample-segment-strip {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 0.86fr;
  min-width: 0;
  min-height: 86px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18px 18px, rgba(15, 23, 42, 0.06) 0 1px, transparent 1.4px) 0 0 / 18px 18px,
    linear-gradient(180deg, #d7e4ee 0%, #9fb9cc 100%);
  border: 1px solid #475569;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.55),
    inset 0 8px 14px rgba(255, 255, 255, 0.24);
}

.second-sample-segment-cell {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(3, minmax(28px, 1fr));
  gap: 5px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: rgba(226, 232, 240, 0.18);
  border-right: 2px solid rgba(30, 41, 59, 0.72);
  padding: 7px;
}

.second-sample-lock-guard {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(32px, 1fr));
  grid-column: 1 / -1;
  grid-row: 1 / -1;
  gap: 5px;
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
}

.second-sample-action-button--sample {
  grid-column: 2 / 3;
  grid-row: 2 / 3;
}

.second-sample-action-button--middle {
  grid-column: 2 / 3;
  grid-row: 3 / 4;
}

.second-sample-action-button--timing {
  color: #7c2d12;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.second-sample-action-button--timing-start {
  grid-column: 1 / 2;
  grid-row: 1 / 2;
}

.second-sample-action-button--timing-end {
  grid-column: 2 / 3;
  grid-row: 1 / 2;
}

.second-sample-action-button--report {
  grid-column: 1 / 2;
  grid-row: 2 / 3;
}

.second-sample-action-button--print {
  grid-column: 1 / 2;
  grid-row: 3 / 4;
}

.second-sample-segment-cell:last-child {
  border-right: 0;
}

.second-sample-segment-cell.no-segment {
  background:
    linear-gradient(180deg, rgba(180, 83, 9, 0.3), rgba(245, 158, 11, 0.2)),
    rgba(251, 191, 36, 0.18);
}

.second-sample-action-button {
  display: flex;
  gap: 4px;
  align-items: center;
  justify-content: center;
  min-width: 0;
  min-height: 32px;
  padding: 5px 6px;
  overflow: hidden;
  color: #0f172a;
  text-align: center;
  cursor: pointer;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92) 0%, rgba(226, 232, 240, 0.86) 100%);
  border: 1px solid rgba(71, 85, 105, 0.52);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 1px 2px rgba(15, 23, 42, 0.1);
}

.second-sample-action-button__icon {
  flex: 0 0 auto;
  font-size: 14px;
}

.second-sample-action-button > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.second-sample-action-button.sent .second-sample-segment-button__text {
  color: #166534;
}

.second-sample-action-button.sample-status-waiting .second-sample-segment-button__text {
  color: #075985;
}

.second-sample-action-button.sample-status-ok .second-sample-segment-button__text {
  color: #166534;
}

.second-sample-action-button.sample-status-ng .second-sample-segment-button__text {
  color: #be123c;
}

.second-sample-action-button:hover {
  background: linear-gradient(180deg, rgba(224, 242, 254, 0.96) 0%, rgba(186, 230, 253, 0.84) 100%);
  border-color: rgba(2, 132, 199, 0.72);
}

.second-sample-action-button:disabled,
.second-sample-action-button.disabled {
  color: #94a3b8;
  cursor: not-allowed;
  background: rgba(226, 232, 240, 0.72);
  border-color: rgba(148, 163, 184, 0.45);
  box-shadow: none;
}

.second-sample-segment-button__text {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.second-sample-action-button--middle {
  color: #075985;
  font-size: 13px;
  font-weight: 900;
  line-height: 18px;
  white-space: nowrap;
}

.second-sample-action-button--middle.saved {
  color: #166534;
}

.second-sample-action-button--report {
  color: #92400e;
  font-size: 13px;
  font-weight: 950;
  line-height: 18px;
  white-space: nowrap;
}

.second-sample-action-button--report.second-sample-action-button--reported {
  color: #0f766e;
  background: linear-gradient(180deg, rgba(204, 251, 241, 0.96) 0%, rgba(153, 246, 228, 0.82) 100%);
  border-color: rgba(15, 118, 110, 0.72);
}

.second-sample-action-button--report .second-sample-action-button__icon {
  font-size: 14px;
}

.second-sample-action-button--print {
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 950;
  line-height: 18px;
  white-space: nowrap;
}

.second-sample-action-button:disabled,
.second-sample-action-button.disabled {
  color: #64748b !important;
  opacity: 1;
}

.second-sample-action-button:disabled .second-sample-action-button__icon,
.second-sample-action-button.disabled .second-sample-action-button__icon,
.second-sample-action-button:disabled > span,
.second-sample-action-button.disabled > span,
.second-sample-action-button:disabled .second-sample-segment-button__text,
.second-sample-action-button.disabled .second-sample-segment-button__text {
  color: #64748b !important;
}

.second-cloth-board .cloth-strip {
  overflow-x: hidden;
}

.cloth-strip {
  position: relative;
  display: grid;
  width: 100%;
  height: 100%;
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

.cloth-meter-ruler {
  position: absolute;
  right: 20px;
  bottom: 20px;
  left: 20px;
  z-index: 2;
  height: 10px;
  pointer-events: none;
  background: repeating-linear-gradient(90deg, rgba(15, 23, 42, 0.56) 0 1px, transparent 1px 28px);
}

.cloth-scale-info {
  position: absolute;
  z-index: 8;
  max-width: 58%;
  padding: 4px 7px;
  overflow: hidden;
  color: #111827;
  font-size: 12px;
  font-weight: 800;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #e2e8f0;
  border: 1px solid #0f172a;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.18);
}

.scale-top-right {
  top: 10px;
  right: 12px;
  display: flex;
  gap: 6px;
  align-items: center;
}

.scale-top-right em {
  font-style: normal;
}

.cloth-progress-label {
  position: absolute;
  bottom: 34px;
  z-index: 9;
  max-width: 260px;
  padding: 4px 8px;
  overflow: hidden;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #075985;
  border: 1px solid #082f49;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.18);
  transform: translateX(-100%);
}

.first-cloth-strip {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
}

.first-cloth-strip--reported {
  border-color: #0f766e;
}

.first-cloth-strip--reported .cloth-progress-fill {
  background:
    linear-gradient(90deg, rgba(20, 184, 166, 0.6), rgba(34, 197, 94, 0.34));
  border-right-color: rgba(15, 118, 110, 0.98);
}

.cloth-progress-fill {
  position: absolute;
  inset: 0 auto 0 0;
  z-index: 1;
  background:
    linear-gradient(90deg, rgba(2, 132, 199, 0.56), rgba(34, 197, 94, 0.28));
  border-right: 4px solid rgba(3, 105, 161, 0.95);
  box-shadow: inset -12px 0 18px rgba(30, 64, 175, 0.12);
}

.first-batch-badge {
  position: relative;
  z-index: 10;
  display: flex;
  width: min(72%, 680px);
  min-height: 76px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 24px;
  color: #0f172a;
  pointer-events: none;
  background: transparent;
  border: 0;
  text-shadow: 0 2px 6px rgba(255, 255, 255, 0.86);
}

.first-batch-badge span {
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
  line-height: 18px;
}

.first-batch-badge em {
  margin-top: 2px;
  color: #0f766e;
  font-size: 12px;
  font-style: normal;
  font-weight: 950;
  line-height: 16px;
}

.first-batch-badge strong {
  display: block;
  width: 100%;
  overflow: hidden;
  color: #082f49;
  font-size: 36px;
  font-weight: 950;
  line-height: 42px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloth-section {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  border-right: 2px solid rgba(30, 41, 59, 0.72);
}

.cloth-section:last-child {
  border-right: 0;
}

.cloth-section em {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.88);
  font-size: 12px;
  font-style: normal;
  letter-spacing: 0.08em;
}

.cloth-section strong,
.cloth-section em {
  width: fit-content;
  max-width: 100%;
  padding: 3px 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(15, 23, 42, 0.72);
}

.segmented-cloth-strip {
  grid-template-columns: 1fr 1fr 1fr 1fr 0.86fr;
  gap: 0;
  min-width: 0;
}

.cloth-segment {
  position: relative;
  z-index: 3;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 18px 8px 20px;
  text-align: left;
  cursor: pointer;
  background: rgba(226, 232, 240, 0.24);
  border: 0;
  border-right: 2px solid rgba(30, 41, 59, 0.72);
}

.cloth-segment:focus-visible {
  outline: 2px solid rgba(14, 165, 233, 0.88);
  outline-offset: -2px;
}

.cloth-segment:last-child {
  border-right: 0;
}

.cloth-segment.no-segment {
  background:
    linear-gradient(180deg, rgba(180, 83, 9, 0.32), rgba(245, 158, 11, 0.22)),
    rgba(251, 191, 36, 0.24);
}

.cloth-segment.inspection-ok {
  background:
    linear-gradient(180deg, rgba(22, 163, 74, 0.32), rgba(14, 165, 233, 0.12)),
    rgba(226, 232, 240, 0.24);
}

.cloth-segment:hover,
.first-cloth-strip:hover {
  background-color: rgba(14, 165, 233, 0.16);
}

.cloth-segment::after {
  position: absolute;
  right: 14px;
  bottom: 8px;
  left: 14px;
  height: 5px;
  content: '';
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.58), rgba(22, 119, 255, 0.3));
  border-radius: 999px;
}

.cloth-segment.inspection-ok::after {
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.9), rgba(22, 163, 74, 0.46));
}

.cloth-segment-topbar {
  position: absolute;
  top: 6px;
  right: 8px;
  z-index: 4;
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.cloth-segment-count {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: auto !important;
  max-width: 54px !important;
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

.cloth-segment strong {
  margin-bottom: 5px;
  color: #fff;
  font-size: 16px;
  line-height: 22px;
}

.cloth-segment span {
  color: #f8fafc;
  font-size: 11px;
  line-height: 16px;
}

.cloth-segment strong,
.cloth-segment span {
  width: fit-content;
  max-width: 100%;
  padding: 1px 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(15, 23, 42, 0.74);
}

.cloth-segment span.cloth-segment-qtime {
  max-width: calc(100% - 12px);
}

.cloth-segment span.cloth-segment-qtime.rough-qtime-bar__qtime-value--danger {
  color: #fff1f2;
  font-weight: 900;
  background: rgba(185, 28, 28, 0.92);
}

.cloth-segment span.cloth-segment-qtime.rough-qtime-bar__qtime-value--warning {
  color: #fffbeb;
  font-weight: 800;
  background: rgba(146, 64, 14, 0.88);
}

.cloth-segment span.cloth-segment-qtime.rough-qtime-bar__qtime-value--normal {
  color: #ecfdf5;
  background: rgba(22, 101, 52, 0.82);
}

.cloth-segment span.cloth-segment-qtime.rough-qtime-bar__qtime-value--empty {
  color: #e2e8f0;
  background: rgba(71, 85, 105, 0.74);
}

.cloth-segment-inspection-ok {
  width: fit-content;
  max-width: 100%;
  padding: 1px 6px;
  margin-bottom: 3px;
  overflow: hidden;
  color: #dcfce7;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(22, 101, 52, 0.86);
}

.cloth-segment-scan-status {
  width: fit-content;
  max-width: 100%;
  padding: 1px 6px;
  margin-bottom: 3px;
  overflow: hidden;
  color: #fef3c7;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(146, 64, 14, 0.86);
}

.cloth-segment-scan-status.confirmed {
  color: #dcfce7;
  background: rgba(21, 128, 61, 0.86);
}

.cloth-segment-scan-status.empty {
  color: #e2e8f0;
  background: rgba(51, 65, 85, 0.78);
}

.cloth-summary-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.second-cloth-board .cloth-summary-row {
  grid-template-columns: repeat(4, 1fr);
}

.cloth-summary-row div {
  padding: 8px 10px;
  background: linear-gradient(180deg, #f8fafc 0%, #e1e9f2 100%);
  border: 1px solid #8794a4;
  border-radius: 2px;
}

.cloth-summary-row span,
.cloth-summary-row strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cloth-summary-row span {
  color: #64748b;
  font-size: 12px;
}

.cloth-summary-row strong {
  color: #075985;
  font-size: 18px;
  line-height: 25px;
}

.first-allocation-summary-row__qtime strong.rough-qtime-bar__qtime-value {
  font-family: var(--vben-font-family-mono);
}

.first-allocation-summary-row__qtime strong.rough-qtime-bar__qtime-value--normal {
  color: #15803d;
}

.first-allocation-summary-row__qtime strong.rough-qtime-bar__qtime-value--warning {
  color: #d97706;
}

.first-allocation-summary-row__qtime strong.rough-qtime-bar__qtime-value--danger {
  color: #dc2626;
}

.first-allocation-summary-row__qtime strong.rough-qtime-bar__qtime-value--empty {
  color: #64748b;
}

.cloth-action-button {
  justify-self: end;
  min-width: 160px;
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

.console-record-table :deep(.rough-record-row--focused > td) {
  background: #ecfeff !important;
  box-shadow: inset 0 1px 0 rgba(14, 116, 144, 0.16), inset 0 -1px 0 rgba(14, 116, 144, 0.16);
}

.modal-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  margin-bottom: 12px;
  background: linear-gradient(180deg, #f8fafc 0%, #e1e9f2 100%);
  border: 1px solid #8794a4;
}

.report-modal-toolbar {
  display: block;
  min-height: 58px;
  padding: 8px 10px 8px;
  margin-bottom: 10px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 24px 100%,
    linear-gradient(180deg, #e8eff7 0%, #d7e2ed 100%);
}

.report-modal-toolbar legend {
  color: #075985;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.report-toolbar-form {
  display: grid;
  grid-template-columns:
    max-content minmax(320px, 2.2fr)
    max-content max-content
    max-content max-content
    max-content max-content;
  gap: 0;
  align-items: stretch;
  width: 100%;
  border: 1px solid #a2adba;
}

.report-toolbar-form label,
.report-toolbar-value {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 8px;
  border-right: 1px solid #a2adba;
}

.report-toolbar-form label {
  justify-content: flex-end;
  min-width: 86px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.report-toolbar-value {
  gap: 8px;
  justify-content: flex-start;
  overflow: visible;
  color: #172033;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 16px;
  font-weight: 950;
  white-space: nowrap;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
}

.report-toolbar-value:last-child {
  border-right: 0;
}

.report-toolbar-value--batch {
  color: #075985;
}

.report-toolbar-value--strong {
  color: #b45309;
}

.report-toolbar-value.warning {
  color: #b91c1c;
  background: linear-gradient(180deg, #fff1f2 0%, #ffe4e6 100%);
}

.report-toolbar-value :deep(.ant-tag) {
  flex: 0 0 auto;
  margin-inline-end: 0;
  font-family: 'Microsoft YaHei', sans-serif;
}

.report-modal-subtitle {
  color: #334155;
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
}

.report-modal-tags {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
  margin-top: 8px;
  border-top: 1px solid #8794a4;
}

.rough-prototype-modal :deep(.ant-modal-body) {
  padding: 12px 16px 10px;
  overflow: hidden;
}

.report-modal-body {
  display: flex;
  flex-direction: column;
  height: clamp(520px, calc(100vh - 180px), 680px);
  min-height: 0;
  overflow: visible;
}

.rough-report-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.rough-report-tabs :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
}

.rough-report-tabs :deep(.ant-tabs-content),
.rough-report-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
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
  padding-right: 4px;
}

.report-modal-body :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}

.report-modal-body :deep(.ant-tabs-tab) {
  padding: 7px 0;
}

.report-modal-body :deep(.ant-table-thead > tr > th),
.report-modal-body :deep(.ant-table-tbody > tr > td) {
  padding: 5px 8px;
}

.report-modal-body--readonly :deep(.ant-input),
.report-modal-body--readonly :deep(.ant-input-number),
.report-modal-body--readonly :deep(.ant-picker),
.report-modal-body--readonly :deep(.ant-radio-wrapper),
.report-modal-body--readonly :deep(.ant-select-selector),
.report-modal-body--readonly :deep(.report-tab-stack button) {
  pointer-events: none;
}

.report-modal-body--readonly :deep(.ant-input),
.report-modal-body--readonly :deep(.ant-input-number),
.report-modal-body--readonly :deep(.ant-picker),
.report-modal-body--readonly :deep(.ant-select-selector) {
  background: #f8fafc;
}

.report-modal-body--time-edit :deep(.ant-input),
.report-modal-body--time-edit :deep(.ant-input-number),
.report-modal-body--time-edit :deep(.ant-radio-wrapper),
.report-modal-body--time-edit :deep(.ant-select-selector),
.report-modal-body--time-edit :deep(.report-tab-stack button) {
  pointer-events: none;
}

.report-modal-body--time-edit :deep(.ant-input),
.report-modal-body--time-edit :deep(.ant-input-number),
.report-modal-body--time-edit :deep(.ant-select-selector) {
  background: #f8fafc;
}

.report-modal-body--time-edit :deep(.report-time-edit-field) {
  pointer-events: auto;
  background: #fff;
}

.rough-check-table {
  border: 1px solid #8794a4;
  border-radius: 0 !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.78),
    0 1px 0 rgba(15, 23, 42, 0.08);
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

.daily-check-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.daily-check-card {
  position: relative;
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 12px;
  min-height: 138px;
  padding: 16px 14px 46px;
  text-align: left;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 22px 100%,
    linear-gradient(180deg, #f8fafc 0%, #d7dee7 100%);
  border: 1px solid #8794a4;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 2px 0 rgba(15, 23, 42, 0.1);
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.daily-check-card:hover {
  border-color: #0284c7;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.9),
    0 4px 14px rgba(2, 132, 199, 0.18);
  transform: translateY(-1px);
}

.daily-check-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58px;
  height: 58px;
  color: #0369a1;
  font-size: 30px;
  background: linear-gradient(180deg, #eef3f8 0%, #cbd5e1 100%);
  border: 1px solid #8794a4;
}

.daily-check-card__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 4px;
}

.daily-check-card__content strong {
  color: #172033;
  font-size: 16px;
  line-height: 22px;
}

.daily-check-card__content em,
.daily-check-card__content span {
  overflow: hidden;
  color: #475569;
  font-size: 12px;
  font-style: normal;
  line-height: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.daily-check-card__tag {
  position: absolute;
  top: 10px;
  right: 10px;
  margin: 0;
}

.daily-check-card__actions {
  position: absolute;
  right: 12px;
  bottom: 10px;
  left: 90px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.record-scan-confirm {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-scan-confirm__hero {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 14px;
  color: #075985;
  background: linear-gradient(180deg, #f8fafc 0%, #e0f2fe 100%);
  border: 1px solid #7dd3fc;
}

.record-scan-confirm__hero svg {
  flex: 0 0 auto;
  font-size: 34px;
}

.record-scan-confirm__hero strong,
.record-scan-confirm__hero span {
  display: block;
}

.record-scan-confirm__hero strong {
  color: #0f172a;
  font-size: 16px;
}

.record-scan-confirm__hero span {
  margin-top: 3px;
  color: #475569;
  font-size: 12px;
}

.record-scan-confirm__expected {
  padding: 8px 10px;
  color: #172033;
  font-weight: 800;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.record-scan-confirm__error {
  color: #dc2626;
  font-size: 13px;
  font-weight: 800;
}

.record-scan-confirm__message {
  color: #047857;
  font-size: 13px;
  font-weight: 800;
}

.report-form-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px 10px;
}

.report-form-grid :deep(.ant-form-item) {
  margin-bottom: 6px;
}

.report-form-grid .report-time-item {
  grid-column: span 1;
  min-width: 0;
}

.report-form-grid .report-time-item :deep(.ant-form-item-control-input-content),
.report-form-grid .report-time-item :deep(.ant-picker) {
  width: 100%;
}

.report-abnormal-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 32px;
  color: #475569;
  font-size: 12px;
}

.report-abnormal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.report-abnormal-table :deep(.ant-input),
.report-abnormal-table :deep(.ant-input-number) {
  width: 100%;
}

.rough-dev-param-fieldset {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.rough-dev-param-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.rough-dev-param-toolbar__meta,
.rough-dev-param-toolbar__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.rough-dev-param-toolbar__meta span {
  padding: 3px 8px;
  color: #075985;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.rough-dev-param-table {
  flex: 0 0 auto;
}

.rough-dev-param-fill-time {
  margin-left: 8px;
  color: #64748b;
}

.rough-dev-param-empty {
  padding: 18px;
  color: #64748b;
  font-size: 13px;
  text-align: center;
  border: 1px dashed #a2adba;
  border-top: 0;
}

.report-modal-body--readonly :deep(.rough-dev-param-toolbar button),
.report-modal-body--readonly :deep(.rough-dev-param-table button) {
  pointer-events: auto;
}

.rough-dev-param-runtime-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #eef3f8;
}

.rough-dev-param-runtime-view__header {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 14px;
  min-height: 56px;
  padding: 8px 12px;
  color: #0f172a;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.rough-dev-param-runtime-view__header strong {
  font-size: 16px;
  font-weight: 900;
}

.rough-dev-param-runtime-view__status {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #475569;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rough-dev-param-runtime-view__body {
  flex: 1 1 auto;
  min-height: 0;
  padding: 8px;
  overflow: auto;
}

.rough-dev-param-runtime-loading {
  padding: 28px;
  color: #64748b;
  font-weight: 800;
  text-align: center;
}

.report-replace-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.report-replace-fieldset {
  min-width: 0;
  margin-bottom: 0;
}

.report-replace-form-grid {
  display: grid;
  grid-template-columns: minmax(120px, 1.3fr) minmax(78px, 0.8fr) minmax(130px, 1.4fr) minmax(150px, 2fr);
  gap: 6px 10px;
}

.report-replace-form-grid :deep(.ant-form-item) {
  margin-bottom: 6px;
}

.report-current-batch-item :deep(.ant-input[disabled]) {
  color: #075985;
  font-weight: 800;
}

.rough-consumable-picker-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  color: #1677ff;
}

.report-source-summary {
  display: grid;
  grid-template-columns: 100px minmax(0, 1.2fr) 86px minmax(0, 1fr) 92px minmax(0, 1.1fr) 92px minmax(0, 1.1fr);
  gap: 0;
  margin-bottom: 8px;
  border: 1px solid #a2adba;
}

.report-source-summary label,
.report-source-summary__value {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 0 8px;
  overflow: hidden;
  min-width: 0;
  border-right: 1px solid #a2adba;
}

.report-source-summary label:nth-child(n + 9),
.report-source-summary__value:nth-child(n + 9) {
  border-top: 1px solid #a2adba;
}

.report-source-summary label {
  justify-content: flex-end;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.report-source-summary__value {
  overflow: hidden;
  color: #172033;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
}

.report-source-summary__value:last-child {
  border-right: 0;
}

.report-source-summary__value--strong {
  color: #b45309;
  font-size: 16px;
}

.report-source-summary__value--control {
  padding: 2px 4px;
}

.report-source-summary__value--control :deep(.ant-picker) {
  height: 28px;
  border-radius: 0;
}

.report-time-item {
  grid-column: span 2;
}

.report-life-alerts {
  display: grid;
  grid-column: span 6;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.report-life-alerts--top {
  flex: 2.2;
  grid-column: auto;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 6px;
  min-width: min(620px, 46vw);
}

.report-title-batch-card {
  flex: 2 1 460px;
  min-width: 380px;
  max-width: none;
}

.report-title-batch-card span {
  flex: 0 0 auto;
  min-width: max-content;
  overflow: visible;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 15px;
  text-align: left;
  text-overflow: clip;
  white-space: nowrap;
}

.report-life-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 34px;
  padding: 5px 9px;
  color: #1f2937;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 20px 100%,
    linear-gradient(180deg, #eef3f8 0%, #dfe7f0 100%);
  border: 1px solid #8794a4;
}

.report-modal-toolbar > .report-life-alert {
  flex: 0 0 auto;
  min-width: max-content;
}

.report-modal-toolbar > .report-life-alert span {
  flex: 0 0 auto;
  min-width: max-content;
  overflow: visible;
  font-size: 16px;
  text-align: left;
  text-overflow: clip;
}

.report-modal-toolbar > .report-life-alert :deep(.ant-tag) {
  flex: 0 0 auto;
  margin-inline-end: 0;
}

.report-modal-toolbar > .report-title-batch-card {
  flex: 1 1 auto;
  min-width: 420px;
}

.report-modal-toolbar > .report-available-alert {
  flex: 0 0 auto;
}

.report-life-alert strong {
  color: #075985;
  font-size: 14px;
  font-weight: 900;
  white-space: nowrap;
}

.report-life-alert span {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #111827;
  font-size: 14px;
  font-weight: 900;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.report-life-alert.warning {
  background: linear-gradient(180deg, #fff1f2 0%, #ffe4e6 100%);
  border-color: #f43f5e;
}

.report-available-alert span {
  color: #b45309;
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

.replace-form-body {
  height: auto;
  overflow: visible;
}

.replace-form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px 10px;
}

.replace-form-grid :deep(.ant-form-item) {
  margin-bottom: 6px;
}

.replace-reason {
  grid-column: span 3;
}

.board-consumable-confirm {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.board-consumable-confirm__summary {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 0;
  overflow: hidden;
  border: 1px solid #c6d0dc;
}

.board-consumable-confirm__summary span,
.board-consumable-confirm__summary strong {
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 5px 8px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.board-consumable-confirm__summary span {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.board-consumable-confirm__summary strong {
  min-width: 0;
  color: #172033;
  font-weight: 800;
  background: #fff;
}

.board-consumable-confirm__summary :deep(.ant-tag) {
  align-self: center;
  width: fit-content;
  max-width: calc(100% - 16px);
  margin: 4px 8px;
  overflow: hidden;
  font-weight: 800;
  text-overflow: ellipsis;
}

.erp-fieldset {
  padding: 8px 10px 2px;
  margin: 0 0 8px;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #8794a4;
}

.erp-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #075985;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.report-required-label {
  color: #dc2626;
  font-weight: 900;
}

.report-loss-sample-emphasis {
  color: #dc2626;
  font-style: normal;
  font-weight: 900;
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

.middle-product-header-input :deep(.ant-input) {
  height: 24px;
  font-weight: 800;
  border-radius: 0;
}

.production-check-head-grid {
  display: grid;
  gap: 0;
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.production-check-head-grid--cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.production-check-head-cell {
  display: grid;
  grid-template-columns: 126px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.production-check-head-cell__label,
.production-check-head-cell__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.production-check-head-cell__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d0dc;
}

.production-check-head-cell__value {
  color: #172033;
  font-weight: 700;
  background: #fff;
}

.production-check-head-cell__input-wrap {
  padding: 2px 4px;
}

.production-check-head-cell__input {
  width: 100%;
}

.production-check-head-cell__input :deep(.ant-input),
.production-check-head-cell__input :deep(.ant-input-number-input) {
  height: 26px;
  font-weight: 800;
}

.record-attachment-panel {
  flex-shrink: 0;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.record-attachment-panel__toolbar {
  display: flex;
  align-items: center;
  min-height: 24px;
}

.record-attachment-panel__title {
  color: #1677ff;
  font-weight: 800;
}

.record-attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin-top: 6px;
}

.record-attachment-item {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  min-height: 28px;
  padding: 2px 6px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
}

.record-attachment-link {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 0;
  color: #1677ff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.record-attachment-time,
.record-attachment-size,
.record-attachment-empty {
  color: #64748b;
  font-size: 12px;
}

.record-attachment-empty {
  display: inline-flex;
  margin-top: 4px;
}

.middle-product-table {
  flex: 1 1 0;
  min-height: 0;
}

.middle-product-record-table {
  flex: 1 1 0;
  min-height: 0;
}

.record-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.25;
}

.record-info-cell strong {
  color: #0f172a;
  font-weight: 900;
}

.record-info-cell span {
  color: #64748b;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
}

.table-action-stack {
  display: flex;
  gap: 6px;
  align-items: center;
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

.middle-product-position {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 8px;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-weight: 900;
}

.middle-product-pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  padding: 4px 2px 0;
  background: #f5f7fa;
  border-top: 1px solid #c6d0dc;
}

.middle-product-signature-row {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 6px;
  overflow: hidden;
  background: #fff;
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.middle-product-signature-cell {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.middle-product-signature-cell__label,
.middle-product-signature-cell__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.middle-product-signature-cell__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d0dc;
}

.middle-product-signature-cell__value {
  color: #172033;
  font-weight: 700;
  background: #fff;
}

.middle-product-signature-cell__input-wrap {
  padding: 2px 4px;
}

.middle-product-signature-cell__input {
  width: 100%;
}

.middle-product-signature-cell__input :deep(.ant-input) {
  height: 26px;
  font-weight: 800;
}

.work-order-complete-confirm p,
.previous-operation-block p,
.first-inspection-detail p {
  margin: 0 0 8px;
  line-height: 1.6;
}

.first-inspection-detail__actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-top: 6px;
}

.full-input,
:deep(.ant-picker) {
  width: 100%;
}

.pp-plan-modal {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #f5f7fa;
}

.pp-plan-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 22px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.pp-plan-toolbar__title {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  min-width: 0;
}

.pp-plan-toolbar__main {
  color: #172033;
  font-size: 16px;
  font-weight: 800;
  white-space: nowrap;
}

.pp-plan-toolbar__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 8px;
}

.toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #334155;
  font-size: 12px;
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
  padding: 8px 10px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #8794a4;
}

.pp-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #075985;
  font-size: 14px;
  font-weight: 800;
}

.pp-form-grid {
  display: grid;
  gap: 8px;
}

.pass-work-form-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.head-item {
  display: flex;
  min-width: 0;
  min-height: 34px;
  border: 1px solid #a2adba;
}

.head-item__label {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  width: 96px;
  padding: 0 8px;
  color: #334155;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #a2adba;
}

.head-item__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 0 8px;
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #e1e9f2 100%);
  border: 1px solid #8794a4;
}

.pp-panel__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  color: #075985;
  font-weight: 800;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.pp-table-wrap,
.pass-work-detail-wrap {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.pass-work-detail-table-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pp-grid {
  width: 100%;
  min-width: 1370px;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid th,
.pp-grid td {
  min-height: 28px;
  padding: 3px 5px;
  color: #172033;
  vertical-align: middle;
  border: 1px solid #c6d0dc;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #263445;
  font-weight: 800;
  text-align: center;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.pp-grid tbody tr:hover {
  background: #e6f4ff;
}

.pp-grid td[rowspan] {
  text-align: center;
  vertical-align: middle;
  background: #eef3f8;
}

.pp-empty-cell {
  color: #94a3b8;
}

.pp-radio-group {
  white-space: nowrap;
}

.daily-check-result-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.rough-detail-grid :deep(.ant-input) {
  min-width: 180px;
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
  flex: 0 0 auto;
  position: sticky;
  bottom: 0;
  z-index: 5;
  padding: 8px 4px 2px;
  margin-top: 8px;
  background: #f5f7fa;
  box-shadow: 0 -4px 10px rgba(15, 23, 42, 0.08);
}
</style>
