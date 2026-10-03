<script lang="ts" setup>
import { computed, nextTick, ref, watch } from 'vue';
import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  Modal as AModal,
  Select,
  Table as ATable,
  Tabs,
  TabPane,
  Tag,
  message,
} from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenModal } from '@vben/common-ui';
import { useQRCode } from '@vueuse/integrations/useQRCode';
import dayjs from 'dayjs';
import { getEquipment, getEquipmentPage } from '#/api/mes/hc/equipment';
import { getPlanOrderDetail } from '#/api/mes/hc/planorder';
import {
  confirmFormulaReport,
  getFormulaReportTimeLogs,
  getFormulaReportTaskList,
  reviseFormulaReportStatisticsData,
  startFormulaReport,
  switchFormulaEquipment,
  updateFormulaReportTime,
} from '#/api/mes/hc/execution/formula-report';
import StartDetailTab from '#/views/mes/work-order-booking/modules/components/StartDetailTab.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import PassStationWorkTab from '#/views/mes/work-order-booking/modules/components/PassStationWorkTab.vue';
import FormulaReportPrintPreviewModal from '../formula/modules/FormulaReportPrintPreviewModal.vue';
import {
  buildThermalWorkOrderTransferTicketHtml,
  buildTransferTicketFields,
  buildTransferTicketQrValue,
  formatTransferTicketMetric,
} from './workOrderTicketPrint';
import { applyPrintFieldTemplate } from './printFieldTemplate';

const emit = defineEmits(['refresh', 'taskChange']);

const BATCHING_PRINT_AGENT_URL = 'http://127.0.0.1:17820';

const activeMainTab = ref('pass-work');
const task = ref<any>(null);
const viewMode = ref<'tabs' | 'booking'>('tabs');
const passWorkTabRef = ref<any>();

const startForm = ref({
  process: '',
  recipeCode: '',
  recipeName: '',
  batchingNo: '',
  feedBatchNo: '',
});

const reportForm = ref({
  productionDate: '',
  startTime: '',
  endTime: '',
  mixerEquipmentId: undefined as number | undefined,
  mixerEquipmentCode: '',
  mixerEquipmentName: '',
  foamingEquipmentId: undefined as number | undefined,
  foamingEquipmentCode: '',
  foamingEquipmentName: '',
  feedQty: undefined as number | undefined,
  viscosity: undefined as number | undefined,
  slurryTemperature: undefined as number | undefined,
  filterBatchNo: '',
  inputWeight: undefined as number | undefined,
  batchingTankNo: '',
  defoamingTankNo: '',
  remark: '',
  recorderName: '',
  recorderTime: '',
  confirmerName: '',
  confirmerTime: '',
});
type BookingDateTimeField =
  | 'confirmerTime'
  | 'endTime'
  | 'productionDate'
  | 'recorderTime'
  | 'startTime';
type BookingDateTimeFieldType = 'date' | 'datetime';
const bookingDateTimeInputCache = ref<Partial<Record<BookingDateTimeField, string>>>({});
const bookingTimeFormRef = ref<HTMLElement | null>(null);

const bookingForm = ref({
  laborHours: 0,
});
const mixerOptions = ref<any[]>([]);
const foamingOptions = ref<any[]>([]);
const deviceSwitchVisible = ref(false);
const deviceSwitchSubmitting = ref(false);
const deviceSwitchForm = ref({
  mixerEquipmentId: undefined as number | undefined,
  foamingEquipmentId: undefined as number | undefined,
});
const timeCorrectionVisible = ref(false);
const timeCorrectionSubmitting = ref(false);
const timeCorrectionForm = ref({
  endTime: '',
  reportDate: '',
  startTime: '',
});
const timeCorrectionFormRef = ref<HTMLElement | null>(null);
const statisticsRevisionVisible = ref(false);
const statisticsRevisionSubmitting = ref(false);
const statisticsRevisionForm = ref({
  feedQty: undefined as number | undefined,
  goodQty: undefined as number | undefined,
  scrapQty: undefined as number | undefined,
  reason: '',
});
const timeLogVisible = ref(false);
const timeLogLoading = ref(false);
const timeLogs = ref<any[]>([]);
const timeLogColumns = [
  { dataIndex: 'operatorName', key: 'operatorName', title: '修改人', width: 120 },
  { dataIndex: 'changeTime', key: 'changeTime', title: '修改时间', width: 170 },
  { key: 'reportDate', title: '报工日期', width: 190 },
  { key: 'startTime', title: '开始时间', width: 320 },
  { key: 'endTime', title: '结束时间', width: 320 },
];
const equipmentStatusCards = ref<any[]>([]);
const transferTicketQrValue = ref('');
const transferTicketQrDataUrl = useQRCode(transferTicketQrValue, {
  errorCorrectionLevel: 'M',
  margin: 1,
  width: 220,
});

const displayStatus = computed(() => {
  if (task.value?.status === 'COMPLETED') return { color: 'success', text: '已完工' };
  if (task.value?.status === 'IN_PROGRESS') return { color: 'processing', text: '生产中' };
  if (task.value?.status === 'RELEASED') return { color: 'processing', text: '已下达' };
  return { color: 'default', text: '待开工' };
});

const canPrintTransferTicket = computed(() => task.value?.status === 'COMPLETED');
const canConfirmReport = computed(() => !!task.value?.operationReportId && task.value?.status === 'COMPLETED');
const batchNoLabel = computed(() => displayText(task.value?.batchNoLabel, '批次号'));
const isFormulaReport = computed(() => task.value?.isFormulaReport === true);

const reportView = computed(() => ({
  ...reportForm.value,
  productionDateDisplay: normalizeDate(reportForm.value.productionDate),
  startTimeDisplay: normalizeBizDateTime(reportForm.value.productionDate, reportForm.value.startTime),
  endTimeDisplay: normalizeBizDateTime(reportForm.value.productionDate, reportForm.value.endTime),
  recorderTimeDisplay: normalizeBizDateTime(reportForm.value.productionDate, reportForm.value.recorderTime),
  confirmerTimeDisplay: normalizeBizDateTime(reportForm.value.productionDate, reportForm.value.confirmerTime),
}));

const currentPlanModelText = computed(() =>
  displayText(task.value?.motherModelCode || task.value?.modelCode || task.value?.motherModelName || task.value?.modelName),
);
const currentPlanBatchText = computed(() =>
  displayText(task.value?.productionBatchNo || task.value?.batchNo || task.value?.parentProductionBatchNo || startForm.value.feedBatchNo),
);

async function syncTaskRuntimeFromBackend(options: { emitChange?: boolean } = {}) {
  if (!task.value?.planOperationId || !task.value?.planNo) return;
  const rows = await getFormulaReportTaskList({
    taskKeyword: task.value.planNo,
    taskStatus: 'ALL',
  });
  const latestTask = (rows || []).find((item: any) => item?.planOperationId === task.value?.planOperationId);
  if (!latestTask) return;
  task.value = {
    ...task.value,
    ...latestTask,
  };
  reportForm.value.productionDate = latestTask.productionDate || reportForm.value.productionDate || '';
  reportForm.value.startTime = latestTask.startTime || reportForm.value.startTime || '';
  reportForm.value.endTime = latestTask.endTime || reportForm.value.endTime || '';
  reportForm.value.recorderName = latestTask.recorderName || reportForm.value.recorderName || '';
  reportForm.value.recorderTime = latestTask.recorderTime || reportForm.value.recorderTime || '';
  reportForm.value.confirmerName = latestTask.confirmerName || reportForm.value.confirmerName || '';
  reportForm.value.confirmerTime = latestTask.confirmerTime || reportForm.value.confirmerTime || '';
  reportForm.value.mixerEquipmentId = latestTask.mixerEquipmentId ?? reportForm.value.mixerEquipmentId;
  reportForm.value.mixerEquipmentCode = latestTask.mixerEquipmentCode || reportForm.value.mixerEquipmentCode || '';
  reportForm.value.mixerEquipmentName = latestTask.mixerEquipmentName || reportForm.value.mixerEquipmentName || '';
  reportForm.value.foamingEquipmentId = latestTask.foamingEquipmentId ?? reportForm.value.foamingEquipmentId;
  reportForm.value.foamingEquipmentCode = latestTask.foamingEquipmentCode || reportForm.value.foamingEquipmentCode || '';
  reportForm.value.foamingEquipmentName = latestTask.foamingEquipmentName || reportForm.value.foamingEquipmentName || '';
  if (options.emitChange !== false) {
    emit('taskChange', { ...task.value });
  }
}

