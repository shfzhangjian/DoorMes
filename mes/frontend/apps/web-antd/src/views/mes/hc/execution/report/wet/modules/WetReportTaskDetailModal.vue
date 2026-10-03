<script lang="ts" setup>
import {
  computed,
  h,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';
import {
  Button,
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal as AModal,
  Radio,
  RadioGroup,
  Select,
  Table as ATable,
  Tabs,
  TabPane,
  Tag,
  Tooltip,
  Upload,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { uploadFile } from '#/api/infra/file';
import { getEquipment, getEquipmentPage } from '#/api/mes/hc/equipment';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import {
  exportProcessFormRecordLayout,
  importProcessFormRecordLayout,
} from '#/api/mes/hc/processform';
import {
  applyWetFai,
  confirmWetReport,
  confirmWetPassWork,
  getWetAbnormalPositionList,
  getWetFaiSummary,
  getWetPassWorkList,
  getWetReportTimeLogs,
  getWetReportTaskList,
  reviseWetReportStatisticsData,
  saveWetPassWork,
  startWetReport,
  switchWetEquipment,
  updateWetReportTime,
} from '#/api/mes/hc/execution/wet-report';
import type { MesHcWetReportApi } from '#/api/mes/hc/execution/wet-report';
import { getGuideClothRuntime } from '#/api/mes/hc/guideclothrecord';
import type { MesHcGuideClothRecordApi } from '#/api/mes/hc/guideclothrecord';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';
import { updateToolingConsumableLedgerUsageStatus } from '#/api/mes/hc/tooling-consumable-ledger';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import ConsumableLedgerSwitchModal from '#/views/mes/hc/base/tooling-consumable-ledger/components/ConsumableLedgerSwitchModal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import FaiDetailModal from '#/views/mes/quality/fai/modules/detail-modal.vue';
import QmsSampleAbnormalLockGuard from '#/views/mes/quality/sample-abnormal-recheck/components/QmsSampleAbnormalLockGuard.vue';
import FormulaReportPrintPreviewModal from '../../formula/modules/FormulaReportPrintPreviewModal.vue';
import {
  buildInspectionTransferTicketPayload,
  buildTransferTicketFields,
  buildTransferTicketQrValue,
  formatTransferTicketMetric,
} from '../../shared/workOrderTicketPrint';
import { applyPrintFieldTemplate } from '../../shared/printFieldTemplate';
import WetFirstInspectionPrintPreviewModal from './WetFirstInspectionPrintPreviewModal.vue';
import {
  buildWetDefaultSheetGroups,
  buildWetPresetRow,
  generateSemiDetails,
  resolveSemiGeneratedLength,
  resolveSemiRowStep,
  resolveWetHeaderFields,
  resolveWetHeaderLayout,
  safeParseJson,
} from './wetDynamicSheetRegistry';
import type {
  WetSheetAttachment,
  WetSheetDetail,
  WetSheetRow,
} from './wetDynamicSheetRegistry';
import { readWetProcessCheckFromLocalCollector } from './wetLocalCollector';
import type {
  WetLocalCollectorFillItem,
  WetLocalCollectorFillResult,
  WetLocalCollectorFillSource,
} from './wetLocalCollector';

const emit = defineEmits(['refresh', 'taskChange']);
const BATCHING_PRINT_AGENT_URL = 'http://127.0.0.1:17820';
const GUIDE_CLOTH_MAX_USE_COUNT = 28;
const GUIDE_CLOTH_MONTH_REMIND_BEFORE_DAYS = 3;
const MAX_PORE_PHOTO_COUNT = 10;
const LOCAL_COLLECTOR_WET_PROCESS_CHECK_FORM_CODE = 'WET_PROCESS_CHECK';

type MainTabKey =
  | 'prepare'
  | 'pore-self-check'
  | 'production-check'
  | 'semi-finished'
  | 'first-inspection'
  | 'report-info'
  | 'abnormal-position';
type RecordMode = 'confirm' | 'edit' | 'view';
type RecordCategory = 'prepare' | 'production-check' | 'semi-finished';
type PoreSelfCheckDialogMode = 'create' | 'view';
type AbnormalPositionOption = '' | 'DETAIL' | 'NONE';
type AbnormalPositionRow = MesHcWetReportApi.AbnormalPositionItem & {
  clientKey: string;
};
type PoreSelfCheckRecord = {
  clientKey: string;
  inspector: string;
  /** 兼容历史单图记录；新数据统一写入 photos。 */
  photo?: string;
  photos: string[];
  remark: string;
  selfCheckResult: string;
  selfCheckTime: string;
};
type WetHeaderFieldType = 'datetime' | 'input' | 'radio' | 'readonly';
type WetHeaderFieldKey =
  | 'batchNo'
  | 'confirmer'
  | 'confirmerTime'
  | 'finalResult'
  | 'generatedLength'
  | 'inOvenTime'
  | 'inSolidifyTime'
  | 'inWashTime'
  | 'machine'
  | 'materialCode'
  | 'modelCode'
  | 'outOvenTime'
  | 'outSolidifyTime'
  | 'outWashTime'
  | 'poreDevelopment'
  | 'productionDate'
  | 'recorder'
  | 'recorderTime'
  | 'semiWidth'
  | 'startTime'
  | 'endTime';

type WetPassWorkRow = MesHcWetReportApi.PassWorkRow;
type WetPassWorkCategory = 'prepare' | 'production-check' | 'semi-finished';
type CollectorFillScope = {
  itemCategory: string;
  stepNode: string;
};
type WetSheetSchemaMeta = {
  allowAttachment?: boolean;
  headerEditableFields?: string[];
  headerLabels?: Record<string, string>;
  modelCode?: string;
  modelPrefix?: string;
  presetTemplate?: string;
  semiType?: string;
  semiWidthLabel?: string;
  sourceExcel?: string;
  sourceSheet?: string;
  thicknessLabels?: string[];
  version?: string;
  wetCategory?: string;
};

const WET_PRODUCTION_CHECK_HEADER_LABELS: Partial<
  Record<WetHeaderFieldKey, string>
> = {
  batchNo: '母批批号',
  endTime: '投料结束时间',
  inOvenTime: '入烘箱时间',
  inSolidifyTime: '入凝固槽时间',
  inWashTime: '入水洗槽时间',
  machine: '机台编号',
  materialCode: '产品料号',
  modelCode: '产品型号',
  outOvenTime: '出烘箱时间',
  outSolidifyTime: '出凝固槽时间',
  outWashTime: '出水洗槽时间',
  startTime: '投料开始时间',
};
const WET_PRODUCTION_CHECK_TIME_HEADER_FIELDS = new Set<WetHeaderFieldKey>([
  'endTime',
  'inOvenTime',
  'inSolidifyTime',
  'inWashTime',
  'outOvenTime',
  'outSolidifyTime',
  'outWashTime',
  'startTime',
]);

const task = ref<any>(null);
const batchNoLabel = computed(() => '母批批号');
const wetSampleLockGuardRef =
  ref<InstanceType<typeof QmsSampleAbnormalLockGuard>>();
const wetSampleLockObjectNo = computed(
  () =>
    task.value?.productionBatchNo ||
    task.value?.batchNo ||
    task.value?.parentProductionBatchNo ||
    '',
);
const activeMainTab = ref<MainTabKey>('prepare');
const viewMode = ref<'booking' | 'tabs'>('tabs');
const pendingAction = ref<
  'CONFIRM_REPORT' | 'FINISH' | 'START' | 'SUBMIT_BOOKING' | null
>(null);
const authVisible = ref(false);
const recordSaveAuthVisible = ref(false);
const recordConfirmAuthVisible = ref(false);
const equipmentOptions = ref<any[]>([]);
const planDetail = ref<MesHcPlanOrderApi.PlanOrder | null>(null);
const deviceSwitchVisible = ref(false);
const deviceSwitchSubmitting = ref(false);
const deviceSwitchForm = ref({
  equipmentId: undefined as number | undefined,
});
const timeCorrectionVisible = ref(false);
const timeCorrectionSubmitting = ref(false);
const timeCorrectionForm = ref({
  endTime: '',
  reportDate: '',
  startTime: '',
});
const timeCorrectionFormRef = ref<HTMLElement | null>(null);
const timeLogVisible = ref(false);
const timeLogLoading = ref(false);
const timeLogs = ref<MesHcWetReportApi.TimeLogItem[]>([]);
const timeLogColumns = [
  {
    dataIndex: 'operatorName',
    key: 'operatorName',
    title: '修改人',
    width: 120,
  },
  { dataIndex: 'changeTime', key: 'changeTime', title: '修改时间', width: 170 },
  { key: 'reportDate', title: '报工日期', width: 190 },
  { key: 'startTime', title: '开始时间', width: 320 },
  { key: 'endTime', title: '结束时间', width: 320 },
];
const quantityRevisionVisible = ref(false);
const quantityRevisionSubmitting = ref(false);
const quantityRevisionForm = ref({
  printLossLength: undefined as number | undefined,
  reason: '',
  receiveLength: undefined as number | undefined,
});
const equipmentStatusCard = ref<any>(null);
const guideClothRuntime = ref<MesHcGuideClothRecordApi.RuntimeInfo | null>(
  null,
);
const guideClothBaseNextUseCount = ref(1);
const qtimeNowTimestamp = ref(Date.now());
let qtimeRefreshTimer: ReturnType<typeof window.setInterval> | undefined;

const reportForm = ref({
  productionDate: '',
  startTime: '',
  endTime: '',
  equipmentId: undefined as number | undefined,
  equipmentCode: '',
  equipmentName: '',
  inWashTime: '',
  outWashTime: '',
  receiveLength: undefined as number | undefined,
  napSampleLength: undefined as number | undefined,
  printSampleLength: undefined as number | undefined,
  petModel: '',
  petBatchNo: '',
  guideClothBatchNo: '',
  guideClothReplaceTime: '',
  guideClothUseCount: undefined as number | undefined,
  guideClothCurrentUsedLength: undefined as number | undefined,
  guideClothUsedLength: undefined as number | undefined,
  guideClothLimitLength: undefined as number | undefined,
  guideClothWarningFlag: 0,
  guideClothWarningText: '',
  guideClothChanged: 'N',
  guideClothChangeReason: '',
  inSolidifyTime: '',
  outSolidifyTime: '',
  inOvenTime: '',
  outOvenTime: '',
  remark: '',
  recorderName: '',
  recorderTime: '',
  confirmerName: '',
  confirmerTime: '',
});
type WetSideConsumableType = 'GUIDE_CLOTH' | 'PET';
type WetConsumableLedgerSelection = MesHcToolingConsumableLedgerApi.Ledger & {
  nextUsageStatus?: 'ACTIVE' | 'USED_UP';
};
type WetPendingConsumableUsageStatus = {
  batchNo?: string;
  ledgerId: number;
  nextUsageStatus?: 'ACTIVE' | 'USED_UP';
};
const wetConsumableSwitchModalRef =
  ref<InstanceType<typeof ConsumableLedgerSwitchModal>>();
const wetConsumableSwitchType = ref<WetSideConsumableType>('PET');
const wetPendingConsumableUsageStatuses = ref<
  Partial<Record<WetSideConsumableType, WetPendingConsumableUsageStatus>>
>({});

function openWetConsumableLedger(consumableType: WetSideConsumableType) {
  wetConsumableSwitchType.value = consumableType;
  wetConsumableSwitchModalRef.value?.open({
    consumableType,
    processCode: 'WET',
    title: consumableType === 'PET' ? '湿法PET边库台账' : '湿法导布边库台账',
  });
}

function clearWetConsumableSelection(consumableType: WetSideConsumableType) {
  delete wetPendingConsumableUsageStatuses.value[consumableType];
}

function rememberWetConsumableSelection(
  consumableType: WetSideConsumableType,
  row: WetConsumableLedgerSelection,
) {
  if (!row.id) {
    clearWetConsumableSelection(consumableType);
    return;
  }
  wetPendingConsumableUsageStatuses.value[consumableType] = {
    batchNo: row.batchNo,
    ledgerId: row.id,
    nextUsageStatus: row.nextUsageStatus,
  };
}

async function updateWetConsumableUsageStatusesAfterBooking() {
  const pendingEntries = Object.entries(
    wetPendingConsumableUsageStatuses.value,
  ).filter(([, item]) => item?.nextUsageStatus === 'USED_UP') as Array<
    [WetSideConsumableType, WetPendingConsumableUsageStatus]
  >;
  if (pendingEntries.length === 0) return;
  for (const [consumableType, item] of pendingEntries) {
    await updateToolingConsumableLedgerUsageStatus(item.ledgerId, 'USED_UP');
    clearWetConsumableSelection(consumableType);
  }
}

function handleWetConsumableSelected(row: WetConsumableLedgerSelection) {
  rememberWetConsumableSelection(wetConsumableSwitchType.value, row);
  if (wetConsumableSwitchType.value === 'PET') {
    reportForm.value.petBatchNo = row.batchNo || '';
    if (row.model) {
      reportForm.value.petModel = row.model;
    }
    return;
  }
  reportForm.value.guideClothBatchNo = row.batchNo || '';
  reportForm.value.guideClothChanged = 'Y';
  applyGuideClothUseCountByFlag(true);
}
type RuntimeDateTimeValue = number | string;
type BookingDateTimeField =
  | 'confirmerTime'
  | 'endTime'
  | 'productionDate'
  | 'recorderTime'
  | 'startTime';
type BookingDateTimeFieldType = 'date' | 'datetime';
const bookingDateTimeInputCache = ref<
  Partial<Record<BookingDateTimeField, string>>
>({});
const bookingTimeFormRef = ref<HTMLElement | null>(null);

const abnormalPositionVisible = ref(false);

async function ensureWetSampleAbnormalUnlocked() {
  return (await wetSampleLockGuardRef.value?.ensureUnlocked()) ?? true;
}
const abnormalPositionOption = ref<AbnormalPositionOption>('');
const abnormalPositionRows = ref<AbnormalPositionRow[]>([]);

const workPrepareRows = ref<WetSheetRow[]>([]);
const productionCheckRows = ref<WetSheetRow[]>([]);
const semiFinishedRows = ref<WetSheetRow[]>([]);
const productionCheckFormNameFilter = ref('');
const semiFinishedFormNameFilter = ref('');
const poreSelfCheckRows = ref<PoreSelfCheckRecord[]>([]);
const poreSelfCheckMeta = ref<Partial<WetPassWorkRow>>({
  formCode: 'WET_PORE_SELF_CHECK',
  name: '泡孔自检',
  result: '未记录',
  status: 'PENDING',
  timing: '报工前',
});
type FirstInspectionRecord = {
  faiId?: number;
  faiNo: string;
  applyTime: string;
  standardText: string;
  returnTime: string;
  inspectionDesc: string;
  inspectTime: string;
  inspector: string;
  judgment: string;
  napSampleLength?: number;
  productBatchNo?: string;
  result: string;
  status: string;
  isCurrent: boolean;
};

const createEmptyFirstInspection = (): FirstInspectionRecord => ({
  faiId: undefined as number | undefined,
  faiNo: '',
  applyTime: '',
  standardText: '',
  returnTime: '',
  inspectionDesc: '',
  inspectTime: '',
  inspector: '',
  judgment: 'PENDING',
  napSampleLength: undefined,
  productBatchNo: '',
  result: '',
  status: 'PENDING',
  isCurrent: true,
});
const firstInspection = ref<FirstInspectionRecord>(
  createEmptyFirstInspection(),
);
const firstInspectionRecords = ref<FirstInspectionRecord[]>([
  createEmptyFirstInspection(),
]);
const wetFirstInspectionRecordId = ref<number>();
const firstInspectionApplying = ref(false);
const firstInspectionRefreshing = ref(false);
const WET_FIRST_INSPECTION_FORM_CODE = 'WET_FIRST_INSPECTION';
const WET_PORE_SELF_CHECK_FORM_CODE = 'WET_PORE_SELF_CHECK';
type FirstInspectionMatchMode = 'PRODUCT_MODEL_PROCESS';
let firstInspectionRefreshTimer:
  | ReturnType<typeof window.setInterval>
  | undefined;
let wetDetailModalOpen = false;

const recordDialogVisible = ref(false);
const recordDialogCategory = ref<RecordCategory>('prepare');
const recordDialogMode = ref<RecordMode>('view');
const currentRecord = ref<WetSheetRow | null>(null);
const poreSelfCheckDialogVisible = ref(false);
const poreSelfCheckDialogMode = ref<PoreSelfCheckDialogMode>('create');
const poreSelfCheckSaving = ref(false);
const porePhotoUploading = ref(false);
const porePhotoUploadPendingCount = ref(0);
const recordExcelLoading = ref(false);
const recordExcelImportInputRef = ref<HTMLInputElement>();
const porePhotoPreviewVisible = ref(false);
const porePhotoPreviewUrls = ref<string[]>([]);
const porePhotoPreviewIndex = ref(0);
const modbusCollecting = ref(false);
const collectorScopeDialogVisible = ref(false);
const collectorScopeForm = ref<CollectorFillScope>({
  itemCategory: '',
  stepNode: '',
});
const poreSelfCheckForm = ref<PoreSelfCheckRecord>({
  clientKey: '',
  inspector: '',
  photos: [],
  remark: '',
  selfCheckResult: 'OK',
  selfCheckTime: '',
});
const sheetCellRefs = new Map<string, HTMLElement>();
const actionPanelExpanded = ref(false);
const recordActionForm = ref({
  result: 'OK',
  inspectionResult: 'OK',
  formRemark: '',
  recorder: '',
  recorderTime: '',
  confirmer: '',
  confirmerTime: '',
  confirmRemark: '',
});

function normalizeDate(value?: string) {
  if (!value) return '';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD') : String(value);
}

function normalizeDateTime(value?: string) {
  if (!value) return '';
  if (isTimeOnlyText(value)) return String(value).trim();
  const date = dayjs(value);
  if (isPlaceholderDateTime(date)) return date.format('HH:mm:ss');
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

function normalizeValidBusinessDateTime(value?: RuntimeDateTimeValue) {
  if (!value || isTimeOnlyText(value)) return '';
  const date = dayjs(value);
  return date.isValid() && date.year() >= 2000
    ? date.format('YYYY-MM-DD HH:mm:ss')
    : '';
}

function isTimeOnlyText(value?: RuntimeDateTimeValue) {
  if (!value) return false;
  return /^\d{2}:\d{2}(:\d{2})?$/.test(String(value).trim());
}

function isPlaceholderDateTime(date: dayjs.Dayjs) {
  return date.isValid() && (date.year() === 1970 || date.year() === 1900);
}

function shouldResetRuntimeDateTime(value?: string) {
  if (!value) return true;
  if (isTimeOnlyText(value)) return true;
  const date = dayjs(value);
  return !date.isValid() || isPlaceholderDateTime(date);
}

function normalizeBizDateTime(
  productionDate?: string,
  value?: RuntimeDateTimeValue,
) {
  if (!value) return '';
  const production = productionDate ? dayjs(productionDate) : null;
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

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function reportDateFromDateTime(
  value?: string,
  fallback = dayjs().format('YYYY-MM-DD'),
) {
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD') : fallback;
}

function normalizeReportDateTimeText(
  value?: RuntimeDateTimeValue,
  fallbackDate?: string,
) {
  const text = normalizeBizDateTime(fallbackDate, value);
  if (!text) return '';
  const date = dayjs(text);
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : '';
}

function normalizeBookingDateTimeValue(
  value: unknown,
  type: BookingDateTimeFieldType,
  fallbackDate?: string,
) {
  const text = String(value ?? '').trim();
  if (!text) return '';
  if (type === 'date') {
    const date = dayjs(text);
    return date.isValid() ? date.format('YYYY-MM-DD') : '';
  }
  return normalizeReportDateTimeText(text, fallbackDate);
}

function applyBookingDateTimeValue(
  key: BookingDateTimeField,
  type: BookingDateTimeFieldType,
  value: unknown,
) {
  const normalized = normalizeBookingDateTimeValue(
    value,
    type,
    reportForm.value.productionDate,
  );
  if (normalized) {
    (reportForm.value as Record<string, any>)[key] = normalized;
  }
  return normalized;
}

function handleBookingDateTimeInput(
  key: BookingDateTimeField,
  type: BookingDateTimeFieldType,
  event: Event,
) {
  const text = String(
    (event.target as HTMLInputElement | null)?.value || '',
  ).trim();
  bookingDateTimeInputCache.value[key] = text;
  if (!text) {
    (reportForm.value as Record<string, any>)[key] = '';
    return;
  }
  applyBookingDateTimeValue(key, type, text);
}

function handleBookingDateTimeChange(
  key: BookingDateTimeField,
  type: BookingDateTimeFieldType,
  value: unknown,
  dateString?: string,
) {
  const text = String(dateString || value || '').trim();
  bookingDateTimeInputCache.value[key] = text;
  if (!text) {
    (reportForm.value as Record<string, any>)[key] = '';
    return;
  }
  applyBookingDateTimeValue(key, type, text);
}

function displayLogValue(value?: null | number | string) {
  const text = String(value ?? '').trim();
  return text || '-';
}

function syncBookingDateTimeFieldsFromDom() {
  const root = bookingTimeFormRef.value;
  if (!root) return;
  const fields = [
    { key: 'productionDate', type: 'date' },
    { key: 'startTime', type: 'datetime' },
    { key: 'endTime', type: 'datetime' },
    { key: 'recorderTime', type: 'datetime' },
    { key: 'confirmerTime', type: 'datetime' },
  ] as const;
  fields.forEach(({ key, type }) => {
    const input = root.querySelector(
      `[data-wet-report-time-field="${key}"] input`,
    ) as HTMLInputElement | null;
    const cachedText = String(
      bookingDateTimeInputCache.value[key] || '',
    ).trim();
    const inputText = String(input?.value || '').trim();
    const normalizedFromInput = normalizeBookingDateTimeValue(
      inputText,
      type,
      reportForm.value.productionDate,
    );
    const normalized =
      normalizedFromInput ||
      normalizeBookingDateTimeValue(
        cachedText,
        type,
        reportForm.value.productionDate,
      );
    if (normalized) {
      (reportForm.value as Record<string, any>)[key] = normalized;
      bookingDateTimeInputCache.value[key] = normalized;
    }
  });
}

function syncTimeCorrectionFieldsFromDom() {
  const root = timeCorrectionFormRef.value;
  if (!root) return;
  const reportDateInput = root.querySelector(
    '[data-wet-time-correction-field="reportDate"] input',
  ) as HTMLInputElement | null;
  const reportDate =
    normalizeBookingDateTimeValue(reportDateInput?.value, 'date') ||
    normalizeBookingDateTimeValue(timeCorrectionForm.value.reportDate, 'date');
  if (reportDate) timeCorrectionForm.value.reportDate = reportDate;
  (['startTime', 'endTime'] as const).forEach((key) => {
    const input = root.querySelector(
      `[data-wet-time-correction-field="${key}"] input`,
    ) as HTMLInputElement | null;
    const normalized =
      normalizeBookingDateTimeValue(input?.value, 'datetime', reportDate) ||
      normalizeBookingDateTimeValue(
        timeCorrectionForm.value[key],
        'datetime',
        reportDate,
      );
    if (normalized) timeCorrectionForm.value[key] = normalized;
  });
}

function ensureBookingReportDateTimes() {
  const productionDate =
    normalizeBookingDateTimeValue(
      bookingDateTimeInputCache.value.productionDate ||
        reportForm.value.productionDate,
      'date',
    ) ||
    reportDateFromDateTime(
      reportForm.value.startTime || reportForm.value.endTime,
    );
  if (!productionDate) {
    message.warning('请填写有效的生产日期');
    return false;
  }
  reportForm.value.productionDate = productionDate;
  const startTime = normalizeBookingDateTimeValue(
    bookingDateTimeInputCache.value.startTime || reportForm.value.startTime,
    'datetime',
    productionDate,
  );
  if (!startTime) {
    message.warning('请填写有效的开始时间，格式：yyyy-MM-dd HH:mm:ss');
    return false;
  }
  const endTime = normalizeBookingDateTimeValue(
    bookingDateTimeInputCache.value.endTime || reportForm.value.endTime,
    'datetime',
    productionDate,
  );
  if (!endTime) {
    message.warning('请填写有效的完工时间，格式：yyyy-MM-dd HH:mm:ss');
    return false;
  }
  if (dayjs(endTime).isBefore(dayjs(startTime))) {
    message.warning('完工时间不能早于开始时间');
    return false;
  }
  const recorderTime = normalizeBookingDateTimeValue(
    bookingDateTimeInputCache.value.recorderTime ||
      reportForm.value.recorderTime,
    'datetime',
    productionDate,
  );
  if (recorderTime && dayjs(endTime).isAfter(dayjs(recorderTime))) {
    message.warning('完工时间不能晚于最终提交记录时间');
    return false;
  }
  reportForm.value.startTime = startTime;
  reportForm.value.endTime = endTime;
  if (recorderTime) reportForm.value.recorderTime = recorderTime;
  bookingDateTimeInputCache.value.startTime = startTime;
  bookingDateTimeInputCache.value.endTime = endTime;
  if (recorderTime) bookingDateTimeInputCache.value.recorderTime = recorderTime;
  bookingDateTimeInputCache.value.productionDate = productionDate;
  return true;
}

const WET_PROCESS_DATE_TIME_FIELDS = [
  { key: 'inWashTime', label: '入水洗槽时间' },
  { key: 'outWashTime', label: '出水洗槽时间' },
  { key: 'inSolidifyTime', label: '入凝固槽时间' },
  { key: 'outSolidifyTime', label: '出凝固槽时间' },
  { key: 'inOvenTime', label: '入烘箱时间' },
  { key: 'outOvenTime', label: '出烘箱时间' },
] as const;

function normalizeWetProcessDateTimesForSubmit() {
  const form = reportForm.value as Record<string, any>;
  for (const { key, label } of WET_PROCESS_DATE_TIME_FIELDS) {
    const rawValue = form[key] as RuntimeDateTimeValue | undefined;
    if (rawValue === undefined || String(rawValue).trim() === '') continue;
    const normalized = normalizeReportDateTimeText(
      rawValue,
      reportForm.value.productionDate,
    );
    if (!normalized) {
      message.warning(`${label}格式无效，请重新选择日期和时间`);
      return false;
    }
    form[key] = normalized;
  }
  return true;
}

function parseQtimeDateTime(value?: string, productionDate?: string) {
  const normalized = normalizeBizDateTime(productionDate, value);
  if (!normalized || isTimeOnlyText(normalized)) return null;
  const date = dayjs(normalized);
  return date.isValid() ? date : null;
}

function formatQtimeDuration(totalMinutes?: number | null) {
  if (totalMinutes === null || totalMinutes === undefined) return '-';
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

function resolveQtimeLevelClass(totalMinutes?: number | null) {
  if (totalMinutes === null || totalMinutes === undefined) {
    return 'wet-prev-op-bar__qtime-value--empty';
  }
  if (totalMinutes >= 1440) return 'wet-prev-op-bar__qtime-value--danger';
  if (totalMinutes >= 60) return 'wet-prev-op-bar__qtime-value--warning';
  return 'wet-prev-op-bar__qtime-value--normal';
}

function resolveBackendQtimeLevelClass(
  qtime?: MesHcWetReportApi.QtimeInfo | null,
) {
  if (!qtime || qtime.status === 'MISSING_SOURCE_TIME') {
    return 'wet-prev-op-bar__qtime-value--empty';
  }
  if (isBackendQtimeTimeout(qtime)) {
    return 'wet-prev-op-bar__qtime-value--danger';
  }
  if (qtime.status === 'NORMAL') {
    return 'wet-prev-op-bar__qtime-value--normal';
  }
  return 'wet-prev-op-bar__qtime-value--warning';
}

function backendQtimeElapsedMinutes(
  qtime?: MesHcWetReportApi.QtimeInfo | null,
) {
  if (!qtime?.sourceEndTime) return qtime?.elapsedMinutes;
  const sourceTime = parseQtimeDateTime(qtime.sourceEndTime);
  if (!sourceTime) return qtime.elapsedMinutes;
  const targetTime = qtime.targetStartTime
    ? parseQtimeDateTime(qtime.targetStartTime)
    : dayjs(qtimeNowTimestamp.value);
  return targetTime
    ? targetTime.diff(sourceTime, 'minute')
    : qtime.elapsedMinutes;
}

function isBackendQtimeTimeout(qtime?: MesHcWetReportApi.QtimeInfo | null) {
  const elapsedMinutes = backendQtimeElapsedMinutes(qtime);
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

function backendQtimeDurationText(qtime?: MesHcWetReportApi.QtimeInfo | null) {
  if (!qtime) return '-';
  const elapsedMinutes = backendQtimeElapsedMinutes(qtime);
  if (
    elapsedMinutes !== undefined &&
    elapsedMinutes !== null &&
    qtime.standardMinutes !== undefined &&
    qtime.standardMinutes !== null
  ) {
    const stateText = qtime.targetStarted ? '实际湿法开工' : '当前湿法开工';
    const standardText = formatQtimeDuration(qtime.standardMinutes);
    const elapsedText = formatQtimeDuration(elapsedMinutes);
    return isBackendQtimeTimeout(qtime)
      ? `${stateText}间隔 ${elapsedText}，已超出额定 ${standardText}`
      : `${stateText}间隔 ${elapsedText}，额定 ${standardText} 内`;
  }
  if (qtime.message) return qtime.message;
  if (qtime.status === 'MISSING_SOURCE_TIME') return '前序未完工';
  if (qtime.status === 'NO_RULE') return '未配置额定 QTIME';
  return '-';
}

onMounted(() => {
  qtimeRefreshTimer = window.setInterval(() => {
    qtimeNowTimestamp.value = Date.now();
  }, 60 * 1000);
});

onBeforeUnmount(() => {
  if (qtimeRefreshTimer) {
    window.clearInterval(qtimeRefreshTimer);
  }
  stopFirstInspectionAutoRefresh();
});

function parseWetExtraJson(extraJson?: string | null) {
  return (
    safeParseJson<{
      abnormalPositionOption?: string;
      guideClothBatchNo?: string;
      guideClothChangeReason?: string;
      guideClothChanged?: string;
      guideClothCurrentUsedLength?: number;
      guideClothLimitLength?: number;
      guideClothReplaceTime?: string;
      guideClothUseCount?: number;
      guideClothUsedLength?: number;
      guideClothWarningFlag?: number;
      inOvenTime?: RuntimeDateTimeValue;
      inSolidifyTime?: RuntimeDateTimeValue;
      inWashTime?: RuntimeDateTimeValue;
      napSampleLength?: number;
      outOvenTime?: RuntimeDateTimeValue;
      outSolidifyTime?: RuntimeDateTimeValue;
      outWashTime?: RuntimeDateTimeValue;
      petBatchNo?: string;
      petModel?: string;
      printLossLength?: number;
      receiveLength?: number;
    }>(extraJson) || {}
  );
}

function applyGuideClothUseCountByFlag(force = false) {
  if (!force && task.value?.status === 'COMPLETED') return;
  reportForm.value.guideClothUseCount =
    reportForm.value.guideClothChanged === 'Y'
      ? 1
      : guideClothBaseNextUseCount.value || 1;
}

function toNumber(value: any, defaultValue = 0) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : defaultValue;
}

function formatMeter(value: any) {
  const numberValue = toNumber(value);
  return Number.isInteger(numberValue)
    ? String(numberValue)
    : numberValue.toFixed(3).replace(/\.?0+$/, '');
}

function normalizeOptionalNumber(value: any) {
  if (value === undefined || value === null || value === '') return undefined;
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function formatOptionalMeter(value: any) {
  const numberValue = normalizeOptionalNumber(value);
  return numberValue === undefined ? '-' : formatMeter(numberValue);
}

function buildClientKey(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function normalizePorePhotoUrls(...values: unknown[]): string[] {
  const urls: string[] = [];
  values.forEach((value) => {
    if (Array.isArray(value)) {
      urls.push(...normalizePorePhotoUrls(...value));
      return;
    }
    const url = String(value || '').trim();
    if (url) urls.push(url);
  });
  return [...new Set(urls)].slice(0, MAX_PORE_PHOTO_COUNT);
}

function createEmptyPoreSelfCheckRecord(
  source: Partial<PoreSelfCheckRecord> = {},
): PoreSelfCheckRecord {
  return {
    clientKey: source.clientKey || buildClientKey('pore-self-check'),
    inspector: source.inspector || '',
    photos: normalizePorePhotoUrls(source.photos, source.photo),
    remark: source.remark || '',
    selfCheckResult: source.selfCheckResult || 'OK',
    selfCheckTime: source.selfCheckTime || '',
  };
}

function normalizePoreSelfCheckRows(headerDataJson?: string | null) {
  const header = safeParseJson<{ records?: Partial<PoreSelfCheckRecord>[] }>(
    headerDataJson,
  );
  const records = Array.isArray(header?.records) ? header.records : [];
  return records
    .filter(
      (item) =>
        item?.selfCheckTime ||
        item?.selfCheckResult ||
        item?.inspector ||
        item?.remark ||
        item?.photo ||
        (Array.isArray(item?.photos) && item.photos.length > 0),
    )
    .map((item) => createEmptyPoreSelfCheckRecord(item));
}

function resolvePoreSelfCheckStartTimeText(
  rows: PoreSelfCheckRecord[] = poreSelfCheckRows.value,
) {
  const firstRecord = rows.find((item) => item.selfCheckTime);
  return firstRecord
    ? normalizeBizDateTime(
        reportForm.value.productionDate || task.value?.productionDate,
        firstRecord.selfCheckTime,
      ) || firstRecord.selfCheckTime
    : '';
}

function applyPoreSelfCheckStartTime(
  rows: PoreSelfCheckRecord[] = poreSelfCheckRows.value,
  emitChange = false,
) {
  const startTime = resolvePoreSelfCheckStartTimeText(rows);
  if (!startTime) return '';
  reportForm.value.startTime = startTime;
  if (task.value) {
    task.value = {
      ...task.value,
      startTime,
    };
    if (emitChange) {
      emit('taskChange', { ...task.value });
    }
  }
  return startTime;
}

function applyPoreSelfCheckRow(source?: WetPassWorkRow) {
  poreSelfCheckMeta.value = source || {
    formCode: WET_PORE_SELF_CHECK_FORM_CODE,
    name: '泡孔自检',
    result: '未记录',
    status: 'PENDING',
    timing: '报工前',
  };
  poreSelfCheckRows.value = normalizePoreSelfCheckRows(
    source?.headerDataJson || source?.presetHeaderDataJson,
  );
  applyPoreSelfCheckStartTime(poreSelfCheckRows.value);
}

function hasPoreSelfCheckRecord() {
  return poreSelfCheckRows.value.length > 0;
}

function resolveWetStartTimeText() {
  return (
    resolvePoreSelfCheckStartTimeText() ||
    normalizeBizDateTime(
      reportForm.value.productionDate || task.value?.productionDate,
      reportForm.value.startTime || task.value?.startTime,
    ) ||
    buildNowText()
  );
}

function resolvePoreSamplingRecorderTimeText() {
  return resolvePoreSelfCheckStartTimeText() || buildNowText();
}

function isPoreSamplingRecord(record?: WetSheetRow | null) {
  const marker =
    `${record?.name || ''} ${record?.formCode || ''} ${record?.id || ''}`.toUpperCase();
  return (
    marker.includes('泡孔') ||
    marker.includes('打样') ||
    marker.includes('PORE')
  );
}

function resolveRecordDialogRecorderTime(record?: WetSheetRow | null) {
  if (
    record?.recorderTime &&
    !shouldResetRuntimeDateTime(record.recorderTime)
  ) {
    return record.recorderTime;
  }
  if (isPoreSamplingRecord(record)) {
    return resolvePoreSamplingRecorderTimeText();
  }
  return reportForm.value.recorderTime || '';
}

function resolveUploadUrl(uploadResult: any) {
  return String(
    uploadResult?.url ||
      uploadResult?.data?.url ||
      uploadResult?.path ||
      uploadResult ||
      '',
  );
}

async function beforePorePhotoUpload(file: File) {
  const rawFile = file as File;
  const isImage = !rawFile.type || rawFile.type.startsWith('image/');
  if (!isImage) {
    AModal.warning({
      content: '泡孔照片仅支持上传图片文件。',
      title: '文件类型不支持',
    });
    return false;
  }
  if (
    poreSelfCheckForm.value.photos.length + porePhotoUploadPendingCount.value >=
    MAX_PORE_PHOTO_COUNT
  ) {
    AModal.warning({
      content: `每条泡孔自检记录最多上传 ${MAX_PORE_PHOTO_COUNT} 张照片。`,
      title: '已达到照片数量上限',
    });
    return false;
  }
  if (rawFile.size > 10 * 1024 * 1024) {
    AModal.warning({
      content: '泡孔照片大小不能超过 10MB。',
      title: '文件过大',
    });
    return false;
  }
  try {
    porePhotoUploadPendingCount.value += 1;
    porePhotoUploading.value = true;
    const uploadResult = await uploadFile({
      directory: 'wet-pore-self-check',
      file: rawFile,
    });
    const url = resolveUploadUrl(uploadResult);
    if (!url) {
      throw new Error('上传成功但未返回照片地址');
    }
    poreSelfCheckForm.value.photos = normalizePorePhotoUrls(
      poreSelfCheckForm.value.photos,
      url,
    );
  } catch (error) {
    AModal.warning({
      content: getErrorMessage(error) || '泡孔照片上传失败，请稍后重试。',
      title: '上传失败',
    });
  } finally {
    porePhotoUploadPendingCount.value = Math.max(
      0,
      porePhotoUploadPendingCount.value - 1,
    );
    porePhotoUploading.value = porePhotoUploadPendingCount.value > 0;
  }
  return false;
}

function normalizeWetAttachments(value?: any): WetSheetAttachment[] {
  if (!value) return [];
  if (typeof value === 'string') {
    const parsed = safeParseJson(value);
    return normalizeWetAttachments(parsed);
  }
  if (!Array.isArray(value)) return [];
  const attachments = value
    .map((item, index) => {
      if (!item) return null;
      const path = String(item.path || item.filePath || '');
      const url = String(item.url || item.fileUrl || path || '');
      const name = String(
        item.name ||
          item.fileName ||
          url.split('/').pop() ||
          `附件${index + 1}`,
      );
      if (!url) return null;
      return {
        name,
        path: path || undefined,
        size: item.size,
        type: item.type,
        uid: item.uid || `${Date.now()}-${index}`,
        uploadTime: item.uploadTime,
        url,
      } as WetSheetAttachment;
    })
    .filter(Boolean) as WetSheetAttachment[];
  return attachments.slice(-1);
}

function normalizeWetImportedHeaderText(value: unknown) {
  const text = String(value ?? '').trim();
  if (!text || text === '-') return '';
  return text;
}

function extractWetImportedHeaderData(value?: Record<string, any> | null) {
  if (!value) return {};
  return Object.entries(value).reduce<Record<string, string | number>>(
    (data, [key, item]) => {
      if (key === 'attachments' || key === 'records') return data;
      if (item === undefined || item === null) return data;
      if (typeof item === 'number') {
        data[key] = item;
        return data;
      }
      const text = normalizeWetImportedHeaderText(item);
      if (text) data[key] = text;
      return data;
    },
    {},
  );
}

function getWetSheetSchemaMeta(schemaJson?: string | null) {
  return safeParseJson<WetSheetSchemaMeta>(schemaJson) || {};
}

function formatWetHeaderLabel(label: string) {
  const normalized = String(label || '')
    .trim()
    .replace(/[：:]+$/, '');
  return normalized ? `${normalized}：` : '';
}

function getWetProductionCheckHeaderLabel(
  key: WetHeaderFieldKey,
  fallback: string,
) {
  const schema = getWetSheetSchemaMeta(currentRecord.value?.schemaJson);
  return formatWetHeaderLabel(
    schema.headerLabels?.[key] ||
      WET_PRODUCTION_CHECK_HEADER_LABELS[key] ||
      fallback,
  );
}

function isWetProductionCheckHeaderEditable(key: WetHeaderFieldKey) {
  if (!WET_PRODUCTION_CHECK_TIME_HEADER_FIELDS.has(key)) return false;
  const schema = getWetSheetSchemaMeta(currentRecord.value?.schemaJson);
  if (!Array.isArray(schema.headerEditableFields)) return true;
  return schema.headerEditableFields.includes(key);
}

function stripWetModelPrefix(name?: string) {
  const text = String(name || '').trim();
  if (!text) return '';
  const stripped = text.replace(/^.*?[（(][^）)]*[）)]\s*/u, '').trim();
  return stripped || text;
}

function getWetPassWorkDisplayName(
  record?: Partial<WetPassWorkRow & WetSheetRow> | null,
) {
  if (!record) return '';
  if (record.displayName) return record.displayName;
  const category = resolveWetPassWorkCategory(record);
  const name = String(record.name || '');
  if (category === 'production-check') return '湿法生产点检表';
  if (category === 'semi-finished') {
    if (name.includes('凝固')) return '凝固半成品记录表';
    if (name.includes('烘箱')) return '烘箱半成品记录表';
    return stripWetModelPrefix(name) || '半成品记录单';
  }
  return stripWetModelPrefix(name);
}

function sanitizeExcelFileName(name?: string) {
  return `${String(name || '湿法过站记录').trim() || '湿法过站记录'}.xlsx`.replace(
    /[\\/:*?"<>|]/gu,
    '_',
  );
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function normalizeFormFilterText(value?: null | number | string) {
  return String(value ?? '')
    .trim()
    .toUpperCase();
}

function resolveCurrentProductModelCode() {
  const detail = planDetail.value as any;
  return String(
    task.value?.motherModelCode ||
      task.value?.modelCode ||
      detail?.motherModelCode ||
      detail?.modelCode ||
      '',
  ).trim();
}

function resolveCurrentProductModelPrefix() {
  const modelCode = normalizeFormFilterText(resolveCurrentProductModelCode());
  return modelCode.length > 4 ? modelCode.slice(0, 4) : modelCode;
}

function applyDefaultWetFormFilters() {
  const modelPrefix = resolveCurrentProductModelPrefix();
  productionCheckFormNameFilter.value = modelPrefix;
  semiFinishedFormNameFilter.value = modelPrefix;
}

function matchWetFormNameFilter(record: WetSheetRow, keyword?: string) {
  const normalizedKeyword = normalizeFormFilterText(keyword);
  if (!normalizedKeyword) return true;
  const schema = getWetSheetSchemaMeta(record.schemaJson);
  const haystack = [
    record.name,
    record.formCode,
    record.id,
    schema.modelCode,
    schema.sourceExcel,
    schema.sourceSheet,
  ]
    .map((item) => normalizeFormFilterText(item))
    .join(' ');
  return haystack.includes(normalizedKeyword);
}

function matchWetCurrentModelScopedForm(record: WetSheetRow) {
  const modelPrefix = resolveCurrentProductModelPrefix();
  if (!modelPrefix) return true;
  const category = resolveWetPassWorkCategory(record);
  if (category !== 'production-check' && category !== 'semi-finished') {
    return true;
  }
  return normalizeFormFilterText(record.name).includes(modelPrefix);
}

function openRecordAttachment(attachment: WetSheetAttachment) {
  if (!attachment?.url) return;
  window.open(attachment.url, '_blank');
}

function removeRecordAttachment(attachment: WetSheetAttachment) {
  if (!currentRecord.value) return;
  currentRecord.value.attachments = (
    currentRecord.value.attachments || []
  ).filter(
    (item) => item.uid !== attachment.uid && item.url !== attachment.url,
  );
}

function openPorePhotoPreview(photos?: string[], initialIndex = 0) {
  const urls = normalizePorePhotoUrls(photos);
  if (urls.length === 0) return;
  porePhotoPreviewUrls.value = urls;
  porePhotoPreviewIndex.value = Math.min(
    Math.max(0, initialIndex),
    urls.length - 1,
  );
  porePhotoPreviewVisible.value = true;
}

const currentPorePhotoPreviewUrl = computed(
  () => porePhotoPreviewUrls.value[porePhotoPreviewIndex.value] || '',
);
const porePhotoUploadLimitReached = computed(
  () =>
    poreSelfCheckForm.value.photos.length + porePhotoUploadPendingCount.value >=
    MAX_PORE_PHOTO_COUNT,
);

function removePorePhoto(url: string) {
  poreSelfCheckForm.value.photos = poreSelfCheckForm.value.photos.filter(
    (item) => item !== url,
  );
}

function showPreviousPorePhoto() {
  if (porePhotoPreviewIndex.value <= 0) return;
  porePhotoPreviewIndex.value -= 1;
}

function showNextPorePhoto() {
  if (porePhotoPreviewIndex.value >= porePhotoPreviewUrls.value.length - 1) {
    return;
  }
  porePhotoPreviewIndex.value += 1;
}

function normalizeAbnormalPositionOption(
  value?: string,
): AbnormalPositionOption {
  const text = String(value || '').toUpperCase();
  return text === 'NONE' || text === 'DETAIL' ? text : '';
}

function handleAbnormalPositionOptionChange(value?: string) {
  const option = normalizeAbnormalPositionOption(value);
  abnormalPositionOption.value = option;
  if (option === 'NONE') {
    abnormalPositionRows.value = [];
    return;
  }
  if (
    option === 'DETAIL' &&
    !abnormalPositionRows.value.length &&
    task.value?.status !== 'COMPLETED'
  ) {
    abnormalPositionRows.value = [createAbnormalPositionRow()];
  }
}

function createAbnormalPositionRow(
  row?: MesHcWetReportApi.AbnormalPositionItem,
): AbnormalPositionRow {
  return {
    abnormalLength: row?.abnormalLength,
    clientKey: `${row?.id || 'new'}-${Date.now()}-${Math.random()}`,
    id: row?.id,
    operationReportId: row?.operationReportId,
    planId: row?.planId,
    planNo: row?.planNo,
    planOperationId: row?.planOperationId,
    positionText: row?.positionText || '',
    remark: row?.remark || '',
    sortOrder: row?.sortOrder,
  };
}

function openAbnormalPositionModal() {
  if (task.value?.status !== 'COMPLETED') {
    handleAbnormalPositionOptionChange('DETAIL');
  }
  if (
    !abnormalPositionRows.value.length &&
    task.value?.status !== 'COMPLETED'
  ) {
    abnormalPositionRows.value = [createAbnormalPositionRow()];
  }
  abnormalPositionVisible.value = true;
}

function addAbnormalPositionRow() {
  abnormalPositionRows.value.push(createAbnormalPositionRow());
}

function removeAbnormalPositionRow(index: number) {
  abnormalPositionRows.value.splice(index, 1);
}

function buildAbnormalPositionPayload() {
  if (!abnormalPositionOption.value) {
    AModal.warning({
      content: '请选择异常位置选项：无或填写异常位置。',
      title: '请确认异常位置',
    });
    activeMainTab.value = 'report-info';
    return null;
  }
  const rows = abnormalPositionRows.value
    .map((row, index) => ({
      abnormalLength:
        row.abnormalLength === undefined || row.abnormalLength === null
          ? undefined
          : Number(row.abnormalLength),
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
  if (abnormalPositionOption.value === 'NONE') {
    return [] as MesHcWetReportApi.AbnormalPositionSaveReq[];
  }
  if (!rows.length) {
    AModal.warning({
      content: '请选择“无”，或填写至少一条异常位置。',
      title: '请完善异常位置',
    });
    activeMainTab.value = 'report-info';
    return null;
  }
  for (const row of rows) {
    if (!row.positionText) {
      AModal.warning({
        content: '异常位置不能为空。',
        title: '请完善异常位置',
      });
      activeMainTab.value = 'abnormal-position';
      return null;
    }
    if (
      row.abnormalLength !== undefined &&
      (!Number.isFinite(row.abnormalLength) || row.abnormalLength < 0)
    ) {
      AModal.warning({
        content: '异常位置米数不能为负数。',
        title: '请完善异常位置',
      });
      activeMainTab.value = 'abnormal-position';
      return null;
    }
  }
  return rows as MesHcWetReportApi.AbnormalPositionSaveReq[];
}

async function loadWetAbnormalPositions() {
  if (!task.value?.operationReportId && !task.value?.planOperationId) {
    abnormalPositionRows.value = [];
    if (task.value?.status !== 'COMPLETED') {
      abnormalPositionOption.value = normalizeAbnormalPositionOption(
        parseWetExtraJson(task.value?.extraJson).abnormalPositionOption,
      );
    }
    return;
  }
  const rows = await getWetAbnormalPositionList({
    operationReportId: task.value?.operationReportId,
    planOperationId: task.value?.operationReportId
      ? undefined
      : task.value?.planOperationId,
  });
  abnormalPositionRows.value = (rows || []).map((row) =>
    createAbnormalPositionRow(row),
  );
  const savedOption = normalizeAbnormalPositionOption(
    parseWetExtraJson(task.value?.extraJson).abnormalPositionOption,
  );
  if (abnormalPositionRows.value.length) {
    abnormalPositionOption.value = 'DETAIL';
  } else if (savedOption) {
    abnormalPositionOption.value = savedOption;
  } else if (task.value?.status === 'COMPLETED') {
    abnormalPositionOption.value = 'NONE';
  }
}

function mapPassWorkDetail(
  detail: MesHcWetReportApi.PassWorkItem,
  includeSemiLength = false,
): WetSheetDetail {
  return {
    category: detail.category || '',
    item: detail.item || '',
    length: includeSemiLength
      ? normalizeSemiDetailLengthValue(detail.actualValue2)
      : undefined,
    node: detail.node || '',
    remark: detail.remark || '',
    result: detail.status || 'OK',
    seq: detail.itemSeq || 0,
    standard: detail.standard || '',
    value: detail.actualValue || '',
  };
}

function applySemiFinishedHeader(
  target: WetSheetRow,
  headerDataJson?: string | null,
) {
  const header = safeParseJson<
    Record<string, any> & {
      attachments?: WetSheetAttachment[];
      finalResult?: string;
      generatedLength?: number;
      poreDevelopment?: string;
      semiWidth?: string;
    }
  >(headerDataJson);
  if (!header) return target;
  target.importedHeaderData = extractWetImportedHeaderData(header);
  target.attachments = normalizeWetAttachments(header.attachments);
  target.finalResult = header.finalResult || target.finalResult;
  target.generatedLength = header.generatedLength || target.generatedLength;
  target.poreDevelopment = header.poreDevelopment ?? target.poreDevelopment;
  target.semiWidth = header.semiWidth ?? target.semiWidth;
  return target;
}

function applyPassWorkHeaderMeta(
  target: WetSheetRow,
  headerDataJson?: string | null,
) {
  const header = safeParseJson<
    Record<string, any> & { attachments?: WetSheetAttachment[] }
  >(headerDataJson);
  if (!header) return target;
  target.importedHeaderData = extractWetImportedHeaderData(header);
  target.attachments = normalizeWetAttachments(header.attachments);
  return target;
}

function resolvePassWorkHeaderDataJson(source?: WetPassWorkRow | null) {
  if (!source) return null;
  return source.headerDataJson || source.presetHeaderDataJson || null;
}

function hasBackendWetTemplate(source?: WetPassWorkRow | null) {
  if (!source) return false;
  return Boolean(
    source.schemaJson ||
    source.presetHeaderDataJson ||
    (source.presetDetails && source.presetDetails.length > 0),
  );
}

function buildBackendFirstFallbackRow(
  source: WetPassWorkRow | undefined,
  registryFallback: WetSheetRow | null,
  hardFallback: WetSheetRow,
): WetSheetRow {
  if (hasBackendWetTemplate(source)) {
    return {
      details: [],
      displayName: source?.displayName || hardFallback.displayName,
      formCode: source?.formCode,
      formId: source?.formId,
      id: source?.id || source?.formCode || hardFallback.id,
      name: source?.name || hardFallback.name,
      result: source?.result || hardFallback.result,
      schemaJson: source?.schemaJson,
      status: 'PENDING',
      timing: source?.timing || hardFallback.timing,
    };
  }
  return registryFallback || hardFallback;
}

function mapPassWorkRow(
  source: WetPassWorkRow,
  fallback: WetSheetRow,
): WetSheetRow {
  const includeSemiLength = isWetSemiSource(source);
  const next: WetSheetRow = {
    ...fallback,
    confirmer: source.confirmer || fallback.confirmer,
    confirmerTime: source.confirmerTime || fallback.confirmerTime,
    details:
      source.details && source.details.length > 0
        ? source.details.map((detail) =>
            mapPassWorkDetail(detail, includeSemiLength),
          )
        : source.presetDetails && source.presetDetails.length > 0
          ? source.presetDetails.map((detail) =>
              mapPassWorkDetail(detail, includeSemiLength),
            )
          : JSON.parse(JSON.stringify(fallback.details || [])),
    displayName: source.displayName || fallback.displayName,
    formCode: source.formCode,
    formId: source.formId,
    id: source.id || fallback.id,
    name: source.name || fallback.name,
    recordId: source.recordId,
    recorder: source.recorder || fallback.recorder,
    recorderTime: source.recorderTime || fallback.recorderTime,
    remark: source.confirmRemark || source.formRemark || fallback.remark,
    result: source.result || fallback.result,
    schemaJson: source.schemaJson || fallback.schemaJson,
    status:
      source.status === 'CONFIRMED'
        ? 'COMPLETED'
        : source.status === 'RECORDED'
          ? 'PENDING'
          : fallback.status,
    timing: source.timing || fallback.timing,
  };
  applyPassWorkHeaderMeta(next, resolvePassWorkHeaderDataJson(source));
  if (source.formCode?.includes('SEMI')) {
    applySemiFinishedHeader(next, resolvePassWorkHeaderDataJson(source));
  }
  return next;
}

function buildWetPassWorkPayload(record: WetSheetRow) {
  const headerData = buildHeaderDataBySchema(
    record,
    recordDialogCategory.value,
  );
  if (recordDialogCategory.value === 'semi-finished') {
    headerData.generatedLength =
      currentRecord.value?.generatedLength || record.generatedLength || 600;
  }
  const attachments = normalizeWetAttachments(currentRecord.value?.attachments);
  if (attachments.length) {
    headerData.attachments = attachments;
  }
  return {
    confirmer:
      recordActionForm.value.confirmer ||
      reportForm.value.confirmerName ||
      undefined,
    confirmerTime:
      recordActionForm.value.confirmerTime ||
      reportForm.value.confirmerTime ||
      undefined,
    confirmRemark: recordActionForm.value.confirmRemark || undefined,
    details: (currentRecord.value?.details || []).map((item) => ({
      actualValue: item.value || '',
      actualValue2:
        recordDialogCategory.value === 'semi-finished' &&
        normalizeSemiDetailLengthValue(item.length) !== undefined
          ? serializeSemiDetailLength(item.length)
          : '',
      category: item.category || '',
      dualLabel1: '',
      dualLabel2:
        recordDialogCategory.value === 'semi-finished' ? 'LENGTH_M' : '',
      item: item.item || '',
      itemSeq: item.seq,
      node: item.node || '',
      remark: item.remark || '',
      standard: item.standard || '',
      status: item.result || 'OK',
      valueMode: item.length !== undefined ? 'NUMBER' : 'TEXT',
    })),
    equipmentCode: reportForm.value.equipmentCode || undefined,
    equipmentId: reportForm.value.equipmentId,
    equipmentName: reportForm.value.equipmentName || undefined,
    formCode: record.formCode || record.id,
    formRemark: recordActionForm.value.formRemark || undefined,
    headerDataJson: Object.keys(headerData).length
      ? JSON.stringify(headerData)
      : undefined,
    inspectionResult:
      recordDialogMode.value === 'confirm'
        ? recordActionForm.value.inspectionResult || 'OK'
        : recordActionForm.value.result || 'OK',
    planId: task.value?.planId,
    planOperationId: task.value?.planOperationId,
    recordId: record.recordId,
    recorder:
      recordActionForm.value.recorder ||
      reportForm.value.recorderName ||
      undefined,
    recorderTime:
      recordActionForm.value.recorderTime ||
      reportForm.value.recorderTime ||
      undefined,
    result:
      recordDialogMode.value === 'confirm'
        ? recordActionForm.value.inspectionResult || 'OK'
        : recordActionForm.value.result || 'OK',
  };
}

function applyFaiSummary(summary?: MesHcWetReportApi.FaiSummary | null) {
  const status = summary?.faiStatus || task.value?.faiStatus || 'PENDING';
  const judgment = summary?.faiJudgment || task.value?.faiJudgment || 'PENDING';
  const displayText =
    summary?.displayText || resolveFirstInspectionDisplayText(status, judgment);
  const napSampleLength = normalizeOptionalNumber(
    summary?.napSampleLength ?? task.value?.napSampleLength,
  );
  const currentInspection: FirstInspectionRecord = {
    faiId: summary?.faiId || task.value?.faiId,
    faiNo: summary?.faiNo || task.value?.faiNo || '',
    applyTime: summary?.faiApplyTime || task.value?.faiApplyTime || '',
    inspectTime: summary?.faiReturnTime || task.value?.faiReturnTime || '',
    inspectionDesc:
      summary?.faiRejectReason || task.value?.faiRejectReason || '',
    inspector: '',
    judgment,
    napSampleLength,
    productBatchNo:
      summary?.productBatchNo ||
      task.value?.productionBatchNo ||
      task.value?.batchNo ||
      '',
    result: displayText,
    returnTime: summary?.faiReturnTime || task.value?.faiReturnTime || '',
    standardText:
      summary?.faiStandardNo || task.value?.faiStandardNo
        ? `${summary?.faiStandardNo || task.value?.faiStandardNo}${summary?.faiStandardVersion ? ` / ${summary.faiStandardVersion}` : ''}`
        : '',
    status,
    isCurrent: true,
  };
  firstInspection.value = currentInspection;
  firstInspectionRecords.value = Array.isArray(summary?.records)
    ? summary.records.map((record) => mapFirstInspectionRecord(record))
    : [currentInspection];
  if (firstInspectionRecords.value.length === 0) {
    firstInspectionRecords.value = [currentInspection];
  }
  const currentRecord =
    firstInspectionRecords.value.find((record) => record.isCurrent) ||
    currentInspection;
  const currentNapSampleLength = normalizeOptionalNumber(
    currentRecord.napSampleLength,
  );
  if (currentNapSampleLength !== undefined) {
    reportForm.value.napSampleLength = currentNapSampleLength;
  }
  task.value = {
    ...task.value,
    faiApplyTime: summary?.faiApplyTime || task.value?.faiApplyTime,
    faiId: summary?.faiId || task.value?.faiId,
    faiJudgment: judgment,
    faiNo: summary?.faiNo || task.value?.faiNo,
    faiRejectReason: summary?.faiRejectReason || task.value?.faiRejectReason,
    faiReturnTime: summary?.faiReturnTime || task.value?.faiReturnTime,
    faiStandardId: summary?.faiStandardId || task.value?.faiStandardId,
    faiStandardNo: summary?.faiStandardNo || task.value?.faiStandardNo,
    faiStatus: status,
    napSampleLength:
      currentNapSampleLength !== undefined
        ? currentNapSampleLength
        : task.value?.napSampleLength,
  };
  syncFirstInspectionAutoRefresh();
}

function mapFirstInspectionRecord(
  record: MesHcWetReportApi.FaiRecord,
): FirstInspectionRecord {
  const status = record.faiStatus || 'PENDING';
  const judgment = record.faiJudgment || 'PENDING';
  const standardText = record.faiStandardNo
    ? `${record.faiStandardNo}${record.faiStandardVersion ? ` / ${record.faiStandardVersion}` : ''}`
    : '';
  return {
    faiId: record.faiId,
    faiNo: record.faiNo || '',
    applyTime: record.faiApplyTime || '',
    inspectTime: record.faiReturnTime || '',
    inspectionDesc: record.faiRejectReason || '',
    inspector: '',
    judgment,
    napSampleLength: normalizeOptionalNumber(record.napSampleLength),
    productBatchNo: record.productBatchNo || '',
    result:
      record.displayText || resolveFirstInspectionDisplayText(status, judgment),
    returnTime: record.faiReturnTime || '',
    standardText,
    status,
    isCurrent: Boolean(record.isCurrent ?? (record as any).current),
  };
}

async function loadWetFaiSummary() {
  if (!task.value?.planId || !task.value?.planOperationId) return;
  const summary = await getWetFaiSummary({
    planId: task.value.planId,
    planOperationId: task.value.planOperationId,
  });
  applyFaiSummary(summary);
  return summary;
}

function hasSubmittedFirstInspection() {
  return Boolean(firstInspection.value.faiId || task.value?.faiId);
}

function stopFirstInspectionAutoRefresh() {
  if (firstInspectionRefreshTimer) {
    window.clearInterval(firstInspectionRefreshTimer);
    firstInspectionRefreshTimer = undefined;
  }
}

function startFirstInspectionAutoRefresh() {
  stopFirstInspectionAutoRefresh();
  if (!wetDetailModalOpen || !hasSubmittedFirstInspection()) return;
  firstInspectionRefreshTimer = window.setInterval(
    () => {
      void refreshWetFaiSummary(false);
    },
    5 * 60 * 1000,
  );
}

function syncFirstInspectionAutoRefresh() {
  if (hasSubmittedFirstInspection()) {
    startFirstInspectionAutoRefresh();
  } else {
    stopFirstInspectionAutoRefresh();
  }
}

async function refreshWetFaiSummary(manual = true) {
  if (
    firstInspectionRefreshing.value ||
    !task.value?.planId ||
    !task.value?.planOperationId ||
    !hasSubmittedFirstInspection()
  ) {
    return;
  }
  firstInspectionRefreshing.value = true;
  try {
    await loadWetFaiSummary();
  } catch (error) {
    if (manual) {
      AModal.warning({
        content: getErrorMessage(error) || '刷新首检状态失败，请稍后重试。',
        title: '刷新失败',
      });
    }
  } finally {
    firstInspectionRefreshing.value = false;
  }
}

function validatePoreSelfCheckBeforeFinish() {
  if (hasPoreSelfCheckRecord()) return true;
  activeMainTab.value = 'pore-self-check';
  viewMode.value = 'tabs';
  AModal.warning({
    content: '报工前必须先在“泡孔自检”页签新增至少一条自检记录。',
    title: '缺少泡孔自检',
  });
  return false;
}

function resolveWetPassWorkCategory(
  row?: Partial<WetPassWorkRow & WetSheetRow> | null,
): WetPassWorkCategory | null {
  if (!row) return null;
  const code = String(row.formCode || row.id || '').toUpperCase();
  if (
    code === WET_PORE_SELF_CHECK_FORM_CODE ||
    code === WET_FIRST_INSPECTION_FORM_CODE
  ) {
    return null;
  }
  const schemaCategory = getWetSheetSchemaMeta(row.schemaJson).wetCategory;
  if (
    schemaCategory === 'prepare' ||
    schemaCategory === 'production-check' ||
    schemaCategory === 'semi-finished'
  ) {
    return schemaCategory;
  }
  const name = String(row.name || '');
  if (
    code === 'WET_STARTUP_CHECK' ||
    code === 'WET_CLEANING_CHECK' ||
    name.includes('开机点检') ||
    name.includes('清洁点检')
  ) {
    return 'prepare';
  }
  if (
    code.startsWith('WET_SOLID_SEMI') ||
    code.startsWith('WET_OVEN_SEMI') ||
    name.includes('半成品记录')
  ) {
    return 'semi-finished';
  }
  if (
    code.startsWith('WET_PROCESS_CHECK') ||
    name.includes('生产点检') ||
    name.includes('湿法点检')
  ) {
    return 'production-check';
  }
  return null;
}

function isWetOvenSemiSource(row?: Partial<WetPassWorkRow & WetSheetRow>) {
  const schema = getWetSheetSchemaMeta(row?.schemaJson);
  const code = String(row?.formCode || row?.id || '').toUpperCase();
  const name = String(row?.name || '');
  return (
    schema.semiType === 'oven' ||
    code.startsWith('WET_OVEN_SEMI') ||
    name.includes('烘箱')
  );
}

function isWetSemiSource(row?: Partial<WetPassWorkRow & WetSheetRow>) {
  return resolveWetPassWorkCategory(row) === 'semi-finished';
}

async function loadWetPassWorkRows() {
  if (!task.value?.planId || !task.value?.planOperationId) return;
  const rows = await getWetPassWorkList({
    planId: task.value.planId,
    planOperationId: task.value.planOperationId,
  });
  const defaults = buildWetDefaultSheetGroups();
  if (!rows?.length) {
    workPrepareRows.value = defaults.prepare;
    productionCheckRows.value = defaults.productionCheck;
    semiFinishedRows.value = defaults.semiFinished;
    applyPoreSelfCheckRow();
    await loadWetFaiSummary();
    return;
  }
  const rowMap = new Map(rows.map((item) => [item.formCode, item]));
  const startupRow = rowMap.get('WET_STARTUP_CHECK');
  const cleaningRow = rowMap.get('WET_CLEANING_CHECK');
  const prepareRows = rows.filter(
    (item) => resolveWetPassWorkCategory(item) === 'prepare',
  );
  const extraPrepareRows = prepareRows.filter(
    (item) =>
      item.formCode !== 'WET_STARTUP_CHECK' &&
      item.formCode !== 'WET_CLEANING_CHECK',
  );
  workPrepareRows.value = [
    mapPassWorkRow(
      startupRow || ({ formCode: 'WET_STARTUP_CHECK' } as any),
      buildBackendFirstFallbackRow(
        startupRow,
        buildWetPresetRow('WET_STARTUP_CHECK', startupRow?.schemaJson),
        defaults.prepare[0]!,
      ),
    ),
    mapPassWorkRow(
      cleaningRow || ({ formCode: 'WET_CLEANING_CHECK' } as any),
      buildBackendFirstFallbackRow(
        cleaningRow,
        buildWetPresetRow('WET_CLEANING_CHECK', cleaningRow?.schemaJson),
        defaults.prepare[1]!,
      ),
    ),
    ...extraPrepareRows.map((row) =>
      mapPassWorkRow(
        row,
        buildBackendFirstFallbackRow(
          row,
          buildWetPresetRow(row.formCode, row.schemaJson),
          defaults.prepare[1]!,
        ),
      ),
    ),
  ];
  const productionRows = rows.filter(
    (item) => resolveWetPassWorkCategory(item) === 'production-check',
  );
  productionCheckRows.value = productionRows.length
    ? productionRows.map((row) =>
        mapPassWorkRow(
          row,
          buildBackendFirstFallbackRow(
            row,
            buildWetPresetRow(row.formCode, row.schemaJson),
            defaults.productionCheck[0]!,
          ),
        ),
      )
    : [
        mapPassWorkRow(
          rowMap.get('WET_PROCESS_CHECK') ||
            ({ formCode: 'WET_PROCESS_CHECK' } as any),
          buildBackendFirstFallbackRow(
            rowMap.get('WET_PROCESS_CHECK'),
            buildWetPresetRow(
              'WET_PROCESS_CHECK',
              rowMap.get('WET_PROCESS_CHECK')?.schemaJson,
            ),
            defaults.productionCheck[0]!,
          ),
        ),
      ];
  applyPoreSelfCheckRow(rowMap.get(WET_PORE_SELF_CHECK_FORM_CODE));
  const semiRows = rows.filter((item) => isWetSemiSource(item));
  semiFinishedRows.value = semiRows.length
    ? semiRows.map((row) => {
        const hardFallback = isWetOvenSemiSource(row)
          ? defaults.semiFinished[1]!
          : defaults.semiFinished[0]!;
        const mapped = mapPassWorkRow(
          row,
          buildBackendFirstFallbackRow(
            row,
            buildWetPresetRow(row.formCode, row.schemaJson),
            hardFallback,
          ),
        );
        if (!mapped.details || mapped.details.length === 0) {
          const length =
            mapped.generatedLength ||
            resolveSemiGeneratedLength(row.schemaJson, 600);
          mapped.generatedLength = length;
          mapped.details = generateSemiDetails(
            length,
            resolveSemiRowStep(row.schemaJson),
          );
        }
        return mapped;
      })
    : defaults.semiFinished.map((item) => {
        const matched =
          rowMap.get(
            item.id === 'wet-solidify-semi'
              ? 'WET_SOLID_SEMI'
              : 'WET_OVEN_SEMI',
          ) ||
          ({
            formCode:
              item.id === 'wet-solidify-semi'
                ? 'WET_SOLID_SEMI'
                : 'WET_OVEN_SEMI',
          } as any);
        const mapped = mapPassWorkRow(
          matched,
          buildBackendFirstFallbackRow(
            matched,
            buildWetPresetRow(matched.formCode, matched.schemaJson),
            item,
          ),
        );
        if (!mapped.details || mapped.details.length === 0) {
          const length =
            mapped.generatedLength ||
            resolveSemiGeneratedLength(matched.schemaJson, 600);
          mapped.generatedLength = length;
          mapped.details = generateSemiDetails(
            length,
            resolveSemiRowStep(matched.schemaJson),
          );
        }
        return mapped;
      });
  const firstInspectionRow = rowMap.get(WET_FIRST_INSPECTION_FORM_CODE);
  wetFirstInspectionRecordId.value = firstInspectionRow?.recordId;
  await loadWetFaiSummary();
}

async function loadEquipmentOptions() {
  const workCenterId = task.value?.workCenterId;
  let list: any[] = [];
  const primaryPage = await getEquipmentPage({
    pageNo: 1,
    pageSize: 200,
    workCenterId,
    status: 0,
  } as any);
  list = primaryPage?.list || [];
  if (list.length === 0) {
    const fallbackPage = await getEquipmentPage({
      pageNo: 1,
      pageSize: 200,
      status: 0,
    } as any);
    list = fallbackPage?.list || [];
  }
  equipmentOptions.value = list.map((item: any) => ({
    code: item.equipmentCode,
    label:
      item.equipmentCode && item.equipmentName
        ? `${item.equipmentCode} / ${item.equipmentName}`
        : item.equipmentCode || item.equipmentName,
    name: item.equipmentName,
    value: item.id,
  }));
  if (!reportForm.value.equipmentId && reportForm.value.equipmentCode) {
    const matched = equipmentOptions.value.find(
      (item: any) => item.code === reportForm.value.equipmentCode,
    );
    if (matched) {
      reportForm.value.equipmentId = matched.value;
      reportForm.value.equipmentName =
        matched.name || matched.label || reportForm.value.equipmentName;
    }
  }
}

async function loadEquipmentStatusCard() {
  if (!reportForm.value.equipmentId) {
    equipmentStatusCard.value = {
      currentEndTime: '-',
      currentOperationName: '-',
      currentOperatorName: '-',
      currentPlanNo: '-',
      currentStartTime: '-',
      equipmentLabel: currentMachineCode.value || '未挂接设备',
      statusMeta: getEquipmentWorkStatusMeta(),
    };
    return;
  }
  const detail = await getEquipment(reportForm.value.equipmentId);
  equipmentStatusCard.value = {
    currentEndTime: normalizeDateTime(detail?.currentEndTime) || '-',
    currentOperationName:
      detail?.currentOperationName || detail?.currentOperationCode || '-',
    currentOperatorName: detail?.currentOperatorName || '-',
    currentPlanNo: detail?.currentPlanNo || '-',
    currentStartTime: normalizeDateTime(detail?.currentStartTime) || '-',
    equipmentLabel:
      detail?.equipmentCode && detail?.equipmentName
        ? `${detail.equipmentCode} / ${detail.equipmentName}`
        : detail?.equipmentCode ||
          detail?.equipmentName ||
          currentMachineCode.value ||
          '未挂接设备',
    statusMeta: getEquipmentWorkStatusMeta(detail?.workStatus),
  };
}

async function loadGuideClothRuntime() {
  const motherModelCode = task.value?.motherModelCode;
  if (!motherModelCode) {
    guideClothRuntime.value = null;
    guideClothBaseNextUseCount.value = 1;
    reportForm.value.guideClothCurrentUsedLength = undefined;
    reportForm.value.guideClothLimitLength = undefined;
    reportForm.value.guideClothReplaceTime = '';
    reportForm.value.guideClothWarningFlag = 0;
    reportForm.value.guideClothWarningText = '';
    applyGuideClothUseCountByFlag();
    return;
  }
  try {
    const runtime = await getGuideClothRuntime(
      motherModelCode,
      reportForm.value.equipmentId || task.value?.equipmentId,
    );
    const validReplaceTime = normalizeValidBusinessDateTime(
      runtime?.replaceTime,
    );
    guideClothRuntime.value = runtime
      ? { ...runtime, replaceTime: validReplaceTime || undefined }
      : runtime;
    guideClothBaseNextUseCount.value = runtime?.nextUseCount || 1;
    reportForm.value.guideClothCurrentUsedLength = runtime?.currentUsedLength;
    reportForm.value.guideClothLimitLength = runtime?.limitLength;
    reportForm.value.guideClothReplaceTime = validReplaceTime;
    reportForm.value.guideClothWarningFlag = runtime?.warningFlag || 0;
    reportForm.value.guideClothWarningText = runtime?.warningText || '';
    if (!reportForm.value.guideClothBatchNo && runtime?.guideClothBatchNo) {
      reportForm.value.guideClothBatchNo = runtime.guideClothBatchNo;
    }
    if (!reportForm.value.petBatchNo && runtime?.petBatchNo) {
      reportForm.value.petBatchNo = runtime.petBatchNo;
    }
    if (!reportForm.value.petModel && runtime?.petModel) {
      reportForm.value.petModel = runtime.petModel;
    }
    if (task.value?.status !== 'COMPLETED') {
      applyGuideClothUseCountByFlag(true);
    }
  } catch {
    guideClothRuntime.value = null;
    guideClothBaseNextUseCount.value = 1;
    reportForm.value.guideClothCurrentUsedLength = undefined;
    reportForm.value.guideClothLimitLength = undefined;
    reportForm.value.guideClothReplaceTime = '';
    reportForm.value.guideClothWarningFlag = 0;
    reportForm.value.guideClothWarningText = '';
    applyGuideClothUseCountByFlag();
  }
}

async function syncTaskRuntimeFromBackend(
  options: { emitChange?: boolean } = {},
) {
  if (!task.value?.planOperationId || !task.value?.planNo) return;
  const rows = await getWetReportTaskList({
    taskKeyword: task.value.planNo,
    taskStatus: 'ALL',
  });
  const latestTask = (rows || []).find(
    (item: any) => item?.planOperationId === task.value?.planOperationId,
  );
  if (!latestTask) return;
  const extra = parseWetExtraJson(latestTask?.extraJson);
  task.value = {
    ...task.value,
    ...latestTask,
  };
  const runtimeProductionDate =
    latestTask?.productionDate || reportForm.value.productionDate || '';
  reportForm.value.productionDate = runtimeProductionDate;
  reportForm.value.startTime =
    latestTask?.startTime || reportForm.value.startTime || '';
  reportForm.value.endTime =
    latestTask?.endTime || reportForm.value.endTime || '';
  reportForm.value.recorderName =
    latestTask?.recorderName || reportForm.value.recorderName || '';
  reportForm.value.recorderTime =
    latestTask?.recorderTime || reportForm.value.recorderTime || '';
  reportForm.value.confirmerName =
    latestTask?.confirmerName || reportForm.value.confirmerName || '';
  reportForm.value.confirmerTime =
    latestTask?.confirmerTime || reportForm.value.confirmerTime || '';
  reportForm.value.equipmentId =
    latestTask?.equipmentId ?? reportForm.value.equipmentId;
  reportForm.value.equipmentCode =
    latestTask?.equipmentCode || reportForm.value.equipmentCode || '';
  reportForm.value.equipmentName =
    latestTask?.equipmentName || reportForm.value.equipmentName || '';
  reportForm.value.inWashTime =
    normalizeReportDateTimeText(extra.inWashTime, runtimeProductionDate) ||
    reportForm.value.inWashTime ||
    '';
  reportForm.value.outWashTime =
    normalizeReportDateTimeText(extra.outWashTime, runtimeProductionDate) ||
    reportForm.value.outWashTime ||
    '';
  reportForm.value.inSolidifyTime =
    normalizeReportDateTimeText(extra.inSolidifyTime, runtimeProductionDate) ||
    reportForm.value.inSolidifyTime ||
    '';
  reportForm.value.outSolidifyTime =
    normalizeReportDateTimeText(extra.outSolidifyTime, runtimeProductionDate) ||
    reportForm.value.outSolidifyTime ||
    '';
  reportForm.value.inOvenTime =
    normalizeReportDateTimeText(extra.inOvenTime, runtimeProductionDate) ||
    reportForm.value.inOvenTime ||
    '';
  reportForm.value.outOvenTime =
    normalizeReportDateTimeText(extra.outOvenTime, runtimeProductionDate) ||
    reportForm.value.outOvenTime ||
    '';
  if (options.emitChange !== false) {
    emit('taskChange', { ...task.value });
  }
  await loadWetAbnormalPositions();
}

function openDeviceSwitchModal() {
  deviceSwitchForm.value = {
    equipmentId: reportForm.value.equipmentId,
  };
  deviceSwitchVisible.value = true;
}

async function handleDeviceSwitchConfirm() {
  deviceSwitchSubmitting.value = true;
  try {
    const target = equipmentOptions.value.find(
      (item: any) => item.value === deviceSwitchForm.value.equipmentId,
    );
    await switchWetEquipment({
      equipmentCode: target?.code,
      equipmentId: deviceSwitchForm.value.equipmentId,
      equipmentName: target?.name,
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
    });
    reportForm.value.equipmentId = deviceSwitchForm.value.equipmentId;
    reportForm.value.equipmentCode = target?.code || '';
    reportForm.value.equipmentName = target?.name || '';
    if (task.value) {
      task.value = {
        ...task.value,
        equipmentCode: reportForm.value.equipmentCode,
        equipmentId: reportForm.value.equipmentId,
        equipmentName: reportForm.value.equipmentName,
      };
      emit('taskChange', { ...task.value });
    }
    await loadEquipmentStatusCard();
    await loadGuideClothRuntime();
    deviceSwitchVisible.value = false;
  } finally {
    deviceSwitchSubmitting.value = false;
  }
}

async function openTimeCorrectionModal() {
  if (!isStarted()) {
    message.warning('请先执行开工确认，再修正报工时间');
    return;
  }
  if (!task.value?.operationReportId) {
    await syncTaskRuntimeFromBackend({ emitChange: false });
  }
  if (!task.value?.operationReportId) {
    message.warning('未找到当前湿法报工记录，无法修正时间');
    return;
  }
  const reportDate =
    reportForm.value.productionDate ||
    task.value?.productionDate ||
    reportDateFromDateTime(
      reportForm.value.startTime || reportForm.value.endTime,
    );
  timeCorrectionForm.value = {
    endTime: normalizeReportDateTimeText(reportForm.value.endTime, reportDate),
    reportDate,
    startTime: normalizeReportDateTimeText(
      reportForm.value.startTime,
      reportDate,
    ),
  };
  timeCorrectionVisible.value = true;
}

async function handleTimeCorrectionConfirm() {
  await nextTick();
  syncTimeCorrectionFieldsFromDom();
  const reportId = Number(task.value?.operationReportId || 0);
  if (!reportId) {
    message.warning('未找到当前湿法报工记录，无法保存');
    return;
  }
  const reportDate =
    timeCorrectionForm.value.reportDate ||
    reportDateFromDateTime(
      timeCorrectionForm.value.startTime || timeCorrectionForm.value.endTime,
    );
  const startTime = normalizeReportDateTimeText(
    timeCorrectionForm.value.startTime,
    reportDate,
  );
  const endTime = normalizeReportDateTimeText(
    timeCorrectionForm.value.endTime,
    reportDate,
  );
  if (!startTime) {
    message.warning('请填写有效的开始时间');
    return;
  }
  if (task.value?.status === 'COMPLETED' && !endTime) {
    message.warning('已完工报工请填写有效的结束时间');
    return;
  }
  if (endTime && dayjs(endTime).isBefore(dayjs(startTime))) {
    message.warning('结束时间不能早于开始时间');
    return;
  }
  const recorderTime = normalizeReportDateTimeText(
    reportForm.value.recorderTime || task.value?.recorderTime,
    reportDate,
  );
  if (
    task.value?.status === 'COMPLETED' &&
    endTime &&
    recorderTime &&
    dayjs(endTime).isAfter(dayjs(recorderTime))
  ) {
    message.warning('完工时间不能晚于最终提交记录时间');
    return;
  }
  timeCorrectionSubmitting.value = true;
  try {
    await updateWetReportTime({
      endTime: endTime || undefined,
      id: reportId,
      reportDate,
      startTime,
    });
    reportForm.value.productionDate = reportDate;
    reportForm.value.startTime = startTime;
    reportForm.value.endTime = endTime;
    task.value = {
      ...(task.value || {}),
      endTime,
      productionDate: reportDate,
      reportDate,
      startTime,
    };
    await syncTaskRuntimeFromBackend({ emitChange: false });
    reportForm.value.productionDate = reportDate;
    reportForm.value.startTime = startTime;
    reportForm.value.endTime = endTime;
    task.value = {
      ...(task.value || {}),
      endTime,
      productionDate: reportDate,
      reportDate,
      startTime,
    };
    await loadWetPassWorkRows();
    await loadEquipmentStatusCard();
    timeCorrectionVisible.value = false;
    message.success('湿法报工开始/结束时间已修正');
  } finally {
    timeCorrectionSubmitting.value = false;
  }
}

function getCurrentWetReceiveLength() {
  return toNumber(reportForm.value.receiveLength ?? task.value?.goodQty, 0);
}

function getCurrentWetPrintLossLength() {
  const extra = parseWetExtraJson(task.value?.extraJson);
  return toNumber(
    reportForm.value.printSampleLength ??
      extra.printLossLength ??
      task.value?.scrapQty,
    0,
  );
}

async function openQuantityRevisionModal() {
  if (!task.value?.operationReportId) {
    await syncTaskRuntimeFromBackend({ emitChange: false });
  }
  if (!task.value?.operationReportId || task.value?.status !== 'COMPLETED') {
    message.warning('未找到已完工的湿法报工记录，无法修订统计数据');
    return;
  }
  const currentLength = getCurrentWetReceiveLength();
  const currentLossLength = getCurrentWetPrintLossLength();
  quantityRevisionForm.value = {
    printLossLength: currentLossLength >= 0 ? currentLossLength : undefined,
    reason: '',
    receiveLength: currentLength >= 0 ? currentLength : undefined,
  };
  quantityRevisionVisible.value = true;
}

async function handleQuantityRevisionConfirm() {
  const reportId = Number(task.value?.operationReportId || 0);
  const receiveLength = Number(quantityRevisionForm.value.receiveLength ?? 0);
  const printLossLength = Number(
    quantityRevisionForm.value.printLossLength ?? 0,
  );
  const reason = String(quantityRevisionForm.value.reason || '').trim();
  if (!reportId) {
    message.warning('未找到当前湿法报工记录，无法保存');
    return;
  }
  if (
    !Number.isFinite(receiveLength) ||
    receiveLength < 0 ||
    !Number.isFinite(printLossLength) ||
    printLossLength < 0
  ) {
    message.warning('统计数据不能为负数');
    return;
  }
  if (!reason) {
    message.warning('请填写修订原因');
    return;
  }
  const currentLength = getCurrentWetReceiveLength();
  const currentLossLength = getCurrentWetPrintLossLength();
  if (
    Math.abs(receiveLength - currentLength) < 0.0001 &&
    Math.abs(printLossLength - currentLossLength) < 0.0001
  ) {
    message.warning('修订后统计数据与当前数据一致，无需更新');
    return;
  }
  const normalizedLength = Number(receiveLength.toFixed(3));
  const normalizedLossLength = Number(printLossLength.toFixed(3));
  quantityRevisionSubmitting.value = true;
  try {
    await reviseWetReportStatisticsData({
      goodQty: normalizedLength,
      id: reportId,
      reason,
      scrapQty: normalizedLossLength,
    });
    await syncTaskRuntimeFromBackend({ emitChange: false });
    reportForm.value.receiveLength = normalizedLength;
    reportForm.value.printSampleLength = normalizedLossLength;
    if (task.value) {
      task.value = {
        ...task.value,
        goodQty: normalizedLength,
        scrapQty: normalizedLossLength,
      };
      emit('taskChange', { ...task.value });
    }
    await loadGuideClothRuntime();
    await loadEquipmentStatusCard();
    emit('refresh');
    quantityRevisionVisible.value = false;
    message.success('湿法母卷批次良品统计数据已修订');
  } finally {
    quantityRevisionSubmitting.value = false;
  }
}

async function openTimeLogModal() {
  if (!task.value?.operationReportId) {
    await syncTaskRuntimeFromBackend({ emitChange: false });
  }
  const reportId = Number(task.value?.operationReportId || 0);
  if (!reportId) {
    message.warning('未找到当前湿法报工记录，无法查看修改日志');
    return;
  }
  timeLogVisible.value = true;
  timeLogLoading.value = true;
  try {
    timeLogs.value = await getWetReportTimeLogs(reportId);
  } finally {
    timeLogLoading.value = false;
  }
}

async function loadPlanDetailContext() {
  if (!task.value?.planId) {
    planDetail.value = null;
    return;
  }
  try {
    planDetail.value = await getPlanOrderDetail(task.value.planId as any);
  } catch {
    planDetail.value = null;
  }
}

function initStateFromTask() {
  bookingDateTimeInputCache.value = {};
  const extra = parseWetExtraJson(task.value?.extraJson);
  const productionDate = task.value?.productionDate || '';
  const defaults = buildWetDefaultSheetGroups();
  wetFirstInspectionRecordId.value = undefined;
  abnormalPositionRows.value = [];
  abnormalPositionOption.value = normalizeAbnormalPositionOption(
    extra.abnormalPositionOption,
  );
  applyPoreSelfCheckRow();
  reportForm.value = {
    confirmerName: task.value?.confirmerName || '',
    confirmerTime: task.value?.confirmerTime || '',
    equipmentCode: task.value?.equipmentCode || '',
    equipmentId: task.value?.equipmentId,
    equipmentName: task.value?.equipmentName || '',
    endTime: task.value?.endTime || '',
    guideClothBatchNo: extra.guideClothBatchNo || '',
    guideClothChangeReason: extra.guideClothChangeReason || '',
    guideClothChanged: extra.guideClothChanged || 'N',
    guideClothCurrentUsedLength: extra.guideClothCurrentUsedLength,
    guideClothLimitLength: extra.guideClothLimitLength,
    guideClothReplaceTime: extra.guideClothReplaceTime || '',
    guideClothUseCount: extra.guideClothUseCount,
    guideClothUsedLength: extra.guideClothUsedLength,
    guideClothWarningFlag: extra.guideClothWarningFlag || 0,
    guideClothWarningText: '',
    inWashTime: normalizeReportDateTimeText(extra.inWashTime, productionDate),
    inOvenTime: normalizeReportDateTimeText(extra.inOvenTime, productionDate),
    inSolidifyTime: normalizeReportDateTimeText(
      extra.inSolidifyTime,
      productionDate,
    ),
    napSampleLength: extra.napSampleLength ?? task.value?.napSampleLength,
    outWashTime: normalizeReportDateTimeText(extra.outWashTime, productionDate),
    outOvenTime: normalizeReportDateTimeText(extra.outOvenTime, productionDate),
    outSolidifyTime: normalizeReportDateTimeText(
      extra.outSolidifyTime,
      productionDate,
    ),
    petBatchNo: extra.petBatchNo || '',
    petModel: extra.petModel || '',
    printSampleLength: extra.printLossLength,
    productionDate,
    receiveLength: extra.receiveLength ?? task.value?.goodQty ?? undefined,
    recorderName: task.value?.recorderName || '',
    recorderTime: task.value?.recorderTime || '',
    remark: task.value?.reportRemark || '',
    startTime: task.value?.startTime || '',
  };
  workPrepareRows.value = defaults.prepare;
  productionCheckRows.value = defaults.productionCheck;
  semiFinishedRows.value = defaults.semiFinished;
  applyDefaultWetFormFilters();
  firstInspection.value = {
    faiId: task.value?.faiId,
    faiNo: task.value?.faiNo || '',
    applyTime: task.value?.faiApplyTime || '',
    inspectionDesc: '',
    inspectTime: '',
    inspector: '',
    judgment: task.value?.faiJudgment || 'PENDING',
    napSampleLength: normalizeOptionalNumber(task.value?.napSampleLength),
    productBatchNo: task.value?.productionBatchNo || task.value?.batchNo || '',
    result: '',
    returnTime: task.value?.faiReturnTime || '',
    standardText: task.value?.faiStandardNo || '',
    status: task.value?.faiStatus || 'PENDING',
    isCurrent: true,
  };
  firstInspectionRecords.value = [firstInspection.value];
}

function isStarted() {
  return (
    !!reportForm.value.startTime ||
    task.value?.status === 'COMPLETED' ||
    task.value?.status === 'IN_PROGRESS'
  );
}

function getOperationStatusMeta(status?: string) {
  if (status === 'FINISHED') return { color: 'success', text: '已完工' };
  if (status === 'RUNNING') return { color: 'processing', text: '生产中' };
  if (status === 'RELEASED') return { color: 'default', text: '待开工' };
  if (status === 'PAUSED') return { color: 'warning', text: '已暂停' };
  if (status === 'CANCELLED') return { color: 'error', text: '已取消' };
  return { color: 'default', text: status || '未开始' };
}

function getStatusMeta(status?: string) {
  if (status === 'COMPLETED') return { color: 'success', text: '已完成' };
  if (status === 'RECORDED') return { color: 'processing', text: '已记录' };
  if (status === 'CONFIRMED') return { color: 'success', text: '已确认' };
  if (status === 'WAITING') return { color: 'processing', text: '待结果' };
  if (status === 'PENDING') return { color: 'default', text: '待检测' };
  if (status === 'INSPECTING') return { color: 'processing', text: '检测中' };
  if (status === 'WAITING_QA') return { color: 'warning', text: '待品质复核' };
  if (status === 'SUSPENDED') return { color: 'warning', text: '已挂起' };
  if (status === 'REWORKING') return { color: 'error', text: '调机中' };
  if (status === 'REJECTED') return { color: 'error', text: '不合格' };
  if (status === 'CANCELED') return { color: 'default', text: '已取消' };
  return { color: 'default', text: '待处理' };
}

function resolveFirstInspectionDisplayText(status?: string, judgment?: string) {
  if (status === 'COMPLETED' && judgment === 'OK') return '已完成';
  if (status === 'REJECTED' && judgment === 'NG')
    return '首检不合格，需重新提交首件检验';
  if (status === 'REJECTED') return '不合格';
  if (status === 'INSPECTING') return '检测中';
  if (status === 'WAITING_QA') return '待品质复核';
  if (status === 'SUSPENDED') return '已挂起';
  if (status === 'REWORKING') return '调机中';
  if (status === 'CANCELED') return '已取消';
  if (status === 'PENDING') return '待检测';
  if (judgment === 'NG') return '首检不合格，需重新提交首件检验';
  return '未提交';
}

function getEquipmentWorkStatusMeta(status?: string) {
  if (status === 'PRODUCING') return { color: 'processing', text: '生产中' };
  if (status === 'MAINTENANCE') return { color: 'warning', text: '检修' };
  if (status === 'FAULT') return { color: 'error', text: '故障' };
  return { color: 'default', text: '待机' };
}

function getDetailFieldRowSpan(
  details: WetSheetDetail[] = [],
  index: number,
  field: 'category' | 'node',
) {
  const current = details[index];
  if (!current?.[field]) return 0;
  if (index > 0 && details[index - 1]?.[field] === current[field]) return 0;
  let span = 1;
  for (let cursor = index + 1; cursor < details.length; cursor += 1) {
    if (details[cursor]?.[field] === current[field]) span += 1;
    else break;
  }
  return span;
}

function resolvePopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}

function buildSheetCellKey(rowIndex: number, field: string) {
  return `${recordDialogCategory.value}:${rowIndex}:${field}`;
}

function setSheetCellRef(rowIndex: number, field: string, el: any) {
  const key = buildSheetCellKey(rowIndex, field);
  if (el) sheetCellRefs.set(key, el as HTMLElement);
  else sheetCellRefs.delete(key);
}

function getEditableSheetFields(category: RecordCategory) {
  if (category === 'production-check') return ['value', 'result', 'remark'];
  if (category === 'semi-finished')
    return ['length', 'item', 'standard', 'value', 'node', 'remark'];
  return ['value', 'result', 'remark'];
}

function focusSheetCell(rowIndex: number, field: string) {
  nextTick(() => {
    const root = sheetCellRefs.get(buildSheetCellKey(rowIndex, field));
    if (!root) return;
    const target =
      (root.querySelector('input:not([disabled])') as HTMLElement | null) ||
      (root.querySelector('textarea:not([disabled])') as HTMLElement | null) ||
      (root.querySelector('.ant-select-selector') as HTMLElement | null) ||
      (root.querySelector(
        '.ant-picker-input input:not([disabled])',
      ) as HTMLElement | null) ||
      (root.querySelector(
        '.ant-radio-wrapper-checked input',
      ) as HTMLElement | null) ||
      (root.querySelector('.ant-radio-wrapper input') as HTMLElement | null);
    target?.focus?.();
    if (target instanceof HTMLInputElement) target.select?.();
  });
}

function moveSheetFocus(
  rowIndex: number,
  field: string,
  direction: 'down' | 'up',
) {
  const rows = currentRecord.value?.details || [];
  const fields = getEditableSheetFields(recordDialogCategory.value);
  const fieldIndex = fields.indexOf(field);
  if (fieldIndex < 0) return;
  let targetRowIndex = rowIndex + (direction === 'down' ? 1 : -1);
  while (targetRowIndex >= 0 && targetRowIndex < rows.length) {
    focusSheetCell(targetRowIndex, field);
    return;
  }
}

function handleSheetCellKeydown(
  event: KeyboardEvent,
  rowIndex: number,
  field: string,
) {
  if (recordDialogMode.value === 'view') return;
  if (event.key === 'Enter') {
    event.preventDefault();
    moveSheetFocus(rowIndex, field, event.shiftKey ? 'up' : 'down');
    return;
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    moveSheetFocus(rowIndex, field, 'down');
    return;
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault();
    moveSheetFocus(rowIndex, field, 'up');
  }
}

const displayStatus = computed(() => {
  if (task.value?.status === 'COMPLETED')
    return { color: 'success', text: '已完工' };
  if (task.value?.status === 'IN_PROGRESS')
    return { color: 'processing', text: '生产中' };
  if (task.value?.status === 'RELEASED')
    return { color: 'processing', text: '已下达' };
  return { color: 'default', text: '待开工' };
});

const canPrintTransferTicket = computed(
  () => task.value?.status === 'COMPLETED',
);
const canConfirmReport = computed(
  () => !!task.value?.operationReportId && task.value?.status === 'COMPLETED',
);
const canReviseReportQuantity = computed(
  () => !!task.value?.operationReportId && task.value?.status === 'COMPLETED',
);
const authActionName = computed(() => {
  if (pendingAction.value === 'START') return '湿法工单开工确认';
  if (pendingAction.value === 'FINISH') return '湿法节点完工与报工';
  if (pendingAction.value === 'SUBMIT_BOOKING') return '湿法报工提交认证';
  if (pendingAction.value === 'CONFIRM_REPORT') return '湿法报工确认';
  return '湿法认证';
});

const currentMachineCode = computed(
  () =>
    reportForm.value.equipmentCode ||
    task.value?.equipmentCode ||
    task.value?.equipmentName ||
    task.value?.deviceId ||
    '-',
);

const filteredProductionCheckRows = computed(() =>
  productionCheckRows.value.filter(
    (record) =>
      matchWetCurrentModelScopedForm(record) &&
      matchWetFormNameFilter(record, productionCheckFormNameFilter.value),
  ),
);

const filteredSemiFinishedRows = computed(() =>
  semiFinishedRows.value.filter(
    (record) =>
      matchWetCurrentModelScopedForm(record) &&
      matchWetFormNameFilter(record, semiFinishedFormNameFilter.value),
  ),
);

watch(
  () => reportForm.value.equipmentId,
  (val) => {
    const matched = equipmentOptions.value.find(
      (item: any) => item.value === val,
    );
    if (!matched) return;
    reportForm.value.equipmentCode = matched.code || '';
    reportForm.value.equipmentName = matched.name || matched.label || '';
    void loadGuideClothRuntime();
  },
);

watch(
  () => reportForm.value.guideClothChanged,
  () => {
    applyGuideClothUseCountByFlag();
  },
);

const guideClothProjectedUseCount = computed(() => {
  if (
    task.value?.status === 'COMPLETED' &&
    reportForm.value.guideClothUseCount !== undefined
  ) {
    return toNumber(reportForm.value.guideClothUseCount);
  }
  if (reportForm.value.guideClothChanged === 'Y') {
    return 1;
  }
  return toNumber(
    reportForm.value.guideClothUseCount ?? guideClothBaseNextUseCount.value,
    1,
  );
});

const guideClothValidReplaceTime = computed(() =>
  normalizeValidBusinessDateTime(
    guideClothRuntime.value?.replaceTime ||
      reportForm.value.guideClothReplaceTime,
  ),
);

const guideClothLastReplaceTimeText = computed(
  () => guideClothValidReplaceTime.value || '未记录',
);

const guideClothNextReplaceDate = computed(() => {
  const replaceTime = guideClothValidReplaceTime.value;
  if (!replaceTime) return null;
  const replaceDate = dayjs(replaceTime);
  return replaceDate.isValid() ? replaceDate.add(1, 'month') : null;
});

const guideClothReminderStartDate = computed(() => {
  const nextReplaceDate = guideClothNextReplaceDate.value;
  return nextReplaceDate
    ? nextReplaceDate.subtract(GUIDE_CLOTH_MONTH_REMIND_BEFORE_DAYS, 'day')
    : null;
});

const guideClothMonthlyReminderDue = computed(() => {
  if (reportForm.value.guideClothChanged === 'Y') return false;
  const reminderStartDate = guideClothReminderStartDate.value;
  if (!reminderStartDate) return true;
  const reportDate = dayjs(
    reportForm.value.productionDate || task.value?.productionDate || dayjs(),
  ).startOf('day');
  return !reportDate.isBefore(reminderStartDate.startOf('day'));
});

const guideClothMonthlyStatusText = computed(() => {
  if (reportForm.value.guideClothChanged === 'Y') return '本次登记更换';
  if (!guideClothNextReplaceDate.value || !guideClothReminderStartDate.value) {
    return '未记录更换时间';
  }
  const nextDateText = normalizeDate(
    guideClothNextReplaceDate.value.format('YYYY-MM-DD'),
  );
  const reminderStartText = normalizeDate(
    guideClothReminderStartDate.value.format('YYYY-MM-DD'),
  );
  return guideClothMonthlyReminderDue.value
    ? `已进入提醒期，${nextDateText} 前需更换`
    : `未到提醒期，${reminderStartText} 起提醒`;
});

const guideClothWarningFlag = computed(() => {
  if (reportForm.value.guideClothChanged === 'Y') return false;
  return (
    guideClothProjectedUseCount.value >= GUIDE_CLOTH_MAX_USE_COUNT ||
    guideClothMonthlyReminderDue.value
  );
});

const guideClothWarningText = computed(() => {
  if (reportForm.value.guideClothChanged === 'Y') {
    return '本次登记导布更换，提交后累计使用次数从 1/28 开始。';
  }
  if (guideClothProjectedUseCount.value >= GUIDE_CLOTH_MAX_USE_COUNT) {
    return `导布累计次数将达到 ${guideClothProjectedUseCount.value}/${GUIDE_CLOTH_MAX_USE_COUNT}，请先更换导布。`;
  }
  if (!guideClothValidReplaceTime.value) {
    return '未记录有效的导布更换时间，请先登记导布更换。';
  }
  if (guideClothMonthlyReminderDue.value) {
    const nextDateText = guideClothNextReplaceDate.value
      ? normalizeDate(guideClothNextReplaceDate.value.format('YYYY-MM-DD'))
      : '未记录';
    return `导布每月需更换一次，上次更换时间：${guideClothLastReplaceTimeText.value}，下次更换日期：${nextDateText}，请先更换导布。`;
  }
  return `导布正常，预计累计使用 ${guideClothProjectedUseCount.value}/${GUIDE_CLOTH_MAX_USE_COUNT} 次。`;
});

const reportView = computed(() => ({
  ...reportForm.value,
  confirmerTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.confirmerTime,
  ),
  endTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.endTime,
  ),
  inWashTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.inWashTime,
  ),
  inOvenTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.inOvenTime,
  ),
  inSolidifyTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.inSolidifyTime,
  ),
  outWashTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.outWashTime,
  ),
  outOvenTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.outOvenTime,
  ),
  outSolidifyTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.outSolidifyTime,
  ),
  productionDateDisplay: normalizeDate(reportForm.value.productionDate),
  recorderTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.recorderTime,
  ),
  startTimeDisplay: normalizeBizDateTime(
    reportForm.value.productionDate,
    reportForm.value.startTime,
  ),
}));

interface WetHeaderFieldConfig {
  key: WetHeaderFieldKey;
  label: string;
  type: WetHeaderFieldType;
  value?: string;
}

const WET_PROCESS_RUNTIME_HEADER_KEYS: WetHeaderFieldKey[] = [
  'startTime',
  'endTime',
  'inWashTime',
  'outWashTime',
  'inSolidifyTime',
  'outSolidifyTime',
  'inOvenTime',
  'outOvenTime',
];

const WET_PRODUCTION_CHECK_INDEPENDENT_TIME_KEYS = new Set<WetHeaderFieldKey>([
  'startTime',
  'endTime',
  'inWashTime',
  'outWashTime',
  'inSolidifyTime',
  'outSolidifyTime',
  'inOvenTime',
  'outOvenTime',
]);

const SUPPORTED_WET_HEADER_FIELD_KEYS = new Set<WetHeaderFieldKey>([
  'batchNo',
  'confirmer',
  'confirmerTime',
  'finalResult',
  'generatedLength',
  'inOvenTime',
  'inSolidifyTime',
  'inWashTime',
  'machine',
  'materialCode',
  'modelCode',
  'outOvenTime',
  'outSolidifyTime',
  'outWashTime',
  'poreDevelopment',
  'productionDate',
  'recorder',
  'recorderTime',
  'semiWidth',
  'startTime',
  'endTime',
]);

const WET_PRODUCTION_CHECK_EXCEL_START_TIME_FALLBACK_KEYS =
  new Set<WetHeaderFieldKey>([
    'endTime',
    'inWashTime',
    'outWashTime',
    'inSolidifyTime',
    'outSolidifyTime',
    'inOvenTime',
    'outOvenTime',
  ]);

const WET_PRODUCTION_CHECK_EXCEL_IMPORT_KEYS = new Set<WetHeaderFieldKey>([
  'startTime',
  'endTime',
  'inWashTime',
  'outWashTime',
  'inSolidifyTime',
  'outSolidifyTime',
  'inOvenTime',
  'outOvenTime',
]);

const WET_SEMI_FINISHED_EXCEL_IMPORT_KEYS = new Set<WetHeaderFieldKey>([
  'generatedLength',
  'semiWidth',
  'poreDevelopment',
  'finalResult',
]);

const REQUIRED_PRODUCTION_CHECK_HEADER_FIELDS: WetHeaderFieldKey[] = [
  'materialCode',
  'modelCode',
  'machine',
  'batchNo',
  'startTime',
  'endTime',
  'inWashTime',
  'outWashTime',
  'inSolidifyTime',
  'outSolidifyTime',
  'inOvenTime',
  'outOvenTime',
];

const REQUIRED_SEMI_HEADER_FIELDS: WetHeaderFieldKey[] = [
  'productionDate',
  'materialCode',
  'modelCode',
  'machine',
  'batchNo',
  'generatedLength',
  'semiWidth',
  'poreDevelopment',
  'finalResult',
];

function normalizeHeaderFieldKeys(
  schemaJson: string | null | undefined,
  fallbackType: 'production-check' | 'semi-finished',
) {
  const configuredFields = resolveWetHeaderFields(schemaJson, fallbackType)
    .map((key) => String(key || '').trim())
    .filter((key): key is WetHeaderFieldKey =>
      SUPPORTED_WET_HEADER_FIELD_KEYS.has(key as WetHeaderFieldKey),
    );
  if (configuredFields.length > 0) {
    return [...new Set(configuredFields)];
  }
  return fallbackType === 'semi-finished'
    ? REQUIRED_SEMI_HEADER_FIELDS
    : REQUIRED_PRODUCTION_CHECK_HEADER_FIELDS;
}

function resolveHeaderGridClass(layout?: string) {
  switch (layout) {
    case 'GRID_2':
      return 'production-check-head-grid--cols-2';
    case 'GRID_3':
      return 'production-check-head-grid--cols-3';
    case 'TOP_FIELDS':
      return 'production-check-head-grid--cols-top';
    default:
      return 'production-check-head-grid--cols-4';
  }
}

function isWetProcessRuntimeHeaderKey(key: WetHeaderFieldKey) {
  return (WET_PROCESS_RUNTIME_HEADER_KEYS as string[]).includes(key);
}

function isWetProductionCheckIndependentTimeKey(key: WetHeaderFieldKey) {
  return WET_PRODUCTION_CHECK_INDEPENDENT_TIME_KEYS.has(key);
}

function normalizeProductionCheckHeaderDateTime(value: unknown) {
  return normalizeReportDateTimeText(
    typeof value === 'number' || typeof value === 'string' ? value : undefined,
    reportForm.value.productionDate || task.value?.productionDate,
  );
}

function getStoredProductionCheckHeaderDateValue(
  key: WetHeaderFieldKey,
  record: WetSheetRow | null | undefined = currentRecord.value,
) {
  return normalizeProductionCheckHeaderDateTime(
    record?.importedHeaderData?.[key],
  );
}

function getProductionCheckHeaderDateValue(
  key: WetHeaderFieldKey,
  record: WetSheetRow | null | undefined = currentRecord.value,
) {
  if (!isWetProductionCheckIndependentTimeKey(key)) return '';
  const persistedValue = getStoredProductionCheckHeaderDateValue(key, record);
  if (persistedValue) return persistedValue;
  return normalizeProductionCheckHeaderDateTime(getReportHeaderDateValue(key));
}

function setProductionCheckHeaderDateValue(
  key: WetHeaderFieldKey,
  value: unknown,
) {
  if (!isWetProductionCheckIndependentTimeKey(key)) return;
  const record = currentRecord.value;
  if (!record) return;
  const normalized = normalizeProductionCheckHeaderDateTime(value);
  record.importedHeaderData = {
    ...(record.importedHeaderData || {}),
    [key]: normalized,
  };
}

function getProcessHeaderPersistValue(
  key: WetHeaderFieldKey,
  record?: WetSheetRow | null,
) {
  if (!record) return undefined;
  if (isWetProductionCheckIndependentTimeKey(key)) {
    return getProductionCheckHeaderDateValue(key, record);
  }
  return getProductionCheckSnapshotValue(key, record);
}

function getProductionCheckSnapshotValue(
  key: WetHeaderFieldKey,
  record: WetSheetRow | null | undefined = currentRecord.value,
) {
  const persisted = record?.importedHeaderData?.[key];
  if (
    persisted !== undefined &&
    persisted !== null &&
    String(persisted).trim()
  ) {
    return String(persisted).trim();
  }
  switch (key) {
    case 'batchNo':
      return task.value?.productionBatchNo || task.value?.batchNo || '';
    case 'machine':
      return currentMachineCode.value || '';
    case 'materialCode':
      return task.value?.motherMaterialCode || task.value?.materialCode || '';
    case 'modelCode':
      return task.value?.motherModelCode || task.value?.modelCode || '';
    case 'productionDate':
      return (
        reportForm.value.productionDate || task.value?.productionDate || ''
      );
    default:
      return '';
  }
}

function getSemiHeaderPersistValue(
  key: WetHeaderFieldKey,
  record?: WetSheetRow | null,
) {
  if (!record) return undefined;
  switch (key) {
    case 'finalResult':
      return record.finalResult || '';
    case 'generatedLength':
      return record.generatedLength ?? '';
    case 'poreDevelopment':
      return record.poreDevelopment || '';
    case 'semiWidth':
      return record.semiWidth || '';
    default:
      return undefined;
  }
}

function getReportHeaderDateValue(key: WetHeaderFieldKey) {
  return String(
    (reportForm.value as Record<string, string | number | undefined>)[key] ||
      '',
  );
}

function setReportHeaderDateValue(key: WetHeaderFieldKey, value: unknown) {
  const text = dayjs.isDayjs(value)
    ? value.format('YYYY-MM-DD HH:mm:ss')
    : String(value || '');
  (reportForm.value as Record<string, string | number | undefined>)[key] = text;
}

function buildHeaderDataBySchema(
  record: WetSheetRow,
  category: RecordCategory,
) {
  const sourceRecord = currentRecord.value || record;
  const keys = normalizeHeaderFieldKeys(
    record.schemaJson,
    category === 'semi-finished' ? 'semi-finished' : 'production-check',
  );
  const headerData: Record<string, any> =
    category === 'production-check'
      ? Object.fromEntries(
          keys
            .filter(
              (key) => sourceRecord.importedHeaderData?.[key] !== undefined,
            )
            .map((key) => [key, sourceRecord.importedHeaderData?.[key]]),
        )
      : { ...(sourceRecord.importedHeaderData || {}) };
  keys.forEach((key) => {
    const value =
      category === 'semi-finished'
        ? getSemiHeaderPersistValue(key, sourceRecord)
        : getProcessHeaderPersistValue(key, sourceRecord);
    if (value !== undefined) {
      headerData[key] = value;
    }
  });
  return headerData;
}

function getWetHeaderFieldValue(
  key: WetHeaderFieldKey,
  category: 'production-check' | 'semi-finished',
) {
  const schema = getWetSheetSchemaMeta(currentRecord.value?.schemaJson);
  const isSolidify =
    category === 'semi-finished' && currentRecord.value?.name?.includes('凝固');
  const fieldMap: Record<WetHeaderFieldKey, WetHeaderFieldConfig> = {
    batchNo: {
      key: 'batchNo',
      label:
        category === 'semi-finished'
          ? '产品批号：'
          : getWetProductionCheckHeaderLabel('batchNo', '母批批号'),
      type: 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckSnapshotValue('batchNo') || '-'
          : task.value?.productionBatchNo || task.value?.batchNo || '-',
    },
    confirmer: {
      key: 'confirmer',
      label: '确认人：',
      type: 'readonly',
      value:
        category === 'semi-finished'
          ? currentRecord.value?.confirmer ||
            reportForm.value.confirmerName ||
            '-'
          : reportForm.value.confirmerName || '-',
    },
    confirmerTime: {
      key: 'confirmerTime',
      label: '确认时间：',
      type: 'readonly',
      value:
        category === 'semi-finished'
          ? normalizeDateTime(currentRecord.value?.confirmerTime) ||
            reportView.value.confirmerTimeDisplay ||
            '-'
          : reportView.value.confirmerTimeDisplay || '-',
    },
    finalResult: {
      key: 'finalResult',
      label:
        category === 'semi-finished'
          ? isSolidify
            ? '综合判定：'
            : '判定：'
          : '判定：',
      type: 'radio',
      value: currentRecord.value?.finalResult || '-',
    },
    generatedLength: {
      key: 'generatedLength',
      label: '半成品长度/m：',
      type: 'input',
      value: currentRecord.value?.generatedLength
        ? String(currentRecord.value.generatedLength)
        : '-',
    },
    inOvenTime: {
      key: 'inOvenTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('inOvenTime', '入烘箱时间')
          : '入烘箱时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('inOvenTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('inOvenTime') || '-'
          : reportView.value.inOvenTimeDisplay || '-',
    },
    inSolidifyTime: {
      key: 'inSolidifyTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('inSolidifyTime', '入凝固槽时间')
          : '入凝固槽时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('inSolidifyTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('inSolidifyTime') || '-'
          : reportView.value.inSolidifyTimeDisplay || '-',
    },
    inWashTime: {
      key: 'inWashTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('inWashTime', '入水洗槽时间')
          : '入水洗槽时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('inWashTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('inWashTime') || '-'
          : reportView.value.inWashTimeDisplay || '-',
    },
    machine: {
      key: 'machine',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('machine', '机台编号')
          : '机台编号：',
      type: 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckSnapshotValue('machine') || '-'
          : currentMachineCode.value || '-',
    },
    materialCode: {
      key: 'materialCode',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('materialCode', '产品料号')
          : '产品料号：',
      type: 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckSnapshotValue('materialCode') || '-'
          : task.value?.motherMaterialCode || task.value?.materialCode || '-',
    },
    modelCode: {
      key: 'modelCode',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('modelCode', '产品型号')
          : '产品型号：',
      type: 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckSnapshotValue('modelCode') || '-'
          : task.value?.motherModelCode || task.value?.modelCode || '-',
    },
    outOvenTime: {
      key: 'outOvenTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('outOvenTime', '出烘箱时间')
          : '出烘箱时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('outOvenTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('outOvenTime') || '-'
          : reportView.value.outOvenTimeDisplay || '-',
    },
    outSolidifyTime: {
      key: 'outSolidifyTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('outSolidifyTime', '出凝固槽时间')
          : '出凝固槽时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('outSolidifyTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('outSolidifyTime') || '-'
          : reportView.value.outSolidifyTimeDisplay || '-',
    },
    outWashTime: {
      key: 'outWashTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('outWashTime', '出水洗槽时间')
          : '出水洗槽时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('outWashTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('outWashTime') || '-'
          : reportView.value.outWashTimeDisplay || '-',
    },
    poreDevelopment: {
      key: 'poreDevelopment',
      label: '泡孔发育：',
      type: 'radio',
      value: currentRecord.value?.poreDevelopment || '-',
    },
    productionDate: {
      key: 'productionDate',
      label: '生产日期：',
      type: 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckSnapshotValue('productionDate') || '-'
          : reportView.value.productionDateDisplay || '-',
    },
    recorder: {
      key: 'recorder',
      label: '记录人：',
      type: 'readonly',
      value:
        category === 'semi-finished'
          ? currentRecord.value?.recorder ||
            reportForm.value.recorderName ||
            '-'
          : reportForm.value.recorderName || '-',
    },
    recorderTime: {
      key: 'recorderTime',
      label: '记录时间：',
      type: 'readonly',
      value:
        category === 'semi-finished'
          ? normalizeDateTime(currentRecord.value?.recorderTime) ||
            reportView.value.recorderTimeDisplay ||
            '-'
          : reportView.value.recorderTimeDisplay || '-',
    },
    semiWidth: {
      key: 'semiWidth',
      label: schema.semiWidthLabel
        ? `${schema.semiWidthLabel}：`
        : isSolidify
          ? '出槽宽幅/m(xxm)：'
          : '宽幅/m(XXm)：',
      type: 'input',
      value: currentRecord.value?.semiWidth || '-',
    },
    startTime: {
      key: 'startTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('startTime', '投料开始时间')
          : '投料开始时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('startTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('startTime') || '-'
          : reportView.value.startTimeDisplay || '-',
    },
    endTime: {
      key: 'endTime',
      label:
        category === 'production-check'
          ? getWetProductionCheckHeaderLabel('endTime', '投料结束时间')
          : '投料结束时间：',
      type:
        category === 'production-check' &&
        isWetProductionCheckHeaderEditable('endTime')
          ? 'datetime'
          : 'readonly',
      value:
        category === 'production-check'
          ? getProductionCheckHeaderDateValue('endTime') || '-'
          : reportView.value.endTimeDisplay || '-',
    },
  };
  return fieldMap[key] || null;
}

function buildProductionCheckHeaderField(
  key: WetHeaderFieldKey,
): WetHeaderFieldConfig | null {
  return getWetHeaderFieldValue(key, 'production-check');
}

function buildSemiFinishedHeaderField(
  key: WetHeaderFieldKey,
): WetHeaderFieldConfig | null {
  return getWetHeaderFieldValue(key, 'semi-finished');
}

function buildRecordDialogTopField(
  key: string,
  category: 'prepare' | 'production-check' | 'semi-finished',
): { field: string; label: string; value: string } | null {
  if (category === 'prepare') {
    const fieldMap: Record<
      string,
      { field: string; label: string; value: string }
    > = {
      productionDate: {
        field: 'productionDate',
        label: '生产日期',
        value: reportView.value.productionDateDisplay || '-',
      },
      materialCode: {
        field: 'materialCode',
        label: '产品料号',
        value:
          task.value?.motherMaterialCode || task.value?.materialCode || '-',
      },
      modelCode: {
        field: 'modelCode',
        label: '产品型号',
        value: task.value?.motherModelCode || task.value?.modelCode || '-',
      },
      confirmer: {
        field: 'confirmer',
        label: '确认人',
        value: currentRecord.value?.confirmer || '-',
      },
      confirmerTime: {
        field: 'confirmerTime',
        label: '确认时间',
        value: normalizeDateTime(currentRecord.value?.confirmerTime) || '-',
      },
      machine: {
        field: 'machine',
        label: '机台编号',
        value: currentMachineCode.value,
      },
      recorder: {
        field: 'recorder',
        label: '记录人',
        value: currentRecord.value?.recorder || '-',
      },
      recorderTime: {
        field: 'recorderTime',
        label: '记录时间',
        value: normalizeDateTime(currentRecord.value?.recorderTime) || '-',
      },
    };
    return fieldMap[key] || null;
  }
  const config = getWetHeaderFieldValue(key as WetHeaderFieldKey, category);
  if (!config) return null;
  return {
    field: config.key,
    label: config.label.replace(/：$/, ''),
    value: config.value || '-',
  };
}

const productionCheckHeaderClass = computed(
  () =>
    `production-check-head-grid ${resolveHeaderGridClass(resolveWetHeaderLayout(currentRecord.value?.schemaJson || productionCheckRows.value[0]?.schemaJson, 'GRID_4'))}`,
);

const prepareHeaderClass = computed(
  () => 'production-check-head-grid production-check-head-grid--cols-4',
);

const productionCheckHeadFields = computed(
  () =>
    normalizeHeaderFieldKeys(
      currentRecord.value?.schemaJson ||
        productionCheckRows.value[0]?.schemaJson,
      'production-check',
    )
      .map((key) => buildProductionCheckHeaderField(key))
      .filter(Boolean) as WetHeaderFieldConfig[],
);

function buildPreviousOperationFromTask(): MesHcPlanOrderApi.Operation | null {
  const row = task.value;
  if (!row) return null;
  const processName = String(
    row.process || row.operationName || row.currentOperationName || '',
  );
  const inferredPreviousOperationName =
    row.previousOperationName || (processName.includes('湿法') ? '配料' : '');
  const hasPrevious =
    inferredPreviousOperationName ||
    row.previousOperationStatus ||
    row.previousProductionDate ||
    row.previousStartTime ||
    row.previousEndTime ||
    row.previousRecorderName ||
    row.previousGoodQty !== undefined;
  if (!hasPrevious) return null;
  return {
    latestEndTime: row.previousEndTime,
    latestRecorderName: row.previousRecorderName,
    latestReportDate: row.previousProductionDate,
    latestStartTime: row.previousStartTime,
    opName: inferredPreviousOperationName,
    operationStatus:
      row.previousOperationStatus ||
      (inferredPreviousOperationName ? 'PENDING' : undefined),
    requiredQty: row.previousGoodQty,
  };
}

function mergePreviousOperationFallback(
  operation?: MesHcPlanOrderApi.Operation | null,
  fallback?: MesHcPlanOrderApi.Operation | null,
): MesHcPlanOrderApi.Operation | null {
  if (!operation) return fallback || null;
  if (!fallback) return operation;
  return {
    ...fallback,
    ...operation,
    latestEndTime: operation.latestEndTime || fallback.latestEndTime,
    latestRecorderName:
      operation.latestRecorderName || fallback.latestRecorderName,
    latestReportDate: operation.latestReportDate || fallback.latestReportDate,
    latestStartTime: operation.latestStartTime || fallback.latestStartTime,
    opName: operation.opName || fallback.opName,
    operationStatus: operation.operationStatus || fallback.operationStatus,
  };
}

const previousOperation = computed(() => {
  const operations = planDetail.value?.operations || [];
  const currentOperationId = task.value?.planOperationId;
  const fallback = buildPreviousOperationFromTask();
  if (!operations.length || !currentOperationId) return fallback;
  const sorted = [...operations].sort(
    (a, b) => (a.opSeq || 0) - (b.opSeq || 0),
  );
  const currentIndex = sorted.findIndex(
    (item) => item.id === currentOperationId,
  );
  if (currentIndex <= 0) return fallback;
  return mergePreviousOperationFallback(
    sorted[currentIndex - 1] || null,
    fallback,
  );
});

function hasOperationReported(operation?: MesHcPlanOrderApi.Operation | null) {
  if (!operation) return false;
  if (operation.latestEndTime) return true;
  return Object.values(operation.reportQtyByDate || {}).some(
    (value) => Number(value || 0) > 0,
  );
}

const previousOperationInfo = computed(() => {
  const operation = previousOperation.value;
  if (!operation) return null;
  const backendQtime = task.value?.qtime;
  if (backendQtime) {
    return {
      qtime: {
        durationText: backendQtimeDurationText(backendQtime),
        levelClass: resolveBackendQtimeLevelClass(backendQtime),
      },
    };
  }
  const previousEndTime = parseQtimeDateTime(
    operation.latestEndTime,
    operation.latestReportDate ||
      reportForm.value.productionDate ||
      task.value?.productionDate,
  );
  const currentStartTimeText = resolvePoreSelfCheckStartTimeText();
  const currentStartTime = currentStartTimeText
    ? parseQtimeDateTime(
        currentStartTimeText,
        reportForm.value.productionDate || task.value?.productionDate,
      )
    : null;
  const nowTime = dayjs(qtimeNowTimestamp.value);
  const qtimeTargetTime = currentStartTime || nowTime;
  const qtimeMinutes = previousEndTime
    ? qtimeTargetTime.diff(previousEndTime, 'minute')
    : null;
  return {
    qtime: {
      durationText: previousEndTime
        ? formatQtimeDuration(qtimeMinutes)
        : '前序未完工',
      levelClass: resolveQtimeLevelClass(qtimeMinutes),
    },
  };
});

const recordDialogTitle = computed(
  () =>
    `${recordDialogMode.value === 'edit' ? '填写' : recordDialogMode.value === 'confirm' ? '确认' : '查看'}${getWetPassWorkDisplayName(currentRecord.value) || '记录单'}`,
);
const recordSaveAuthActionName = computed(
  () => `保存${getWetPassWorkDisplayName(currentRecord.value) || '记录单'}`,
);
const recordConfirmAuthActionName = computed(
  () => `确认${getWetPassWorkDisplayName(currentRecord.value) || '记录单'}`,
);

const recordDialogTopFields = computed(() => {
  const record = currentRecord.value;
  if (!record) return [];
  if (recordDialogCategory.value === 'prepare') {
    return [
      'productionDate',
      'materialCode',
      'modelCode',
      'machine',
      'recorder',
      'recorderTime',
      'confirmer',
      'confirmerTime',
    ]
      .map((key) =>
        buildRecordDialogTopField(key as WetHeaderFieldKey, 'prepare'),
      )
      .filter(Boolean) as Array<{
      field: string;
      label: string;
      value: string;
    }>;
  }
  if (recordDialogCategory.value === 'production-check') {
    return normalizeHeaderFieldKeys(record.schemaJson, 'production-check')
      .map((key) => buildRecordDialogTopField(key, 'production-check'))
      .filter(Boolean) as Array<{
      field: string;
      label: string;
      value: string;
    }>;
  }
  return normalizeHeaderFieldKeys(record.schemaJson, 'semi-finished')
    .map((key) => buildRecordDialogTopField(key, 'semi-finished'))
    .filter(Boolean) as Array<{ field: string; label: string; value: string }>;
});

const prepareHeadFields = computed(() => {
  if (recordDialogCategory.value !== 'prepare') return [];
  return recordDialogTopFields.value;
});

const semiFinishedHeadFields = computed(() => {
  if (recordDialogCategory.value !== 'semi-finished' || !currentRecord.value)
    return [];
  const isSolidify = currentRecord.value.name?.includes('凝固');
  const schemaJson = currentRecord.value.schemaJson;
  const fields = normalizeHeaderFieldKeys(schemaJson, 'semi-finished')
    .map((key) => {
      if (!isSolidify && key === 'poreDevelopment') return null;
      return buildSemiFinishedHeaderField(key);
    })
    .filter(Boolean) as WetHeaderFieldConfig[];
  return fields;
});

const semiFinishedHeaderClass = computed(() => {
  const layout = currentRecord.value?.name?.includes('烘箱')
    ? 'GRID_4'
    : resolveWetHeaderLayout(currentRecord.value?.schemaJson, 'GRID_3');
  return `production-check-head-grid ${resolveHeaderGridClass(layout)}`;
});

const semiFinishedThicknessHeaders = computed(() => {
  const schema = getWetSheetSchemaMeta(currentRecord.value?.schemaJson);
  if (schema.thicknessLabels && schema.thicknessLabels.length >= 4) {
    return schema.thicknessLabels.slice(0, 4);
  }
  const position = currentRecord.value?.name?.includes('凝固')
    ? '出槽'
    : '出箱';
  return [
    `左侧10cm${position}厚度/mm`,
    `左侧20cm${position}厚度/mm`,
    `右侧10cm${position}厚度/mm`,
    `右侧20cm${position}厚度/mm`,
  ];
});

const showRecordAttachmentPanel = computed(() => {
  if (!currentRecord.value) return false;
  if (
    recordDialogCategory.value !== 'production-check' &&
    recordDialogCategory.value !== 'semi-finished'
  ) {
    return false;
  }
  const schema = getWetSheetSchemaMeta(currentRecord.value.schemaJson);
  return schema.allowAttachment !== false;
});

const abnormalPositionOptionText = computed(() => {
  if (abnormalPositionOption.value === 'NONE') return '无';
  if (abnormalPositionOption.value === 'DETAIL') return '填写异常位置';
  return '未选择';
});

const recordDialogDetailRows = computed(() => {
  const details = currentRecord.value?.details || [];
  return details;
});

const semiDetailSummary = computed(() => {
  const record = currentRecord.value;
  const details = record?.details || [];
  if (recordDialogCategory.value !== 'semi-finished' || !details.length) {
    return '';
  }
  const firstLength = resolveSemiDetailLengthByIndex(
    record,
    details[0]!,
    0,
  );
  const lastLength = resolveSemiDetailLengthByIndex(
    record,
    details[details.length - 1]!,
    details.length - 1,
  );
  return `共 ${details.length} 行（${formatSemiLength(firstLength)}～${formatSemiLength(lastLength)}m）`;
});

const semiDetailConsistencyHint = computed(() => {
  const record = currentRecord.value;
  const details = record?.details || [];
  if (recordDialogCategory.value !== 'semi-finished' || !record || !details.length) {
    return '';
  }
  const generatedLength = normalizeSemiDetailLengthValue(record.generatedLength);
  if (generatedLength === undefined || generatedLength <= 0) return '';
  const rowStep = resolveSemiRowStep(record.schemaJson);
  const expectedRowCount = Math.ceil(generatedLength / rowStep);
  const lastLength = resolveSemiDetailLengthByIndex(
    record,
    details[details.length - 1]!,
    details.length - 1,
  );
  if (
    details.length === expectedRowCount &&
    Math.abs(lastLength - generatedLength) < 0.0001
  ) {
    return '';
  }
  return `表头长度 ${formatSemiLength(generatedLength)}m 与当前 ${details.length} 行明细不一致，请按当前长度重建明细。`;
});

function getDetailRenderRowIndex(index: number) {
  return index;
}

function normalizeSemiDetailLengthValue(value: unknown) {
  if (value === undefined || value === null || value === '') return undefined;
  const numberValue = Number(String(value).trim().replace(/m$/iu, ''));
  return Number.isFinite(numberValue) && numberValue >= 0
    ? numberValue
    : undefined;
}

function formatSemiLength(value: number) {
  return value.toFixed(3);
}

function serializeSemiDetailLength(value: unknown) {
  const normalized = normalizeSemiDetailLengthValue(value);
  return normalized === undefined ? '' : String(normalized);
}

function resolveSemiDetailLengthByIndex(
  record: WetSheetRow | null | undefined,
  row: WetSheetDetail,
  absoluteIndex: number,
) {
  const explicitLength = normalizeSemiDetailLengthValue(row.length);
  if (explicitLength !== undefined) {
    return explicitLength;
  }
  const totalLength =
    record?.generatedLength ||
    resolveSemiGeneratedLength(record?.schemaJson, 600);
  const totalRows = Math.max(1, record?.details?.length || 1);
  const rowStep = Math.max(1, Math.ceil(totalLength / totalRows));
  return Math.min(totalLength, (absoluteIndex + 1) * rowStep);
}

function getSemiDetailLengthByIndex(
  row: WetSheetDetail,
  absoluteIndex: number,
) {
  return resolveSemiDetailLengthByIndex(
    currentRecord.value,
    row,
    absoluteIndex,
  );
}

function getSemiDetailLength(row: WetSheetDetail, index: number) {
  return getSemiDetailLengthByIndex(row, getDetailRenderRowIndex(index));
}

function updateSemiDetailLength(row: WetSheetDetail, value: unknown) {
  const normalized = normalizeSemiDetailLengthValue(value);
  if (normalized === undefined) {
    delete row.length;
    return;
  }
  row.length = normalized;
}

function hydrateSemiDetailLengths(record: WetSheetRow) {
  record.details = buildSemiFinishedRows(record);
}

function rebuildSemiFinishedDetails() {
  const record = currentRecord.value;
  if (!record) return;
  const generatedLength = normalizeSemiDetailLengthValue(record.generatedLength);
  if (generatedLength === undefined || generatedLength <= 0) {
    message.warning('请先填写大于 0 的半成品长度');
    return;
  }
  const rebuild = () => {
    record.details = generateSemiDetails(
      generatedLength,
      resolveSemiRowStep(record.schemaJson),
    );
  };
  const hasFilledValue = (record.details || []).some((detail) =>
    [detail.item, detail.node, detail.remark, detail.standard, detail.value].some(
      (value) => Boolean(String(value || '').trim()),
    ),
  );
  if (!hasFilledValue) {
    rebuild();
    return;
  }
  AModal.confirm({
    cancelText: '取消',
    content: '重建会清空当前明细中的已填写厚度和备注，是否继续？',
    okText: '重建明细',
    title: '确认按当前长度重建明细',
    onOk: rebuild,
  });
}

function wetExcelCell(
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

function isWetRecordHeaderImportable(key?: string) {
  if (!key || recordDialogMode.value === 'view') return false;
  const category = recordDialogCategory.value;
  if (category !== 'production-check' && category !== 'semi-finished') {
    return false;
  }
  const configuredKeys = normalizeHeaderFieldKeys(
    currentRecord.value?.schemaJson,
    category,
  ) as WetHeaderFieldKey[];
  const typedKey = key as WetHeaderFieldKey;
  if (!configuredKeys.includes(typedKey)) return false;
  return category === 'production-check'
    ? WET_PRODUCTION_CHECK_EXCEL_IMPORT_KEYS.has(typedKey)
    : WET_SEMI_FINISHED_EXCEL_IMPORT_KEYS.has(typedKey);
}

function resolveWetExcelHeaderValueType(
  key?: string,
): MesHcProcessFormApi.LayoutHeaderItem['valueType'] {
  if (key === 'productionDate') return 'DATE';
  if (
    key === 'recorderTime' ||
    key === 'confirmerTime' ||
    WET_PROCESS_RUNTIME_HEADER_KEYS.includes(key as WetHeaderFieldKey)
  ) {
    return 'DATETIME';
  }
  if (key === 'generatedLength') return 'NUMBER';
  return 'TEXT';
}

function getWetRecordExcelHeaderValue(key: string, value: unknown) {
  const headerKey = key as WetHeaderFieldKey;
  if (recordDialogCategory.value !== 'production-check') return value;
  if (headerKey === 'startTime') {
    return getProductionCheckHeaderDateValue(headerKey);
  }
  if (WET_PRODUCTION_CHECK_EXCEL_START_TIME_FALLBACK_KEYS.has(headerKey)) {
    const actualValue =
      headerKey === 'endTime'
        ? getStoredProductionCheckHeaderDateValue(headerKey)
        : normalizeProductionCheckHeaderDateTime(value);
    return actualValue || getProductionCheckHeaderDateValue('startTime');
  }
  return value;
}

function buildWetRecordExcelHeaderItems(): MesHcProcessFormApi.LayoutHeaderItem[] {
  const runtimeFields =
    recordDialogCategory.value === 'semi-finished'
      ? semiFinishedHeadFields.value
      : recordDialogCategory.value === 'production-check'
        ? productionCheckHeadFields.value
        : prepareHeadFields.value.map((field) => ({
            key: field.field,
            label: field.label,
            value: field.value,
          }));
  return [
    { editable: false, label: '计划号', value: task.value?.planNo || '' },
    {
      editable: false,
      label: '当前工序',
      value: task.value?.process || '湿法',
    },
    {
      editable: false,
      label: '执行时机',
      value: currentRecord.value?.timing || '',
    },
    ...runtimeFields.map((field: any) => {
      const bindKey = String(field.key || '');
      const importable = isWetRecordHeaderImportable(bindKey);
      return {
        bindField: importable ? 'headerData' : undefined,
        bindKey: importable ? bindKey : undefined,
        editable: importable,
        label: String(field.label || '').replace(/：$/u, ''),
        value: String(getWetRecordExcelHeaderValue(bindKey, field.value) || ''),
        valueType: resolveWetExcelHeaderValueType(bindKey),
      };
    }),
  ];
}

function buildWetRecordExcelColumns(): MesHcProcessFormApi.LayoutColumn[] {
  if (recordDialogCategory.value === 'semi-finished') {
    return [
      { title: '长度/m', width: 100 },
      ...semiFinishedThicknessHeaders.value.map((title) => ({
        title,
        width: 180,
      })),
      { title: '备注', width: 260 },
    ];
  }
  if (recordDialogCategory.value === 'production-check') {
    return [
      { title: '物料/生产环节', width: 120 },
      { title: '确认节点', width: 120 },
      { title: '点检项目', width: 220 },
      { title: '点检标准', width: 180 },
      { title: '实际/记录', width: 180 },
      { title: '异常备注', width: 220 },
    ];
  }
  if (currentRecord.value?.name?.includes('清洁点检表')) {
    return [
      { title: '工序', width: 120 },
      { title: '点检项目', width: 220 },
      { title: '检查标准', width: 420 },
      { title: 'OK/NG', width: 120 },
      { title: '备注', width: 260 },
    ];
  }
  return [
    { title: '序号', width: 90 },
    { title: '点检项目', width: 260 },
    { title: '标准', width: 360 },
    { title: 'OK/NG', width: 120 },
    { title: '备注', width: 260 },
  ];
}

function buildSemiFinishedRows(record?: null | WetSheetRow) {
  return (record?.details || []).map((row, index) => ({
    ...row,
    length: resolveSemiDetailLengthByIndex(record, row, index),
    seq: row.seq || index + 1,
  }));
}

function buildSemiFinishedExcelRows() {
  return buildSemiFinishedRows(currentRecord.value);
}

function buildWetRecordExcelRows(): MesHcProcessFormApi.LayoutRow[] {
  const rows =
    recordDialogCategory.value === 'semi-finished'
      ? buildSemiFinishedExcelRows()
      : currentRecord.value?.details || [];
  if (recordDialogCategory.value === 'semi-finished') {
    return rows.map((row, index) => ({
      cells: [
        wetExcelCell(0, getSemiDetailLengthByIndex(row, index), {
          bindField: 'length',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
        wetExcelCell(1, row.item || '', {
          bindField: 'item',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
        wetExcelCell(2, row.standard || '', {
          bindField: 'standard',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
        wetExcelCell(3, row.value || '', {
          bindField: 'value',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
        wetExcelCell(4, row.node || '', {
          bindField: 'node',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
        wetExcelCell(5, row.remark || '', {
          bindField: 'remark',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
      ],
    }));
  }
  if (recordDialogCategory.value === 'production-check') {
    return rows.map((row, index) => {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getDetailFieldRowSpan(rows, index, 'category');
      const nodeSpan = getDetailFieldRowSpan(rows, index, 'node');
      if (categorySpan > 0) {
        cells.push(
          wetExcelCell(0, row.category || '', { rowSpan: categorySpan }),
        );
      }
      if (nodeSpan > 0) {
        cells.push(wetExcelCell(1, row.node || '', { rowSpan: nodeSpan }));
      }
      cells.push(wetExcelCell(2, row.item || ''));
      cells.push(wetExcelCell(3, row.standard || ''));
      cells.push(
        wetExcelCell(4, row.value || '', {
          bindField: 'value',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
      );
      cells.push(
        wetExcelCell(5, row.remark || '', {
          bindField: 'remark',
          bindKey: String(index),
          editable: recordDialogMode.value !== 'view',
        }),
      );
      return { cells };
    });
  }
  return rows.map((row, index) => {
    const bindKey = String(index);
    if (currentRecord.value?.name?.includes('清洁点检表')) {
      const cells: MesHcProcessFormApi.LayoutCell[] = [];
      const categorySpan = getDetailFieldRowSpan(rows, index, 'category');
      if (categorySpan > 0) {
        cells.push(
          wetExcelCell(0, row.category || '', { rowSpan: categorySpan }),
        );
      }
      cells.push(wetExcelCell(1, row.item || ''));
      cells.push(wetExcelCell(2, row.standard || ''));
      cells.push(
        wetExcelCell(3, row.result || 'OK', {
          bindField: 'result',
          bindKey,
          editable: recordDialogMode.value !== 'view',
        }),
      );
      cells.push(
        wetExcelCell(4, row.remark || '', {
          bindField: 'remark',
          bindKey,
          editable: recordDialogMode.value !== 'view',
        }),
      );
      return { cells };
    }
    return {
      cells: [
        wetExcelCell(0, row.seq || index + 1),
        wetExcelCell(1, row.item || ''),
        wetExcelCell(2, row.standard || ''),
        wetExcelCell(3, row.result || 'OK', {
          bindField: 'result',
          bindKey,
          editable: recordDialogMode.value !== 'view',
        }),
        wetExcelCell(4, row.remark || '', {
          bindField: 'remark',
          bindKey,
          editable: recordDialogMode.value !== 'view',
        }),
      ],
    };
  });
}

function buildWetRecordExcelLayout(): MesHcProcessFormApi.LayoutExcelReq {
  const formName =
    getWetPassWorkDisplayName(currentRecord.value) || '湿法过站记录';
  return {
    columns: buildWetRecordExcelColumns(),
    detailTitle:
      recordDialogCategory.value === 'semi-finished'
        ? '半成品记录明细'
        : '明细项目',
    fileName: sanitizeExcelFileName(
      `${task.value?.planNo || '湿法'}_${formName}`,
    ),
    headerItems: buildWetRecordExcelHeaderItems(),
    rows: buildWetRecordExcelRows(),
    sheetName: '湿法过站记录',
    title: formName,
    visualMode:
      recordDialogCategory.value === 'semi-finished'
        ? 'wet-semi'
        : 'wet-pass-work',
  };
}

function normalizeWetResultFlag(value?: string) {
  const text = String(value || '')
    .trim()
    .toUpperCase();
  if (!text) return '';
  if (text.includes('NG') || text.includes('不合格')) return 'NG';
  if (text.includes('OK') || text.includes('合格')) return 'OK';
  return text;
}

function rememberImportedWetHeaderValue(
  record: WetSheetRow,
  key: string,
  value: number | string,
) {
  record.importedHeaderData = {
    ...(record.importedHeaderData || {}),
    [key]: value,
  };
}

function applyImportedWetHeaderValue(
  record: WetSheetRow,
  key: string,
  rawValue: unknown,
) {
  const value = normalizeWetImportedHeaderText(rawValue);
  if (!value) return false;
  if (key === 'generatedLength') {
    const numberValue = Number(value);
    rememberImportedWetHeaderValue(
      record,
      key,
      Number.isFinite(numberValue) && numberValue > 0 ? numberValue : value,
    );
    if (Number.isFinite(numberValue) && numberValue > 0) {
      record.generatedLength = numberValue;
    }
    return true;
  }
  if (key === 'semiWidth') {
    record.semiWidth = value;
    rememberImportedWetHeaderValue(record, key, value);
    return true;
  }
  if (key === 'poreDevelopment') {
    const normalized = normalizeWetResultFlag(value) || value;
    record.poreDevelopment = normalized;
    rememberImportedWetHeaderValue(record, key, normalized);
    return true;
  }
  if (key === 'finalResult') {
    const normalized = normalizeWetResultFlag(value) || value;
    record.finalResult = normalized;
    rememberImportedWetHeaderValue(record, key, normalized);
    return true;
  }
  if (key === 'recorder') {
    record.recorder = value;
    recordActionForm.value.recorder = value;
    rememberImportedWetHeaderValue(record, key, value);
    return true;
  }
  if (key === 'recorderTime') {
    const text = normalizeDateTime(value);
    record.recorderTime = text;
    recordActionForm.value.recorderTime = text;
    rememberImportedWetHeaderValue(record, key, text);
    return true;
  }
  if (key === 'confirmer') {
    record.confirmer = value;
    recordActionForm.value.confirmer = value;
    rememberImportedWetHeaderValue(record, key, value);
    return true;
  }
  if (key === 'confirmerTime') {
    const text = normalizeDateTime(value);
    record.confirmerTime = text;
    recordActionForm.value.confirmerTime = text;
    rememberImportedWetHeaderValue(record, key, text);
    return true;
  }
  if (isWetProductionCheckIndependentTimeKey(key as WetHeaderFieldKey)) {
    const text = normalizeProductionCheckHeaderDateTime(value);
    if (!text) return false;
    rememberImportedWetHeaderValue(record, key, text);
    return true;
  }
  if (isWetProcessRuntimeHeaderKey(key as WetHeaderFieldKey)) {
    const text =
      key === 'productionDate'
        ? normalizeDate(value)
        : normalizeDateTime(value);
    (reportForm.value as Record<string, string | number | undefined>)[key] =
      text;
    rememberImportedWetHeaderValue(record, key, text);
    return true;
  }
  rememberImportedWetHeaderValue(record, key, value);
  return true;
}

function resolveImportedWetDetailRowCount(
  resp: MesHcProcessFormApi.LayoutImportResp,
) {
  return (resp.cellValues || []).reduce((rowCount, item) => {
    const rowIndex = Number(item.bodyRowIndex ?? item.bindKey);
    return Number.isInteger(rowIndex) && rowIndex >= 0
      ? Math.max(rowCount, rowIndex + 1)
      : rowCount;
  }, 0);
}

function replaceSemiDetailsForExcelImport(
  record: WetSheetRow,
  rowCount: number,
) {
  if (rowCount <= 0) return;
  record.details = Array.from({ length: rowCount }, (_, index) => ({
    category: '',
    item: '',
    node: '',
    remark: '',
    result: 'OK',
    seq: index + 1,
    standard: '',
    value: '',
  }));
}

function applyImportedWetRecordExcel(
  resp: MesHcProcessFormApi.LayoutImportResp,
) {
  const record = currentRecord.value;
  if (!record) return { appliedCount: 0, importedDetailRowCount: 0 };
  const importedDetailRowCount =
    recordDialogCategory.value === 'semi-finished'
      ? resolveImportedWetDetailRowCount(resp)
      : 0;
  if (importedDetailRowCount > 0) {
    replaceSemiDetailsForExcelImport(record, importedDetailRowCount);
  }
  let appliedCount = 0;
  (resp.headerValues || []).forEach((item) => {
    if (item.bindField !== 'headerData' || !item.bindKey) return;
    if (applyImportedWetHeaderValue(record, item.bindKey, item.value)) {
      appliedCount += 1;
    }
  });
  (resp.cellValues || []).forEach((item) => {
    const rowIndex = Number(item.bodyRowIndex ?? item.bindKey);
    const row = record.details?.[rowIndex];
    if (!row || !item.bindField) return;
    const value = String(item.value ?? '').trim();
    if (!value && value !== '0') return;
    if (item.bindField === 'result') return;
    if (item.bindField === 'length') {
      const length = normalizeSemiDetailLengthValue(value);
      if (length === undefined) return;
      row.length = length;
      appliedCount += 1;
      return;
    }
    (row as any)[item.bindField] = value;
    appliedCount += 1;
  });
  if (recordDialogCategory.value === 'semi-finished') {
    hydrateSemiDetailLengths(record);
  }
  return { appliedCount, importedDetailRowCount };
}

async function attachImportedWetRecordExcel(file: File) {
  if (!currentRecord.value) return;
  const uploaded = (await uploadFile({
    directory: 'wet-pass-work-import',
    file,
  })) as any;
  const url = resolveUploadUrl(uploaded);
  currentRecord.value.attachments = [
    {
      name: uploaded?.name || file.name,
      path: uploaded?.path,
      size: uploaded?.size || file.size,
      type: uploaded?.type || file.type,
      uid: `${Date.now()}-${file.name}`,
      uploadTime: buildNowText(),
      url,
    },
  ].filter((item) => item.url);
}

async function handleWetRecordExportExcel() {
  if (!currentRecord.value) return;
  recordExcelLoading.value = true;
  try {
    const layout = buildWetRecordExcelLayout();
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({
      fileName: layout.fileName || '湿法过站记录.xlsx',
      source: data,
    });
  } finally {
    recordExcelLoading.value = false;
  }
}

function triggerWetRecordImportExcel() {
  recordExcelImportInputRef.value?.click();
}

async function handleWetRecordExcelImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!/\.xls[xm]?$/iu.test(file.name)) {
    AModal.warning({
      content: '请选择 Excel 文件。',
      title: '文件类型不支持',
    });
    return;
  }
  recordExcelLoading.value = true;
  try {
    const layout = buildWetRecordExcelLayout();
    const resp = await importProcessFormRecordLayout(file, layout);
    const { appliedCount, importedDetailRowCount } =
      applyImportedWetRecordExcel(resp);
    await attachImportedWetRecordExcel(file);
    const warningText = (resp.messages || [])
      .filter((item) => item.startsWith('警告：'))
      .join('；');
    const resultText =
      importedDetailRowCount > 0
        ? `导入成功，已按 Excel 加载 ${importedDetailRowCount} 行明细，回填 ${appliedCount} 个有值单元格。请核实后点击保存或确认。`
        : `导入成功，已回填 ${appliedCount} 个有值单元格。请核实后点击保存或确认。`;
    AModal.success({
      content: warningText ? `${resultText} ${warningText}` : resultText,
      title: '导入完成',
    });
  } finally {
    recordExcelLoading.value = false;
  }
}

function normalizeCollectorScopeText(value?: string) {
  return String(value || '').trim();
}

function getProductionCheckDetailRows() {
  return (currentRecord.value?.details || []).filter((row) => !row.placeholder);
}

function buildCollectorSelectOptions(values: string[]) {
  const seen = new Set<string>();
  return values
    .map((value) => normalizeCollectorScopeText(value))
    .filter((value) => {
      if (!value || seen.has(value)) return false;
      seen.add(value);
      return true;
    })
    .map((value) => ({ label: value, value }));
}

const collectorItemCategoryOptions = computed(() =>
  buildCollectorSelectOptions(
    getProductionCheckDetailRows().map((row) => row.category || ''),
  ),
);

const collectorStepNodeOptions = computed(() => {
  const selectedCategory = normalizeCollectorScopeText(
    collectorScopeForm.value.itemCategory,
  );
  const rows = getProductionCheckDetailRows().filter(
    (row) =>
      !selectedCategory ||
      normalizeCollectorScopeText(row.category) === selectedCategory,
  );
  return buildCollectorSelectOptions(rows.map((row) => row.node || ''));
});

const collectorScopePreviewRows = computed(() => {
  const scope = collectorScopeForm.value;
  return getProductionCheckDetailRows().filter((row) =>
    matchesCollectorScope(row, scope),
  );
});

type CollectorFillApplyMode = 'blank' | 'overwrite';
type CollectorFillApplySummary = {
  applied: number;
  existing: number;
  missing: number;
  skipped: number;
  total: number;
};

const COLLECTOR_SKIP_QUALITIES = new Set(['ALARM', 'MISSING', 'STALE']);

function normalizeCollectorQuality(value?: string) {
  return String(value || '')
    .trim()
    .toUpperCase();
}

function normalizeCollectorActualValue(value: unknown) {
  if (value === undefined || value === null) return '';
  return String(value).trim();
}

function matchesCollectorScope(
  row: WetSheetDetail,
  scope?: CollectorFillScope,
) {
  if (!scope) return true;
  const itemCategory = normalizeCollectorScopeText(scope.itemCategory);
  const stepNode = normalizeCollectorScopeText(scope.stepNode);
  if (
    itemCategory &&
    normalizeCollectorScopeText(row.category) !== itemCategory
  ) {
    return false;
  }
  if (stepNode && normalizeCollectorScopeText(row.node) !== stepNode) {
    return false;
  }
  return true;
}

function resolveCollectorItemQuality(item: WetLocalCollectorFillItem) {
  return normalizeCollectorQuality(
    item.iotSource?.quality || item.iotSource?.resultFlag || item.status,
  );
}

function isCollectorItemFillable(item: WetLocalCollectorFillItem) {
  const actualValue = normalizeCollectorActualValue(item.actualValue);
  if (!actualValue) return false;
  const quality = resolveCollectorItemQuality(item);
  return !COLLECTOR_SKIP_QUALITIES.has(quality);
}

function resolveCollectorItemStatus(item: WetLocalCollectorFillItem) {
  const quality = resolveCollectorItemQuality(item);
  return quality === 'NG'
    ? 'NG'
    : item.status || item.iotSource?.resultFlag || 'OK';
}

function buildCollectorRemark(item: WetLocalCollectorFillItem) {
  if (item.remark) return String(item.remark).trim();
  const source = item.iotSource;
  const sourceName =
    source?.displayName ||
    source?.channelName ||
    source?.channelKey ||
    source?.measurementKey ||
    '本机采集';
  const collectedAt = source?.collectedAt
    ? normalizeDateTime(source.collectedAt)
    : '';
  return collectedAt
    ? `自动采集 ${collectedAt} ${sourceName}`
    : `自动采集 ${sourceName}`;
}

function mergeCollectorRemark(existing?: string, next?: string) {
  const current = String(existing || '').trim();
  const incoming = String(next || '').trim();
  if (!incoming) return current;
  if (!current) return incoming;
  if (current.includes(incoming)) return current;
  return `${current}；${incoming}`;
}

function buildCollectorFillRequest(
  record: WetSheetRow,
  scope?: CollectorFillScope,
) {
  return {
    batchNo:
      task.value?.productionBatchNo ||
      task.value?.batchNo ||
      task.value?.parentProductionBatchNo ||
      undefined,
    equipmentCode: reportForm.value.equipmentCode || undefined,
    equipmentId: reportForm.value.equipmentId,
    equipmentName: reportForm.value.equipmentName || undefined,
    eventType: 'MANUAL_FILL',
    formCode: LOCAL_COLLECTOR_WET_PROCESS_CHECK_FORM_CODE,
    itemCategory: scope?.itemCategory || undefined,
    modelCode:
      task.value?.motherModelCode || task.value?.modelCode || undefined,
    planId: task.value?.planId,
    planOperationId: task.value?.planOperationId,
    readFresh: true,
    recordId: record.recordId,
    stepNode: scope?.stepNode || undefined,
  };
}

function applyCollectorFillItems(
  fillItems: WetLocalCollectorFillItem[],
  mode: CollectorFillApplyMode,
  scope?: CollectorFillScope,
): CollectorFillApplySummary {
  const rows = currentRecord.value?.details || [];
  const rowBySeq = new Map<number, WetSheetDetail>();
  rows.forEach((row) => {
    const seq = Number(row.seq);
    if (Number.isFinite(seq)) rowBySeq.set(seq, row);
  });

  const summary: CollectorFillApplySummary = {
    applied: 0,
    existing: 0,
    missing: 0,
    skipped: 0,
    total: fillItems.length,
  };

  fillItems.forEach((item) => {
    const seq = Number(item.itemSeq);
    const row = Number.isFinite(seq) ? rowBySeq.get(seq) : undefined;
    if (!row) {
      summary.missing += 1;
      return;
    }
    if (!matchesCollectorScope(row, scope)) {
      return;
    }
    if (!isCollectorItemFillable(item)) {
      summary.skipped += 1;
      return;
    }
    const nextValue = normalizeCollectorActualValue(item.actualValue);
    const currentValue = normalizeCollectorActualValue(row.value);
    if (mode === 'blank' && currentValue) {
      summary.existing += 1;
      return;
    }
    row.value = nextValue;
    row.result = resolveCollectorItemStatus(item);
    row.remark = mergeCollectorRemark(row.remark, buildCollectorRemark(item));
    row.collectorSource = {
      ...(item.iotSource || {}),
      quality: item.iotSource?.quality || item.status || row.result,
    };
    summary.applied += 1;
  });

  return summary;
}

function buildCollectorApplyMessage(summary: CollectorFillApplySummary) {
  const parts = [`已填入 ${summary.applied} 项`];
  if (summary.existing) parts.push(`${summary.existing} 项已有值未覆盖`);
  if (summary.skipped)
    parts.push(`${summary.skipped} 项无有效采集值或状态异常`);
  if (summary.missing) parts.push(`${summary.missing} 项未匹配到当前点检行`);
  return parts.join('，');
}

function getCollectorSourceText(source?: WetLocalCollectorFillSource) {
  if (!source) return '本机采集';
  return (
    source.displayName ||
    source.channelName ||
    source.channelKey ||
    source.measurementKey ||
    '本机采集'
  );
}

function getCollectorSourceTagColor(
  source?: WetLocalCollectorFillSource,
  result?: string,
) {
  const quality = normalizeCollectorQuality(source?.quality || result);
  if (quality === 'NG') return 'error';
  if (COLLECTOR_SKIP_QUALITIES.has(quality)) return 'warning';
  return 'processing';
}

function getCollectorSourceTooltip(row: WetSheetDetail) {
  const source = row.collectorSource;
  if (!source) return '';
  const parts = [
    `来源：${getCollectorSourceText(source)}`,
    source.collectedAt
      ? `采集时间：${normalizeDateTime(source.collectedAt)}`
      : '',
    source.equipmentCode ? `设备：${source.equipmentCode}` : '',
    source.quality ? `质量：${source.quality}` : '',
  ].filter(Boolean);
  return parts.join(' / ');
}

function warnLocalCollectorError(error: unknown) {
  AModal.warning({
    content:
      error instanceof Error
        ? error.message
        : `本机采集异常：${String(error || '未知错误')}`,
    title: '记录参数失败',
  });
}

function syncCollectorStepNodeAfterCategoryChange(value?: string) {
  if (value !== undefined) {
    collectorScopeForm.value.itemCategory = value;
  }
  const options = collectorStepNodeOptions.value;
  if (
    options.some((option) => option.value === collectorScopeForm.value.stepNode)
  ) {
    return;
  }
  collectorScopeForm.value.stepNode = options[0]?.value || '';
}

function openCollectorScopeDialog() {
  if (
    recordDialogCategory.value !== 'production-check' ||
    recordDialogMode.value === 'view' ||
    !currentRecord.value
  ) {
    return;
  }
  const rows = getProductionCheckDetailRows();
  if (!rows.length) {
    message.warning('当前点检表没有可记录的明细项目');
    return;
  }
  const firstRow =
    rows.find((row) => row.category && row.node) ||
    rows.find((row) => row.category) ||
    rows[0];
  const nextCategory = collectorItemCategoryOptions.value.some(
    (option) => option.value === collectorScopeForm.value.itemCategory,
  )
    ? collectorScopeForm.value.itemCategory
    : normalizeCollectorScopeText(firstRow?.category);
  collectorScopeForm.value = {
    itemCategory: nextCategory,
    stepNode: '',
  };
  syncCollectorStepNodeAfterCategoryChange();
  collectorScopeDialogVisible.value = true;
}

async function handleCollectorScopeConfirm() {
  if (!collectorScopeForm.value.itemCategory) {
    message.warning('请选择物料/生产环节');
    return;
  }
  if (!collectorScopeForm.value.stepNode) {
    message.warning('请选择确认节点');
    return;
  }
  const success = await handleCollectorFillCurrentRecord(
    'blank',
    collectorScopeForm.value,
  );
  if (success) {
    collectorScopeDialogVisible.value = false;
  }
}

async function handleCollectorFillCurrentRecord(
  mode: CollectorFillApplyMode = 'blank',
  scope?: CollectorFillScope,
) {
  if (
    recordDialogCategory.value !== 'production-check' ||
    recordDialogMode.value === 'view' ||
    !currentRecord.value
  ) {
    return false;
  }
  const request = buildCollectorFillRequest(currentRecord.value, scope);
  modbusCollecting.value = true;
  try {
    const fill: WetLocalCollectorFillResult =
      await readWetProcessCheckFromLocalCollector(request);
    if (fill.formCode && fill.formCode !== request.formCode) {
      AModal.warning({
        content: `本机采集器返回的点检表编码为 ${fill.formCode}，当前报工点检表编码为 ${request.formCode}。为避免通道错填，请先在采集器中配置当前点检表映射。`,
        title: '点检表映射不一致',
      });
      return false;
    }
    const fillItems = fill.fillItems || [];
    if (!fillItems.length) {
      const scopeText = scope
        ? `${scope.itemCategory || '-'} / ${scope.stepNode || '-'}`
        : '当前点检范围';
      message.warning(
        `本机采集器没有返回 ${scopeText} 的可填充点检项，请确认采集器点检映射已启用并保存。`,
      );
      return false;
    }
    const summary = applyCollectorFillItems(fillItems, mode, scope);
    if (summary.applied > 0) {
      message.success(
        `${buildCollectorApplyMessage(summary)}，请核实后保存或确认。`,
      );
    } else {
      message.warning(buildCollectorApplyMessage(summary));
    }
    if (mode === 'blank' && summary.existing > 0) {
      AModal.confirm({
        cancelText: '保持现有值',
        content: `${summary.existing} 项已有人工或历史值，当前未覆盖。是否用本次采集值覆盖这些已有值？`,
        okText: '覆盖已有值',
        title: '确认覆盖已有点检值',
        onOk: () => {
          const overwriteSummary = applyCollectorFillItems(
            fillItems,
            'overwrite',
            scope,
          );
          message.success(
            `${buildCollectorApplyMessage(overwriteSummary)}，请核实后保存或确认。`,
          );
        },
      });
    }
    return true;
  } catch (error) {
    warnLocalCollectorError(error);
    return false;
  } finally {
    modbusCollecting.value = false;
  }
}

async function openProductionCheckRecordAndCollect(record: WetSheetRow) {
  if (modbusCollecting.value) return;
  openRecord(record, 'production-check', 'edit');
  await nextTick();
  if (
    !recordDialogVisible.value ||
    recordDialogCategory.value !== 'production-check' ||
    currentRecord.value?.id !== record.id
  ) {
    return;
  }
  openCollectorScopeDialog();
}

function openRecord(
  record: WetSheetRow,
  category: RecordCategory,
  mode: RecordMode,
) {
  if (!isStarted()) {
    AModal.warning({
      content: '湿法工序必须先开工后，才能填写或确认相关记录单。',
      title: '请先执行开工确认',
    });
    return;
  }
  if (
    category === 'semi-finished' &&
    (!record.details || record.details.length === 0)
  ) {
    const generatedLength =
      record.generatedLength ||
      resolveSemiGeneratedLength(record.schemaJson, 600);
    record.generatedLength = generatedLength;
    record.details = generateSemiDetails(
      generatedLength,
      resolveSemiRowStep(record.schemaJson),
    );
    record.result = '已生成';
  }
  recordDialogCategory.value = category;
  recordDialogMode.value = mode;
  currentRecord.value = JSON.parse(JSON.stringify(record));
  if (category === 'semi-finished' && currentRecord.value) {
    hydrateSemiDetailLengths(currentRecord.value);
  }
  actionPanelExpanded.value = false;
  recordActionForm.value = {
    confirmer: record.confirmer || reportForm.value.confirmerName || '',
    confirmerTime: record.confirmerTime || reportForm.value.confirmerTime || '',
    confirmRemark: record.remark || '',
    formRemark: record.remark || '',
    inspectionResult: record.result === '异常已确认' ? 'NG' : 'OK',
    recorder: record.recorder || reportForm.value.recorderName || '',
    recorderTime: resolveRecordDialogRecorderTime(record),
    result: record.result === '异常待确认' ? 'NG' : 'OK',
  };
  recordDialogVisible.value = true;
}

function closeRecordDialog() {
  recordDialogVisible.value = false;
  currentRecord.value = null;
}

function validateProductionCheckHeaderTimeRange() {
  if (
    recordDialogCategory.value !== 'production-check' ||
    !currentRecord.value
  ) {
    return true;
  }
  const startTime = getProductionCheckHeaderDateValue(
    'startTime',
    currentRecord.value,
  );
  const endTime = getProductionCheckHeaderDateValue(
    'endTime',
    currentRecord.value,
  );
  if (startTime && endTime && dayjs(endTime).isBefore(dayjs(startTime))) {
    message.warning('投料结束时间不能早于投料开始时间');
    return false;
  }
  return true;
}

async function saveCurrentRecord() {
  if (!currentRecord.value) return;
  if (!validateProductionCheckHeaderTimeRange()) return;
  const now = buildNowText();
  if (shouldResetRuntimeDateTime(recordActionForm.value.recorderTime)) {
    recordActionForm.value.recorderTime = isPoreSamplingRecord(
      currentRecord.value,
    )
      ? resolvePoreSamplingRecorderTimeText()
      : now;
  }
  await saveWetPassWork(buildWetPassWorkPayload(currentRecord.value));
  await loadWetPassWorkRows();
  closeRecordDialog();
}

async function confirmCurrentRecord() {
  if (!currentRecord.value) return;
  if (!validateProductionCheckHeaderTimeRange()) return;
  const now = buildNowText();
  if (shouldResetRuntimeDateTime(recordActionForm.value.recorderTime)) {
    recordActionForm.value.recorderTime = !shouldResetRuntimeDateTime(
      currentRecord.value.recorderTime,
    )
      ? (currentRecord.value.recorderTime ?? now)
      : isPoreSamplingRecord(currentRecord.value)
        ? resolvePoreSamplingRecorderTimeText()
        : now;
  }
  if (shouldResetRuntimeDateTime(recordActionForm.value.confirmerTime)) {
    recordActionForm.value.confirmerTime = now;
  }
  await confirmWetPassWork(buildWetPassWorkPayload(currentRecord.value));
  await loadWetPassWorkRows();
  closeRecordDialog();
}

function resolveAuthUserDisplayName(userInfo: any) {
  return (
    userInfo?.empName ||
    userInfo?.nickname ||
    userInfo?.username ||
    userInfo?.empNo ||
    ''
  );
}

function openRecordSaveAuth() {
  if (!currentRecord.value) return;
  recordSaveAuthVisible.value = true;
}

async function handleRecordSaveAuthSuccess(userInfo: any) {
  const recorder = resolveAuthUserDisplayName(userInfo);
  if (recorder) {
    recordActionForm.value.recorder = recorder;
  }
  recordActionForm.value.recorderTime = buildNowText();
  recordSaveAuthVisible.value = false;
  await saveCurrentRecord();
}

function openRecordConfirmAuth() {
  if (!currentRecord.value) return;
  recordConfirmAuthVisible.value = true;
}

async function handleRecordConfirmAuthSuccess(userInfo: any) {
  const confirmer = resolveAuthUserDisplayName(userInfo);
  if (confirmer) {
    recordActionForm.value.confirmer = confirmer;
  }
  recordActionForm.value.confirmerTime = buildNowText();
  recordConfirmAuthVisible.value = false;
  await confirmCurrentRecord();
}

function openPoreSelfCheckCreate() {
  if (!isStarted()) {
    AModal.warning({
      content: '湿法工序必须先开工后，才能新增泡孔自检记录。',
      title: '请先执行开工确认',
    });
    return;
  }
  poreSelfCheckDialogMode.value = 'create';
  poreSelfCheckForm.value = createEmptyPoreSelfCheckRecord({
    inspector: reportForm.value.recorderName || '',
    selfCheckResult: 'OK',
    selfCheckTime: resolvePoreSelfCheckStartTimeText() || buildNowText(),
  });
  poreSelfCheckDialogVisible.value = true;
}

function openPoreSelfCheckView(row: PoreSelfCheckRecord) {
  poreSelfCheckDialogMode.value = 'view';
  poreSelfCheckForm.value = createEmptyPoreSelfCheckRecord(row);
  poreSelfCheckDialogVisible.value = true;
}

function closePoreSelfCheckDialog() {
  poreSelfCheckDialogVisible.value = false;
}

async function savePoreSelfCheckRecord() {
  const form = createEmptyPoreSelfCheckRecord(poreSelfCheckForm.value);
  if (!form.selfCheckTime) {
    form.selfCheckTime = buildNowText();
  }
  form.selfCheckTime =
    normalizeBizDateTime(
      reportForm.value.productionDate || task.value?.productionDate,
      form.selfCheckTime,
    ) || form.selfCheckTime;
  if (!form.selfCheckTime) {
    AModal.warning({
      content: '请填写泡孔自检时间。',
      title: '自检时间不能为空',
    });
    return;
  }
  if (!form.selfCheckResult) {
    AModal.warning({
      content: '请选择泡孔自检结果。',
      title: '自检结果不能为空',
    });
    return;
  }
  if (!form.inspector) {
    AModal.warning({
      content: '请填写泡孔自检检验人。',
      title: '检验人不能为空',
    });
    return;
  }
  poreSelfCheckSaving.value = true;
  try {
    const records = [...poreSelfCheckRows.value, form];
    await saveWetPassWork({
      details: [],
      equipmentCode: reportForm.value.equipmentCode || undefined,
      equipmentId: reportForm.value.equipmentId,
      equipmentName: reportForm.value.equipmentName || undefined,
      formCode: WET_PORE_SELF_CHECK_FORM_CODE,
      formRemark: form.remark || undefined,
      headerDataJson: JSON.stringify({ records }),
      inspectionResult: form.selfCheckResult || 'OK',
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
      recordId: poreSelfCheckMeta.value.recordId,
      recorder: form.inspector || reportForm.value.recorderName || undefined,
      recorderTime: form.selfCheckTime || undefined,
      result: form.selfCheckResult || 'OK',
    });
    applyPoreSelfCheckStartTime(records, true);
    await loadWetPassWorkRows();
    closePoreSelfCheckDialog();
  } finally {
    poreSelfCheckSaving.value = false;
  }
}

async function submitFirstInspection() {
  if (!isStarted()) {
    AModal.warning({
      content: '湿法工序必须先开工后才能提交首检申请。',
      title: '请先执行开工确认',
    });
    return;
  }
  if (firstInspectionApplying.value) {
    return;
  }
  openNapSampleLengthApplyConfirm();
}

function openNapSampleLengthApplyConfirm() {
  let napSampleLengthValue = normalizeOptionalNumber(
    reportForm.value.napSampleLength ?? firstInspection.value.napSampleLength,
  );
  AModal.confirm({
    cancelText: '取消',
    content: h('div', { class: 'wet-nap-sample-confirm' }, [
      h(
        'div',
        { class: 'wet-nap-sample-confirm__label' },
        '请填写本次首检的 NAP层送检(米)，该值会写入送检记录并自动回填到湿法报工。',
      ),
      h(InputNumber, {
        addonAfter: 'm',
        defaultValue: napSampleLengthValue,
        min: 0.001,
        placeholder: '请输入 NAP层送检米数',
        precision: 3,
        step: 0.1,
        style: { width: '100%' },
        onChange: (value: any) => {
          napSampleLengthValue = normalizeOptionalNumber(value);
        },
      }),
    ]),
    okText: '提交首检申请',
    onOk: async () => {
      const napSampleLength = normalizeOptionalNumber(napSampleLengthValue);
      if (napSampleLength === undefined || napSampleLength <= 0) {
        AModal.warning({
          content: '请填写大于 0 的 NAP层送检(米)。',
          title: 'NAP层送检不能为空',
        });
        return Promise.reject(new Error('NAP层送检不能为空'));
      }
      try {
        await submitFirstInspectionByMode(
          'PRODUCT_MODEL_PROCESS',
          napSampleLength,
        );
      } catch (error) {
        AModal.warning({
          content:
            getErrorMessage(error) ||
            '提交首检申请失败，请检查任务状态后重试。',
          title: isFaiStandardNotFoundError(error)
            ? '未找到首检标准'
            : '提交失败',
        });
        return Promise.reject(error);
      }
    },
    title: '填写NAP层送检(米)',
  });
}

async function submitFirstInspectionByMode(
  standardMatchMode: FirstInspectionMatchMode,
  napSampleLength: number,
  options: { showSuccess?: boolean } = {},
) {
  const isReapply = canReapplyFirstInspection();
  firstInspectionApplying.value = true;
  try {
    const summary = await applyWetFai({
      planId: task.value.planId,
      planOperationId: task.value.planOperationId,
      napSampleLength,
      remark: isReapply ? '湿法首检不合格后重新提交' : '湿法报工首检申请',
      standardMatchMode,
      submitterName: reportForm.value.recorderName || undefined,
      triggerReason: isReapply ? 'REWORK_RECHECK' : 'NEW_ORDER',
    });
    applyFaiSummary(summary);
    emit('refresh');
    if (options.showSuccess !== false) {
      AModal.success({
        content: `首检申请已提交，已生成首件检验单 ${summary.faiNo || ''}`,
        okText: '知道了',
        title: '提交成功',
      });
    }
  } finally {
    firstInspectionApplying.value = false;
  }
}

async function ensureFirstInspectionSubmittedBeforeBooking() {
  if (hasSubmittedFirstInspection()) return true;
  const napSampleLength = normalizeOptionalNumber(
    reportForm.value.napSampleLength ?? firstInspection.value.napSampleLength,
  );
  if (napSampleLength === undefined || napSampleLength <= 0) {
    viewMode.value = 'booking';
    AModal.warning({
      content:
        '报工提交前需要先提交首检申请，请在报工信息中填写大于 0 的 NAP层送检(米)。',
      title: 'NAP层送检不能为空',
    });
    return false;
  }
  try {
    await submitFirstInspectionByMode(
      'PRODUCT_MODEL_PROCESS',
      napSampleLength,
      {
        showSuccess: false,
      },
    );
    return true;
  } catch (error) {
    AModal.warning({
      content:
        getErrorMessage(error) ||
        '报工提交前自动提交首检申请失败，请检查任务状态后重试。',
      title: isFaiStandardNotFoundError(error)
        ? '未找到首检标准'
        : '首检提交失败',
    });
    return false;
  }
}

function getErrorMessage(error: unknown) {
  const data = (error as any)?.response?.data || (error as any)?.data || {};
  return String(
    data.msg ||
      data.message ||
      (error as any)?.msg ||
      (error as any)?.message ||
      '',
  );
}

function isFaiStandardNotFoundError(error: unknown) {
  const code =
    (error as any)?.code ||
    (error as any)?.response?.data?.code ||
    (error as any)?.data?.code;
  const message = getErrorMessage(error);
  return (
    code === 1008100062 ||
    message.includes('未找到启用且已审核的FAI检验标准') ||
    message.includes('未找到物料编码与工段同时匹配的 FAI 检验标准') ||
    message.includes('未找到产品型号与工段同时匹配的 FAI 检验标准')
  );
}

function viewFirstInspectionDetail(row = firstInspection.value) {
  if (!row.faiId) return;
  faiDetailModalApi.setData({ id: row.faiId }).open();
}

function getFirstInspectionStatusMeta(row = firstInspection.value) {
  if (row.faiId) return getStatusMeta(row.status);
  return { color: 'default', text: '未提交' };
}

function canReapplyFirstInspection() {
  return (
    firstInspection.value.status === 'CANCELED' ||
    firstInspection.value.judgment === 'NG'
  );
}

function firstInspectionActionText() {
  return canReapplyFirstInspection() ? '重新提交首检申请' : '提交首检申请';
}

const firstInspectionBannerStatus = computed(() => {
  if (!hasSubmittedFirstInspection()) {
    return {
      className: 'wet-prev-op-bar__fai-status--empty',
      text: '-',
      visible: false,
    };
  }
  const judgment = String(firstInspection.value.judgment || '').toUpperCase();
  const status = String(firstInspection.value.status || '').toUpperCase();
  if (judgment === 'OK') {
    return {
      className: 'wet-prev-op-bar__fai-status--ok',
      text: 'OK',
      visible: true,
    };
  }
  if (judgment === 'NG' || status === 'REJECTED') {
    return {
      className: 'wet-prev-op-bar__fai-status--ng',
      text: 'NG',
      visible: true,
    };
  }
  return {
    className: 'wet-prev-op-bar__fai-status--pending',
    text: getFirstInspectionStatusMeta(firstInspection.value).text,
    visible: true,
  };
});

function goFirstInspectionTab() {
  activeMainTab.value = 'first-inspection';
}

async function handleStartClick() {
  if (!planDetail.value && task.value?.planId) {
    await loadPlanDetailContext();
  }
  if (
    previousOperation.value &&
    !hasOperationReported(previousOperation.value)
  ) {
    AModal.warning({
      title: '前序工序尚未报工',
      content: `前序工序【${previousOperation.value.opName || '未知工序'}】尚未完成报工，请先完成前序工序过站报工后再执行湿法开工确认。`,
    });
    return;
  }
  await loadEquipmentOptions();
  pendingAction.value = 'START';
  authVisible.value = true;
}

async function handleFinishClick() {
  if (!isStarted()) {
    AModal.warning({
      content: '湿法工序必须先完成开工确认后，才允许进入过站报工。',
      title: '请先执行开工确认',
    });
    return;
  }
  if (!(await ensureWetSampleAbnormalUnlocked())) {
    return;
  }
  if (!validatePoreSelfCheckBeforeFinish()) {
    return;
  }
  if (!reportForm.value.productionDate) {
    reportForm.value.productionDate = reportDateFromDateTime(
      reportForm.value.startTime,
    );
  }
  const startTime = normalizeReportDateTimeText(
    reportForm.value.startTime,
    reportForm.value.productionDate,
  );
  if (!startTime) {
    message.warning(
      '未找到有效的开工开始时间，请先执行开工确认或修正开始时间。',
    );
    return;
  }
  reportForm.value.startTime = startTime;
  await loadEquipmentOptions();
  pendingAction.value = 'FINISH';
  authVisible.value = true;
}

function handleReportConfirmClick() {
  if (!task.value?.operationReportId) {
    message.warning('当前还没有可确认的湿法报工记录。');
    return;
  }
  pendingAction.value = 'CONFIRM_REPORT';
  authVisible.value = true;
}

async function handleAuthSuccess(userInfo: any) {
  const now = buildNowText();
  if (pendingAction.value === 'START') {
    const effectiveProductionDate =
      reportForm.value.productionDate || dayjs().format('YYYY-MM-DD');
    reportForm.value.productionDate = effectiveProductionDate;
    reportForm.value.equipmentId = userInfo.equipmentId;
    reportForm.value.equipmentCode = userInfo.equipmentCode || '';
    reportForm.value.equipmentName = userInfo.equipmentName || '';
    reportForm.value.startTime = now;
    reportForm.value.recorderName = userInfo.empName;
    reportForm.value.recorderTime = now;
    await startWetReport({
      equipmentCode: reportForm.value.equipmentCode || undefined,
      equipmentId: reportForm.value.equipmentId,
      equipmentName: reportForm.value.equipmentName || undefined,
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
      recorderName: reportForm.value.recorderName || undefined,
      recorderTime: reportForm.value.recorderTime || undefined,
      reportDate: effectiveProductionDate,
      startTime: reportForm.value.startTime || undefined,
    });
    if (task.value) {
      task.value = {
        ...task.value,
        equipmentCode: reportForm.value.equipmentCode,
        equipmentId: reportForm.value.equipmentId,
        equipmentName: reportForm.value.equipmentName,
        productionDate: effectiveProductionDate,
        recorderName: reportForm.value.recorderName,
        recorderTime: reportForm.value.recorderTime,
        startTime: reportForm.value.startTime,
        status: 'IN_PROGRESS',
      };
      emit('taskChange', { ...task.value });
    }
    await syncTaskRuntimeFromBackend();
    await loadGuideClothRuntime();
    await loadEquipmentStatusCard();
    await loadWetPassWorkRows();
    reportForm.value.productionDate = effectiveProductionDate;
    reportForm.value.startTime = now;
    reportForm.value.recorderName = userInfo.empName;
    reportForm.value.recorderTime = now;
    await nextTick();
  } else if (pendingAction.value === 'FINISH') {
    reportForm.value.equipmentId = userInfo.equipmentId;
    reportForm.value.equipmentCode =
      userInfo.equipmentCode || reportForm.value.equipmentCode || '';
    reportForm.value.equipmentName =
      userInfo.equipmentName || reportForm.value.equipmentName || '';
    reportForm.value.endTime = now;
    reportForm.value.recorderTime = '';
    bookingDateTimeInputCache.value.endTime = now;
    bookingDateTimeInputCache.value.recorderTime = '';
    viewMode.value = 'booking';
  } else if (pendingAction.value === 'SUBMIT_BOOKING') {
    reportForm.value.recorderName =
      userInfo.empName ||
      userInfo.nickname ||
      userInfo.username ||
      userInfo.empNo ||
      '';
    reportForm.value.recorderTime = now;
    bookingDateTimeInputCache.value.recorderTime = now;
    await handleBookingSubmit();
  } else if (pendingAction.value === 'CONFIRM_REPORT') {
    if (!task.value?.operationReportId) {
      message.warning('当前还没有可确认的湿法报工记录。');
      pendingAction.value = null;
      return;
    }
    const confirmerName = userInfo.empName;
    const confirmerTime = now;
    await confirmWetReport({
      confirmerName,
      confirmerTime,
      id: task.value.operationReportId,
    });
    reportForm.value.confirmerName = confirmerName;
    reportForm.value.confirmerTime = confirmerTime;
    task.value = {
      ...(task.value || {}),
      confirmerName,
      confirmerTime,
    };
    emit('taskChange', { ...task.value });
    message.success(`认证通过！确认人【${confirmerName}】已确认湿法报工。`);
    await syncTaskRuntimeFromBackend();
  }
  pendingAction.value = null;
}

async function handleBookingSubmit() {
  await nextTick();
  syncBookingDateTimeFieldsFromDom();
  if (!ensureBookingReportDateTimes()) {
    return;
  }
  if (!normalizeWetProcessDateTimesForSubmit()) {
    return;
  }
  if (!(await ensureWetSampleAbnormalUnlocked())) {
    return;
  }
  if (!validatePoreSelfCheckBeforeFinish()) {
    return;
  }
  if (shouldResetRuntimeDateTime(reportForm.value.recorderTime)) {
    message.warning('未获取最终提交认证记录时间，请重新进行提交认证');
    return;
  }
  const finalLength = Number(reportForm.value.receiveLength ?? 0);
  const abnormalPositions = buildAbnormalPositionPayload();
  if (abnormalPositions === null) return;
  if (!(await ensureFirstInspectionSubmittedBeforeBooking())) {
    return;
  }
  let operationReportId: number | undefined;
  let bookingSubmitted = false;
  if (typeof task.value?.submitBooking === 'function') {
    operationReportId = await task.value.submitBooking({
      abnormalPositions,
      abnormalPositionOption: abnormalPositionOption.value || undefined,
      batchNo: task.value?.batchNo || undefined,
      equipmentCode: reportForm.value.equipmentCode || undefined,
      equipmentId: reportForm.value.equipmentId || undefined,
      equipmentName: reportForm.value.equipmentName || undefined,
      endTime: reportForm.value.endTime || undefined,
      guideClothBatchNo: reportForm.value.guideClothBatchNo || undefined,
      guideClothChangeReason:
        reportForm.value.guideClothChangeReason || undefined,
      guideClothChanged: reportForm.value.guideClothChanged || undefined,
      guideClothUseCount: reportForm.value.guideClothUseCount ?? undefined,
      inOvenTime: reportForm.value.inOvenTime || undefined,
      inSolidifyTime: reportForm.value.inSolidifyTime || undefined,
      inWashTime: reportForm.value.inWashTime || undefined,
      napSampleLength: reportForm.value.napSampleLength ?? undefined,
      outOvenTime: reportForm.value.outOvenTime || undefined,
      outSolidifyTime: reportForm.value.outSolidifyTime || undefined,
      outWashTime: reportForm.value.outWashTime || undefined,
      petBatchNo: reportForm.value.petBatchNo || undefined,
      petModel: reportForm.value.petModel || undefined,
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
      printLossLength: reportForm.value.printSampleLength ?? undefined,
      receiveLength: finalLength,
      recorderName: reportForm.value.recorderName || undefined,
      recorderTime: reportForm.value.recorderTime || undefined,
      remark: reportForm.value.remark || undefined,
      reportDate: reportForm.value.productionDate || undefined,
      startTime: reportForm.value.startTime || undefined,
    });
    bookingSubmitted = true;
  }
  task.value = {
    ...task.value,
    equipmentCode: reportForm.value.equipmentCode,
    equipmentId: reportForm.value.equipmentId,
    equipmentName: reportForm.value.equipmentName,
    endTime: reportForm.value.endTime,
    goodQty: finalLength,
    productionDate: reportForm.value.productionDate,
    recorderName: reportForm.value.recorderName,
    recorderTime: reportForm.value.recorderTime,
    reportRemark: reportForm.value.remark,
    startTime: reportForm.value.startTime,
    status: 'COMPLETED',
    operationReportId: operationReportId || task.value?.operationReportId,
    extraJson: JSON.stringify({
      ...parseWetExtraJson(task.value?.extraJson),
      abnormalPositionOption: abnormalPositionOption.value || undefined,
    }),
  };
  if (bookingSubmitted) {
    try {
      await updateWetConsumableUsageStatusesAfterBooking();
    } catch {
      message.warning(
        '湿法报工已提交，但边库耗材使用状态更新失败，请在边库台账中核对处理',
      );
    }
  }
  await loadWetPassWorkRows();
  await loadEquipmentStatusCard();
  emit('taskChange', { ...task.value });
  bookingDateTimeInputCache.value = {};
  promptPrintTransferTicketAfterBooking(finalLength);
}

async function handleBookingConfirm() {
  await nextTick();
  syncBookingDateTimeFieldsFromDom();
  if (!ensureBookingReportDateTimes()) {
    return;
  }
  pendingAction.value = 'SUBMIT_BOOKING';
  authVisible.value = true;
}

function finishWetBookingFlow() {
  modalApi.close();
  emit('refresh');
}

function promptPrintTransferTicketAfterBooking(finalLength: number) {
  AModal.confirm({
    cancelText: '暂不打印',
    content: `已记录本次湿法过站报工，收卷米数：${finalLength} ${task.value?.uom || 'm'}。是否立即打印工艺流转单和首检送检单？`,
    okText: '确认连续打印',
    onCancel: finishWetBookingFlow,
    onOk: async () => {
      await printWetBookingDocumentsAfterBooking();
      finishWetBookingFlow();
    },
    title: '湿法报工成功',
  });
}

const [PrintPreviewModal, printPreviewModalApi] = useVbenModal({
  connectedComponent: FormulaReportPrintPreviewModal,
});

const [FirstInspectionPrintPreviewModal, firstInspectionPrintPreviewModalApi] =
  useVbenModal({
    connectedComponent: WetFirstInspectionPrintPreviewModal,
  });

const [FaiDetailPreviewModal, faiDetailModalApi] = useVbenModal({
  connectedComponent: FaiDetailModal,
});

async function handlePrintClick() {
  if (!task.value?.planId) {
    AModal.warning({
      content: '当前任务未关联生产计划，无法生成打印预览。',
      title: '无法打印',
    });
    return;
  }
  const planDetail = await getPlanOrderDetail(task.value.planId as any);
  printPreviewModalApi
    .setData({
      planDetail,
      reportForm: { ...(reportForm.value || {}) },
      startForm: { ...(reportForm.value || {}) },
      task: { ...(task.value || {}) },
    })
    .open();
}

async function sendWetTransferTicketToPrintAgent(ticketPayload: any) {
  const response = await fetch(
    `${BATCHING_PRINT_AGENT_URL}/print/transfer-ticket`,
    {
      body: JSON.stringify(ticketPayload),
      headers: {
        'Content-Type': 'application/json',
      },
      method: 'POST',
    },
  );
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result?.success || !result?.accepted) {
    throw new Error(
      result?.message || `本机打印服务返回异常：${response.status}`,
    );
  }
  return result;
}

async function sendWetTransferTicketsToPrintAgent(ticketPayloads: any[]) {
  const response = await fetch(
    `${BATCHING_PRINT_AGENT_URL}/print/transfer-tickets`,
    {
      body: JSON.stringify({
        continueOnError: false,
        items: ticketPayloads,
      }),
      headers: {
        'Content-Type': 'application/json',
      },
      method: 'POST',
    },
  );
  const result = await response.json().catch(() => ({}));
  if (!response.ok || !result?.success || result?.accepted === false) {
    throw new Error(
      result?.message || `本机打印服务返回异常：${response.status}`,
    );
  }
  return result;
}

function formatWetPrintBatchPrinterText(result: any) {
  const names = Array.from(
    new Set(
      (Array.isArray(result?.itemResults) ? result.itemResults : [])
        .map((item: any) => item?.printerName)
        .filter(Boolean),
    ),
  );
  if (names.length > 0) return names.join('、');
  return result?.printerName || '配置打印机';
}

function resolveCurrentWetProcessName() {
  const equipmentProcessName =
    equipmentStatusCard.value?.currentOperationName &&
    equipmentStatusCard.value.currentOperationName !== '-'
      ? equipmentStatusCard.value.currentOperationName
      : '';
  return (
    task.value?.processName ||
    task.value?.operationName ||
    task.value?.currentOperationName ||
    task.value?.process ||
    equipmentProcessName ||
    '湿法'
  );
}

async function buildWetTransferTicketPayload() {
  const latestPlanDetail = task.value?.planId
    ? await getPlanOrderDetail(task.value.planId as any)
    : {};
  const planNo = task.value?.planNo || (latestPlanDetail as any)?.planNo || '';
  const materialCode =
    task.value?.motherMaterialCode ||
    (latestPlanDetail as any)?.motherMaterialCode ||
    task.value?.materialCode ||
    (latestPlanDetail as any)?.materialCode ||
    '';
  const modelCode =
    task.value?.motherModelCode ||
    (latestPlanDetail as any)?.motherModelCode ||
    task.value?.modelCode ||
    (latestPlanDetail as any)?.modelCode ||
    '';
  const productionBatchNo =
    task.value?.productionBatchNo ||
    task.value?.batchNo ||
    (latestPlanDetail as any)?.productionBatchNo ||
    '';
  const processName = resolveCurrentWetProcessName();
  const startTime =
    reportView.value.startTimeDisplay || task.value?.startTime || '';
  const endTime = reportView.value.endTimeDisplay || task.value?.endTime || '';
  const recorderName =
    reportForm.value.recorderName || task.value?.recorderName || '';
  const receiveLength = reportForm.value.receiveLength ?? task.value?.goodQty;
  const receiveLengthText = formatTransferTicketMetric(
    receiveLength,
    task.value?.uom || 'm',
  );
  const fallbackFields = buildTransferTicketFields({
    endTime,
    extraFields: [
      {
        label: '收卷米',
        value: receiveLengthText,
      },
    ],
    includePlanNo: false,
    materialCode,
    modelCode,
    planNo,
    processName,
    productionBatchNo,
    recorderName,
    startTime,
  });
  const fields = await applyPrintFieldTemplate('WET_TRANSFER', fallbackFields, {
    endTime,
    materialCode,
    modelCode,
    planNo,
    processName,
    productionBatchNo,
    receiveLength: receiveLengthText,
    recorderName,
    startTime,
  });
  return {
    copies: 1,
    endTime,
    fields,
    labelGapDots: 24,
    labelGapMm: 3,
    labelHeightMm: 50,
    labelWidthMm: 80,
    materialCode,
    modelCode,
    offsetXmm: 0,
    offsetYmm: 0,
    planNo,
    printerKey: 'wet',
    printMode: 'raw',
    processName,
    productionBatchNo,
    qrTopText: planNo || '-',
    qrValue: buildTransferTicketQrValue(planNo, productionBatchNo),
    rawProtocol: 'pplb',
    receiveLength,
    recorderName,
    startTime,
    title: '工艺流转单',
    waitForSpooler: true,
  };
}

async function buildWetFirstInspectionTransferTicketPayload() {
  const inspectionNo = firstInspection.value.faiNo || task.value?.faiNo || '';
  if (!inspectionNo) {
    throw new Error('当前首检记录还没有首检单号，无法打印首检送检单');
  }
  const modelCode = task.value?.motherModelCode || task.value?.modelCode || '';
  const productionBatchNo =
    firstInspection.value.productBatchNo ||
    task.value?.productionBatchNo ||
    task.value?.batchNo ||
    '';
  const processName = resolveCurrentWetProcessName();
  const applyTime =
    firstInspection.value.applyTime ||
    task.value?.faiApplyTime ||
    reportForm.value.recorderTime ||
    '';
  const napSampleLength =
    firstInspection.value.napSampleLength ?? reportForm.value.napSampleLength;
  const fallbackFields = [
    { label: '型号', value: modelCode },
    { label: '产品批次', value: productionBatchNo },
    { label: '当前工序', value: processName },
    { label: '送检时间', value: applyTime },
    { label: '送检(米)', value: napSampleLength ?? '-' },
    { label: '首检单号', value: inspectionNo },
  ];
  const fields = await applyPrintFieldTemplate(
    'WET_INSPECTION',
    fallbackFields,
    {
      applyTime,
      inspectionNo,
      modelCode,
      napSampleLength: napSampleLength ?? '-',
      processName,
      productionBatchNo,
    },
  );
  return buildInspectionTransferTicketPayload({
    applicantName: reportForm.value.recorderName || task.value?.recorderName,
    applyTime,
    fields,
    inspectionNo,
    inspectionTransferNo: inspectionNo,
    inspectionType: '研发',
    materialCode: task.value?.motherMaterialCode || task.value?.materialCode,
    modelCode,
    planNo: task.value?.planNo,
    printerKey: 'inspection',
    processName,
    productionBatchNo,
    sampleType: 'NAP层送样',
    title: '首检送检单',
  });
}

async function printWetBookingDocumentsAfterBooking() {
  try {
    const transferPayload = await buildWetTransferTicketPayload();
    const inspectionPayload =
      await buildWetFirstInspectionTransferTicketPayload();
    const result = await sendWetTransferTicketsToPrintAgent([
      transferPayload,
      inspectionPayload,
    ]);
    AModal.success({
      content: `工艺流转单和首检送检单已连续发送到 ${formatWetPrintBatchPrinterText(result)}，数量：${result.printedCount || 2}。`,
      okText: '知道了',
      title: '连续打印完成',
    });
  } catch (error: any) {
    AModal.warning({
      content: `未能连续打印流转单和首检送检单：${error?.message || error}。请确认当前任务数据完整，并已启动工序流转单 Python 打印服务。`,
      title: '连续打印失败',
    });
  }
}

async function printWetTransferTicketAfterBooking() {
  if (!canPrintTransferTicket.value) {
    AModal.warning({
      content: '请先完成湿法报工后再打印流转单。',
      title: '无法打印',
    });
    return;
  }
  if (!task.value?.planNo && !task.value?.planId) {
    AModal.warning({
      content: '当前任务缺少计划信息，无法打印流转单。',
      title: '无法打印',
    });
    return;
  }
  try {
    const ticketPayload = await buildWetTransferTicketPayload();
    const result = await sendWetTransferTicketToPrintAgent(ticketPayload);
    AModal.success({
      content: `湿法流转单已确认发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`,
      okText: '知道了',
      title: '打印流转单',
    });
  } catch (error: any) {
    AModal.warning({
      content: `未能生成或发送流转单：${error?.message || error}。请确认当前任务数据完整，并已启动工序流转单 Python 打印服务。`,
      title: '打印流转单失败',
    });
  }
}

function handleFirstInspectionPrintClick(row = firstInspection.value) {
  firstInspectionPrintPreviewModalApi
    .setData({
      firstInspection: { ...row },
      task: { ...(task.value || {}) },
    })
    .open();
}

const [Modal, modalApi] = useVbenModal({
  class: 'hc-workstation-detail-modal',
  closable: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  onOpenChange(isOpen) {
    wetDetailModalOpen = isOpen;
    if (isOpen) {
      modalApi.setState({ fullscreen: true });
      task.value = modalApi.getData<any>();
      planDetail.value = null;
      activeMainTab.value = 'prepare';
      viewMode.value = 'tabs';
      initStateFromTask();
      syncFirstInspectionAutoRefresh();
      void loadPlanDetailContext();
      void loadGuideClothRuntime();
      void loadEquipmentOptions();
      void loadEquipmentStatusCard();
      void loadWetPassWorkRows();
      void loadWetAbnormalPositions();
    } else {
      stopFirstInspectionAutoRefresh();
    }
  },
  showCancelButton: false,
  showConfirmButton: false,
});
</script>

<template>
  <Modal>
    <div class="pp-plan-modal">
      <div class="pp-plan-toolbar">
        <div class="pp-plan-toolbar__title">
          <span class="pp-plan-toolbar__main">湿法报工工作台</span>
          <span v-if="task?.productionStartDate" class="pp-plan-toolbar__sub"
            >计划生产日期：{{ task.productionStartDate }}</span
          >
          <button
            class="wet-first-inspection-status"
            type="button"
            title="点击查看首检详情"
            @click="goFirstInspectionTab"
          >
            <span>首检状态</span>
            <Tag :color="getFirstInspectionStatusMeta().color" class="!m-0">
              {{ getFirstInspectionStatusMeta().text }}
            </Tag>
          </button>
        </div>

        <div class="pp-plan-toolbar__actions">
          <template v-if="viewMode === 'tabs'">
            <Button
              size="small"
              :disabled="
                task?.status === 'COMPLETED' || task?.status === 'IN_PROGRESS'
              "
              @click="handleStartClick"
            >
              <IconifyIcon icon="lucide:play" class="mr-1" />
              {{
                task?.status === 'IN_PROGRESS' ? '已开工生产中' : '执行开工确认'
              }}
            </Button>
            <Button
              size="small"
              type="primary"
              danger
              :disabled="task?.status === 'COMPLETED'"
              @click="handleFinishClick"
            >
              <IconifyIcon icon="lucide:check-square" class="mr-1" /> 过站报工
            </Button>
            <Button
              size="small"
              :disabled="!isStarted() || task?.status === 'COMPLETED'"
              @click="openDeviceSwitchModal"
            >
              <IconifyIcon icon="lucide:repeat" class="mr-1" /> 切换工位设备
            </Button>
            <Button
              v-access:code="['mes:sfc:wet-report:report-time:update']"
              size="small"
              :disabled="!isStarted()"
              @click="openTimeCorrectionModal"
            >
              <IconifyIcon icon="lucide:clock-3" class="mr-1" />
              修订开工完工时间
            </Button>
            <Button
              v-if="canReviseReportQuantity"
              v-access:code="['mes:sfc:wet-report:statistics-data:revise']"
              size="small"
              @click="openQuantityRevisionModal"
            >
              <IconifyIcon icon="lucide:chart-no-axes-column" class="mr-1" />
              修订统计数据
            </Button>
            <Button
              size="small"
              title="查看修正日志"
              :disabled="!isStarted()"
              @click="openTimeLogModal"
            >
              <IconifyIcon icon="lucide:history" />
            </Button>
            <Button
              size="small"
              :disabled="!canConfirmReport"
              @click="handleReportConfirmClick"
            >
              <IconifyIcon icon="lucide:badge-check" class="mr-1" /> 确认
            </Button>
            <Button size="small" @click="handlePrintClick">
              <IconifyIcon icon="lucide:file-down" class="mr-1" /> 导出工单
            </Button>
            <Button
              v-if="canPrintTransferTicket"
              size="small"
              @click="printWetTransferTicketAfterBooking"
            >
              <IconifyIcon icon="lucide:qr-code" class="mr-1" /> 打印流转单
            </Button>
            <Button size="small" @click="modalApi.close()">
              <IconifyIcon icon="lucide:x" class="mr-1" /> 关闭窗口
            </Button>
          </template>
          <template v-else>
            <Button size="small" @click="viewMode = 'tabs'">
              <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回过程作业
            </Button>
            <Button size="small" @click="handlePrintClick">
              <IconifyIcon icon="lucide:file-down" class="mr-1" /> 导出工单
            </Button>
            <Button
              v-if="canPrintTransferTicket"
              size="small"
              @click="printWetTransferTicketAfterBooking"
            >
              <IconifyIcon icon="lucide:qr-code" class="mr-1" /> 打印流转单
            </Button>
            <Button size="small" @click="modalApi.close()">
              <IconifyIcon icon="lucide:x" class="mr-1" /> 关闭窗口
            </Button>
          </template>
        </div>
      </div>

      <div v-show="viewMode === 'tabs'" class="pp-plan-body">
        <QmsSampleAbnormalLockGuard
          defer-to-cut-round
          ref="wetSampleLockGuardRef"
          class="wet-sample-abnormal-lock-guard"
          object-label="母卷"
          :object-no="wetSampleLockObjectNo"
          object-type="MOTHER_ROLL"
          process-name="湿法"
          source-process-code="WET"
        >
          <div
            v-if="previousOperationInfo || firstInspectionBannerStatus.visible"
            class="wet-prev-op-bar wet-prev-op-bar--qtime"
          >
            <div
              class="wet-prev-op-bar__content wet-prev-op-bar__content--qtime"
            >
              <div
                v-if="previousOperationInfo"
                class="wet-prev-op-bar__item wet-prev-op-bar__item--qtime"
              >
                <span class="wet-prev-op-bar__label">
                  <IconifyIcon icon="lucide:timer" class="mr-1" />上道工序时长
                </span>
                <span
                  :class="[
                    'wet-prev-op-bar__qtime-value',
                    previousOperationInfo.qtime.levelClass,
                  ]"
                >
                  {{ previousOperationInfo.qtime.durationText }}
                </span>
              </div>
              <div
                v-if="firstInspectionBannerStatus.visible"
                class="wet-prev-op-bar__item wet-prev-op-bar__item--qtime"
              >
                <span class="wet-prev-op-bar__label">
                  <IconifyIcon icon="lucide:clipboard-check" class="mr-1" />首检
                </span>
                <span
                  :class="[
                    'wet-prev-op-bar__fai-status',
                    firstInspectionBannerStatus.className,
                  ]"
                >
                  {{ firstInspectionBannerStatus.text }}
                </span>
              </div>
            </div>
          </div>

          <fieldset class="pp-fieldset">
            <legend>1. 基本信息</legend>
            <div class="pp-form-grid">
              <div class="pp-form-item">
                <label>生产计划号</label>
                <div class="pp-readonly-box pp-readonly-box--code">
                  {{ task?.planNo || task?.id || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>产品型号</label>
                <div class="pp-readonly-box pp-readonly-box--code">
                  {{ task?.motherModelCode || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>{{ batchNoLabel }}</label>
                <div class="pp-readonly-box pp-readonly-box--code">
                  {{ task?.productionBatchNo || task?.batchNo || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>产品料号</label>
                <div class="pp-readonly-box">
                  {{ task?.motherMaterialCode || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>状态</label>
                <div class="pp-readonly-box">
                  <Tag :color="displayStatus.color" class="!m-0">{{
                    displayStatus.text
                  }}</Tag>
                </div>
              </div>
              <div class="pp-form-item">
                <label>生产日期</label>
                <div class="pp-readonly-box">
                  {{ reportView.productionDateDisplay || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>开始时间</label>
                <div class="pp-readonly-box">
                  {{ reportView.startTimeDisplay || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>结束时间</label>
                <div class="pp-readonly-box">
                  {{ reportView.endTimeDisplay || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>记录人</label>
                <div class="pp-readonly-box">
                  {{ reportForm.recorderName || '-' }}
                </div>
              </div>
              <div class="pp-form-item">
                <label>记录时间</label>
                <div class="pp-readonly-box">
                  {{ reportView.recorderTimeDisplay || '-' }}
                </div>
              </div>
              <div class="pp-form-item pp-form-item--full">
                <label>执行要求</label>
                <div class="pp-readonly-box pp-readonly-box--multiline">
                  {{ task?.requirements || '-' }}
                </div>
              </div>
            </div>
          </fieldset>

          <div class="wet-tabs-shell">
            <Tabs
              v-model:activeKey="activeMainTab"
              class="custom-main-tabs flex h-full flex-col"
            >
              <TabPane key="prepare" tab="开机/清洁点检" class="h-full">
                <div class="pass-work-panel">
                  <div class="pp-panel">
                    <div class="pp-panel__header">
                      <div class="pp-panel__title">
                        <IconifyIcon
                          icon="lucide:clipboard-check"
                          class="mr-2 text-[#1677ff]"
                        />
                        开机/清洁点检
                      </div>
                      <div class="pp-panel__desc">
                        湿法段设备开机点检表、湿法段设备清洁点检表合并展示
                      </div>
                    </div>
                    <div
                      class="pp-table-wrap pass-work-list-wrap pore-self-check-table-wrap"
                    >
                      <table class="pp-grid">
                        <thead>
                          <tr>
                            <th width="260">表单名称</th>
                            <th width="130">执行时机</th>
                            <th width="120">单据状态</th>
                            <th width="120">执行结果</th>
                            <th width="180">记录人/时间</th>
                            <th width="180">确认人/确认时间</th>
                            <th width="240">操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="record in workPrepareRows"
                            :key="record.id"
                          >
                            <td>
                              {{ getWetPassWorkDisplayName(record) || '-' }}
                            </td>
                            <td align="center">{{ record.timing || '-' }}</td>
                            <td align="center">
                              <Tag
                                :color="getStatusMeta(record.status).color"
                                class="!m-0"
                                >{{ getStatusMeta(record.status).text }}</Tag
                              >
                            </td>
                            <td align="center">{{ record.result || '-' }}</td>
                            <td>
                              <div>{{ record.recorder || '-' }}</div>
                              <div class="pp-grid__sub">
                                {{
                                  normalizeDateTime(record.recorderTime) || '-'
                                }}
                              </div>
                            </td>
                            <td>
                              <div>{{ record.confirmer || '-' }}</div>
                              <div class="pp-grid__sub">
                                {{
                                  normalizeDateTime(record.confirmerTime) || '-'
                                }}
                              </div>
                            </td>
                            <td align="center">
                              <div class="pass-work-actions">
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="record.status === 'COMPLETED'"
                                  @click="openRecord(record, 'prepare', 'edit')"
                                >
                                  填写
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="record.status === 'COMPLETED'"
                                  @click="
                                    openRecord(record, 'prepare', 'confirm')
                                  "
                                >
                                  确认
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  @click="openRecord(record, 'prepare', 'view')"
                                  >查看</Button
                                >
                              </div>
                            </td>
                          </tr>
                          <tr v-if="!workPrepareRows.length">
                            <td
                              colspan="7"
                              class="pp-empty-cell"
                              align="center"
                            >
                              暂无过站工作数据
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </TabPane>

              <TabPane key="pore-self-check" tab="泡孔自检" class="h-full">
                <div class="pass-work-panel pore-self-check-panel">
                  <div class="pp-panel">
                    <div class="pp-panel__header">
                      <div class="pp-panel__header-main">
                        <div class="pp-panel__title">
                          <IconifyIcon
                            icon="lucide:image-check"
                            class="mr-2 text-[#1677ff]"
                          />
                          泡孔自检
                        </div>
                        <div class="pp-panel__desc">
                          报工前必须至少新增一条泡孔自检记录
                        </div>
                      </div>
                      <Button
                        v-if="task?.status !== 'COMPLETED'"
                        size="small"
                        type="primary"
                        @click="openPoreSelfCheckCreate"
                      >
                        <IconifyIcon icon="lucide:plus" class="mr-1" />
                        新建
                      </Button>
                    </div>
                    <div
                      class="pp-table-wrap pass-work-list-wrap pore-self-check-fill-table-wrap"
                    >
                      <table class="pp-grid">
                        <thead>
                          <tr>
                            <th width="180">自检时间</th>
                            <th width="140">检验人</th>
                            <th>备注</th>
                            <th width="220">泡孔照片</th>
                            <th width="110">操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="row in poreSelfCheckRows"
                            :key="row.clientKey"
                          >
                            <td align="center">
                              {{ normalizeDateTime(row.selfCheckTime) || '-' }}
                            </td>
                            <td>{{ row.inspector || '-' }}</td>
                            <td>{{ row.remark || '-' }}</td>
                            <td class="pore-photo-cell">
                              <div
                                v-if="row.photos.length"
                                class="pore-photo-summary"
                              >
                                <button
                                  class="pore-photo-thumb"
                                  type="button"
                                  @click="openPorePhotoPreview(row.photos)"
                                >
                                  <img :src="row.photos[0]" alt="泡孔照片" />
                                  <span>预览</span>
                                </button>
                                <span>共 {{ row.photos.length }} 张</span>
                              </div>
                              <span v-else>-</span>
                            </td>
                            <td align="center">
                              <Button
                                size="small"
                                type="link"
                                @click="openPoreSelfCheckView(row)"
                                >查看</Button
                              >
                            </td>
                          </tr>
                          <tr v-if="!poreSelfCheckRows.length">
                            <td
                              colspan="5"
                              class="pp-empty-cell"
                              align="center"
                            >
                              暂无泡孔自检记录
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                    <div class="pore-self-check-status">
                      <span>单据状态</span>
                      <Tag
                        :color="getStatusMeta(poreSelfCheckMeta.status).color"
                        class="!m-0"
                      >
                        {{ getStatusMeta(poreSelfCheckMeta.status).text }}
                      </Tag>
                      <span
                        >最近结果：{{ poreSelfCheckMeta.result || '-' }}</span
                      >
                    </div>
                  </div>
                </div>
              </TabPane>

              <TabPane key="production-check" tab="工艺参数点检" class="h-full">
                <div class="pass-work-panel">
                  <div class="pp-panel pp-panel--form-list">
                    <div class="pp-panel__header">
                      <div class="pp-panel__title">
                        <IconifyIcon
                          icon="lucide:clipboard-check"
                          class="mr-2 text-[#1677ff]"
                        />
                        工艺参数点检
                      </div>
                      <div class="pp-panel__desc">
                        湿法工艺参数点检表动态记录当前工艺关键参数与过程状态
                      </div>
                    </div>
                    <div class="wet-form-filterbar">
                      <Input
                        v-model:value="productionCheckFormNameFilter"
                        allow-clear
                        class="wet-form-filterbar__input"
                        placeholder="按表单名/型号过滤"
                        size="small"
                      />
                      <span class="wet-form-filterbar__meta">
                        {{ filteredProductionCheckRows.length }}/{{
                          productionCheckRows.length
                        }}
                      </span>
                    </div>
                    <div
                      class="pp-table-wrap pass-work-list-wrap wet-form-list-wrap"
                    >
                      <table class="pp-grid wet-form-list-table">
                        <thead>
                          <tr>
                            <th width="260">表单名称</th>
                            <th width="130">执行时机</th>
                            <th width="120">单据状态</th>
                            <th width="120">执行结果</th>
                            <th width="180">记录人/时间</th>
                            <th width="180">确认人/确认时间</th>
                            <th width="180">操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="record in filteredProductionCheckRows"
                            :key="record.id"
                          >
                            <td>
                              {{ getWetPassWorkDisplayName(record) || '-' }}
                            </td>
                            <td align="center">{{ record.timing || '-' }}</td>
                            <td align="center">
                              <Tag
                                :color="getStatusMeta(record.status).color"
                                class="!m-0"
                                >{{ getStatusMeta(record.status).text }}</Tag
                              >
                            </td>
                            <td align="center">{{ record.result || '-' }}</td>
                            <td>
                              <div>{{ record.recorder || '-' }}</div>
                              <div class="pp-grid__sub">
                                {{
                                  normalizeDateTime(record.recorderTime) || '-'
                                }}
                              </div>
                            </td>
                            <td>
                              <div>{{ record.confirmer || '-' }}</div>
                              <div class="pp-grid__sub">
                                {{
                                  normalizeDateTime(record.confirmerTime) || '-'
                                }}
                              </div>
                            </td>
                            <td align="center">
                              <div class="pass-work-actions">
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="record.status === 'COMPLETED'"
                                  @click="
                                    openRecord(
                                      record,
                                      'production-check',
                                      'edit',
                                    )
                                  "
                                >
                                  填写
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="record.status === 'COMPLETED'"
                                  :loading="
                                    modbusCollecting &&
                                    currentRecord?.id === record.id
                                  "
                                  @click="
                                    openProductionCheckRecordAndCollect(record)
                                  "
                                >
                                  <IconifyIcon
                                    icon="lucide:radio-receiver"
                                    class="mr-1"
                                  />
                                  记录参数
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="record.status === 'COMPLETED'"
                                  @click="
                                    openRecord(
                                      record,
                                      'production-check',
                                      'confirm',
                                    )
                                  "
                                >
                                  确认
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  @click="
                                    openRecord(
                                      record,
                                      'production-check',
                                      'view',
                                    )
                                  "
                                  >查看</Button
                                >
                              </div>
                            </td>
                          </tr>
                          <tr v-if="!filteredProductionCheckRows.length">
                            <td
                              colspan="7"
                              class="pp-empty-cell"
                              align="center"
                            >
                              暂无匹配的工艺参数点检表
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </TabPane>

              <TabPane key="semi-finished" tab="半成品记录单" class="h-full">
                <div class="pass-work-panel">
                  <div class="pp-panel pp-panel--form-list">
                    <div class="pp-panel__header">
                      <div class="pp-panel__title">
                        <IconifyIcon
                          icon="lucide:clipboard-check"
                          class="mr-2 text-[#1677ff]"
                        />
                        半成品记录单
                      </div>
                      <div class="pp-panel__desc">
                        凝固半成品记录表、烘箱半成品记录表按当前型号模板初始化长度明细
                      </div>
                    </div>
                    <div class="wet-form-filterbar">
                      <Input
                        v-model:value="semiFinishedFormNameFilter"
                        allow-clear
                        class="wet-form-filterbar__input"
                        placeholder="按表单名/型号过滤"
                        size="small"
                      />
                      <span class="wet-form-filterbar__meta">
                        {{ filteredSemiFinishedRows.length }}/{{
                          semiFinishedRows.length
                        }}
                      </span>
                    </div>
                    <div
                      class="pp-table-wrap pass-work-list-wrap wet-form-list-wrap"
                    >
                      <table
                        class="pp-grid wet-form-list-table wet-form-list-table--semi"
                      >
                        <thead>
                          <tr>
                            <th width="260">表单名称</th>
                            <th width="130">执行时机</th>
                            <th width="120">单据状态</th>
                            <th width="180">记录人/记录时间</th>
                            <th width="180">确认人/确认时间</th>
                            <th width="180">操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="record in filteredSemiFinishedRows"
                            :key="record.id"
                          >
                            <td>
                              {{ getWetPassWorkDisplayName(record) || '-' }}
                            </td>
                            <td align="center">{{ record.timing || '-' }}</td>
                            <td align="center">
                              <Tag
                                :color="getStatusMeta(record.status).color"
                                class="!m-0"
                                >{{ getStatusMeta(record.status).text }}</Tag
                              >
                            </td>
                            <td>
                              <div>{{ record.recorder || '-' }}</div>
                              <div class="pp-grid__sub">
                                {{
                                  normalizeDateTime(record.recorderTime) || '-'
                                }}
                              </div>
                            </td>
                            <td>
                              <div>{{ record.confirmer || '-' }}</div>
                              <div class="pp-grid__sub">
                                {{
                                  normalizeDateTime(record.confirmerTime) || '-'
                                }}
                              </div>
                            </td>
                            <td align="center">
                              <div class="pass-work-actions">
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="record.status === 'COMPLETED'"
                                  @click="
                                    openRecord(record, 'semi-finished', 'edit')
                                  "
                                >
                                  填写
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="record.status === 'COMPLETED'"
                                  @click="
                                    openRecord(
                                      record,
                                      'semi-finished',
                                      'confirm',
                                    )
                                  "
                                >
                                  确认
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  @click="
                                    openRecord(record, 'semi-finished', 'view')
                                  "
                                  >查看</Button
                                >
                              </div>
                            </td>
                          </tr>
                          <tr v-if="!filteredSemiFinishedRows.length">
                            <td
                              colspan="6"
                              class="pp-empty-cell"
                              align="center"
                            >
                              暂无匹配的半成品记录单
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </TabPane>

              <TabPane key="report-info" tab="报工信息" class="h-full">
                <div class="pass-work-panel">
                  <div class="pp-panel">
                    <div class="pp-panel__header">
                      <div class="pp-panel__title">
                        <IconifyIcon
                          icon="lucide:clipboard-check"
                          class="mr-2 text-[#1677ff]"
                        />
                        报工信息
                      </div>
                      <div class="pp-panel__desc">
                        湿法过站报工信息与工艺参数回显
                      </div>
                    </div>
                    <div class="wet-report-info-grid">
                      <div class="pp-form-item pp-form-item--full">
                        <label>计划单号</label>
                        <div class="pp-readonly-box pp-readonly-box--code">
                          {{ task?.planNo || task?.id || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>生产日期</label>
                        <div class="pp-readonly-box">
                          {{ reportView.productionDateDisplay || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>开始时间</label>
                        <div class="pp-readonly-box">
                          {{ reportView.startTimeDisplay || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>结束时间</label>
                        <div class="pp-readonly-box">
                          {{ reportView.endTimeDisplay || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>机台编号</label>
                        <div class="pp-readonly-box">
                          {{ currentMachineCode }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label class="wet-report-emphasis-label"
                          >收卷米数(报工数)</label
                        >
                        <div class="pp-readonly-box">
                          {{ reportForm.receiveLength ?? '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label class="wet-report-emphasis-label"
                          >NAP层送检(米)</label
                        >
                        <div class="pp-readonly-box">
                          {{ reportForm.napSampleLength ?? '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label class="wet-report-emphasis-label"
                          >固定损耗（米）</label
                        >
                        <div class="pp-readonly-box">
                          {{ reportForm.printSampleLength ?? '-' }}
                        </div>
                        <div
                          class="wet-abnormal-option wet-abnormal-option--under-loss"
                        >
                          <span class="wet-abnormal-option__label"
                            >异常位置</span
                          >
                          <Tag
                            :color="
                              abnormalPositionOption === 'DETAIL'
                                ? 'warning'
                                : abnormalPositionOption === 'NONE'
                                  ? 'success'
                                  : 'default'
                            "
                          >
                            {{ abnormalPositionOptionText }}
                          </Tag>
                          <Tooltip title="查看异常位置明细">
                            <Button
                              aria-label="查看异常位置明细"
                              shape="circle"
                              size="small"
                              @click="openAbnormalPositionModal"
                            >
                              <IconifyIcon icon="lucide:map-pin" />
                            </Button>
                          </Tooltip>
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label class="wet-report-emphasis-label">PET型号</label>
                        <div class="pp-readonly-box">
                          {{ reportForm.petModel || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label class="wet-report-emphasis-label">PET批号</label>
                        <div class="pp-readonly-box">
                          {{ reportForm.petBatchNo || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item pp-form-item--span-2">
                        <label>报工备注</label>
                        <div class="pp-readonly-box pp-readonly-box--multiline">
                          {{ reportForm.remark || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>记录人</label>
                        <div class="pp-readonly-box">
                          {{ reportForm.recorderName || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>记录时间</label>
                        <div class="pp-readonly-box">
                          {{ reportView.recorderTimeDisplay || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>确认人</label>
                        <div class="pp-readonly-box">
                          {{ reportForm.confirmerName || '-' }}
                        </div>
                      </div>
                      <div class="pp-form-item">
                        <label>确认时间</label>
                        <div class="pp-readonly-box">
                          {{ reportView.confirmerTimeDisplay || '-' }}
                        </div>
                      </div>
                    </div>
                    <fieldset class="pp-fieldset wet-guide-life-fieldset">
                      <legend>导布寿命</legend>
                      <div
                        class="wet-guide-life-tip"
                        :class="{ 'is-warning': guideClothWarningFlag }"
                      >
                        <IconifyIcon
                          icon="lucide:info"
                          class="wet-guide-life-tip__icon"
                        />
                        <span>{{ guideClothWarningText }}</span>
                      </div>
                      <div class="wet-guide-life-grid">
                        <div class="pp-form-item">
                          <label>导布批号</label>
                          <div class="pp-readonly-box">
                            {{ reportForm.guideClothBatchNo || '-' }}
                          </div>
                        </div>
                        <div class="pp-form-item">
                          <label>上次更换时间</label>
                          <div class="pp-readonly-box">
                            {{ guideClothLastReplaceTimeText }}
                          </div>
                        </div>
                        <div class="pp-form-item">
                          <label>更换提醒状态</label>
                          <div class="pp-readonly-box">
                            {{ guideClothMonthlyStatusText }}
                          </div>
                        </div>
                        <div class="pp-form-item">
                          <label>累计使用次数</label>
                          <div class="pp-readonly-box">
                            {{ guideClothProjectedUseCount }}/{{
                              GUIDE_CLOTH_MAX_USE_COUNT
                            }}
                          </div>
                        </div>
                        <div class="pp-form-item">
                          <label>是否更换</label>
                          <div class="pp-readonly-box">
                            {{
                              reportForm.guideClothChanged === 'Y' ? '是' : '否'
                            }}
                          </div>
                        </div>
                        <div class="pp-form-item pp-form-item--span-3">
                          <label>导布更换原因</label>
                          <div class="pp-readonly-box">
                            {{ reportForm.guideClothChangeReason || '-' }}
                          </div>
                        </div>
                      </div>
                    </fieldset>
                  </div>
                </div>
              </TabPane>

              <TabPane key="first-inspection" tab="首检检验单" class="h-full">
                <div class="pass-work-panel">
                  <div class="pp-panel">
                    <div class="pp-panel__header">
                      <div class="pp-panel__header-main">
                        <div class="pp-panel__title">
                          <IconifyIcon
                            icon="lucide:clipboard-check"
                            class="mr-2 text-[#1677ff]"
                          />
                          首检检验单
                        </div>
                        <div class="pp-panel__desc">
                          湿法只发起和展示首检；检测项目与录入仍由 FAI
                          首件检验单维护
                        </div>
                      </div>
                      <div class="pp-panel__desc">
                        <Button
                          size="small"
                          :disabled="!hasSubmittedFirstInspection()"
                          :loading="firstInspectionRefreshing"
                          @click="refreshWetFaiSummary(true)"
                        >
                          <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
                          刷新状态
                        </Button>
                      </div>
                    </div>
                    <div class="pp-table-wrap pass-work-list-wrap">
                      <table class="pp-grid">
                        <thead>
                          <tr>
                            <th width="220">表单名称</th>
                            <th width="170">首检单号</th>
                            <th width="140">单据状态</th>
                            <th width="170">采用标准</th>
                            <th width="150">NAP层送检(米)</th>
                            <th>处理结果</th>
                            <th width="180">最近回写时间</th>
                            <th width="260">操作</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="(row, index) in firstInspectionRecords"
                            :key="row.faiId || `first-inspection-${index}`"
                          >
                            <td>
                              首检检验单
                              <Tag
                                v-if="row.isCurrent && row.faiId"
                                class="!m-0 ml-2"
                                color="processing"
                              >
                                当前
                              </Tag>
                            </td>
                            <td align="center">
                              <Button
                                v-if="row.faiId"
                                size="small"
                                type="link"
                                @click="viewFirstInspectionDetail(row)"
                              >
                                {{ row.faiNo || '-' }}
                              </Button>
                              <span v-else>-</span>
                            </td>
                            <td align="center">
                              <Tag
                                :color="getFirstInspectionStatusMeta(row).color"
                                class="!m-0"
                                >{{
                                  getFirstInspectionStatusMeta(row).text
                                }}</Tag
                              >
                            </td>
                            <td align="center">
                              {{ row.standardText || '-' }}
                            </td>
                            <td align="center">
                              {{ formatOptionalMeter(row.napSampleLength) }}
                            </td>
                            <td>
                              {{
                                !row.faiId
                                  ? '点击提交首检申请后，系统会自动创建 FAI 首件检验单。'
                                  : row.result || '-'
                              }}
                            </td>
                            <td align="center">
                              {{ row.returnTime || row.inspectTime || '-' }}
                            </td>
                            <td align="center">
                              <div class="pass-work-actions inspection-actions">
                                <Button
                                  v-if="row.isCurrent"
                                  size="small"
                                  type="link"
                                  :disabled="
                                    Boolean(row.faiId) &&
                                    !canReapplyFirstInspection()
                                  "
                                  :loading="firstInspectionApplying"
                                  @click="submitFirstInspection"
                                >
                                  {{ firstInspectionActionText() }}
                                </Button>
                                <Button
                                  v-else
                                  size="small"
                                  type="link"
                                  disabled
                                >
                                  历史单据
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="!row.faiId"
                                  @click="handleFirstInspectionPrintClick(row)"
                                >
                                  打印检验单
                                </Button>
                                <Button
                                  size="small"
                                  type="link"
                                  :disabled="!row.faiId"
                                  @click="viewFirstInspectionDetail(row)"
                                >
                                  查看首检报告
                                </Button>
                              </div>
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </TabPane>

              <TabPane key="abnormal-position" tab="异常位置" class="h-full">
                <div class="pass-work-panel">
                  <div class="pp-panel">
                    <div class="pp-panel__header">
                      <div class="pp-panel__title">
                        <IconifyIcon
                          icon="lucide:map-pin"
                          class="mr-2 text-[#1677ff]"
                        />
                        异常位置
                      </div>
                      <div class="pp-panel__desc">
                        湿法固定损耗对应异常位置明细
                      </div>
                    </div>
                    <div
                      class="wet-abnormal-option wet-abnormal-option--readonly"
                    >
                      <span class="wet-abnormal-option__label"
                        >异常位置选项</span
                      >
                      <Tag
                        :color="
                          abnormalPositionOption === 'DETAIL'
                            ? 'warning'
                            : abnormalPositionOption === 'NONE'
                              ? 'success'
                              : 'default'
                        "
                      >
                        {{ abnormalPositionOptionText }}
                      </Tag>
                    </div>
                    <div class="pp-table-wrap">
                      <table class="pp-grid">
                        <thead>
                          <tr>
                            <th width="80">序号</th>
                            <th>异常位置</th>
                            <th width="160">米数</th>
                            <th>备注</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="(row, index) in abnormalPositionRows"
                            :key="row.clientKey"
                          >
                            <td align="center">{{ index + 1 }}</td>
                            <td>{{ row.positionText || '-' }}</td>
                            <td align="right">
                              {{ row.abnormalLength ?? '-' }}
                            </td>
                            <td>{{ row.remark || '-' }}</td>
                          </tr>
                          <tr v-if="!abnormalPositionRows.length">
                            <td
                              colspan="4"
                              class="pp-empty-cell"
                              align="center"
                            >
                              暂无异常位置明细
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </TabPane>
            </Tabs>
          </div>
        </QmsSampleAbnormalLockGuard>
      </div>

      <div
        v-if="viewMode === 'booking'"
        ref="bookingTimeFormRef"
        class="pp-plan-body"
      >
        <fieldset class="pp-fieldset">
          <legend>报工信息</legend>
          <div class="wet-report-booking-grid">
            <div class="pp-form-item">
              <label>计划单号</label>
              <div class="pp-readonly-box pp-readonly-box--code">
                {{ task?.planNo || task?.id || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>产品型号</label>
              <div class="pp-readonly-box pp-readonly-box--code">
                {{ task?.motherModelCode || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>{{ batchNoLabel }}</label>
              <div class="pp-readonly-box pp-readonly-box--code">
                {{ task?.productionBatchNo || task?.batchNo || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>产品料号</label>
              <div class="pp-readonly-box">
                {{ task?.motherMaterialCode || '-' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>执行要求</label>
              <div class="pp-readonly-box">{{ task?.requirements || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>生产日期</label>
              <div
                data-wet-report-time-field="productionDate"
                @input="
                  handleBookingDateTimeInput('productionDate', 'date', $event)
                "
              >
                <DatePicker
                  v-model:value="reportForm.productionDate"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                  class="w-full"
                  @change="
                    (value, dateString) =>
                      handleBookingDateTimeChange(
                        'productionDate',
                        'date',
                        value,
                        dateString,
                      )
                  "
                />
              </div>
            </div>
            <div class="pp-form-item">
              <label>开始时间</label>
              <div
                data-wet-report-time-field="startTime"
                @input="
                  handleBookingDateTimeInput('startTime', 'datetime', $event)
                "
              >
                <DatePicker
                  v-model:value="reportForm.startTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  @change="
                    (value, dateString) =>
                      handleBookingDateTimeChange(
                        'startTime',
                        'datetime',
                        value,
                        dateString,
                      )
                  "
                />
              </div>
            </div>
            <div class="pp-form-item">
              <label>完工时间</label>
              <div
                data-wet-report-time-field="endTime"
                @input="
                  handleBookingDateTimeInput('endTime', 'datetime', $event)
                "
              >
                <DatePicker
                  v-model:value="reportForm.endTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  @change="
                    (value, dateString) =>
                      handleBookingDateTimeChange(
                        'endTime',
                        'datetime',
                        value,
                        dateString,
                      )
                  "
                />
              </div>
            </div>
            <div class="pp-form-item">
              <label>机台编号</label>
              <Select
                v-model:value="reportForm.equipmentId"
                :options="equipmentOptions"
                :field-names="{ label: 'label', value: 'value' }"
                placeholder="请选择机台编号"
                :get-popup-container="resolvePopupContainer"
              />
            </div>
            <div class="pp-form-item">
              <label class="wet-report-emphasis-label">收卷米数(报工数)</label>
              <InputNumber
                v-model:value="reportForm.receiveLength"
                class="w-full"
                :min="0"
                :precision="3"
              />
            </div>
            <div class="pp-form-item">
              <label class="wet-report-emphasis-label">NAP层送检(米)</label>
              <InputNumber
                v-model:value="reportForm.napSampleLength"
                class="w-full"
                :min="0"
                :precision="3"
              />
            </div>
            <div class="pp-form-item">
              <label class="wet-report-emphasis-label">固定损耗（米）</label>
              <InputNumber
                v-model:value="reportForm.printSampleLength"
                class="w-full"
                :min="0"
                :precision="3"
              />
            </div>
            <div class="pp-form-item pp-form-item--span-3 wet-pet-guide-note">
              <span class="wet-pet-guide-note__title">备注备注：</span>
              <span
                class="wet-pet-guide-note__content"
                title="1. PET为一次性使用，禁止使用回收PET；2. 如无特殊情况，湿法车间导布每月更换一次，或者累计使用次数≤28次；如导布表面出现明显片状脏污、导布边缘裂口长度≥2cm，需立即更换导布；"
              >
                1. PET为一次性使用，禁止使用回收PET；2.
                如无特殊情况，湿法车间导布每月更换一次，或者累计使用次数≤28次；如导布表面出现明显片状脏污、导布边缘裂口长度≥2cm，需立即更换导布；
              </span>
            </div>
            <div class="pp-form-item wet-abnormal-option-cell">
              <div class="wet-abnormal-option wet-abnormal-option--under-loss">
                <RadioGroup
                  :value="abnormalPositionOption"
                  class="pp-radio-group"
                  @update:value="handleAbnormalPositionOptionChange"
                >
                  <Radio value="NONE">无</Radio>
                  <Radio value="DETAIL">填写异常位置</Radio>
                </RadioGroup>
                <Tooltip title="填写异常位置明细">
                  <Button
                    aria-label="填写异常位置明细"
                    shape="circle"
                    size="small"
                    :disabled="abnormalPositionOption === 'NONE'"
                    @click="openAbnormalPositionModal"
                  >
                    <IconifyIcon icon="lucide:map-pin" />
                  </Button>
                </Tooltip>
              </div>
            </div>
            <div class="pp-form-item">
              <label class="wet-report-emphasis-label">PET型号</label>
              <Input v-model:value="reportForm.petModel" />
            </div>
            <div class="pp-form-item">
              <label class="wet-report-emphasis-label">PET批号</label>
              <Input
                v-model:value="reportForm.petBatchNo"
                @update:value="clearWetConsumableSelection('PET')"
              >
                <template #suffix>
                  <Tooltip title="选择湿法PET边库批次">
                    <Button
                      class="wet-consumable-picker-button"
                      size="small"
                      type="text"
                      @click.stop="openWetConsumableLedger('PET')"
                    >
                      <IconifyIcon icon="lucide:package-search" />
                    </Button>
                  </Tooltip>
                </template>
              </Input>
            </div>
            <div class="pp-form-item pp-form-item--span-2">
              <label>报工备注</label>
              <Input v-model:value="reportForm.remark" />
            </div>
            <div class="pp-form-item">
              <label>记录人</label>
              <Input v-model:value="reportForm.recorderName" />
            </div>
            <div class="pp-form-item">
              <label>记录时间</label>
              <div class="pp-readonly-box">
                {{ reportView.recorderTimeDisplay || '以最终提交认证时间为准' }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>确认人</label>
              <Input v-model:value="reportForm.confirmerName" disabled />
            </div>
            <div class="pp-form-item">
              <label>确认时间</label>
              <div data-wet-report-time-field="confirmerTime">
                <DatePicker
                  v-model:value="reportForm.confirmerTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  disabled
                />
              </div>
            </div>
          </div>
        </fieldset>

        <fieldset class="pp-fieldset wet-guide-life-fieldset">
          <legend>导布寿命</legend>
          <div
            class="wet-guide-life-tip"
            :class="{ 'is-warning': guideClothWarningFlag }"
          >
            <IconifyIcon icon="lucide:info" class="wet-guide-life-tip__icon" />
            <span>{{ guideClothWarningText }}</span>
          </div>
          <div class="wet-guide-life-grid">
            <div class="pp-form-item">
              <label>导布批号</label>
              <Input
                v-model:value="reportForm.guideClothBatchNo"
                @update:value="clearWetConsumableSelection('GUIDE_CLOTH')"
              >
                <template #suffix>
                  <Tooltip title="选择湿法导布边库批次">
                    <Button
                      class="wet-consumable-picker-button"
                      size="small"
                      type="text"
                      @click.stop="openWetConsumableLedger('GUIDE_CLOTH')"
                    >
                      <IconifyIcon icon="lucide:package-search" />
                    </Button>
                  </Tooltip>
                </template>
              </Input>
            </div>
            <div class="pp-form-item">
              <label>上次更换时间</label>
              <div class="pp-readonly-box">
                {{ guideClothLastReplaceTimeText }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>更换提醒状态</label>
              <div class="pp-readonly-box">
                {{ guideClothMonthlyStatusText }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>累计使用次数</label>
              <div class="pp-readonly-box">
                {{ guideClothProjectedUseCount }}/{{
                  GUIDE_CLOTH_MAX_USE_COUNT
                }}
              </div>
            </div>
            <div class="pp-form-item">
              <label>是否更换</label>
              <RadioGroup v-model:value="reportForm.guideClothChanged">
                <Radio value="N">否</Radio>
                <Radio value="Y">是</Radio>
              </RadioGroup>
            </div>
            <div class="pp-form-item pp-form-item--span-3">
              <label>导布更换原因</label>
              <Input
                v-model:value="reportForm.guideClothChangeReason"
                :disabled="reportForm.guideClothChanged !== 'Y'"
              />
            </div>
          </div>
        </fieldset>

        <div class="pp-panel pp-panel--booking-actions">
          <div class="pp-booking-panel__actions">
            <Button size="small" @click="viewMode = 'tabs'">
              <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回过程作业
            </Button>
            <Button
              size="small"
              type="primary"
              danger
              @click="handleBookingConfirm"
            >
              <IconifyIcon icon="lucide:check" class="mr-1" /> 确认提交报工
            </Button>
          </div>
        </div>
      </div>
    </div>

    <AModal
      v-model:open="recordDialogVisible"
      :title="null"
      width="100vw"
      :footer="null"
      wrap-class-name="hc-pass-work-modal"
    >
      <div class="pp-plan-modal">
        <div class="pp-plan-toolbar">
          <div class="pp-plan-toolbar__title pass-work-dialog-title">
            <span class="pp-plan-toolbar__main">{{ recordDialogTitle }}</span>
            <div class="toolbar-meta">
              <span>计划号：{{ task?.planNo || '-' }}</span>
              <span v-if="recordDialogCategory === 'production-check'"
                >生产日期：{{ reportView.productionDateDisplay || '-' }}</span
              >
              <span>当前工序：{{ task?.process || '湿法' }}</span>
              <span>执行时机：{{ currentRecord?.timing || '-' }}</span>
            </div>
          </div>
          <div class="pp-plan-toolbar__actions">
            <input
              ref="recordExcelImportInputRef"
              accept=".xls,.xlsx,.xlsm"
              style="display: none"
              type="file"
              @change="handleWetRecordExcelImportChange"
            />
            <Button
              size="small"
              :loading="recordExcelLoading"
              @click="handleWetRecordExportExcel"
              >导出Excel</Button
            >
            <Button
              v-if="recordDialogMode !== 'view'"
              size="small"
              :loading="recordExcelLoading"
              @click="triggerWetRecordImportExcel"
              >导入Excel</Button
            >
            <Button
              v-if="
                recordDialogCategory === 'production-check' &&
                recordDialogMode !== 'view'
              "
              size="small"
              :loading="modbusCollecting"
              @click="openCollectorScopeDialog"
            >
              <IconifyIcon icon="lucide:radio-receiver" class="mr-1" />
              记录参数
            </Button>
            <Button
              v-if="recordDialogMode === 'edit'"
              size="small"
              type="primary"
              @click="openRecordSaveAuth"
              >保存</Button
            >
            <Button
              v-if="recordDialogMode === 'confirm'"
              size="small"
              type="primary"
              danger
              @click="openRecordConfirmAuth"
              >确认</Button
            >
            <Button size="small" @click="closeRecordDialog">关闭</Button>
          </div>
        </div>

        <div class="pp-plan-body">
          <fieldset class="pp-fieldset">
            <legend>表单信息</legend>
            <div
              v-if="recordDialogCategory === 'production-check'"
              :class="productionCheckHeaderClass"
            >
              <div
                v-for="field in productionCheckHeadFields"
                :key="field.key"
                class="production-check-head-cell"
              >
                <span class="production-check-head-cell__label">{{
                  field.label
                }}</span>
                <div
                  v-if="
                    field.type === 'datetime' && recordDialogMode !== 'view'
                  "
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <DatePicker
                    :value="getProductionCheckHeaderDateValue(field.key)"
                    allow-clear
                    show-time
                    value-format="YYYY-MM-DD HH:mm:ss"
                    class="production-check-head-cell__picker"
                    @update:value="
                      (value) =>
                        setProductionCheckHeaderDateValue(field.key, value)
                    "
                    @change="
                      (_, dateString) =>
                        setProductionCheckHeaderDateValue(field.key, dateString)
                    "
                  />
                </div>
                <span v-else class="production-check-head-cell__value">{{
                  field.value || '-'
                }}</span>
              </div>
            </div>
            <div
              v-else-if="
                recordDialogCategory === 'semi-finished' && currentRecord
              "
              :class="semiFinishedHeaderClass"
            >
              <div
                v-for="field in semiFinishedHeadFields"
                :key="field.key"
                class="production-check-head-cell"
              >
                <span class="production-check-head-cell__label">{{
                  field.label
                }}</span>
                <div
                  v-if="field.type === 'input' && recordDialogMode !== 'view'"
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <InputNumber
                    v-if="field.key === 'generatedLength'"
                    v-model:value="currentRecord.generatedLength"
                    :controls="false"
                    :precision="3"
                    class="production-check-head-cell__input"
                  />
                  <Input
                    v-else
                    v-model:value="currentRecord.semiWidth"
                    class="production-check-head-cell__input"
                  />
                </div>
                <div
                  v-else-if="
                    field.type === 'radio' && recordDialogMode !== 'view'
                  "
                  class="production-check-head-cell__value production-check-head-cell__input-wrap"
                >
                  <div class="choice-field production-check-head-cell__choice">
                    <RadioGroup
                      v-if="field.key === 'poreDevelopment'"
                      v-model:value="currentRecord.poreDevelopment"
                      size="small"
                      class="pp-radio-group"
                    >
                      <Radio value="OK">OK</Radio>
                      <Radio value="NG">NG</Radio>
                    </RadioGroup>
                    <RadioGroup
                      v-else
                      v-model:value="currentRecord.finalResult"
                      size="small"
                      class="pp-radio-group"
                    >
                      <Radio value="OK">OK</Radio>
                      <Radio value="NG">NG</Radio>
                    </RadioGroup>
                  </div>
                </div>
                <span v-else class="production-check-head-cell__value">
                  {{
                    field.key === 'semiWidth'
                      ? currentRecord?.semiWidth || '-'
                      : field.key === 'poreDevelopment'
                        ? currentRecord?.poreDevelopment || '-'
                        : field.key === 'finalResult'
                          ? currentRecord?.finalResult || '-'
                          : field.value || '-'
                  }}
                </span>
              </div>
            </div>
            <div v-else :class="prepareHeaderClass">
              <div
                v-for="field in prepareHeadFields"
                :key="field.field"
                class="production-check-head-cell"
              >
                <span class="production-check-head-cell__label"
                  >{{ field.label }}：</span
                >
                <span class="production-check-head-cell__value">{{
                  field.value || '-'
                }}</span>
              </div>
            </div>
          </fieldset>

          <div v-if="showRecordAttachmentPanel" class="record-attachment-panel">
            <div class="record-attachment-panel__toolbar">
              <span class="record-attachment-panel__title">原始导入附件</span>
            </div>
            <div
              v-if="currentRecord?.attachments?.length"
              class="record-attachment-list"
            >
              <div
                v-for="attachment in currentRecord.attachments"
                :key="attachment.uid || attachment.url"
                class="record-attachment-item"
              >
                <button
                  class="record-attachment-link"
                  type="button"
                  @click="openRecordAttachment(attachment)"
                >
                  <IconifyIcon icon="lucide:paperclip" />
                  <span>{{ attachment.name }}</span>
                </button>
                <span
                  v-if="attachment.uploadTime"
                  class="record-attachment-time"
                >
                  导入时间：{{
                    normalizeDateTime(attachment.uploadTime) ||
                    attachment.uploadTime
                  }}
                </span>
                <span
                  v-if="formatAttachmentSize(attachment.size)"
                  class="record-attachment-size"
                >
                  {{ formatAttachmentSize(attachment.size) }}
                </span>
                <Button
                  v-if="recordDialogMode !== 'view'"
                  danger
                  size="small"
                  type="link"
                  @click="removeRecordAttachment(attachment)"
                >
                  <IconifyIcon icon="lucide:trash-2" />
                </Button>
              </div>
            </div>
            <span v-else class="record-attachment-empty">暂无原始导入附件</span>
          </div>

          <div class="pp-panel pass-work-detail-panel">
            <div class="pp-panel__header">
              <span>明细项目</span>
            </div>
            <div
              v-if="
                recordDialogCategory === 'semi-finished' &&
                (currentRecord?.details?.length || 0)
              "
              class="semi-detail-toolbar"
            >
              <div>
                <span class="semi-detail-toolbar__summary">
                  {{ semiDetailSummary }}
                </span>
                <span
                  v-if="semiDetailConsistencyHint"
                  class="semi-detail-toolbar__warning"
                >
                  {{ semiDetailConsistencyHint }}
                </span>
                <Button
                  v-if="
                    semiDetailConsistencyHint && recordDialogMode !== 'view'
                  "
                  size="small"
                  type="link"
                  @click="rebuildSemiFinishedDetails"
                >
                  按当前长度重建明细
                </Button>
              </div>
            </div>
            <div class="pp-table-wrap pass-work-detail-wrap">
              <div class="pass-work-detail-table-body">
                <div
                  v-if="
                    recordDialogCategory !== 'semi-finished' &&
                    recordDialogCategory !== 'production-check'
                  "
                  class="daily-check-result-tip"
                >
                  开机、清洁保养时遇到问题请记录在备注列说明情况。
                </div>
                <table class="pp-grid">
                  <thead>
                    <tr v-if="recordDialogCategory === 'semi-finished'">
                      <th width="100">长度/m</th>
                      <th
                        v-for="header in semiFinishedThicknessHeaders"
                        :key="header"
                        width="180"
                      >
                        {{ header }}
                      </th>
                      <th width="260">备注</th>
                    </tr>
                    <tr v-else-if="recordDialogCategory === 'production-check'">
                      <th width="120">物料/生产环节</th>
                      <th width="120">确认节点</th>
                      <th width="220">点检项目</th>
                      <th width="180">点检标准</th>
                      <th width="180">实际/记录</th>
                      <th width="220">异常备注</th>
                    </tr>
                    <tr v-else-if="currentRecord?.name?.includes('清洁点检表')">
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
                    <tr
                      v-for="(row, index) in recordDialogDetailRows"
                      :key="`${currentRecord?.id}-${row.seq}`"
                    >
                      <template
                        v-if="recordDialogCategory === 'production-check'"
                      >
                        <td
                          v-if="
                            getDetailFieldRowSpan(
                              currentRecord?.details || [],
                              index,
                              'category',
                            ) > 0
                          "
                          :rowspan="
                            getDetailFieldRowSpan(
                              currentRecord?.details || [],
                              index,
                              'category',
                            )
                          "
                        >
                          {{ row.category || '-' }}
                        </td>
                        <td
                          v-if="
                            getDetailFieldRowSpan(
                              currentRecord?.details || [],
                              index,
                              'node',
                            ) > 0
                          "
                          :rowspan="
                            getDetailFieldRowSpan(
                              currentRecord?.details || [],
                              index,
                              'node',
                            )
                          "
                        >
                          {{ row.node || '-' }}
                        </td>
                        <td>{{ row.item || '-' }}</td>
                        <td>{{ row.standard || '-' }}</td>
                        <td>
                          <div class="collector-value-cell">
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="(el) => setSheetCellRef(index, 'value', el)"
                              @keydown="
                                handleSheetCellKeydown($event, index, 'value')
                              "
                            >
                              <Input
                                v-model:value="row.value"
                                size="small"
                                :placeholder="
                                  row.placeholder ? '请填写记录值' : ''
                                "
                              />
                            </div>
                            <span v-else>{{ row.value || '-' }}</span>
                            <Tooltip
                              v-if="row.collectorSource"
                              :title="getCollectorSourceTooltip(row)"
                            >
                              <Tag
                                :color="
                                  getCollectorSourceTagColor(
                                    row.collectorSource,
                                    row.result,
                                  )
                                "
                                class="collector-source-tag"
                              >
                                {{
                                  getCollectorSourceText(row.collectorSource)
                                }}
                              </Tag>
                            </Tooltip>
                          </div>
                        </td>
                        <td>
                          <div
                            v-if="recordDialogMode !== 'view'"
                            :ref="(el) => setSheetCellRef(index, 'remark', el)"
                            @keydown="
                              handleSheetCellKeydown($event, index, 'remark')
                            "
                          >
                            <Input v-model:value="row.remark" size="small" />
                          </div>
                          <span v-else>{{ row.remark || '-' }}</span>
                        </td>
                      </template>
                      <template
                        v-else-if="currentRecord?.name?.includes('清洁点检表')"
                      >
                        <td
                          v-if="
                            getDetailFieldRowSpan(
                              currentRecord?.details || [],
                              index,
                              'category',
                            ) > 0
                          "
                          :rowspan="
                            getDetailFieldRowSpan(
                              currentRecord?.details || [],
                              index,
                              'category',
                            )
                          "
                        >
                          {{ row.category || '-' }}
                        </td>
                        <td>{{ row.item || '-' }}</td>
                        <td>{{ row.standard || '-' }}</td>
                        <td>
                          <RadioGroup
                            v-if="recordDialogMode !== 'view'"
                            v-model:value="row.result"
                            class="pp-radio-group"
                            size="small"
                          >
                            <Radio value="OK">OK</Radio>
                            <Radio value="NG">NG</Radio>
                          </RadioGroup>
                          <span v-else>{{ row.result || '-' }}</span>
                        </td>
                        <td>
                          <div
                            v-if="recordDialogMode !== 'view'"
                            :ref="(el) => setSheetCellRef(index, 'remark', el)"
                            @keydown="
                              handleSheetCellKeydown($event, index, 'remark')
                            "
                          >
                            <Input v-model:value="row.remark" size="small" />
                          </div>
                          <span v-else>{{ row.remark || '-' }}</span>
                        </td>
                      </template>
                      <template v-else>
                        <template
                          v-if="recordDialogCategory === 'semi-finished'"
                        >
                          <td align="center">
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="
                                (el) =>
                                  setSheetCellRef(
                                    getDetailRenderRowIndex(index),
                                    'length',
                                    el,
                                  )
                              "
                              @keydown="
                                handleSheetCellKeydown(
                                  $event,
                                  getDetailRenderRowIndex(index),
                                  'length',
                                )
                              "
                            >
                              <InputNumber
                                :value="getSemiDetailLength(row, index)"
                                :min="0"
                                :precision="3"
                                :step="0.1"
                                class="w-full"
                                size="small"
                                @change="
                                  (value) => updateSemiDetailLength(row, value)
                                "
                              />
                            </div>
                            <span v-else>{{
                              getSemiDetailLength(row, index)
                            }}</span>
                          </td>
                          <td>
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="
                                (el) =>
                                  setSheetCellRef(
                                    getDetailRenderRowIndex(index),
                                    'item',
                                    el,
                                  )
                              "
                              @keydown="
                                handleSheetCellKeydown(
                                  $event,
                                  getDetailRenderRowIndex(index),
                                  'item',
                                )
                              "
                            >
                              <Input v-model:value="row.item" size="small" />
                            </div>
                            <span v-else>{{ row.item || '-' }}</span>
                          </td>
                          <td>
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="
                                (el) =>
                                  setSheetCellRef(
                                    getDetailRenderRowIndex(index),
                                    'standard',
                                    el,
                                  )
                              "
                              @keydown="
                                handleSheetCellKeydown(
                                  $event,
                                  getDetailRenderRowIndex(index),
                                  'standard',
                                )
                              "
                            >
                              <Input
                                v-model:value="row.standard"
                                size="small"
                              />
                            </div>
                            <span v-else>{{ row.standard || '-' }}</span>
                          </td>
                          <td>
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="
                                (el) =>
                                  setSheetCellRef(
                                    getDetailRenderRowIndex(index),
                                    'value',
                                    el,
                                  )
                              "
                              @keydown="
                                handleSheetCellKeydown(
                                  $event,
                                  getDetailRenderRowIndex(index),
                                  'value',
                                )
                              "
                            >
                              <Input v-model:value="row.value" size="small" />
                            </div>
                            <span v-else>{{ row.value || '-' }}</span>
                          </td>
                          <td>
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="
                                (el) =>
                                  setSheetCellRef(
                                    getDetailRenderRowIndex(index),
                                    'node',
                                    el,
                                  )
                              "
                              @keydown="
                                handleSheetCellKeydown(
                                  $event,
                                  getDetailRenderRowIndex(index),
                                  'node',
                                )
                              "
                            >
                              <Input v-model:value="row.node" size="small" />
                            </div>
                            <span v-else>{{ row.node || '-' }}</span>
                          </td>
                          <td>
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="
                                (el) =>
                                  setSheetCellRef(
                                    getDetailRenderRowIndex(index),
                                    'remark',
                                    el,
                                  )
                              "
                              @keydown="
                                handleSheetCellKeydown(
                                  $event,
                                  getDetailRenderRowIndex(index),
                                  'remark',
                                )
                              "
                            >
                              <Input v-model:value="row.remark" size="small" />
                            </div>
                            <span v-else>{{ row.remark || '-' }}</span>
                          </td>
                        </template>
                        <template v-else>
                          <td align="center">{{ row.seq }}</td>
                          <td>{{ row.item }}</td>
                          <td>{{ row.standard }}</td>
                          <td>
                            <RadioGroup
                              v-if="recordDialogMode !== 'view'"
                              v-model:value="row.result"
                              class="pp-radio-group"
                              size="small"
                            >
                              <Radio value="OK">OK</Radio>
                              <Radio value="NG">NG</Radio>
                            </RadioGroup>
                            <span v-else>{{ row.result || '-' }}</span>
                          </td>
                          <td>
                            <div
                              v-if="recordDialogMode !== 'view'"
                              :ref="
                                (el) => setSheetCellRef(index, 'remark', el)
                              "
                              @keydown="
                                handleSheetCellKeydown($event, index, 'remark')
                              "
                            >
                              <Input v-model:value="row.remark" size="small" />
                            </div>
                            <span v-else>{{ row.remark || '-' }}</span>
                          </td>
                        </template>
                      </template>
                    </tr>
                    <tr v-if="!(currentRecord?.details || []).length">
                      <td
                        :colspan="
                          recordDialogCategory === 'semi-finished' ||
                          recordDialogCategory === 'production-check'
                            ? 6
                            : 5
                        "
                        class="pp-empty-cell"
                        align="center"
                      >
                        暂无明细数据
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <fieldset class="pp-fieldset pp-fieldset--plain">
            <Form layout="vertical" class="detail-form__body">
              <div class="form-section">
                <div
                  class="form-section__title form-section__title--toggle"
                  @click="actionPanelExpanded = !actionPanelExpanded"
                >
                  <span>执行与验证记录</span>
                  <IconifyIcon
                    :icon="
                      actionPanelExpanded
                        ? 'lucide:chevron-up'
                        : 'lucide:chevron-down'
                    "
                  />
                </div>
                <div v-show="actionPanelExpanded" class="form-section-grid">
                  <div class="form-section form-section--inner">
                    <div class="form-section__subtitle">执行记录</div>
                    <div class="form-grid form-grid--section">
                      <FormItem label="执行结果" class="form-grid__inline">
                        <div
                          v-if="recordDialogMode !== 'view'"
                          class="choice-field"
                        >
                          <RadioGroup
                            v-model:value="recordActionForm.result"
                            size="small"
                            class="pp-radio-group"
                          >
                            <Radio value="OK">OK</Radio>
                            <Radio value="NG">NG</Radio>
                          </RadioGroup>
                        </div>
                        <div v-else class="head-item__value">
                          {{ recordActionForm.result || '-' }}
                        </div>
                      </FormItem>
                      <FormItem label="记录人">
                        <div class="head-item__value">
                          {{
                            recordActionForm.recorder ||
                            reportForm.recorderName ||
                            '-'
                          }}
                        </div>
                      </FormItem>
                      <FormItem label="记录时间">
                        <div class="head-item__value">
                          {{
                            recordActionForm.recorderTime ||
                            reportView.recorderTimeDisplay ||
                            '-'
                          }}
                        </div>
                      </FormItem>
                      <FormItem label="执行备注">
                        <Input
                          v-if="recordDialogMode !== 'view'"
                          v-model:value="recordActionForm.formRemark"
                          size="small"
                        />
                        <div v-else class="head-item__value">
                          {{ recordActionForm.formRemark || '-' }}
                        </div>
                      </FormItem>
                    </div>
                  </div>
                  <div class="form-section form-section--inner">
                    <div class="form-section__subtitle">验证记录</div>
                    <div class="form-grid form-grid--section">
                      <FormItem label="检验结果" class="form-grid__inline">
                        <div
                          v-if="recordDialogMode !== 'view'"
                          class="choice-field"
                        >
                          <RadioGroup
                            v-model:value="recordActionForm.inspectionResult"
                            size="small"
                            class="pp-radio-group"
                          >
                            <Radio value="OK">OK</Radio>
                            <Radio value="NG">NG</Radio>
                          </RadioGroup>
                        </div>
                        <div v-else class="head-item__value">
                          {{ recordActionForm.inspectionResult || '-' }}
                        </div>
                      </FormItem>
                      <FormItem label="确认人">
                        <div class="head-item__value">
                          {{
                            recordActionForm.confirmer ||
                            reportForm.confirmerName ||
                            '-'
                          }}
                        </div>
                      </FormItem>
                      <FormItem label="确认时间">
                        <div class="head-item__value">
                          {{
                            recordActionForm.confirmerTime ||
                            reportView.confirmerTimeDisplay ||
                            '-'
                          }}
                        </div>
                      </FormItem>
                      <FormItem label="确认备注">
                        <Input
                          v-if="recordDialogMode !== 'view'"
                          v-model:value="recordActionForm.confirmRemark"
                          size="small"
                        />
                        <div v-else class="head-item__value">
                          {{ recordActionForm.confirmRemark || '-' }}
                        </div>
                      </FormItem>
                    </div>
                  </div>
                </div>
              </div>
            </Form>
          </fieldset>
        </div>
      </div>
    </AModal>

    <AModal
      v-model:open="collectorScopeDialogVisible"
      title="记录参数"
      width="760px"
      ok-text="读取并填写"
      cancel-text="取消"
      :confirm-loading="modbusCollecting"
      @ok="handleCollectorScopeConfirm"
    >
      <div class="collector-scope-dialog">
        <div class="collector-scope-grid">
          <div class="pp-form-item">
            <label>物料/生产环节</label>
            <Select
              v-model:value="collectorScopeForm.itemCategory"
              :options="collectorItemCategoryOptions"
              placeholder="请选择物料/生产环节"
              @change="syncCollectorStepNodeAfterCategoryChange"
            />
          </div>
          <div class="pp-form-item">
            <label>确认节点</label>
            <Select
              v-model:value="collectorScopeForm.stepNode"
              :options="collectorStepNodeOptions"
              placeholder="请选择确认节点"
            />
          </div>
        </div>
        <div class="collector-scope-summary">
          当前范围：{{ collectorScopeForm.itemCategory || '-' }} /
          {{ collectorScopeForm.stepNode || '-' }}，共
          {{ collectorScopePreviewRows.length }} 项
        </div>
        <div class="collector-scope-preview">
          <table class="pp-grid collector-scope-preview-table">
            <thead>
              <tr>
                <th width="90">序号</th>
                <th width="160">点检项目</th>
                <th>点检标准</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in collectorScopePreviewRows" :key="row.seq">
                <td align="center">{{ row.seq }}</td>
                <td>{{ row.item || '-' }}</td>
                <td>{{ row.standard || '-' }}</td>
              </tr>
              <tr v-if="!collectorScopePreviewRows.length">
                <td colspan="3" class="pp-empty-cell" align="center">
                  当前范围暂无点检项目
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </AModal>

    <AModal
      v-model:open="poreSelfCheckDialogVisible"
      :title="
        poreSelfCheckDialogMode === 'create' ? '新增泡孔自检' : '查看泡孔自检'
      "
      ok-text="保存"
      cancel-text="关闭"
      width="720px"
      :confirm-loading="poreSelfCheckSaving"
      :footer="poreSelfCheckDialogMode === 'create' ? undefined : null"
      @ok="savePoreSelfCheckRecord"
    >
      <div class="pore-self-check-form">
        <div class="pp-form-grid pp-form-grid--pore-self-check">
          <div class="pp-form-item">
            <label>自检时间</label>
            <DatePicker
              v-model:value="poreSelfCheckForm.selfCheckTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              class="w-full"
              :disabled="poreSelfCheckDialogMode === 'view'"
            />
          </div>
          <div class="pp-form-item">
            <label>检验人</label>
            <Input
              v-model:value="poreSelfCheckForm.inspector"
              :disabled="poreSelfCheckDialogMode === 'view'"
            />
          </div>
          <div class="pp-form-item pp-form-item--full">
            <label>泡孔照片</label>
            <div class="pore-photo-upload">
              <div class="pore-photo-upload__actions">
                <Upload
                  accept="image/*"
                  multiple
                  :before-upload="beforePorePhotoUpload"
                  :disabled="
                    poreSelfCheckDialogMode === 'view' ||
                    porePhotoUploadLimitReached
                  "
                  :show-upload-list="false"
                >
                  <Button
                    size="small"
                    :disabled="
                      poreSelfCheckDialogMode === 'view' ||
                      porePhotoUploadLimitReached
                    "
                    :loading="porePhotoUploading"
                  >
                    <IconifyIcon icon="lucide:upload" class="mr-1" />
                    上传照片（最多 {{ MAX_PORE_PHOTO_COUNT }} 张）
                  </Button>
                </Upload>
                <span class="pore-photo-upload__hint">
                  已上传 {{ poreSelfCheckForm.photos.length }} /
                  {{ MAX_PORE_PHOTO_COUNT }} 张；单张不超过 10MB
                </span>
              </div>
              <div
                v-if="poreSelfCheckForm.photos.length"
                class="pore-photo-preview-grid"
              >
                <div
                  v-for="(photo, index) in poreSelfCheckForm.photos"
                  :key="photo"
                  class="pore-photo-preview-item"
                >
                  <button
                    class="pore-photo-preview-inline"
                    type="button"
                    @click="
                      openPorePhotoPreview(poreSelfCheckForm.photos, index)
                    "
                  >
                    <img :src="photo" alt="泡孔照片" />
                  </button>
                  <Button
                    v-if="poreSelfCheckDialogMode !== 'view'"
                    class="pore-photo-remove"
                    danger
                    shape="circle"
                    size="small"
                    @click="removePorePhoto(photo)"
                  >
                    <IconifyIcon icon="lucide:x" />
                  </Button>
                </div>
              </div>
            </div>
          </div>
          <div class="pp-form-item pp-form-item--full">
            <label>备注</label>
            <Input
              v-model:value="poreSelfCheckForm.remark"
              :disabled="poreSelfCheckDialogMode === 'view'"
            />
          </div>
        </div>
      </div>
    </AModal>

    <AModal
      v-model:open="porePhotoPreviewVisible"
      title="泡孔照片预览"
      :footer="null"
      width="760px"
    >
      <div class="pore-photo-preview-modal">
        <img
          v-if="currentPorePhotoPreviewUrl"
          :src="currentPorePhotoPreviewUrl"
          alt="泡孔照片"
        />
      </div>
      <div
        v-if="porePhotoPreviewUrls.length > 1"
        class="pore-photo-preview-toolbar"
      >
        <Button
          size="small"
          :disabled="porePhotoPreviewIndex === 0"
          @click="showPreviousPorePhoto"
        >
          上一张
        </Button>
        <span>
          {{ porePhotoPreviewIndex + 1 }} / {{ porePhotoPreviewUrls.length }}
        </span>
        <Button
          size="small"
          :disabled="porePhotoPreviewIndex >= porePhotoPreviewUrls.length - 1"
          @click="showNextPorePhoto"
        >
          下一张
        </Button>
      </div>
      <div
        v-if="porePhotoPreviewUrls.length > 1"
        class="pore-photo-preview-thumbs"
      >
        <button
          v-for="(photo, index) in porePhotoPreviewUrls"
          :key="photo"
          :class="{
            'is-active': index === porePhotoPreviewIndex,
          }"
          type="button"
          @click="porePhotoPreviewIndex = index"
        >
          <img :src="photo" alt="泡孔照片缩略图" />
        </button>
      </div>
    </AModal>

    <ConsumableLedgerSwitchModal
      ref="wetConsumableSwitchModalRef"
      @selected="handleWetConsumableSelected"
    />

    <AuthModal
      v-model:visible="authVisible"
      :actionName="authActionName"
      authMode="username"
      :equipment-id="
        pendingAction === 'SUBMIT_BOOKING' ? undefined : reportForm.equipmentId
      "
      :equipment-options="
        pendingAction === 'SUBMIT_BOOKING' ? [] : equipmentOptions
      "
      equipment-label="机台编号"
      @success="handleAuthSuccess"
    />
    <AuthModal
      v-model:visible="recordSaveAuthVisible"
      :action-name="recordSaveAuthActionName"
      auth-mode="username"
      @success="handleRecordSaveAuthSuccess"
    />
    <AuthModal
      v-model:visible="recordConfirmAuthVisible"
      :action-name="recordConfirmAuthActionName"
      auth-mode="username"
      @success="handleRecordConfirmAuthSuccess"
    />
    <PrintPreviewModal />
    <FirstInspectionPrintPreviewModal />
    <FaiDetailPreviewModal />
    <AModal
      v-model:open="abnormalPositionVisible"
      title="异常位置明细"
      ok-text="确定"
      cancel-text="关闭"
      width="820px"
      @ok="abnormalPositionVisible = false"
    >
      <div class="abnormal-position-dialog">
        <div class="abnormal-position-toolbar">
          <span>计划号：{{ task?.planNo || '-' }}</span>
          <RadioGroup
            :value="abnormalPositionOption"
            :disabled="task?.status === 'COMPLETED'"
            class="pp-radio-group"
            @update:value="handleAbnormalPositionOptionChange"
          >
            <Radio value="NONE">无</Radio>
            <Radio value="DETAIL">填写异常位置</Radio>
          </RadioGroup>
          <Button
            v-if="task?.status !== 'COMPLETED'"
            size="small"
            type="primary"
            :disabled="abnormalPositionOption !== 'DETAIL'"
            @click="addAbnormalPositionRow"
          >
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
              <th v-if="task?.status !== 'COMPLETED'" width="80">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, index) in abnormalPositionRows"
              :key="row.clientKey"
            >
              <td align="center">{{ index + 1 }}</td>
              <td>
                <Input
                  v-model:value="row.positionText"
                  :disabled="
                    task?.status === 'COMPLETED' ||
                    abnormalPositionOption !== 'DETAIL'
                  "
                  placeholder="请输入异常位置"
                />
              </td>
              <td>
                <InputNumber
                  v-model:value="row.abnormalLength"
                  :disabled="
                    task?.status === 'COMPLETED' ||
                    abnormalPositionOption !== 'DETAIL'
                  "
                  :min="0"
                  :precision="3"
                  class="w-full"
                />
              </td>
              <td>
                <Input
                  v-model:value="row.remark"
                  :disabled="
                    task?.status === 'COMPLETED' ||
                    abnormalPositionOption !== 'DETAIL'
                  "
                  placeholder="备注"
                />
              </td>
              <td v-if="task?.status !== 'COMPLETED'" align="center">
                <Button
                  danger
                  size="small"
                  type="link"
                  :disabled="abnormalPositionOption !== 'DETAIL'"
                  @click="removeAbnormalPositionRow(index)"
                >
                  删除
                </Button>
              </td>
            </tr>
            <tr v-if="!abnormalPositionRows.length">
              <td
                :colspan="task?.status === 'COMPLETED' ? 4 : 5"
                class="pp-empty-cell"
                align="center"
              >
                暂无异常位置明细
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AModal>
    <AModal
      v-model:open="timeCorrectionVisible"
      title="修订开工完工时间"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="timeCorrectionSubmitting"
      @ok="handleTimeCorrectionConfirm"
    >
      <div
        ref="timeCorrectionFormRef"
        class="pp-form-grid pp-form-grid--time-correction"
      >
        <div class="pp-form-item" data-wet-time-correction-field="reportDate">
          <label>报工日期</label>
          <DatePicker
            v-model:value="timeCorrectionForm.reportDate"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            class="w-full"
          />
        </div>
        <div class="pp-form-item" data-wet-time-correction-field="startTime">
          <label class="pp-form-label--required">开始时间</label>
          <DatePicker
            v-model:value="timeCorrectionForm.startTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            class="w-full"
          />
        </div>
        <div class="pp-form-item" data-wet-time-correction-field="endTime">
          <label
            :class="{ 'pp-form-label--required': task?.status === 'COMPLETED' }"
            >完工时间</label
          >
          <DatePicker
            v-model:value="timeCorrectionForm.endTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            class="w-full"
          />
        </div>
      </div>
    </AModal>
    <AModal
      v-model:open="quantityRevisionVisible"
      title="修订湿法统计数据"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="quantityRevisionSubmitting"
      @ok="handleQuantityRevisionConfirm"
    >
      <Form layout="vertical">
        <FormItem label="当前收卷米数(报工数)(m)">
          <InputNumber
            :value="getCurrentWetReceiveLength()"
            class="w-full"
            disabled
            :precision="3"
          />
        </FormItem>
        <FormItem label="收卷米数(报工数)(m)" required>
          <InputNumber
            v-model:value="quantityRevisionForm.receiveLength"
            class="w-full"
            :min="0"
            :precision="3"
          />
        </FormItem>
        <FormItem label="当前固定损耗(米)">
          <InputNumber
            :value="getCurrentWetPrintLossLength()"
            class="w-full"
            disabled
            :precision="3"
          />
        </FormItem>
        <FormItem label="固定损耗(米)" required>
          <InputNumber
            v-model:value="quantityRevisionForm.printLossLength"
            class="w-full"
            :min="0"
            :precision="3"
          />
        </FormItem>
        <FormItem label="修订原因" required>
          <Input.TextArea
            v-model:value="quantityRevisionForm.reason"
            :maxlength="200"
            :rows="3"
            show-count
          />
        </FormItem>
      </Form>
    </AModal>
    <AModal
      v-model:open="timeLogVisible"
      title="报工时间修改日志"
      :footer="null"
      width="980px"
    >
      <ATable
        :columns="timeLogColumns"
        :data-source="timeLogs"
        :loading="timeLogLoading"
        :pagination="false"
        :scroll="{ x: 920 }"
        bordered
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'operatorName'">
            {{ displayLogValue(record.operatorName) }}
          </template>
          <template v-else-if="column.key === 'changeTime'">
            {{ displayLogValue(record.changeTime) }}
          </template>
          <template v-else-if="column.key === 'reportDate'">
            <div class="wet-time-log-diff">
              <span>{{ displayLogValue(record.beforeReportDate) }}</span>
              <IconifyIcon icon="lucide:arrow-right" />
              <strong>{{ displayLogValue(record.afterReportDate) }}</strong>
            </div>
          </template>
          <template v-else-if="column.key === 'startTime'">
            <div class="wet-time-log-diff">
              <span>{{ displayLogValue(record.beforeStartTime) }}</span>
              <IconifyIcon icon="lucide:arrow-right" />
              <strong>{{ displayLogValue(record.afterStartTime) }}</strong>
            </div>
          </template>
          <template v-else-if="column.key === 'endTime'">
            <div class="wet-time-log-diff">
              <span>{{ displayLogValue(record.beforeEndTime) }}</span>
              <IconifyIcon icon="lucide:arrow-right" />
              <strong>{{ displayLogValue(record.afterEndTime) }}</strong>
            </div>
          </template>
        </template>
      </ATable>
    </AModal>
    <AModal
      v-model:open="deviceSwitchVisible"
      title="切换工位设备"
      ok-text="确认切换"
      cancel-text="取消"
      :confirm-loading="deviceSwitchSubmitting"
      @ok="handleDeviceSwitchConfirm"
    >
      <div class="device-switch-dialog">
        <div class="device-switch-dialog__hint">
          <IconifyIcon
            icon="lucide:cable"
            class="device-switch-dialog__hint-icon"
          />
          <span>当前挂接设备信息</span>
        </div>
        <div
          v-if="equipmentStatusCard"
          class="device-switch-current-grid device-switch-current-grid--single"
        >
          <div class="device-switch-current-card">
            <div class="device-switch-current-card__head">
              <span class="device-switch-current-card__title"
                >当前挂接设备</span
              >
              <Tag :color="equipmentStatusCard.statusMeta.color" class="!m-0">{{
                equipmentStatusCard.statusMeta.text
              }}</Tag>
            </div>
            <div class="device-switch-current-card__name">
              {{ equipmentStatusCard.equipmentLabel }}
            </div>
            <div class="device-switch-current-card__meta">
              计划号：{{ equipmentStatusCard.currentPlanNo }}
            </div>
            <div class="device-switch-current-card__meta">
              工序：{{ equipmentStatusCard.currentOperationName }}
            </div>
            <div class="device-switch-current-card__meta">
              开工时间：{{ equipmentStatusCard.currentStartTime }}
            </div>
            <div class="device-switch-current-card__meta">
              完工时间：{{ equipmentStatusCard.currentEndTime }}
            </div>
            <div class="device-switch-current-card__meta">
              操作人：{{ equipmentStatusCard.currentOperatorName }}
            </div>
          </div>
        </div>
        <div class="pp-form-grid pp-form-grid--switch">
          <div class="pp-form-item">
            <label>机台编号</label>
            <Select
              v-model:value="deviceSwitchForm.equipmentId"
              :options="equipmentOptions"
              :field-names="{ label: 'label', value: 'value' }"
            />
          </div>
        </div>
      </div>
    </AModal>
  </Modal>
</template>

<style scoped>
.wet-consumable-picker-button {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.custom-main-tabs {
  min-height: 0;
}

:deep(.custom-main-tabs .ant-tabs-nav) {
  padding: 0 12px;
  margin-bottom: 0;
  background-color: #ffffff;
  border-bottom: 1px solid #edf0f3;
}

:deep(.custom-main-tabs .ant-tabs-tab) {
  font-size: 13px;
  font-weight: 600;
  padding: 10px 12px !important;
}

:deep(.custom-main-tabs .ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

:deep(.custom-main-tabs .ant-tabs-content) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

:deep(.custom-main-tabs .ant-tabs-tabpane) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.pp-plan-modal {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
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

.pp-plan-toolbar__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.pp-plan-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.pp-plan-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wet-first-inspection-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 26px;
  padding: 0 8px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
  font-size: 12px;
}

.wet-first-inspection-status:hover {
  border-color: #1677ff;
  color: #1677ff;
}

.first-inspection-apply {
  padding-top: 8px;
}

.first-inspection-apply__hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.first-inspection-apply__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: -6px 0 12px;
  color: #64748b;
  font-size: 12px;
}

.device-switch-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.device-switch-dialog__hint {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
}

.device-switch-dialog__hint-icon {
  font-size: 16px;
}

.device-switch-current-grid {
  display: grid;
  gap: 12px;
}

.device-switch-current-grid--single {
  grid-template-columns: minmax(0, 1fr);
}

.device-switch-current-card {
  padding: 8px 10px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.device-switch-current-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.device-switch-current-card__title {
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
}

.device-switch-current-card__name {
  margin-bottom: 4px;
  color: #111827;
  font-weight: 600;
}

.device-switch-current-card__meta {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.pp-plan-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.wet-prev-op-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 12px;
  border: 1px solid #fed7aa;
  border-left: 4px solid #f97316;
  background: #fff7ed;
}

.wet-prev-op-bar--qtime {
  justify-content: center;
  padding: 6px 12px;
}

.wet-prev-op-bar__content {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px 20px;
}

.wet-prev-op-bar__content--qtime {
  flex: 0 0 auto;
  justify-content: center;
}

.wet-prev-op-bar__item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.wet-prev-op-bar__label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.wet-prev-op-bar__value {
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
}

.wet-prev-op-bar__item--qtime {
  gap: 8px;
}

.wet-prev-op-bar__qtime-value {
  font-family: var(--vben-font-family-mono);
  font-size: 18px;
  font-weight: 800;
  line-height: 22px;
}

.wet-prev-op-bar__qtime-value--normal {
  color: #15803d;
}

.wet-prev-op-bar__qtime-value--warning {
  color: #d97706;
}

.wet-prev-op-bar__qtime-value--danger {
  color: #dc2626;
}

.wet-prev-op-bar__qtime-value--empty {
  color: #64748b;
}

.wet-prev-op-bar__fai-status {
  min-width: 48px;
  padding: 2px 10px;
  border-radius: 999px;
  font-family: var(--vben-font-family-mono);
  font-size: 15px;
  font-weight: 800;
  line-height: 20px;
  text-align: center;
}

.wet-prev-op-bar__fai-status--ok {
  background: #dcfce7;
  color: #15803d;
}

.wet-prev-op-bar__fai-status--ng {
  background: #fee2e2;
  color: #dc2626;
}

.wet-prev-op-bar__fai-status--pending {
  background: #e0f2fe;
  color: #0369a1;
}

.wet-prev-op-bar__fai-status--empty {
  background: #f1f5f9;
  color: #64748b;
}

.pp-fieldset {
  margin: 0;
  padding: 8px 12px 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.pp-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
  font-size: 12px;
}

.pp-fieldset--plain {
  padding-top: 10px;
}

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-form-grid--switch {
  grid-template-columns: minmax(0, 1fr);
}

.pp-form-grid--time-correction {
  grid-template-columns: minmax(0, 1fr);
}

.pp-form-grid--pore-self-check {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.wet-time-log-diff {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #4b5563;
}

.wet-time-log-diff span,
.wet-time-log-diff strong {
  min-width: 0;
  white-space: nowrap;
}

.wet-time-log-diff strong {
  color: #1677ff;
}

.wet-report-info-grid,
.wet-report-booking-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 12px;
}

.wet-report-info-grid {
  padding: 12px;
}

.wet-report-booking-grid {
  padding: 0;
}

.abnormal-position-dialog {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.abnormal-position-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #4b5563;
  font-size: 13px;
}

.wet-abnormal-option {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  min-height: 32px;
}

.wet-abnormal-option--readonly {
  justify-content: flex-start;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.wet-abnormal-option--under-loss {
  gap: 6px 8px;
  margin-top: 6px;
}

.wet-abnormal-option--under-loss .pp-radio-group {
  min-height: 28px;
  padding: 0;
}

.wet-abnormal-option-cell {
  justify-content: center;
  min-height: 32px;
}

.wet-abnormal-option-cell .wet-abnormal-option--under-loss {
  margin-top: 0;
}

.pp-form-item.wet-pet-guide-note {
  display: flex;
  flex-direction: row;
  gap: 8px;
  align-items: center;
  min-height: 32px;
  padding: 5px 8px;
  overflow: hidden;
  color: #475569;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.wet-pet-guide-note__title {
  flex: 0 0 auto;
  color: #334155;
  font-weight: 800;
}

.wet-pet-guide-note__content {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.wet-abnormal-option__label {
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
}

.abnormal-position-edit-table :deep(.ant-input-number),
.abnormal-position-edit-table :deep(.ant-input) {
  width: 100%;
}

.pore-photo-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pore-photo-summary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
}

.pore-self-check-table-wrap {
  flex: 1 1 auto;
  height: auto;
  max-height: calc(100vh - 360px);
  min-height: 160px;
  overflow: auto;
}

.pore-self-check-fill-table-wrap {
  flex: 1 1 0;
  height: 100%;
  max-height: none;
  min-height: 0;
  overflow: auto;
}

.pore-photo-thumb {
  display: inline-flex;
  max-width: 100%;
  height: 34px;
  align-items: center;
  gap: 6px;
  padding: 2px 8px 2px 2px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #fff;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

.pore-photo-thumb img {
  width: 42px;
  height: 28px;
  border-radius: 3px;
  object-fit: cover;
}

.pore-self-check-status {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 6px 10px;
  border-top: 1px solid #e5e7eb;
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
}

.pore-self-check-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pore-photo-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pore-photo-upload__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.pore-photo-upload__hint {
  color: #6b7280;
  font-size: 12px;
}

.pore-photo-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(96px, 1fr));
  gap: 8px;
}

.pore-photo-preview-item {
  position: relative;
  min-width: 0;
}

.pore-photo-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 !important;
  box-shadow: 0 1px 3px rgb(15 23 42 / 25%);
}

.record-attachment-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.record-attachment-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-attachment-panel__title {
  color: #374151;
  font-size: 13px;
  font-weight: 700;
}

.record-attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.record-attachment-item {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 2px;
  padding: 2px 4px 2px 8px;
  border: 1px solid #d1d5db;
  background: #f9fafb;
}

.record-attachment-link {
  display: inline-flex;
  max-width: 360px;
  align-items: center;
  gap: 6px;
  padding: 0;
  overflow: hidden;
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 12px;
}

.record-attachment-link span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-attachment-time,
.record-attachment-size {
  color: #6b7280;
  font-size: 12px;
  white-space: nowrap;
}

.record-attachment-empty {
  color: #6b7280;
  font-size: 12px;
}

.pore-photo-preview-inline {
  display: block;
  width: 100%;
  height: 96px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #f9fafb;
  cursor: pointer;
}

.pore-photo-preview-inline img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pore-photo-preview-modal {
  display: flex;
  max-height: 70vh;
  align-items: center;
  justify-content: center;
  overflow: auto;
}

.pore-photo-preview-modal img {
  max-width: 100%;
  max-height: 68vh;
  object-fit: contain;
}

.pore-photo-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 10px;
  color: #475569;
  font-size: 13px;
}

.pore-photo-preview-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.pore-photo-preview-thumbs button {
  width: 64px;
  height: 48px;
  flex: 0 0 auto;
  overflow: hidden;
  padding: 0;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #f8fafc;
  cursor: pointer;
}

.pore-photo-preview-thumbs button.is-active {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px #2563eb;
}

.pore-photo-preview-thumbs img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pore-photo-thumb:hover,
.pore-photo-preview-inline:hover {
  border-color: #2563eb;
}

.pore-photo-thumb:focus-visible,
.pore-photo-preview-inline:focus-visible {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.pore-photo-thumb span {
  font-size: 12px;
}

.pp-form-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.pp-form-item--full {
  grid-column: 1 / -1;
}

.pp-form-item--span-2 {
  grid-column: span 2;
}

.pp-form-item--span-3 {
  grid-column: span 3;
}

.pp-form-item label {
  color: #4b5563;
  font-weight: 700;
}

.pp-form-item label.pp-form-label--required {
  color: #dc2626;
}

.pp-form-item label.pp-form-label--required::after {
  content: '*';
  margin-left: 2px;
  color: #dc2626;
}

.pp-form-item label.wet-report-emphasis-label {
  color: #dc2626;
}

.pp-form-item label.wet-report-emphasis-label::after {
  content: '*';
  margin-left: 2px;
  color: #dc2626;
}

.pp-readonly-box {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 0;
  background: #fafafa;
  color: #374151;
  line-height: 1.4;
}

.pp-readonly-box--code {
  color: #1677ff;
  font-family: Consolas, Monaco, monospace;
}

.pp-readonly-box--multiline {
  align-items: flex-start;
  white-space: pre-wrap;
}

.wet-guide-life-fieldset {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 8px;
  background: #f8fbff;
}

.pp-panel > .wet-guide-life-fieldset {
  margin: 0 12px 12px;
}

.wet-guide-life-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 5px 10px;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #166534;
  font-size: 12px;
  font-weight: 600;
}

.wet-guide-life-tip.is-warning {
  border-color: #fecaca;
  background: #fef2f2;
  color: #b91c1c;
}

.wet-guide-life-tip__icon {
  flex: 0 0 auto;
  font-size: 15px;
}

.wet-guide-life-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 12px;
}

.pp-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  background: #fff;
  overflow: hidden;
}

.pp-panel--form-list {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  height: 100%;
}

.pp-panel--booking-actions {
  flex: 0 0 auto;
  min-height: auto;
}

.pp-booking-panel__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #e5e7eb;
}

.wet-tabs-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pass-work-panel {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #f5f7fa;
  padding: 8px;
}

.pore-self-check-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
}

.pore-self-check-panel > .pp-panel {
  height: 100%;
}

.pp-panel__header {
  height: 34px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-weight: 700;
}

.pp-panel__header-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.pp-panel__title {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.pp-panel__header-main .pp-panel__desc {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pp-panel__header > .pp-panel__desc {
  flex-shrink: 0;
}

.pp-panel__desc {
  font-size: 12px;
  color: #6b7280;
}

.wet-form-filterbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 6px 10px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.wet-form-filterbar__input {
  width: min(320px, 100%);
}

.wet-form-filterbar__meta {
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 12px;
}

.pp-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.semi-detail-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  padding: 8px 12px;
  color: #4b5563;
  font-size: 12px;
  background: #fff;
  border-bottom: 1px solid #eef0f4;
}

.semi-detail-toolbar__summary {
  font-variant-numeric: tabular-nums;
}

.semi-detail-toolbar__warning {
  margin-left: 10px;
  color: #d97706;
}

@media (max-width: 768px) {
  .semi-detail-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .semi-detail-toolbar__warning {
    display: inline-block;
    margin-top: 4px;
    margin-left: 0;
  }
}

.pp-grid {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid th,
.pp-grid td {
  min-height: 28px;
  padding: 3px 5px;
  border: 1px solid #e5e7eb;
  vertical-align: middle;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  font-weight: 400;
  text-align: center;
}

.pp-grid tbody tr:hover {
  background: #e6f4ff;
}

.pp-grid td[rowspan] {
  vertical-align: middle;
}

.pp-grid__sub {
  color: #9ca3af;
  font-size: 12px;
}

.pp-empty-cell {
  color: #9ca3af;
}

.toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 12px;
  color: #4b5563;
}

.pass-work-list-wrap,
.pass-work-detail-wrap {
  height: 100%;
}

.wet-form-list-wrap {
  min-height: 0;
  height: 100%;
  max-height: calc(100dvh - 260px);
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.wet-form-list-table {
  min-width: 1180px;
}

.wet-form-list-table--semi {
  min-width: 920px;
}

.pass-work-detail-wrap {
  display: flex;
  min-height: 0;
  flex-direction: column;
}

.pass-work-detail-table-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.collector-value-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
}

.collector-source-tag {
  width: fit-content;
  max-width: 100%;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collector-scope-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.collector-scope-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.collector-scope-summary {
  padding: 8px 10px;
  border: 1px solid #bae0ff;
  background: #f0f7ff;
  color: #0958d9;
  font-size: 13px;
  font-weight: 600;
}

.collector-scope-preview {
  max-height: 320px;
  overflow: auto;
  border: 1px solid #e5e7eb;
}

.collector-scope-preview-table th {
  top: 0;
}

.daily-check-result-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.pass-work-form-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.pass-work-form-grid--prepare {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.production-check-head-grid {
  display: grid;
  border: 1px solid #e5e7eb;
  border-bottom: 0;
  border-right: 0;
}

.production-check-head-grid--cols-4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.production-check-head-grid--cols-3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.production-check-head-grid--cols-2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.production-check-head-grid--cols-top {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.production-check-head-cell {
  min-width: 0;
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 0;
  padding: 0;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.production-check-head-cell__label {
  flex-shrink: 0;
  width: 116px;
  align-self: stretch;
  display: flex;
  align-items: center;
  padding: 0 8px;
  border-right: 1px solid #e5e7eb;
  background: #f5f5f5;
  color: #111827;
  font-weight: 400;
}

.production-check-head-cell__value {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  min-height: 32px;
  padding: 0 8px;
  background: #fff;
  color: #111827;
  line-height: 1.4;
  border-left: 0;
}

.production-check-head-cell__picker {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  padding: 0 8px !important;
  background: #fff;
}

.production-check-head-cell__input-wrap {
  display: flex;
  align-items: center;
  min-width: 0;
  flex: 1;
  padding: 0 8px !important;
  background: #fff;
}

.production-check-head-cell__input {
  width: 100%;
}

.production-check-head-cell__choice {
  width: 100%;
  min-height: 32px;
  height: 32px;
  border: 1px solid #d9d9d9;
  border-radius: 0 !important;
  box-sizing: border-box;
  background: #fff;
}

.production-check-head-cell__picker :deep(.ant-picker),
:deep(.production-check-head-cell__picker.ant-picker) {
  width: 100%;
  min-height: 32px !important;
  height: 32px !important;
  border: 1px solid #d9d9d9 !important;
  box-shadow: none !important;
  padding: 0 8px !important;
  border-radius: 0 !important;
  background: #fff !important;
  box-sizing: border-box;
}

.production-check-head-cell__input-wrap :deep(.ant-input),
.production-check-head-cell__input-wrap :deep(.ant-input-number),
.production-check-head-cell__input-wrap :deep(.ant-input-number-input-wrap),
.production-check-head-cell__input-wrap :deep(.ant-input-number-input),
:deep(.production-check-head-cell__input.ant-input),
:deep(.production-check-head-cell__input.ant-input-number),
:deep(.production-check-head-cell__input .ant-input-number-input-wrap),
:deep(.production-check-head-cell__input .ant-input-number-input) {
  width: 100%;
}

.production-check-head-cell__input-wrap :deep(.ant-input),
.production-check-head-cell__input-wrap :deep(.ant-input-number),
:deep(.production-check-head-cell__input.ant-input),
:deep(.production-check-head-cell__input.ant-input-number) {
  min-height: 32px !important;
  height: 32px !important;
  border: 1px solid #d9d9d9 !important;
  box-shadow: none !important;
  border-radius: 0 !important;
  background: #fff !important;
  box-sizing: border-box;
}

.production-check-head-cell__input-wrap :deep(.ant-input),
:deep(.production-check-head-cell__input.ant-input) {
  padding: 0 8px !important;
}

.production-check-head-cell__input-wrap :deep(.ant-input-number-input),
:deep(.production-check-head-cell__input .ant-input-number-input) {
  height: 30px !important;
  padding: 0 8px !important;
}

.head-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.head-item__label {
  color: #4b5563;
  font-weight: 700;
}

.head-item__value {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 0;
  background: #fafafa;
  color: #374151;
  font-weight: 400;
  line-height: 1.4;
}

.pass-work-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.inspection-actions {
  width: 100%;
  flex-direction: column;
  justify-content: center;
  align-items: stretch;
  gap: 2px;
}

.inspection-actions :deep(.ant-btn-link) {
  height: 24px;
  padding: 0;
}

.pass-work-dialog-title {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.detail-form__body {
  padding: 8px 10px 0;
}

.form-section-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-section {
  min-width: 0;
  padding: 0 0 2px;
}

.form-section--inner {
  padding: 0;
}

.form-section__title {
  margin-bottom: 10px;
  padding: 0 0 6px;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.form-section__title--toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.form-section__subtitle {
  margin-bottom: 10px;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.form-grid {
  display: grid;
  gap: 8px 12px;
}

.form-grid--section {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-form__body :deep(.ant-form-item) {
  margin-bottom: 8px;
  min-width: 0;
}

.detail-form__body :deep(.ant-form-item-label > label) {
  font-size: 12px;
  color: #4b5563;
  font-weight: 700;
}

.detail-form__body :deep(.ant-form-item-control-input) {
  min-height: 32px;
}

.detail-form__body :deep(.ant-form-item-control-input-content) {
  min-width: 0;
}

.detail-form__body :deep(.ant-input),
.detail-form__body :deep(.ant-input-affix-wrapper),
.detail-form__body :deep(.ant-picker),
.detail-form__body :deep(.ant-select-selector) {
  min-height: 32px !important;
  height: 32px !important;
}

.detail-form__body :deep(.ant-input) {
  line-height: 30px;
}

.pp-radio-group {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  min-height: 32px;
  padding: 0 6px;
}

.choice-field {
  min-height: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 5px;
  border: 1px solid #d9d9d9;
  background: #fff;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-btn) {
  border-radius: 0 !important;
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  margin: 0 !important;
  padding-bottom: 0;
  max-width: none;
}

.hc-pass-work-modal [class*='modal__header'],
.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal [class*='modal__close'],
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal [class*='modal__body'],
.hc-pass-work-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal [class*='modal__content'],
.hc-pass-work-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}
</style>