const authVisible = ref(false);
const pendingAction = ref<
  'CONFIRM_REPORT' | 'FINISH' | 'START' | 'SUBMIT_REPORT' | null
>(null);
const authActionName = computed(() => {
  if (pendingAction.value === 'START') return '工单开工执行确认';
  if (pendingAction.value === 'FINISH') return '节点完工与报工流转';
  if (pendingAction.value === 'SUBMIT_REPORT') return '配料报工提交确认';
  if (pendingAction.value === 'CONFIRM_REPORT') return '配料报工确认';
  return '工单认证';
});

function normalizeDate(value?: string) {
  if (!value) return '';
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD') : String(value);
}

function displayText(value?: null | number | string, fallback = '-') {
  const text = String(value ?? '').trim();
  return text || fallback;
}

function isTimeOnlyText(value?: string) {
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

function normalizeDateTime(value?: string) {
  if (!value) return '';
  if (isTimeOnlyText(value)) return String(value).trim();
  const date = dayjs(value);
  if (isPlaceholderDateTime(date)) return date.format('HH:mm:ss');
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

function normalizeBizDateTime(productionDate?: string, value?: string) {
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

function reportDateFromDateTime(value?: string, fallback = dayjs().format('YYYY-MM-DD')) {
  const date = dayjs(value);
  return date.isValid() ? date.format('YYYY-MM-DD') : fallback;
}

function normalizeReportDateTimeText(value?: string, fallbackDate?: string) {
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
  const text = String((event.target as HTMLInputElement | null)?.value || '').trim();
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
      `[data-report-time-field="${key}"] input`,
    ) as HTMLInputElement | null;
    const cachedText = String(bookingDateTimeInputCache.value[key] || '').trim();
    const inputText = String(input?.value || '').trim();
    const normalizedFromInput = normalizeBookingDateTimeValue(
      inputText,
      type,
      reportForm.value.productionDate,
    );
    const normalized =
      normalizedFromInput ||
      normalizeBookingDateTimeValue(cachedText, type, reportForm.value.productionDate);
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
    '[data-time-correction-field="reportDate"] input',
  ) as HTMLInputElement | null;
  const reportDate =
    normalizeBookingDateTimeValue(reportDateInput?.value, 'date') ||
    normalizeBookingDateTimeValue(timeCorrectionForm.value.reportDate, 'date');
  if (reportDate) timeCorrectionForm.value.reportDate = reportDate;
  (['startTime', 'endTime'] as const).forEach((key) => {
    const input = root.querySelector(
      `[data-time-correction-field="${key}"] input`,
    ) as HTMLInputElement | null;
    const normalized =
      normalizeBookingDateTimeValue(input?.value, 'datetime', reportDate) ||
      normalizeBookingDateTimeValue(timeCorrectionForm.value[key], 'datetime', reportDate);
    if (normalized) timeCorrectionForm.value[key] = normalized;
  });
}

function ensureBookingReportDateTimes() {
  const productionDate =
    normalizeBookingDateTimeValue(
      bookingDateTimeInputCache.value.productionDate || reportForm.value.productionDate,
      'date',
    ) ||
    reportDateFromDateTime(reportForm.value.startTime || reportForm.value.endTime);
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
    message.warning('请填写有效的结束时间，格式：yyyy-MM-dd HH:mm:ss');
    return false;
  }
  if (dayjs(endTime).isBefore(dayjs(startTime))) {
    message.warning('结束时间不能早于开始时间');
    return false;
  }
  reportForm.value.startTime = startTime;
  reportForm.value.endTime = endTime;
  bookingDateTimeInputCache.value.startTime = startTime;
  bookingDateTimeInputCache.value.endTime = endTime;
  bookingDateTimeInputCache.value.productionDate = productionDate;
  return true;
}

function validateFormulaTraceFields() {
  reportForm.value.filterBatchNo = String(reportForm.value.filterBatchNo || '').trim();
  reportForm.value.batchingTankNo = String(reportForm.value.batchingTankNo || '').trim();
  reportForm.value.defoamingTankNo = String(reportForm.value.defoamingTankNo || '').trim();
  if (!reportForm.value.filterBatchNo) {
    message.warning('请填写滤网批号');
    return false;
  }
  const inputWeight = Number(reportForm.value.inputWeight);
  if (!Number.isFinite(inputWeight) || inputWeight <= 0) {
    message.warning('投料重量必须大于0');
    return false;
  }
  if (!reportForm.value.batchingTankNo) {
    message.warning('请填写配料罐罐号');
    return false;
  }
  if (!reportForm.value.defoamingTankNo) {
    message.warning('请填写脱泡罐罐号');
    return false;
  }
  return true;
}

function isStarted() {
  return !!reportForm.value.startTime || task.value?.status === 'IN_PROGRESS' || task.value?.status === 'COMPLETED';
}

function buildEquipmentOptionLabel(code?: string, name?: string) {
  const safeCode = String(code || '').trim();
  const safeName = String(name || '').trim();
  if (safeCode && safeName) return `${safeCode} / ${safeName}`;
  return safeCode || safeName || '-';
}

function buildWorkStatusMeta(status?: string) {
  if (status === 'PRODUCING') return { color: 'processing', text: '生产中' };
  if (status === 'MAINTENANCE') return { color: 'warning', text: '检修' };
  if (status === 'FAULT') return { color: 'error', text: '故障' };
  return { color: 'default', text: '待机' };
}

function ensureSelectedOption(
  options: any[],
  id?: number,
  code?: string,
  name?: string,
) {
  if (!id && !code && !name) return options;
  const exists = options.some((item) => item.value === id || (code && item.code === code));
  if (exists) return options;
  return [
    {
      value: id,
      label: buildEquipmentOptionLabel(code, name),
      code,
      name,
    },
    ...options,
  ];
}

async function loadWorkCenterEquipmentOptions() {
  const workCenterId = task.value?.workCenterId;
  let list: any[] = [];
  const primaryPage = await getEquipmentPage({
    pageNo: 1,
    pageSize: 200,
    workCenterId,
    equipmentType: '配料设备',
    status: 0,
  });
  list = primaryPage?.list || [];
  if (list.length === 0) {
    const fallbackPage = await getEquipmentPage({
      pageNo: 1,
      pageSize: 200,
      equipmentType: '配料设备',
      status: 0,
    });
    list = fallbackPage?.list || [];
  }
  if (list.length === 0) {
    const broadPage = await getEquipmentPage({
      pageNo: 1,
      pageSize: 200,
      status: 0,
    });
    list = broadPage?.list || [];
  }
  const toOption = (item: any) => ({
    value: item.id,
    label: buildEquipmentOptionLabel(item.equipmentCode, item.equipmentName),
    code: item.equipmentCode,
    name: item.equipmentName,
  });
  mixerOptions.value = list
    .filter((item: any) => String(item.equipmentName || '').includes('搅拌'))
    .map(toOption);
  foamingOptions.value = list
    .filter((item: any) => {
      const name = String(item.equipmentName || '');
      return name.includes('泡发') || name.includes('发泡') || name.includes('脱泡');
    })
    .map(toOption);
  mixerOptions.value = ensureSelectedOption(
    mixerOptions.value,
    reportForm.value.mixerEquipmentId,
    reportForm.value.mixerEquipmentCode,
    reportForm.value.mixerEquipmentName,
  );
  foamingOptions.value = ensureSelectedOption(
    foamingOptions.value,
    reportForm.value.foamingEquipmentId,
    reportForm.value.foamingEquipmentCode,
    reportForm.value.foamingEquipmentName,
  );
  if (!reportForm.value.mixerEquipmentId && reportForm.value.mixerEquipmentCode) {
    const matchedMixer = mixerOptions.value.find((item) => item.code === reportForm.value.mixerEquipmentCode);
    if (matchedMixer) {
      reportForm.value.mixerEquipmentId = matchedMixer.value;
      reportForm.value.mixerEquipmentName = matchedMixer.name || reportForm.value.mixerEquipmentName;
    }
  }
  if (!reportForm.value.foamingEquipmentId && reportForm.value.foamingEquipmentCode) {
    const matchedFoaming = foamingOptions.value.find((item) => item.code === reportForm.value.foamingEquipmentCode);
    if (matchedFoaming) {
      reportForm.value.foamingEquipmentId = matchedFoaming.value;
      reportForm.value.foamingEquipmentName = matchedFoaming.name || reportForm.value.foamingEquipmentName;
    }
  }
}

async function loadEquipmentStatusCards() {
  const buildCard = async (roleLabel: string, equipmentId?: number, equipmentCode?: string, equipmentName?: string) => {
    if (!equipmentId) {
      return {
        currentEndTime: '-',
        currentOperationName: '-',
        currentOperatorName: '-',
        currentPlanNo: '-',
        currentStartTime: '-',
        equipmentLabel: buildEquipmentOptionLabel(equipmentCode, equipmentName),
        roleLabel,
        statusMeta: buildWorkStatusMeta(),
      };
    }
    const detail = await getEquipment(equipmentId);
    return {
      currentEndTime: normalizeDateTime(detail?.currentEndTime) || '-',
      currentOperationName: detail?.currentOperationName || detail?.currentOperationCode || '-',
      currentOperatorName: detail?.currentOperatorName || '-',
      currentPlanNo: detail?.currentPlanNo || '-',
      currentStartTime: normalizeDateTime(detail?.currentStartTime) || '-',
      equipmentLabel: buildEquipmentOptionLabel(detail?.equipmentCode || equipmentCode, detail?.equipmentName || equipmentName),
      roleLabel,
      statusMeta: buildWorkStatusMeta(detail?.workStatus),
    };
  };
  equipmentStatusCards.value = await Promise.all([
    buildCard('搅拌机台', reportForm.value.mixerEquipmentId, reportForm.value.mixerEquipmentCode, reportForm.value.mixerEquipmentName),
    buildCard('脱泡机台', reportForm.value.foamingEquipmentId, reportForm.value.foamingEquipmentCode, reportForm.value.foamingEquipmentName),
  ]);
}

function openDeviceSwitchModal() {
  deviceSwitchForm.value = {
    mixerEquipmentId: reportForm.value.mixerEquipmentId,
    foamingEquipmentId: reportForm.value.foamingEquipmentId,
  };
  deviceSwitchVisible.value = true;
}

async function handleDeviceSwitchConfirm() {
  deviceSwitchSubmitting.value = true;
  try {
    const mixerTarget = mixerOptions.value.find((item) => item.value === deviceSwitchForm.value.mixerEquipmentId);
    const foamingTarget = foamingOptions.value.find((item) => item.value === deviceSwitchForm.value.foamingEquipmentId);
    await switchFormulaEquipment({
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
      mixerEquipmentId: deviceSwitchForm.value.mixerEquipmentId,
      mixerEquipmentCode: mixerTarget?.code,
      mixerEquipmentName: mixerTarget?.name,
      foamingEquipmentId: deviceSwitchForm.value.foamingEquipmentId,
      foamingEquipmentCode: foamingTarget?.code,
      foamingEquipmentName: foamingTarget?.name,
    });
    reportForm.value.mixerEquipmentId = deviceSwitchForm.value.mixerEquipmentId;
    reportForm.value.mixerEquipmentCode = mixerTarget?.code || '';
    reportForm.value.mixerEquipmentName = mixerTarget?.name || '';
    reportForm.value.foamingEquipmentId = deviceSwitchForm.value.foamingEquipmentId;
    reportForm.value.foamingEquipmentCode = foamingTarget?.code || '';
    reportForm.value.foamingEquipmentName = foamingTarget?.name || '';
    if (task.value) {
      task.value = {
        ...task.value,
        equipmentId: reportForm.value.mixerEquipmentId,
        equipmentCode: reportForm.value.mixerEquipmentCode,
        equipmentName: reportForm.value.mixerEquipmentName,
        foamingEquipmentId: reportForm.value.foamingEquipmentId,
        foamingEquipmentCode: reportForm.value.foamingEquipmentCode,
        foamingEquipmentName: reportForm.value.foamingEquipmentName,
        mixerEquipmentId: reportForm.value.mixerEquipmentId,
        mixerEquipmentCode: reportForm.value.mixerEquipmentCode,
        mixerEquipmentName: reportForm.value.mixerEquipmentName,
      };
      emit('taskChange', { ...task.value });
    }
    await loadEquipmentStatusCards();
    deviceSwitchVisible.value = false;
    message.success('工位设备已切换。');
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
    message.warning('未找到当前配料报工记录，无法修正时间');
    return;
  }
  const reportDate =
    reportForm.value.productionDate ||
    task.value?.productionDate ||
    reportDateFromDateTime(reportForm.value.startTime || reportForm.value.endTime);
  timeCorrectionForm.value = {
    endTime: normalizeReportDateTimeText(reportForm.value.endTime, reportDate),
    reportDate,
    startTime: normalizeReportDateTimeText(reportForm.value.startTime, reportDate),
  };
  timeCorrectionVisible.value = true;
}

async function handleTimeCorrectionConfirm() {
  await nextTick();
  syncTimeCorrectionFieldsFromDom();
  const reportId = Number(task.value?.operationReportId || 0);
  if (!reportId) {
    message.warning('未找到当前配料报工记录，无法保存');
    return;
  }
  const reportDate =
    timeCorrectionForm.value.reportDate ||
    reportDateFromDateTime(timeCorrectionForm.value.startTime || timeCorrectionForm.value.endTime);
  const startTime = normalizeReportDateTimeText(timeCorrectionForm.value.startTime, reportDate);
  const endTime = normalizeReportDateTimeText(timeCorrectionForm.value.endTime, reportDate);
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
  timeCorrectionSubmitting.value = true;
  try {
    await updateFormulaReportTime({
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
    timeCorrectionVisible.value = false;
    message.success('配料报工开始/结束时间已修正');
  } finally {
    timeCorrectionSubmitting.value = false;
  }
}

async function openStatisticsRevisionModal() {
  if (!task.value?.operationReportId) {
    await syncTaskRuntimeFromBackend({ emitChange: false });
  }
  const reportId = Number(task.value?.operationReportId || 0);
  if (!reportId) {
    message.warning('未找到当前配料报工记录，无法修订统计数据');
    return;
  }
  const feedQty = Number(task.value?.feedQty ?? reportForm.value.feedQty ?? task.value?.goodQty ?? 0);
  const goodQty = Number(task.value?.goodQty ?? reportForm.value.feedQty ?? feedQty);
  const scrapQty = Number(task.value?.scrapQty ?? 0);
  statisticsRevisionForm.value = {
    feedQty: Number.isFinite(feedQty) ? feedQty : 0,
    goodQty: Number.isFinite(goodQty) ? goodQty : 0,
    reason: '',
    scrapQty: Number.isFinite(scrapQty) ? scrapQty : 0,
  };
  statisticsRevisionVisible.value = true;
}

async function handleStatisticsRevisionConfirm() {
  const reportId = Number(task.value?.operationReportId || 0);
  if (!reportId) {
    message.warning('未找到当前配料报工记录，无法保存');
    return;
  }
  const reason = statisticsRevisionForm.value.reason.trim();
  if (!reason) {
    message.warning('请填写修订原因');
    return;
  }
  const feedQty = Number(statisticsRevisionForm.value.feedQty ?? 0);
  const goodQty = Number(statisticsRevisionForm.value.goodQty ?? 0);
  const scrapQty = Number(statisticsRevisionForm.value.scrapQty ?? 0);
  if (![feedQty, goodQty, scrapQty].every((value) => Number.isFinite(value) && value >= 0)) {
    message.warning('统计数据不能为负数');
    return;
  }
  statisticsRevisionSubmitting.value = true;
  try {
    await reviseFormulaReportStatisticsData({
      feedQty,
      goodQty,
      id: reportId,
      reason,
      scrapQty,
    });
    task.value = {
      ...(task.value || {}),
      feedQty,
      goodQty,
      scrapQty,
    };
    reportForm.value.feedQty = feedQty;
    await syncTaskRuntimeFromBackend({ emitChange: false });
    emit('taskChange', { ...task.value });
    statisticsRevisionVisible.value = false;
    message.success('配料母卷批次良品统计数据已修订');
  } finally {
    statisticsRevisionSubmitting.value = false;
  }
}

async function openTimeLogModal() {
  if (!task.value?.operationReportId) {
    await syncTaskRuntimeFromBackend({ emitChange: false });
  }
  const reportId = Number(task.value?.operationReportId || 0);
  if (!reportId) {
    message.warning('未找到当前配料报工记录，无法查看修改日志');
    return;
  }
  timeLogVisible.value = true;
  timeLogLoading.value = true;
  try {
    timeLogs.value = await getFormulaReportTimeLogs(reportId);
  } finally {
    timeLogLoading.value = false;
  }
}

function initStateFromTask() {
  bookingDateTimeInputCache.value = {};
  startForm.value.process = task.value?.process || '';
  startForm.value.recipeCode = task.value?.recipeCode || '';
  startForm.value.recipeName = task.value?.recipeName || '';
  startForm.value.batchingNo = task.value?.batchingNo || '';
  startForm.value.feedBatchNo = task.value?.feedBatchNo || '';

  reportForm.value = {
    productionDate: task.value?.productionDate || task.value?.reportDate || '',
    startTime: task.value?.startTime || '',
    endTime: task.value?.endTime || '',
    mixerEquipmentId: task.value?.mixerEquipmentId ?? task.value?.equipmentId,
    mixerEquipmentCode: task.value?.mixerEquipmentCode || task.value?.equipmentCode || '',
    mixerEquipmentName: task.value?.mixerEquipmentName || task.value?.equipmentName || '',
    foamingEquipmentId: task.value?.foamingEquipmentId ?? task.value?.equipmentId,
    foamingEquipmentCode: task.value?.foamingEquipmentCode || task.value?.equipmentCode || '',
    foamingEquipmentName: task.value?.foamingEquipmentName || task.value?.equipmentName || '',
    feedQty: task.value?.feedQty ?? undefined,
    viscosity: task.value?.viscosity ?? undefined,
    slurryTemperature: task.value?.slurryTemperature ?? undefined,
    filterBatchNo: task.value?.filterBatchNo || '',
    inputWeight: task.value?.inputWeight ?? undefined,
    batchingTankNo: task.value?.batchingTankNo || '',
    defoamingTankNo: task.value?.defoamingTankNo || '',
    remark: task.value?.reportRemark || '',
    recorderName: task.value?.recorderName || '',
    recorderTime: task.value?.recorderTime || '',
    confirmerName: task.value?.confirmerName || '',
    confirmerTime: task.value?.confirmerTime || '',
  };

  bookingForm.value.laborHours = 8;
}

const handleStartClick = async () => {
  await loadWorkCenterEquipmentOptions();
  pendingAction.value = 'START';
  authVisible.value = true;
};

const handleFinishClick = async () => {
  if (!isStarted()) {
    AModal.warning({
      title: '请先执行开工确认',
      content: '配料工位必须先完成开工确认并登记搅拌机台、脱泡机台后，才允许执行过站报工。',
    });
    return;
  }
  const now = buildNowText();
  if (!reportForm.value.productionDate) {
    reportForm.value.productionDate = reportDateFromDateTime(reportForm.value.startTime);
  }
  const startTime = normalizeReportDateTimeText(reportForm.value.startTime, reportForm.value.productionDate);
  if (!startTime) {
    message.warning('未找到有效的开工开始时间，请先执行开工确认或修正开始时间。');
    return;
  }
  reportForm.value.startTime = startTime;
  if (shouldResetRuntimeDateTime(reportForm.value.recorderTime)) {
    reportForm.value.recorderTime = now;
  }
  const result = passWorkTabRef.value?.validateBeforeFinish?.();
  if (result && !result.valid) {
    AModal.warning({
      title: '过站工作未完成',
      content: result.message || '请先完成全部过站工作检查后再执行过站报工。',
    });
    return;
  }
  await loadWorkCenterEquipmentOptions();
  pendingAction.value = 'FINISH';
  authVisible.value = true;
};

const handleReportConfirmClick = () => {
  if (!task.value?.operationReportId) {
    message.warning('当前还没有可确认的报工记录。');
    return;
  }
  pendingAction.value = 'CONFIRM_REPORT';
  authVisible.value = true;
};

const handleCloseClick = () => {
  modalApi.close();
};

const [PrintPreviewModal, printPreviewModalApi] = useVbenModal({
  connectedComponent: FormulaReportPrintPreviewModal,
});

const handlePrintClick = async () => {
  if (!task.value?.planId) {
    message.warning('当前任务未关联生产计划，无法生成打印预览。');
    return;
  }
  const planDetail = await getPlanOrderDetail(task.value.planId);
  printPreviewModalApi
    .setData({
      task: { ...(task.value || {}) },
      reportForm: { ...(reportForm.value || {}) },
      startForm: { ...(startForm.value || {}) },
      planDetail,
    })
    .open();
};

async function createTransferTicketQrDataUrl(value: string) {
  transferTicketQrValue.value = '';
  await nextTick();
  return await new Promise<string>((resolve) => {
    let stop: (() => void) | undefined;
    const timer = window.setTimeout(() => {
      stop?.();
      resolve('');
    }, 1500);
    stop = watch(
      transferTicketQrDataUrl,
      (dataUrl) => {
        if (!dataUrl) return;
        window.clearTimeout(timer);
        stop?.();
        resolve(dataUrl);
      },
    );
    transferTicketQrValue.value = value || 'PLAN';
  });
}

async function openBrowserTransferTicketPreview(ticketPayload: any) {
  const qrValue = ticketPayload.qrValue || ticketPayload.productionBatchNo || ticketPayload.planNo;
  const qrDataUrl = await createTransferTicketQrDataUrl(qrValue);
  const printWindow = window.open('', '_blank', 'width=620,height=420');
  if (!printWindow) {
    message.warning('浏览器阻止了打印窗口，请允许弹窗后重试。');
    return;
  }
  printWindow.document.open();
  printWindow.document.write(
    buildThermalWorkOrderTransferTicketHtml({
      endTime: ticketPayload.endTime,
      fields: ticketPayload.fields,
      materialCode: ticketPayload.materialCode,
      modelCode: ticketPayload.modelCode,
      offsetXmm: ticketPayload.offsetXmm,
      offsetYmm: ticketPayload.offsetYmm,
      planNo: ticketPayload.planNo,
      processName: ticketPayload.processName || '配料',
      productionBatchNo: ticketPayload.productionBatchNo,
      qrDataUrl,
      qrTopText: ticketPayload.qrTopText || ticketPayload.planNo,
      qrValue,
      recorderName: ticketPayload.recorderName,
      startTime: ticketPayload.startTime,
    }),
  );
  printWindow.document.close();
  printWindow.focus();
  window.setTimeout(() => {
    printWindow.print();
  }, 250);
}

async function sendTransferTicketToPrintAgent(ticketPayload: any) {
  const response = await fetch(`${BATCHING_PRINT_AGENT_URL}/print/transfer-ticket`, {
    body: JSON.stringify(ticketPayload),
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

const handlePrintTransferTicketClick = async () => {
  if (!canPrintTransferTicket.value) {
    message.warning('请先完成配料报工后再打印流转单。');
    return;
  }
  if (!task.value?.planNo && !task.value?.planId) {
    message.warning('当前任务缺少计划信息，无法打印流转单。');
    return;
  }
  const planDetail = task.value?.planId ? await getPlanOrderDetail(task.value.planId) : {};
  const planNo = task.value?.planNo || (planDetail as any)?.planNo || '';
  const productionBatchNo =
    task.value?.productionBatchNo ||
    task.value?.batchNo ||
    (planDetail as any)?.productionBatchNo ||
    '';
  const materialCode =
    (planDetail as any)?.motherMaterialCode ||
    task.value?.motherMaterialCode ||
    (planDetail as any)?.materialCode ||
    task.value?.materialCode ||
    '';
  const modelCode =
    (planDetail as any)?.motherModelCode ||
    task.value?.motherModelCode ||
    (planDetail as any)?.modelCode ||
    task.value?.modelCode ||
    '';
  const processName =
    task.value?.processName ||
    task.value?.operationName ||
    task.value?.currentOperationName ||
    task.value?.process ||
    '配料';
  const startTime = reportView.value.startTimeDisplay || task.value?.startTime || '';
  const endTime = reportView.value.endTimeDisplay || task.value?.endTime || '';
  const recorderName = reportForm.value.recorderName || task.value?.recorderName || '';
  const feedQty = reportForm.value.feedQty ?? task.value?.feedQty;
  const fallbackFields = buildTransferTicketFields({
    endTime,
    extraFields: [
      { label: '配料重量', value: formatTransferTicketMetric(feedQty, 'kg') },
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
  const fields = await applyPrintFieldTemplate('FORMULA_TRANSFER', fallbackFields, {
    endTime,
    feedQty: formatTransferTicketMetric(feedQty, 'kg'),
    materialCode,
    modelCode,
    planNo,
    processName,
    productionBatchNo,
    recorderName,
    startTime,
  });
  const ticketPayload = {
    copies: 1,
    endTime,
    feedQty,
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
    printerKey: 'batching',
    printMode: 'raw',
    processName,
    productionBatchNo,
    qrTopText: planNo || '-',
    qrValue: buildTransferTicketQrValue(planNo, productionBatchNo),
    rawProtocol: 'pplb',
    recorderName,
    startTime,
    title: '工艺流转单',
    waitForSpooler: true,
  };
  try {
    const result = await sendTransferTicketToPrintAgent(ticketPayload);
    message.success(`流转单已确认发送到 ${result.printerName || '配置打印机'}，数量：${result.printedCount || 1}。`);
  } catch (error: any) {
    AModal.confirm({
      title: '本机打印服务未连接',
      content: `未能连接配料打印驱动服务：${error?.message || error}。请先启动 Python 打印服务；需要临时预览时可使用浏览器预览。`,
      okText: '浏览器预览',
      cancelText: '取消',
      onOk: async () => {
        await openBrowserTransferTicketPreview(ticketPayload);
      },
    });
  }
};

const handleAuthSuccess = async (userInfo: any) => {
  const now = buildNowText();
  if (pendingAction.value === 'START') {
    if (!reportForm.value.productionDate) {
      reportForm.value.productionDate = dayjs().format('YYYY-MM-DD');
    }
    const operationReportId = await startFormulaReport({
      planId: task.value?.planId,
      planOperationId: task.value?.planOperationId,
      reportDate: reportForm.value.productionDate,
      startTime: now,
      recorderName: userInfo.empName,
      recorderTime: now,
      mixerEquipmentId: userInfo.mixerEquipmentId,
      mixerEquipmentCode: userInfo.mixerEquipmentCode,
      mixerEquipmentName: userInfo.mixerEquipmentName,
      foamingEquipmentId: userInfo.foamingEquipmentId,
      foamingEquipmentCode: userInfo.foamingEquipmentCode,
      foamingEquipmentName: userInfo.foamingEquipmentName,
    });
    reportForm.value.startTime = now;
    reportForm.value.recorderName = userInfo.empName;
    reportForm.value.recorderTime = now;
    reportForm.value.mixerEquipmentId = userInfo.mixerEquipmentId;
    reportForm.value.mixerEquipmentCode = userInfo.mixerEquipmentCode;
    reportForm.value.mixerEquipmentName = userInfo.mixerEquipmentName;
    reportForm.value.foamingEquipmentId = userInfo.foamingEquipmentId;
    reportForm.value.foamingEquipmentCode = userInfo.foamingEquipmentCode;
    reportForm.value.foamingEquipmentName = userInfo.foamingEquipmentName;
    if (task.value) {
      task.value.status = 'IN_PROGRESS';
      task.value.operationReportId = operationReportId;
      task.value.startTime = reportForm.value.startTime;
      task.value.recorderName = reportForm.value.recorderName;
      task.value.recorderTime = reportForm.value.recorderTime;
      task.value.mixerEquipmentId = reportForm.value.mixerEquipmentId;
      task.value.mixerEquipmentCode = reportForm.value.mixerEquipmentCode;
      task.value.mixerEquipmentName = reportForm.value.mixerEquipmentName;
      task.value.foamingEquipmentId = reportForm.value.foamingEquipmentId;
      task.value.foamingEquipmentCode = reportForm.value.foamingEquipmentCode;
      task.value.foamingEquipmentName = reportForm.value.foamingEquipmentName;
      emit('taskChange', { ...task.value });
      message.success(`认证通过！操作人【${userInfo.empName}】已完成开工确认。`);
    }
    await syncTaskRuntimeFromBackend();
    await loadEquipmentStatusCards();
  } else if (pendingAction.value === 'FINISH') {
    reportForm.value.endTime = now;
    message.success(`认证通过！操作人【${userInfo.empName}】可继续修正并提交报工信息。`);
    viewMode.value = 'booking';
  } else if (pendingAction.value === 'SUBMIT_REPORT') {
    const reportQty = Number(reportForm.value.feedQty ?? 0);
    pendingAction.value = null;
    await handleBookingSubmit({
      finalGood: reportQty,
      finalScrap: 0,
    });
    return;
  } else if (pendingAction.value === 'CONFIRM_REPORT') {
    if (!task.value?.operationReportId) {
      message.warning('当前还没有可确认的报工记录。');
      pendingAction.value = null;
      return;
    }
    const confirmerName = userInfo.empName;
    const confirmerTime = now;
    await confirmFormulaReport({
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
    message.success(`认证通过！确认人【${confirmerName}】已确认配料报工。`);
    await syncTaskRuntimeFromBackend();
  }
  pendingAction.value = null;
};

const handleBookingSubmit = async ({ finalGood, finalScrap }: any) => {
  await nextTick();
  syncBookingDateTimeFieldsFromDom();
  if (!ensureBookingReportDateTimes()) {
    return;
  }
  if (isFormulaReport.value && !validateFormulaTraceFields()) {
    return;
  }
  const now = buildNowText();
  if (shouldResetRuntimeDateTime(reportForm.value.recorderTime)) {
    reportForm.value.recorderTime = now;
  }
  const reportDateKey = dayjs(
    reportForm.value.productionDate || reportForm.value.endTime || new Date(),
  ).format('YYYY-MM-DD');
  let operationReportId: number | undefined;
  if (typeof task.value?.submitBooking === 'function') {
    operationReportId = await task.value.submitBooking({
      goodQty: finalGood,
      scrapQty: finalScrap,
      laborHours: bookingForm.value.laborHours,
      reportDate: reportForm.value.productionDate || undefined,
      startTime: reportForm.value.startTime || undefined,
      endTime: reportForm.value.endTime || undefined,
      batchingNo: startForm.value.batchingNo || undefined,
      feedBatchNo: startForm.value.feedBatchNo || undefined,
      recipeCode: startForm.value.recipeCode || undefined,
      recipeName: startForm.value.recipeName || undefined,
      feedQty: reportForm.value.feedQty,
      ...(isFormulaReport.value
        ? {
            filterBatchNo: reportForm.value.filterBatchNo,
            inputWeight: reportForm.value.inputWeight,
            batchingTankNo: reportForm.value.batchingTankNo,
            defoamingTankNo: reportForm.value.defoamingTankNo,
          }
        : {}),
      stirStartTime: undefined,
      stirEndTime: undefined,
      remark: reportForm.value.remark || undefined,
      scrapReason: undefined,
      recorderName: reportForm.value.recorderName || undefined,
      recorderTime: reportForm.value.recorderTime || undefined,
      mixerEquipmentId: reportForm.value.mixerEquipmentId || undefined,
      mixerEquipmentCode: reportForm.value.mixerEquipmentCode || undefined,
      mixerEquipmentName: reportForm.value.mixerEquipmentName || undefined,
      foamingEquipmentId: reportForm.value.foamingEquipmentId || undefined,
      foamingEquipmentCode: reportForm.value.foamingEquipmentCode || undefined,
      foamingEquipmentName: reportForm.value.foamingEquipmentName || undefined,
      viscosity: reportForm.value.viscosity,
      slurryTemperature: reportForm.value.slurryTemperature,
    });
  }
  task.value = {
    ...(task.value || {}),
  };
  const updatedQtyByDate = {
    ...(task.value?.reportQtyByDate || {}),
    [reportDateKey]: Number(finalGood || 0),
  };
  task.value = {
    ...task.value,
    status: 'COMPLETED',
    operationReportId: operationReportId || task.value?.operationReportId,
    startTime: reportForm.value.startTime,
    endTime: reportForm.value.endTime,
    feedQty: reportForm.value.feedQty,
    ...(isFormulaReport.value
      ? {
          filterBatchNo: reportForm.value.filterBatchNo,
          inputWeight: reportForm.value.inputWeight,
          batchingTankNo: reportForm.value.batchingTankNo,
          defoamingTankNo: reportForm.value.defoamingTankNo,
        }
      : {}),
    productionDate: reportForm.value.productionDate,
    recorderName: reportForm.value.recorderName,
    recorderTime: reportForm.value.recorderTime,
    mixerEquipmentId: reportForm.value.mixerEquipmentId,
    mixerEquipmentCode: reportForm.value.mixerEquipmentCode,
    mixerEquipmentName: reportForm.value.mixerEquipmentName,
    foamingEquipmentId: reportForm.value.foamingEquipmentId,
    foamingEquipmentCode: reportForm.value.foamingEquipmentCode,
    foamingEquipmentName: reportForm.value.foamingEquipmentName,
    viscosity: reportForm.value.viscosity,
    slurryTemperature: reportForm.value.slurryTemperature,
    reportRemark: reportForm.value.remark,
    goodQty: finalGood,
    scrapQty: finalScrap,
    reportDate: reportDateKey,
    reportQtyByDate: updatedQtyByDate,
    dispatchQty: Object.values(updatedQtyByDate).reduce((sum: number, value: any) => sum + Number(value || 0), 0),
  };
  await loadEquipmentStatusCards();
  emit('taskChange', { ...task.value });
  bookingDateTimeInputCache.value = {};
  promptPrintTransferTicketAfterBooking(finalGood, reportDateKey);
};

function finishBatchingBookingFlow() {
  modalApi.close();
  emit('refresh');
}

function promptPrintTransferTicketAfterBooking(finalGood: number, reportDateKey: string) {
  AModal.confirm({
    title: '过站报工成功',
    content: `已提交本次过站报工，实际配料量：${finalGood} ${task.value?.uom || 'kg'}。对应计划日期 ${reportDateKey} 的报工量和累计报工量已回写。是否立即打印工艺流转单？`,
    okText: '确认打印',
    cancelText: '暂不打印',
    onCancel: finishBatchingBookingFlow,
    onOk: async () => {
      await handlePrintTransferTicketClick();
      finishBatchingBookingFlow();
    },
  });
}

const handleBookingConfirm = async () => {
  await nextTick();
  syncBookingDateTimeFieldsFromDom();
  if (!ensureBookingReportDateTimes()) {
    return;
  }
  if (isFormulaReport.value && !validateFormulaTraceFields()) {
    return;
  }
  pendingAction.value = 'SUBMIT_REPORT';
  authVisible.value = true;
};

const [Modal, modalApi] = useVbenModal({
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  footer: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  showCancelButton: false,
  showConfirmButton: false,
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'hc-workstation-detail-modal',
  onOpenChange(isOpen) {
    if (isOpen) {
      modalApi.setState({ fullscreen: true });
      task.value = modalApi.getData<any>();
      viewMode.value = 'tabs';
      activeMainTab.value = 'pass-work';
      initStateFromTask();
      void loadWorkCenterEquipmentOptions();
      void loadEquipmentStatusCards();
    }
  },
});
</script>

<template>
  <Modal>
    <div class="pp-plan-modal">
      <div class="pp-plan-toolbar">
        <div class="pp-plan-toolbar__title">
          <span class="pp-plan-toolbar__main">配料报工工作台</span>
        </div>

        <div class="pp-plan-toolbar__actions">
          <template v-if="viewMode === 'tabs'">
            <Button size="small" @click="handleStartClick" :disabled="task?.status === 'COMPLETED' || task?.status === 'IN_PROGRESS'">
              <IconifyIcon icon="lucide:play" class="mr-1" /> {{ task?.status === 'IN_PROGRESS' ? '已开工生产中' : '执行开工确认' }}
            </Button>
            <Button size="small" type="primary" danger @click="handleFinishClick" :disabled="task?.status === 'COMPLETED'">
              <IconifyIcon icon="lucide:check-square" class="mr-1" /> 过站报工
            </Button>
            <Button size="small" @click="openDeviceSwitchModal" :disabled="!isStarted() || task?.status === 'COMPLETED'">
              <IconifyIcon icon="lucide:repeat" class="mr-1" /> 切换工位设备
            </Button>
            <Button
              v-access:code="['mes:sfc:formula-report:report-time:update']"
              size="small"
              @click="openTimeCorrectionModal"
              :disabled="!isStarted()"
            >
              <IconifyIcon icon="lucide:clock-3" class="mr-1" /> 修订开工完工时间
            </Button>
            <Button
              v-access:code="['mes:sfc:formula-report:statistics-data:revise']"
              size="small"
              @click="openStatisticsRevisionModal"
              :disabled="!canConfirmReport"
            >
              <IconifyIcon icon="lucide:chart-no-axes-column" class="mr-1" /> 修订统计数据
            </Button>
            <Button size="small" title="查看修正日志" @click="openTimeLogModal" :disabled="!isStarted()">
              <IconifyIcon icon="lucide:history" />
            </Button>
            <Button size="small" @click="handleReportConfirmClick" :disabled="!canConfirmReport">
              <IconifyIcon icon="lucide:badge-check" class="mr-1" /> 确认
            </Button>
            <Button size="small" @click="handlePrintClick">
              <IconifyIcon icon="lucide:file-down" class="mr-1" /> 导出工单
            </Button>
            <Button v-if="canPrintTransferTicket" size="small" @click="handlePrintTransferTicketClick">
              <IconifyIcon icon="lucide:qr-code" class="mr-1" /> 打印流转单
            </Button>
            <Button size="small" @click="handleCloseClick">
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
            <Button v-if="canPrintTransferTicket" size="small" @click="handlePrintTransferTicketClick">
              <IconifyIcon icon="lucide:qr-code" class="mr-1" /> 打印流转单
            </Button>
            <Button size="small" @click="handleCloseClick">
              <IconifyIcon icon="lucide:x" class="mr-1" /> 关闭窗口
            </Button>
          </template>
        </div>
      </div>

      <div v-show="viewMode === 'tabs'" class="pp-plan-body">
        <fieldset class="pp-fieldset">
          <legend>1. 基本信息</legend>
          <div class="pp-form-grid">
            <div class="pp-form-item">
              <label>生产计划号</label>
              <div class="pp-readonly-box pp-readonly-box--code">{{ task?.planNo || task?.id || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>产品型号</label>
              <div class="pp-readonly-box pp-readonly-box--code">{{ task?.motherModelCode || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>{{ batchNoLabel }}</label>
              <div class="pp-readonly-box pp-readonly-box--code">{{ task?.productionBatchNo || task?.batchNo || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>产品料号</label>
              <div class="pp-readonly-box">{{ task?.motherMaterialCode || task?.motherMaterialName || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>状态</label>
              <div class="pp-readonly-box">
                <Tag :color="displayStatus.color" class="!m-0">{{ displayStatus.text }}</Tag>
              </div>
            </div>
            <div class="pp-form-item">
              <label>生产日期</label>
              <div class="pp-readonly-box">{{ reportView.productionDateDisplay || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>开始时间</label>
              <div class="pp-readonly-box">{{ reportView.startTimeDisplay || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>结束时间</label>
              <div class="pp-readonly-box">{{ reportView.endTimeDisplay || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>记录人</label>
              <div class="pp-readonly-box">{{ reportForm.recorderName || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>记录时间</label>
              <div class="pp-readonly-box">{{ reportView.recorderTimeDisplay || '-' }}</div>
            </div>
            <div class="pp-form-item pp-form-item--full">
              <label>执行要求</label>
              <div class="pp-readonly-box pp-readonly-box--multiline">{{ task?.requirements || '-' }}</div>
            </div>
          </div>
        </fieldset>

        <div class="pp-panel">
          <Tabs v-model:activeKey="activeMainTab" class="custom-main-tabs flex h-full flex-col">
            <TabPane key="pass-work" tab="过站工作" class="h-full">
              <PassStationWorkTab ref="passWorkTabRef" :task="task" :startForm="startForm" />
            </TabPane>

            <TabPane key="detail" tab="报工信息" class="h-full">
              <StartDetailTab :task="task" :reportForm="reportView" :is-formula-report="isFormulaReport" />
            </TabPane>
          </Tabs>
        </div>
      </div>

      <div v-if="viewMode === 'booking'" ref="bookingTimeFormRef" class="pp-plan-body">
        <fieldset class="pp-fieldset">
          <legend>报工信息</legend>
          <div class="booking-plan-check">
            <span>当前计划型号：{{ currentPlanModelText }}</span>
            <span>当前计划{{ batchNoLabel }}：{{ currentPlanBatchText }}</span>
            <strong>请核实型号、{{ batchNoLabel }}与现场物料一致后再提交报工。</strong>
          </div>
          <div class="pp-form-grid">
            <div class="pp-form-item">
              <label>计划单号</label>
              <div class="pp-readonly-box pp-readonly-box--code">{{ task?.planNo || task?.id || '-' }}</div>
            </div>
            <div class="pp-form-item">
              <label>生产日期</label>
              <div
                data-report-time-field="productionDate"
                @input="handleBookingDateTimeInput('productionDate', 'date', $event)"
              >
                <DatePicker
                  v-model:value="reportForm.productionDate"
                  value-format="YYYY-MM-DD"
                  format="YYYY-MM-DD"
                  class="w-full"
                  @change="(value, dateString) => handleBookingDateTimeChange('productionDate', 'date', value, dateString)"
                />
              </div>
            </div>
            <div class="pp-form-item">
              <label>开始时间</label>
              <div
                data-report-time-field="startTime"
                @input="handleBookingDateTimeInput('startTime', 'datetime', $event)"
              >
                <DatePicker
                  v-model:value="reportForm.startTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  @change="(value, dateString) => handleBookingDateTimeChange('startTime', 'datetime', value, dateString)"
                />
              </div>
            </div>
            <div class="pp-form-item">
              <label>结束时间</label>
              <div
                data-report-time-field="endTime"
                @input="handleBookingDateTimeInput('endTime', 'datetime', $event)"
              >
                <DatePicker
                  v-model:value="reportForm.endTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  @change="(value, dateString) => handleBookingDateTimeChange('endTime', 'datetime', value, dateString)"
                />
              </div>
            </div>
            <div class="pp-form-item">
              <label>搅拌机台</label>
              <Select
                v-model:value="reportForm.mixerEquipmentId"
                :options="mixerOptions"
                :field-names="{ label: 'label', value: 'value' }"
                placeholder="请选择搅拌机台"
                @change="(value) => {
                  const target = mixerOptions.find((item) => item.value === value);
                  reportForm.mixerEquipmentCode = target?.code || '';
                  reportForm.mixerEquipmentName = target?.name || '';
                }"
              />
            </div>
            <div class="pp-form-item">
              <label>脱泡机台</label>
              <Select
                v-model:value="reportForm.foamingEquipmentId"
                :options="foamingOptions"
                :field-names="{ label: 'label', value: 'value' }"
                placeholder="请选择脱泡机台"
                @change="(value) => {
                  const target = foamingOptions.find((item) => item.value === value);
                  reportForm.foamingEquipmentCode = target?.code || '';
                  reportForm.foamingEquipmentName = target?.name || '';
                }"
              />
            </div>
            <template v-if="isFormulaReport">
              <div class="pp-form-item">
                <label class="pp-form-label--required">投料重量(kg)</label>
                <InputNumber v-model:value="reportForm.inputWeight" class="w-full" :min="0.001" :precision="3" />
              </div>
            </template>
            <div class="pp-form-item">
              <label class="pp-form-label--required">实际配料量(报工量)</label>
              <InputNumber v-model:value="reportForm.feedQty" class="w-full" :min="0" :precision="3" />
            </div>
            <div class="pp-form-item">
              <label class="pp-form-label--required">粘度(mPa.s)</label>
              <InputNumber v-model:value="reportForm.viscosity" class="w-full" :min="0" :precision="3" />
            </div>
            <div class="pp-form-item">
              <label class="pp-form-label--required">浆料温度(℃)</label>
              <InputNumber v-model:value="reportForm.slurryTemperature" class="w-full" :precision="3" />
            </div>
            <template v-if="isFormulaReport">
              <div class="pp-form-item">
                <label class="pp-form-label--required">滤网批号</label>
                <Input v-model:value="reportForm.filterBatchNo" :maxlength="64" />
              </div>
              <div class="pp-form-item">
                <label class="pp-form-label--required">配料罐罐号</label>
                <Input v-model:value="reportForm.batchingTankNo" :maxlength="64" />
              </div>
              <div class="pp-form-item">
                <label class="pp-form-label--required">脱泡罐罐号</label>
                <Input v-model:value="reportForm.defoamingTankNo" :maxlength="64" />
              </div>
            </template>
            <div class="pp-form-item">
              <label>记录人</label>
              <Input v-model:value="reportForm.recorderName" />
            </div>
            <div class="pp-form-item">
              <label>记录时间</label>
              <div
                data-report-time-field="recorderTime"
                @input="handleBookingDateTimeInput('recorderTime', 'datetime', $event)"
              >
                <DatePicker
                  v-model:value="reportForm.recorderTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  @change="(value, dateString) => handleBookingDateTimeChange('recorderTime', 'datetime', value, dateString)"
                />
              </div>
            </div>
            <div class="pp-form-item">
              <label>确认人</label>
              <Input v-model:value="reportForm.confirmerName" disabled />
            </div>
            <div class="pp-form-item">
              <label>确认时间</label>
              <div data-report-time-field="confirmerTime">
                <DatePicker
                  v-model:value="reportForm.confirmerTime"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  class="w-full"
                  disabled
                />
              </div>
            </div>
            <div class="pp-form-item pp-form-item--span-2">
              <label>报工备注</label>
              <Input v-model:value="reportForm.remark" />
            </div>
          </div>
        </fieldset>

        <div class="pp-panel pp-panel--booking-actions">
          <div class="pp-booking-panel__actions">
            <Button size="small" @click="viewMode = 'tabs'">
              <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回过程作业
            </Button>
            <Button size="small" type="primary" danger @click="handleBookingConfirm">
              <IconifyIcon icon="lucide:check" class="mr-1" /> 确认提交报工
            </Button>
          </div>
        </div>
      </div>
    </div>

    <AuthModal
      v-model:visible="authVisible"
      :actionName="authActionName"
      authMode="username"
      :showEquipmentFields="pendingAction === 'START'"
      :mixer-options="mixerOptions"
      :foaming-options="foamingOptions"
      :mixer-equipment-id="reportForm.mixerEquipmentId"
      :foaming-equipment-id="reportForm.foamingEquipmentId"
      @success="handleAuthSuccess"
    />
    <PrintPreviewModal />
    <AModal
      v-model:open="timeCorrectionVisible"
      title="修订开工完工时间"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="timeCorrectionSubmitting"
      @ok="handleTimeCorrectionConfirm"
    >
      <div ref="timeCorrectionFormRef" class="pp-form-grid pp-form-grid--time-correction">
        <div class="pp-form-item" data-time-correction-field="reportDate">
          <label>报工日期</label>
          <DatePicker
            v-model:value="timeCorrectionForm.reportDate"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            class="w-full"
          />
        </div>
        <div class="pp-form-item" data-time-correction-field="startTime">
          <label class="pp-form-label--required">开始时间</label>
          <DatePicker
            v-model:value="timeCorrectionForm.startTime"
            show-time
            value-format="YYYY-MM-DD HH:mm:ss"
            class="w-full"
          />
        </div>
        <div class="pp-form-item" data-time-correction-field="endTime">
          <label :class="{ 'pp-form-label--required': task?.status === 'COMPLETED' }">结束时间</label>
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
      v-model:open="statisticsRevisionVisible"
      title="修订配料统计数据"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="statisticsRevisionSubmitting"
      @ok="handleStatisticsRevisionConfirm"
    >
      <div class="pp-form-grid pp-form-grid--statistics-revision">
        <div class="pp-form-item">
          <label>实际配料量(报工量)</label>
          <InputNumber
            v-model:value="statisticsRevisionForm.feedQty"
            :min="0"
            :precision="3"
            class="w-full"
            addon-after="kg"
          />
        </div>
        <div class="pp-form-item">
          <label>完工数</label>
          <InputNumber
            v-model:value="statisticsRevisionForm.goodQty"
            :min="0"
            :precision="3"
            class="w-full"
            addon-after="kg"
          />
        </div>
        <div class="pp-form-item">
          <label>损耗数</label>
          <InputNumber
            v-model:value="statisticsRevisionForm.scrapQty"
            :min="0"
            :precision="3"
            class="w-full"
            addon-after="kg"
          />
        </div>
        <div class="pp-form-item pp-form-item--full">
          <label class="pp-form-label--required">修订原因</label>
          <Input.TextArea
            v-model:value="statisticsRevisionForm.reason"
            :maxlength="200"
            show-count
            placeholder="请输入修订原因"
          />
        </div>
      </div>
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
            <div class="formula-time-log-diff">
              <span>{{ displayLogValue(record.beforeReportDate) }}</span>
              <IconifyIcon icon="lucide:arrow-right" />
              <strong>{{ displayLogValue(record.afterReportDate) }}</strong>
            </div>
          </template>
          <template v-else-if="column.key === 'startTime'">
            <div class="formula-time-log-diff">
              <span>{{ displayLogValue(record.beforeStartTime) }}</span>
              <IconifyIcon icon="lucide:arrow-right" />
              <strong>{{ displayLogValue(record.afterStartTime) }}</strong>
            </div>
          </template>
          <template v-else-if="column.key === 'endTime'">
            <div class="formula-time-log-diff">
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
          <IconifyIcon icon="lucide:cable" class="device-switch-dialog__hint-icon" />
          <span>当前挂接设备信息</span>
        </div>
        <div class="device-switch-current-grid">
          <div v-for="card in equipmentStatusCards" :key="card.roleLabel" class="device-switch-current-card">
            <div class="device-switch-current-card__head">
              <span class="device-switch-current-card__title">{{ card.roleLabel }}</span>
              <Tag :color="card.statusMeta.color" class="!m-0">{{ card.statusMeta.text }}</Tag>
            </div>
            <div class="device-switch-current-card__name">{{ card.equipmentLabel || '未挂接设备' }}</div>
            <div class="device-switch-current-card__meta">计划号：{{ card.currentPlanNo || '-' }}</div>
            <div class="device-switch-current-card__meta">工序：{{ card.currentOperationName || '-' }}</div>
            <div class="device-switch-current-card__meta">开工时间：{{ card.currentStartTime || '-' }}</div>
            <div class="device-switch-current-card__meta">完工时间：{{ card.currentEndTime || '-' }}</div>
            <div class="device-switch-current-card__meta">操作人：{{ card.currentOperatorName || '-' }}</div>
          </div>
        </div>
        <div class="pp-form-grid pp-form-grid--switch">
          <div class="pp-form-item">
            <label>搅拌机台</label>
            <Select v-model:value="deviceSwitchForm.mixerEquipmentId" :options="mixerOptions" :field-names="{ label: 'label', value: 'value' }" />
          </div>
          <div class="pp-form-item">
            <label>脱泡机台</label>
            <Select v-model:value="deviceSwitchForm.foamingEquipmentId" :options="foamingOptions" :field-names="{ label: 'label', value: 'value' }" />
          </div>
        </div>
      </div>
    </AModal>
  </Modal>
</template>

<style scoped>
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
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}
:deep(.custom-main-tabs .ant-tabs-content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
:deep(.custom-main-tabs .ant-tabs-tabpane) {
  flex: 1;
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

.pp-plan-desc {
  font-size: 12px;
  font-weight: 400;
  color: #6b7280;
}

.pp-plan-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.device-switch-current-card {
  padding: 8px 10px;
  border: 1px solid #dbeafe;
  background: #f8fbff;
  border-radius: 8px;
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

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.booking-plan-check {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  align-items: center;
  margin-bottom: 10px;
  padding: 8px 10px;
  color: #374151;
  background: #fffbeb;
  border: 1px solid #fcd34d;
}

.booking-plan-check strong {
  color: #b45309;
}

.pp-form-grid--switch {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.pp-form-grid--time-correction {
  grid-template-columns: 1fr;
}

.formula-time-log-diff {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  max-width: 100%;
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.formula-time-log-diff span,
.formula-time-log-diff strong {
  overflow: hidden;
  text-overflow: ellipsis;
}

.formula-time-log-diff strong {
  color: #0f172a;
  font-weight: 700;
}

.pp-form-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.pp-form-item label {
  color: #4b5563;
  font-weight: 700;
}

.pp-form-item label.pp-form-label--required {
  color: #dc2626;
}

.pp-form-item--full {
  grid-column: 1 / -1;
}

.pp-form-item--span-2 {
  grid-column: span 2;
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

.pp-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
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

:deep(.pp-fieldset) {
  border: 1px solid #e5e7eb;
  background: #fff;
  margin: 0;
  padding: 8px 12px 10px;
}

:deep(.pp-fieldset > legend) {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
  font-size: 12px;
}

:deep(.pp-readonly-box) {
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

:deep(.pp-readonly-box--code) {
  color: #1677ff;
  font-family: Consolas, Monaco, monospace;
}

:deep(.pp-readonly-box--multiline) {
  align-items: flex-start;
  white-space: pre-wrap;
}

:deep(.ant-form-item) {
  margin-bottom: 0;
}

:deep(.ant-input),
:deep(.ant-input-number),
:deep(.ant-picker),
:deep(.ant-select-selector) {
  border-radius: 0 !important;
}

:deep(.ant-modal-close),
:deep([class*='modal__close']) {
  display: none !important;
}
</style>

<style>
.hc-workstation-detail-modal [class*='modal__header'],
.hc-workstation-detail-modal .ant-modal-header {
  display: none !important;
}

.hc-workstation-detail-modal [class*='modal__body'],
.hc-workstation-detail-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-workstation-detail-modal [class*='modal__content'],
.hc-workstation-detail-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
}
</style>
