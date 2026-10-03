<script lang="ts" setup>
import type { Dayjs } from 'dayjs';
import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';
import type { MesHcScheduleWorkbenchApi } from '#/api/mes/hc/schedule-workbench';
import type { ProductionInstructionContext } from '#/views/mes/hc/shared/production-instruction/types';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { useAccess } from '@vben/access';
import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';
import {
  Button,
  Drawer,
  Empty,
  Input,
  Modal,
  RangePicker,
  Select,
  Spin,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  exportScheduleWorkbench,
  getScheduleWorkbench,
  getScheduleWorkbenchChangeoverDetail,
  updateWetWaterChangeApplyStatus,
} from '#/api/mes/hc/schedule-workbench';
import ProductionInstructionFeedbackDrawer from '#/views/mes/hc/shared/production-instruction/ProductionInstructionFeedbackDrawer.vue';
import ProductionInstructionIssueModal from '#/views/mes/hc/shared/production-instruction/ProductionInstructionIssueModal.vue';

import { formatNumber } from '../plan-order/data';
import Form from '../plan-order/modules/form.vue';
import '../../package-fg/shared/cut-round-board.css';

const OPERATION_OPTIONS = [
  { label: '配料', value: '配料' },
  { label: '湿法', value: '湿法' },
  { label: '磨皮', value: '磨皮' },
  { label: '磨皮1', value: '磨皮1' },
  { label: '磨皮2', value: '磨皮2' },
  { label: '粘胶1', value: '粘胶1' },
  { label: '分切', value: '分切' },
  { label: '压槽', value: '压槽' },
  { label: '粘胶2', value: '粘胶2' },
  { label: '裁切', value: '裁切' },
  { label: '包装入库', value: '包装入库' },
  { label: '包装出库', value: '包装出库' },
];

const DATE_COLUMN_WIDTH = 220;
const SIMPLE_DATE_COLUMN_WIDTH = 168;
const EMPTY_DATE_COLUMN_WIDTH = 88;
const STATIC_TABLE_WIDTH = 890;
const PICKED_STATIC_WIDTH = 68;
const ADVANCED_EXTRA_STATIC_WIDTH = 180;
const TABLE_CARD_VISIBLE_LIMIT = 4;
const TABLE_CARD_WEIGHT_LIMIT = 13;
const SIMPLE_MODE_VISIBLE_CARD_LIMIT = 4;
const COMPLEX_MODE_VISIBLE_CARD_LIMIT = 1;
const SIMPLE_MODE_OPERATION_NAMES = new Set([
  '配料',
  '湿法',
  '磨皮',
  '磨皮1',
  '磨皮2',
  '粘胶1',
  '分切',
  '压槽',
  '粘胶2',
  '裁切',
]);
const PLAN_SPLIT_PENDING_PLAN_KEY = 'mes:plan-split:pending-plan-no';

type CardDisplayMode = 'complex' | 'simple';
type ChangeoverDetailType = 'MODEL' | 'SIZE';

interface ScheduleCardRow {
  danger?: boolean;
  label: string;
  value: string;
}

interface ScheduleCard {
  key: string;
  operationName: string;
  segmentBatchNo?: string;
  titleText: string;
  factSource: string;
  statusClass: string;
  statusText: string;
  reportTimeText: string;
  glueBoardModel: string;
  firstInspectionResult: string;
  inspectionResultLabel: string;
  showInspectionResult: boolean;
  coaNgFlag: boolean;
  ngPieceFlag: boolean;
  showCoa: boolean;
  rows: ScheduleCardRow[];
  detailRows: ScheduleCardRow[];
}

interface InstructionOperationDetail extends MesHcScheduleWorkbenchApi.PlannedOperation {
  operationCode?: string;
  planOperationId?: number;
  processCode?: string;
  processId?: number;
  processName?: string;
}

function naturalWeekRange(base: Dayjs = dayjs(), weekOffset = 0): [Dayjs, Dayjs] {
  const target = base.add(weekOffset, 'week');
  const mondayOffset = target.day() === 0 ? -6 : 1 - target.day();
  const start = target.add(mondayOffset, 'day').startOf('day');
  return [start, start.add(6, 'day')] as [Dayjs, Dayjs];
}

const loading = ref(false);
const exporting = ref(false);
const router = useRouter();
const { hasAccessByCodes } = useAccess();
const canOpenPlanDetail = computed(() =>
  hasAccessByCodes(['mes:pp:schedule-workbench:plan-detail']),
);
const drawerOpen = ref(false);
const selectedRow = ref<MesHcScheduleWorkbenchApi.Row>();
const selectedCell = ref<MesHcScheduleWorkbenchApi.Cell>();
const selectedReportCard = ref<ScheduleCard>();
const selectedReportRow = ref<MesHcScheduleWorkbenchApi.Row>();
const selectedReportCell = ref<MesHcScheduleWorkbenchApi.Cell>();
const reportDetailOpen = ref(false);
const changeoverDetailOpen = ref(false);
const changeoverDetailLoading = ref(false);
const changeoverDetailType = ref<ChangeoverDetailType>('MODEL');
const changeoverDetailRow = ref<MesHcScheduleWorkbenchApi.Row>();
const changeoverDetailRows = ref<MesHcScheduleWorkbenchApi.ChangeoverDetail[]>([]);
const issueInstructionOpen = ref(false);
const instructionRecordOpen = ref(false);
const cardDisplayMode = ref<CardDisplayMode>('simple');
const tableMaximized = ref(false);
const workbench = ref<MesHcScheduleWorkbenchApi.RespVO>({
  dateColumns: [],
  metrics: [],
  rows: [],
  wetWaterChangeApplies: [],
});

const query = reactive({
  dateRange: naturalWeekRange() as [Dayjs, Dayjs] | null,
  keyword: '',
  modelCode: '',
  operationName: undefined as string | undefined,
});

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: Form,
  destroyOnClose: true,
});

const dateColumns = computed(() => workbench.value.dateColumns || []);
const rows = computed(() => workbench.value.rows || []);
const metrics = computed(() => workbench.value.metrics || []);
const ngSummaries = computed(() => workbench.value.ngSummaries || []);
const waterChangeApplies = computed(() => workbench.value.wetWaterChangeApplies || []);
const hasScheduleTableData = computed(() => rows.value.length > 0 || waterChangeApplies.value.length > 0);
const waterChangePendingCount = computed(
  () => waterChangeApplies.value.filter((apply) => apply.status !== 'APPROVED').length,
);
const waterChangeStatusSummary = computed(() => {
  if (waterChangeApplies.value.length === 0) return '-';
  if (waterChangePendingCount.value > 0) return `${waterChangePendingCount.value}条申请`;
  return `${waterChangeApplies.value.length}条同意执行`;
});
const waterChangeSummaryText = computed(() => {
  if (waterChangeApplies.value.length === 0) return '-';
  return waterChangeApplies.value
    .map((apply) => `${formatDateValue(apply.changeStartDate)}~${formatDateValue(apply.changeEndDate)} ${waterChangeStatusLabel(apply.status)}`)
    .join('；');
});
const changeoverDetailTitle = computed(() => `${changeoverTypeLabel(changeoverDetailType.value)}改型明细`);
const changeoverDetailPlanText = computed(() =>
  changeoverDetailType.value === 'MODEL'
    ? changeoverDetailRow.value?.modelCode || '-'
    : normalizeSizeText(changeoverDetailRow.value?.sizeSpec) || '-',
);
const showAdvancedColumns = computed(() => cardDisplayMode.value === 'complex');
const showPickedColumn = computed(() => false);
const legendNote = computed(() => {
  const completionText = '完成：无二磨整单，有二磨按归属；拆批缩分母';
  const ngText = 'NG仅片数';
  if (cardDisplayMode.value === 'complex') {
    return `${completionText}；${ngText}；COA高亮`;
  }
  return `${completionText}；${ngText}`;
});
const reportInstructionContext = computed<ProductionInstructionContext | undefined>(() => {
  const row = selectedReportRow.value;
  const card = selectedReportCard.value;
  if (!row || !card) return undefined;
  const segmentBatchNo = normalizeInstructionBatchNo(card.segmentBatchNo);
  const productionBatchNo = normalizeInstructionBatchNo(rowMotherRollNo(row, false));
  const operationDetail = Object.values(row.cells || {})
    .flatMap((cell) => cell.plannedOperationDetails || [])
    .find((detail) => isSameOperationName(detail.operationName, card.operationName));
  return {
    batchNo: segmentBatchNo || productionBatchNo,
    operationName: card.operationName || undefined,
    planId: row.planId,
    planNo: row.planNo || undefined,
    planOperationId: operationDetail?.planOperationId,
    processName: card.operationName || undefined,
    productModelCode: row.modelCode || undefined,
    productionBatchNo,
    segmentBatchNo,
  };
});
const reportInstructionOperationOptions = computed(() => {
  const map = new Map<string, {
    label: string;
    operationCode?: string;
    operationName?: string;
    planOperationId?: number;
    processCode?: string;
    processId?: number;
    processName?: string;
    value: string;
  }>();
  const addOperation = (
    operationName?: string,
    detail?: InstructionOperationDetail,
  ) => {
    const label = String(operationName || detail?.operationName || detail?.processName || '').trim();
    if (!label) return;
    const operationCode = String(detail?.operationCode || detail?.processCode || '').trim();
    const value = operationCode || label;
    const current = map.get(value);
    map.set(value, {
      label,
      operationCode: operationCode || current?.operationCode,
      operationName: label,
      planOperationId: Number(detail?.planOperationId) || current?.planOperationId,
      processCode: String(detail?.processCode || detail?.operationCode || current?.processCode || '').trim(),
      processId: Number(detail?.processId) || current?.processId,
      processName: String(detail?.processName || current?.processName || label).trim(),
      value,
    });
  };

  addOperation(selectedReportCard.value?.operationName);
  Object.values(selectedReportRow.value?.cells || {}).forEach((cell) => {
    cell.plannedOperationDetails?.forEach((detail) => addOperation(detail.operationName, detail));
    cell.plannedOperations?.forEach((operationName) => addOperation(operationName));
    cell.facts?.forEach((fact) => addOperation(fact.operationName));
  });
  return [...map.values()];
});
const tableMinWidth = computed(
  () => {
    const dateWidth = dateColumns.value.reduce((total, date) => total + dateColumnWidth(date), 0);
    return `${STATIC_TABLE_WIDTH - (showPickedColumn.value ? 0 : PICKED_STATIC_WIDTH) + (showAdvancedColumns.value ? ADVANCED_EXTRA_STATIC_WIDTH : 0) + dateWidth}px`;
  },
);
const metricMap = computed(() =>
  Object.fromEntries(metrics.value.map((metric) => [metric.code || '', metric])),
);
const dateRangeText = computed(() => {
  const start = workbench.value.startDate || query.dateRange?.[0]?.format('YYYY-MM-DD');
  const end = workbench.value.endDate || query.dateRange?.[1]?.format('YYYY-MM-DD');
  return `${formatDateValue(start)} 至 ${formatDateValue(end)}`;
});

function buildParams(): MesHcScheduleWorkbenchApi.ReqVO {
  const [start, end] = query.dateRange || [];
  return {
    startDate: start?.format('YYYY-MM-DD'),
    endDate: end?.format('YYYY-MM-DD'),
    keyword: query.keyword?.trim() || undefined,
    modelCode: query.modelCode?.trim() || undefined,
    operationName: query.operationName,
  };
}

function syncDateRangeFromResponse(resp: MesHcScheduleWorkbenchApi.RespVO) {
  if (!resp.startDate || !resp.endDate) return;
  const start = dayjs(resp.startDate);
  const end = dayjs(resp.endDate);
  if (!start.isValid() || !end.isValid()) return;
  query.dateRange = [start, end] as [Dayjs, Dayjs];
}

async function loadWorkbench() {
  loading.value = true;
  try {
    const resp = await getScheduleWorkbench(buildParams());
    workbench.value = resp;
    syncDateRangeFromResponse(resp);
  } finally {
    loading.value = false;
  }
}

function currentDateRange() {
  const [start, end] = query.dateRange || [];
  if (start?.isValid?.() && end?.isValid?.()) {
    return [start, end] as [Dayjs, Dayjs];
  }
  return naturalWeekRange();
}

async function shiftDateRange(weekOffset: number) {
  const [start] = currentDateRange();
  query.dateRange = naturalWeekRange(start, weekOffset) as [Dayjs, Dayjs];
  await loadWorkbench();
}

async function handleExportExcel() {
  exporting.value = true;
  const hideLoading = message.loading({ content: '正在导出排程工作台...', duration: 0 });
  try {
    const data = await exportScheduleWorkbench(buildParams());
    downloadFileFromBlobPart({ fileName: '排程工作台.xlsx', source: data });
    message.success('导出成功');
  } finally {
    hideLoading();
    exporting.value = false;
  }
}

function handleCreateSchedule() {
  formModalApi.setData(null).open();
}

function openPlan(row: MesHcScheduleWorkbenchApi.Row) {
  formModalApi.setData({
    id: row.planId,
    planStatus: isDraftPlan(row) ? 'DRAFT' : row.planStatus,
  } as MesHcPlanOrderApi.PlanOrder).open();
}

function changeoverTypeLabel(type: ChangeoverDetailType) {
  return type === 'MODEL' ? '型号' : '规格';
}

function rowChangeoverSummaries(row: MesHcScheduleWorkbenchApi.Row, type: ChangeoverDetailType) {
  return type === 'MODEL'
    ? row.modelChangeoverSummaries || []
    : row.sizeChangeoverSummaries || [];
}

function hasChangeoverSummary(row: MesHcScheduleWorkbenchApi.Row, type: ChangeoverDetailType) {
  return rowChangeoverSummaries(row, type).some((item) => Number(item.qty || 0) > 0 && item.actualValue);
}

function changeoverPlanText(row: MesHcScheduleWorkbenchApi.Row, type: ChangeoverDetailType) {
  return type === 'MODEL' ? row.modelCode || '-' : normalizeSizeText(row.sizeSpec) || '-';
}

function changeoverSummaryText(summary: MesHcScheduleWorkbenchApi.ChangeoverSummary) {
  return `改 ${summary.actualValue || '-'}（${Number(summary.qty || 0)}${summary.unit || '片'}）`;
}

function changeoverSummaryTitle(row: MesHcScheduleWorkbenchApi.Row, type: ChangeoverDetailType) {
  const planText = changeoverPlanText(row, type);
  const summaryText = rowChangeoverSummaries(row, type)
    .map(changeoverSummaryText)
    .join('；');
  return `${planText}${summaryText ? `；${summaryText}` : ''}`;
}

function changeoverDetailActualText(detail: MesHcScheduleWorkbenchApi.ChangeoverDetail) {
  if (changeoverDetailType.value === 'SIZE' && detail.actualSuffix) {
    return `${detail.actualValue || '-'} / ${detail.actualSuffix}`;
  }
  return detail.actualValue || '-';
}

async function openChangeoverDetail(
  row: MesHcScheduleWorkbenchApi.Row,
  type: ChangeoverDetailType,
) {
  if (!row.planId || !hasChangeoverSummary(row, type)) {
    return;
  }
  changeoverDetailRow.value = row;
  changeoverDetailType.value = type;
  changeoverDetailRows.value = [];
  changeoverDetailOpen.value = true;
  changeoverDetailLoading.value = true;
  try {
    changeoverDetailRows.value = await getScheduleWorkbenchChangeoverDetail({
      planId: row.planId,
      type,
    });
  } catch {
    message.error(`${changeoverTypeLabel(type)}改型明细加载失败`);
  } finally {
    changeoverDetailLoading.value = false;
  }
}

function openCell(row: MesHcScheduleWorkbenchApi.Row, cell?: MesHcScheduleWorkbenchApi.Cell) {
  selectedRow.value = row;
  selectedCell.value = cell;
  drawerOpen.value = true;
}

function toggleTableMaximized() {
  tableMaximized.value = !tableMaximized.value;
}

function openReportCardDetail(
  row: MesHcScheduleWorkbenchApi.Row | undefined,
  cell: MesHcScheduleWorkbenchApi.Cell | undefined,
  card: ScheduleCard,
) {
  selectedReportRow.value = row;
  selectedReportCell.value = cell;
  selectedReportCard.value = card;
  reportDetailOpen.value = true;
}

function handleTableCardClick(
  row: MesHcScheduleWorkbenchApi.Row,
  cell: MesHcScheduleWorkbenchApi.Cell | undefined,
  card: ScheduleCard,
) {
  if (cardDisplayMode.value === 'simple') {
    openCell(row, cell);
    return;
  }
  openReportCardDetail(row, cell, card);
}

function openIssueInstruction() {
  if (!reportInstructionContext.value) {
    message.warning('当前工序缺少指令上下文');
    return;
  }
  if (issueInstructionOpen.value) return;
  issueInstructionOpen.value = true;
}

function openInstructionRecords() {
  if (!reportInstructionContext.value) {
    message.warning('当前工序缺少指令上下文');
    return;
  }
  instructionRecordOpen.value = true;
}

async function openProcessFlowFromReport() {
  const context = reportInstructionContext.value;
  const batchNo = normalizeInstructionBatchNo(context?.segmentBatchNo || context?.productionBatchNo || context?.batchNo);
  if (!batchNo) {
    message.warning('当前工序缺少批号，无法查看工序流转图');
    return;
  }
  reportDetailOpen.value = false;
  drawerOpen.value = false;
  await router.push({ name: 'MesHcPlanBatchTrace', query: { batchNo } });
}

async function openPlanSplitFromReport() {
  const planNo = String(selectedReportRow.value?.planNo || '').trim();
  if (!planNo) {
    message.warning('当前报工详情缺少计划号，无法进入拆批管理');
    return;
  }
  sessionStorage.setItem(PLAN_SPLIT_PENDING_PLAN_KEY, planNo);
  reportDetailOpen.value = false;
  drawerOpen.value = false;
  await router.push({ name: 'MesHcExecutionPlanSplit' });
}

function handleInstructionIssued() {
  instructionRecordOpen.value = true;
}

function cellOf(row: MesHcScheduleWorkbenchApi.Row, date: string) {
  return row.cells?.[date];
}

function weekdayLabel(date: string) {
  const parsed = dayjs(date);
  return `${parsed.format('MM-DD')} ${['日', '一', '二', '三', '四', '五', '六'][parsed.day()]}`;
}

function metricValue(metric: MesHcScheduleWorkbenchApi.Metric) {
  const value = Number(metric.value || 0);
  if (metric.unit === 'm') {
    return formatNumber(value, 3);
  }
  return formatNumber(value, 0);
}

function formatDateValue(value?: unknown) {
  if (!value) return '-';
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value.map((item) => Number(item));
    if (year && month && day) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
  }
  if (typeof value === 'string') {
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.format('YYYY-MM-DD') : value;
  }
  return String(value);
}

function formatDateTimeValue(value?: unknown) {
  if (!value) return '-';
  if (typeof value === 'string') {
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : value;
  }
  return String(value);
}

function normalizeSizeText(value?: unknown) {
  const text = String(value || '').trim();
  if (!text) return '';
  if (text.includes('740') || text.toUpperCase() === 'B') return '740';
  if (text.includes('775') || text.toUpperCase() === 'A') return '775';
  return text.replace(/mm$/i, '');
}

function waterChangeAppliesForDate(date: string) {
  const target = dayjs(date);
  if (!target.isValid()) return [] as MesHcScheduleWorkbenchApi.WetWaterChangeApply[];
  return waterChangeApplies.value.filter((apply) => {
    const start = dayjs(formatDateValue(apply.changeStartDate));
    const end = dayjs(formatDateValue(apply.changeEndDate));
    return start.isValid() && end.isValid() && !target.isBefore(start, 'day') && !target.isAfter(end, 'day');
  });
}

function waterChangeStatusLabel(status?: string) {
  return status === 'APPROVED' ? '同意执行' : '申请';
}

function waterChangeStatusClass(status?: string) {
  return status === 'APPROVED' ? 'is-approved' : 'is-apply';
}

function waterChangeCardTitle(apply: MesHcScheduleWorkbenchApi.WetWaterChangeApply) {
  return [
    `状态：${waterChangeStatusLabel(apply.status)}`,
    `日期：${formatDateValue(apply.changeStartDate)} 至 ${formatDateValue(apply.changeEndDate)}`,
    `区域：${apply.areaDesc || '-'}`,
    `申请人：${apply.applicantName || '-'}`,
  ].join('；');
}

function openWaterChangeConfirm(apply: MesHcScheduleWorkbenchApi.WetWaterChangeApply) {
  if (!apply.id) {
    message.warning('换水申请缺少ID，无法更新状态');
    return;
  }
  const rangeText = `${formatDateValue(apply.changeStartDate)} 至 ${formatDateValue(apply.changeEndDate)}`;
  if (apply.status === 'APPROVED') {
    Modal.info({
      title: '换水申请已同意执行',
      content: `日期：${rangeText}；区域：${apply.areaDesc || '-'}；确认人：${apply.confirmerName || '-'}`,
    });
    return;
  }
  Modal.confirm({
    title: '确认同意执行换水申请？',
    content: `日期：${rangeText}；区域：${apply.areaDesc || '-'}；申请人：${apply.applicantName || '-'}`,
    okText: '同意执行',
    cancelText: '取消',
    async onOk() {
      await updateWetWaterChangeApplyStatus({
        ...apply,
        status: 'APPROVED',
        confirmTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      });
      message.success('换水申请已更新为同意执行');
      await loadWorkbench();
    },
  });
}

function isDiscretePostPlan(row?: MesHcScheduleWorkbenchApi.Row) {
  const planMode = String(row?.planMode || '').toUpperCase();
  const sourceType = String(row?.sourceType || '').toUpperCase();
  const requirement = String(row?.requirement || '').toUpperCase();
  return (
    planMode === 'DISCRETE_POST' ||
    sourceType === 'NG_INVENTORY' ||
    sourceType === 'DISCRETE_WIP' ||
    requirement.includes('DISCRETE_POST_PROCESS')
  );
}

function prodTypeLabel(row?: MesHcScheduleWorkbenchApi.Row) {
  if (isDiscretePostPlan(row)) return '试验加工';
  const prodType = row?.prodType;
  if (prodType === 'TRIAL_PROCESS') return '试验加工';
  if (prodType === 'MASS') return '量产计划';
  if (prodType === 'RND_TRIAL') return '研发试制';
  return prodType || '-';
}

function processOperationClass(operationName?: string) {
  if (operationName === '配料') return 'is-feeding';
  if (operationName === '湿法') return 'is-wet';
  if (operationName?.includes('包装')) return 'is-package';
  return 'is-post';
}

const LOSS_OPERATIONS = new Set(['湿法', '磨皮', '磨皮1', '磨皮2']);
const SELF_CHECK_NG_OPERATIONS = new Set(['粘胶1', '分切', '压槽', '粘胶2', '裁切']);
const REPORT_DETAIL_MOTHER_BATCH_OPERATIONS = new Set(['配料', '湿法', '磨皮1']);

function supportsLoss(operationName?: string) {
  return LOSS_OPERATIONS.has(operationName || '');
}

function supportsSelfCheckNg(operationName?: string) {
  return SELF_CHECK_NG_OPERATIONS.has(operationName || '');
}

function isNgPieceFact(fact?: MesHcScheduleWorkbenchApi.Fact) {
  return Boolean(fact && supportsSelfCheckNg(fact.operationName) && Number(fact.ngQty || 0) > 0);
}

function normalizedOperationName(value?: string) {
  const text = String(value || '').trim();
  if (text === '磨皮') return '磨皮';
  return text;
}

function isSameOperationName(left?: string, right?: string) {
  const leftName = normalizedOperationName(left);
  const rightName = normalizedOperationName(right);
  if (!leftName || !rightName) return false;
  if (leftName === rightName) return true;
  return leftName === '磨皮' && (rightName === '磨皮1' || rightName === '磨皮2');
}

function shouldShowReportDetailMotherBatch(card?: ScheduleCard) {
  return REPORT_DETAIL_MOTHER_BATCH_OPERATIONS.has(normalizedOperationName(card?.operationName));
}

function shouldShowReportDetailSegmentBatch(card?: ScheduleCard) {
  return !shouldShowReportDetailMotherBatch(card);
}

function isFactCompleted(fact: MesHcScheduleWorkbenchApi.Fact) {
  return Boolean(fact.completedFlag);
}

function statusClassFromLabel(statusLabel?: string, statusCode?: string) {
  const text = `${statusCode || ''} ${statusLabel || ''}`.toUpperCase();
  if (/CANCEL|作废|取消/.test(text)) return 'is-status-cancelled';
  if (/MAINT|REPAIR|维修|保养/.test(text)) return 'is-status-maintenance';
  if (/PAUSE|SUSPEND|暂停/.test(text)) return 'is-status-paused';
  if (/FINISH|COMPLETE|DONE|已完成/.test(text)) return 'is-status-completed';
  if (/RUNNING|PROCESS|加工中|执行中/.test(text)) return 'is-status-processing';
  return 'is-status-planned';
}

function statusTextFromClass(statusClass: string, fallback?: string) {
  if (statusClass === 'is-status-cancelled') return '作废取消';
  if (statusClass === 'is-status-maintenance') return '产线维修保养';
  if (statusClass === 'is-status-paused') return '暂停';
  if (statusClass === 'is-status-completed') return '已完成';
  if (statusClass === 'is-status-processing') return '加工中';
  return fallback || '计划工序';
}

function displayFacts(cell?: MesHcScheduleWorkbenchApi.Cell) {
  const facts = (cell?.facts || []).filter((fact) => !isHiddenCardOperation(fact.operationName));
  if (cardDisplayMode.value === 'complex') return facts;
  return facts.filter((fact) => isSimpleModeOperation(fact.operationName));
}

function isCancelledPlan(row?: MesHcScheduleWorkbenchApi.Row) {
  return ['CANCELLED', 'CANCELED'].includes(String(row?.planStatus || '').toUpperCase());
}

function isDraftPlan(row?: MesHcScheduleWorkbenchApi.Row) {
  return ['DRAFT', 'NOT_RELEASED'].includes(String(row?.planStatus || '').toUpperCase());
}

function hasNgPieceFact(cell?: MesHcScheduleWorkbenchApi.Cell) {
  return displayFacts(cell).some((fact) => isNgPieceFact(fact));
}

function hasCoaAbnormalFact(cell?: MesHcScheduleWorkbenchApi.Cell) {
  return cardDisplayMode.value === 'complex' && displayFacts(cell).some((fact) => fact.coaNgFlag);
}

function cellClass(cell?: MesHcScheduleWorkbenchApi.Cell) {
  if (!cell) return '';
  const facts = displayFacts(cell);
  if (hasCoaAbnormalFact(cell)) return 'schedule-workbench-cell--coa-ng';
  if (hasNgPieceFact(cell)) return 'schedule-workbench-cell--ng';
  if (facts.length > 0) return 'schedule-workbench-cell--done';
  if (cell.plannedOperations && cell.plannedOperations.length > 0) {
    return 'schedule-workbench-cell--planned';
  }
  return '';
}

function rowMotherRollNo(row?: MesHcScheduleWorkbenchApi.Row, includePreview = true) {
  const inventoryMotherBatchNos = normalizeMotherBatchNoList(
    row?.inventorySourceBatchNos,
    isDiscretePostPlan(row) ? '\n' : '、',
  );
  return inventoryMotherBatchNos || row?.motherRollNo || row?.productionBatchNo || row?.batchNo ||
    (includePreview ? row?.motherBatchPreviewNo : '') || '-';
}

function normalizeMotherBatchNoList(value?: string, separator = '、') {
  const batchNos = String(value || '')
    .split(/[、,，;；\s]+/)
    .map((item) => normalizeMotherBatchNo(item))
    .filter(Boolean);
  return Array.from(new Set(batchNos)).join(separator);
}

function normalizeMotherBatchNo(value?: string) {
  const text = String(value || '')
    .trim()
    .replace(/\s+/g, '')
    .toUpperCase();
  if (!text || text === '-') return '';
  const clean = text.split('-J')[0];
  const match = clean.match(/^([A-Z]\d{2}[A-Z]\d{3}[A-Z])/);
  return match?.[1] || clean;
}

function normalizeInstructionBatchNo(value?: string) {
  const text = String(value || '')
    .replace(/[（(][^）)]*[）)]/g, '')
    .trim();
  return text === '-' ? '' : text;
}

function segmentMark(value?: string) {
  const text = value?.trim();
  if (!text) return '';
  const standardMatch = text.match(/^[A-Z]\d{2}[A-Z]\d{3}[A-Z]([PQRS])/i);
  if (standardMatch?.[1]) {
    return standardMatch[1].toUpperCase();
  }
  const endMatch = text.match(/([PQRS])(?:-\w+)?$/i);
  return endMatch?.[1]?.toUpperCase() || '';
}

function factReportTimeValue(fact: MesHcScheduleWorkbenchApi.Fact) {
  if (!fact.lastReportTime) return Number.MAX_SAFE_INTEGER;
  const parsed = dayjs(fact.lastReportTime);
  return parsed.isValid() ? parsed.valueOf() : Number.MAX_SAFE_INTEGER;
}

function formatReportTime(value?: string) {
  if (!value) return '-';
  const parsed = dayjs(value);
  return parsed.isValid() ? parsed.format('HH:mm') : '-';
}

function formatInspectionResult(value?: string) {
  const text = value?.trim();
  if (!text) return '-';
  if (/[、,，/]/.test(text)) {
    const parts = text
      .split(/[、,，/]/)
      .map((item) => formatInspectionResult(item))
      .filter((item) => item && item !== '-');
    return [...new Set(parts)].join('、') || '-';
  }
  const upperText = text.toUpperCase();
  if (['PENDING', 'WAITING', 'APPLIED', 'SUBMITTED', 'INSPECTING'].includes(upperText)) {
    return '待检';
  }
  if (['OK', 'PASS', 'PASSED', 'QUALIFIED', 'NORMAL'].includes(upperText)) {
    return '检验合格';
  }
  if (['NG', 'FAIL', 'FAILED', 'REJECT', 'REJECTED', 'ABNORMAL', 'QUALITY_ABNORMAL'].includes(upperText)) {
    return '检验NG';
  }
  if (['COMPLETED', 'FINISHED', 'DONE'].includes(upperText)) {
    return '已检';
  }
  if (['待检', '检验合格', '合格', '检验NG', '不合格'].includes(text)) {
    if (text === '合格') return '检验合格';
    if (text === '不合格') return '检验NG';
    return text;
  }
  return text;
}

function isInspectionNgResult(value?: string) {
  const text = value || '';
  return /检验NG|不合格|REJECT|FAIL|NG|异常/i.test(text);
}

function sortedFacts(cell?: MesHcScheduleWorkbenchApi.Cell) {
  return [...(cell?.facts || [])].sort((left, right) => {
    const diff = factReportTimeValue(left) - factReportTimeValue(right);
    if (diff !== 0) return diff;
    return (left.operationName || '').localeCompare(right.operationName || '', 'zh-Hans-CN');
  });
}

function supportsCoa(fact: MesHcScheduleWorkbenchApi.Fact) {
  return Boolean(fact.coaFlag);
}

function shouldShowMetric(operationName: string, metric: 'input' | 'lossNg' | 'pending' | 'report') {
  if (metric === 'lossNg') {
    return supportsLoss(operationName);
  }
  if (operationName === '配料') {
    return metric === 'report';
  }
  if (operationName === '湿法') {
    return metric === 'input' || metric === 'report' || metric === 'lossNg';
  }
  if (operationName === '包装入库' || operationName === '包装出库') {
    return metric === 'input' || metric === 'report' || metric === 'pending';
  }
  return true;
}

function formatQuantity(value?: number, unit?: string) {
  const numericValue = Number(value || 0);
  const digits = unit === '片' ? 0 : 3;
  return `${formatNumber(numericValue, digits)}${unit || ''}`;
}

function metricValueText(
  fact: MesHcScheduleWorkbenchApi.Fact,
  metric: 'input' | 'lossNg' | 'pending' | 'report',
) {
  if (metric === 'input') return formatQuantity(fact.inputQty, fact.inputUnit);
  if (metric === 'pending') return formatQuantity(fact.pendingQty, fact.pendingUnit);
  if (metric === 'lossNg') return formatQuantity(fact.lossNgQty, fact.lossNgUnit);
  return formatQuantity(fact.reportQty, fact.reportUnit);
}

function buildSimpleCardRows(operationName: string, fact: MesHcScheduleWorkbenchApi.Fact) {
  void operationName;
  void fact;
  return [] as ScheduleCardRow[];
}

function isSimpleModeOperation(operationName?: string) {
  return SIMPLE_MODE_OPERATION_NAMES.has(operationName || '');
}

function isHiddenCardOperation(operationName?: string) {
  const name = String(operationName || '').trim();
  return name.includes('入库') || name.includes('出库');
}

function completionTagColor(row: MesHcScheduleWorkbenchApi.Row) {
  if (isCancelledPlan(row)) return 'error';
  const status = String(row.completionStatus || '');
  const [finishedText, totalText] = String(status || '0/0').split('/');
  const finishedCount = Number(finishedText);
  const totalCount = Number(totalText);
  if (!Number.isFinite(totalCount) || totalCount <= 0) return 'default';
  if (!Number.isFinite(finishedCount)) return 'default';
  return finishedCount >= totalCount ? 'green' : 'orange';
}

function buildComplexCardRows(
  operationName: string,
  fact: MesHcScheduleWorkbenchApi.Fact,
  firstInspectionResult: string,
  inspectionResultLabel: string,
  showCoa: boolean,
  ngPieceFlag: boolean,
) {
  const rows: ScheduleCardRow[] = [];
  if (shouldShowMetric(operationName, 'input')) {
    rows.push({ label: '投入量', value: metricValueText(fact, 'input') });
  }
  if (shouldShowMetric(operationName, 'report')) {
    rows.push({ label: '报工量', value: metricValueText(fact, 'report') });
  }
  if (shouldShowMetric(operationName, 'pending')) {
    rows.push({ label: '未加工量', value: metricValueText(fact, 'pending') });
  }
  if (shouldShowMetric(operationName, 'lossNg')) {
    rows.push({ label: '损耗量', value: metricValueText(fact, 'lossNg') });
  }
  if (supportsSelfCheckNg(operationName)) {
    rows.push({ danger: ngPieceFlag, label: '工序自检NG数', value: formatQuantity(fact.ngQty, '片') });
  }
  if (showCoa) {
    rows.push({ label: 'COA异常', value: fact.coaNgFlag ? '有异常' : '正常' });
  }
  if (firstInspectionResult !== '-') {
    rows.push({
      danger: isInspectionNgResult(firstInspectionResult),
      label: inspectionResultLabel,
      value: firstInspectionResult,
    });
  }
  return rows;
}

function factToCard(
  fact: MesHcScheduleWorkbenchApi.Fact,
  index: number,
  displayMode: CardDisplayMode = cardDisplayMode.value,
  plannedOperation?: MesHcScheduleWorkbenchApi.PlannedOperation,
): ScheduleCard {
  const operationName = fact.operationName || '-';
  const segmentText = segmentMark(fact.segmentBatchNo);
  const showCoa = displayMode === 'complex' && supportsCoa(fact);
  const ngPieceFlag = isNgPieceFact(fact);
  const firstInspectionResult = formatInspectionResult(fact.firstInspectionResult);
  const inspectionResultLabel = showCoa ? 'COA检验结果' : '首检结果';
  const completed = isFactCompleted(fact);
  const plannedStatusClass = statusClassFromLabel(plannedOperation?.statusLabel, plannedOperation?.statusCode);
  const statusClass = plannedStatusClass === 'is-status-maintenance' || plannedStatusClass === 'is-status-paused'
    ? plannedStatusClass
    : completed ? 'is-status-completed' : 'is-status-processing';
  const detailRows = buildComplexCardRows(
    operationName,
    fact,
    firstInspectionResult,
    inspectionResultLabel,
    showCoa,
    ngPieceFlag,
  );
  const rows = displayMode === 'simple'
    ? buildSimpleCardRows(operationName, fact)
    : detailRows;
  return {
    key: `${operationName}-${fact.segmentBatchNo || 'all'}-${fact.factSource || 'fact'}-${index}`,
    operationName,
    segmentBatchNo: fact.segmentBatchNo,
    titleText: segmentText ? `${segmentText} ${operationName}` : operationName,
    factSource: fact.factSource || '-',
    statusClass,
    statusText: statusTextFromClass(statusClass),
    reportTimeText: formatReportTime(fact.lastReportTime),
    glueBoardModel: fact.glueBoardModel || '-',
    firstInspectionResult,
    inspectionResultLabel,
    showInspectionResult: firstInspectionResult !== '-',
    coaNgFlag: showCoa && Boolean(fact.coaNgFlag),
    ngPieceFlag,
    showCoa,
    rows,
    detailRows,
  };
}

function plannedOperationDetails(cell?: MesHcScheduleWorkbenchApi.Cell) {
  if (cell?.plannedOperationDetails?.length) return cell.plannedOperationDetails;
  return (cell?.plannedOperations || []).map((operationName) => ({
    operationName,
    statusCode: '',
    statusLabel: '计划工序',
  }));
}

function processCards(
  row?: MesHcScheduleWorkbenchApi.Row,
  cell?: MesHcScheduleWorkbenchApi.Cell,
  displayMode: CardDisplayMode = cardDisplayMode.value,
) {
  if (!row || !cell) return [] as ScheduleCard[];
  const facts = sortedFacts(cell)
    .filter((fact) => !isHiddenCardOperation(fact.operationName))
    .filter((fact) => displayMode === 'complex' || isSimpleModeOperation(fact.operationName));
  const plannedOperations = plannedOperationDetails(cell);
  return facts.map((fact, index) => factToCard(
    fact,
    index,
    displayMode,
    plannedOperations.find((operation) => isSameOperationName(operation.operationName, fact.operationName)),
  ));
}

function drawerProcessCards(row?: MesHcScheduleWorkbenchApi.Row, cell?: MesHcScheduleWorkbenchApi.Cell) {
  return processCards(row, cell, 'complex');
}

function hasDisplayFactContent(row?: MesHcScheduleWorkbenchApi.Row, cell?: MesHcScheduleWorkbenchApi.Cell) {
  return processCards(row, cell).length > 0;
}

function hasDateDisplayCards(date: string) {
  return waterChangeAppliesForDate(date).length > 0
    || rows.value.some((row) => hasDisplayFactContent(row, cellOf(row, date)));
}

function dateColumnWidth(date: string) {
  if (!hasDateDisplayCards(date)) return EMPTY_DATE_COLUMN_WIDTH;
  return cardDisplayMode.value === 'simple' ? SIMPLE_DATE_COLUMN_WIDTH : DATE_COLUMN_WIDTH;
}

function tableCardWeight(card: ScheduleCard) {
  return 1 + Math.ceil(card.rows.length / 2);
}

function visibleProcessCards(row?: MesHcScheduleWorkbenchApi.Row, cell?: MesHcScheduleWorkbenchApi.Cell) {
  const cards = processCards(row, cell);
  const visibleLimit = cardDisplayMode.value === 'simple' ? SIMPLE_MODE_VISIBLE_CARD_LIMIT : COMPLEX_MODE_VISIBLE_CARD_LIMIT;
  const visibleCards: ScheduleCard[] = [];
  let weight = 0;
  for (const card of cards) {
    if (visibleCards.length >= Math.min(TABLE_CARD_VISIBLE_LIMIT, visibleLimit)) break;
    const nextWeight = weight + tableCardWeight(card);
    if (visibleCards.length > 0 && nextWeight > TABLE_CARD_WEIGHT_LIMIT) break;
    visibleCards.push(card);
    weight = nextWeight;
  }
  return visibleCards;
}

function hiddenProcessCardCount(row?: MesHcScheduleWorkbenchApi.Row, cell?: MesHcScheduleWorkbenchApi.Cell) {
  return Math.max(processCards(row, cell).length - visibleProcessCards(row, cell).length, 0);
}

function plannedSummary(cell?: MesHcScheduleWorkbenchApi.Cell) {
  return cell?.plannedOperations?.join('、') || '';
}

function progressQuantityText(value?: number, fractionDigits = 0) {
  const numericValue = Number(value || 0);
  if (numericValue <= 0) return '-';
  return formatNumber(numericValue, fractionDigits);
}

function metricText(code: string) {
  const metric = metricMap.value[code];
  return metric ? `${metricValue(metric)}${metric.unit || ''}` : '-';
}

async function handleFormSuccess() {
  message.success('排程数据已更新');
  await loadWorkbench();
}

onMounted(loadWorkbench);
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleFormSuccess" />
    <div class="schedule-workbench package-fg-console">
      <section class="prototype-banner schedule-workbench-banner">
        <span class="console-main-icon"><IconifyIcon icon="lucide:calendar-clock" /></span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">排程工作台</h2>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">周期</span>
              <span class="console-meta-value">{{ dateRangeText }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">排程</span>
              <span class="console-meta-value">{{ metricText('scheduleTotal') }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">排程工作台</span>
              <span class="console-meta-value">{{ rows.length }}条 / {{ dateColumns.length }}天</span>
            </span>
          </div>
        </div>
        <div class="console-action-group schedule-workbench-action-group">
          <button class="action-tile" type="button" @click="loadWorkbench">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="shiftDateRange(-1)">
            <IconifyIcon icon="lucide:chevron-left" />
            <span>上周</span>
          </button>
          <button class="action-tile" type="button" @click="shiftDateRange(1)">
            <IconifyIcon icon="lucide:chevron-right" />
            <span>下周</span>
          </button>
          <button
            v-access:code="['mes:pp:schedule-workbench:create']"
            class="action-tile is-primary"
            type="button"
            @click="handleCreateSchedule"
          >
            <IconifyIcon icon="lucide:calendar-plus" />
            <span>新增计划</span>
          </button>
          <button
            v-access:code="['mes:pp:schedule-workbench:export']"
            class="action-tile"
            type="button"
            :disabled="exporting"
            @click="handleExportExcel"
          >
            <IconifyIcon icon="lucide:file-spreadsheet" />
            <span>{{ exporting ? '导出中' : '导出' }}</span>
          </button>
          <button class="action-tile" type="button" @click="toggleTableMaximized">
            <IconifyIcon :icon="tableMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
            <span>{{ tableMaximized ? '还原' : '最大化' }}</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar schedule-workbench__query plan-order-query-panel">
        <div class="schedule-workbench__query-grid">
          <label class="plan-order-simple-query-label">排程日期</label>
          <RangePicker
            v-model:value="query.dateRange"
            allow-clear
            format="YYYY-MM-DD"
          />
          <label class="plan-order-simple-query-label">关键词</label>
          <Input
            v-model:value="query.keyword"
            allow-clear
            placeholder="计划号 / 型号 / 批号 / 利库批次 / 执行要求"
            @press-enter="loadWorkbench"
          />
          <label class="plan-order-simple-query-label">工序</label>
          <Select
            v-model:value="query.operationName"
            allow-clear
            :options="OPERATION_OPTIONS"
            placeholder="全部工序"
          />
          <label class="plan-order-simple-query-label">型号</label>
          <Input v-model:value="query.modelCode" allow-clear placeholder="产品型号" />
        </div>
      </section>

      <section
        :class="[
          'schedule-workbench__table-host',
          cardDisplayMode === 'simple' ? 'is-simple-mode' : 'is-complex-mode',
          { 'is-maximized': tableMaximized },
        ]"
      >
        <div v-if="tableMaximized" class="schedule-workbench__table-toolbar">
          <div class="schedule-workbench__table-title"></div>
          <div class="schedule-workbench__table-tools">
            <Button
              v-if="tableMaximized"
              size="small"
              class="schedule-workbench__maximize-btn"
              @click="toggleTableMaximized"
            >
              <IconifyIcon :icon="tableMaximized ? 'lucide:minimize-2' : 'lucide:maximize-2'" />
              <span>{{ tableMaximized ? '还原' : '最大化' }}</span>
            </Button>
          </div>
        </div>
        <Spin :spinning="loading">
          <div class="schedule-workbench-table-wrap">
            <table v-if="hasScheduleTableData" class="schedule-workbench-table" :style="{ minWidth: tableMinWidth }">
              <colgroup>
                <col class="schedule-workbench-col-index" />
                <col class="schedule-workbench-col-plan-date" />
                <col class="schedule-workbench-col-mother-roll" />
                <col class="schedule-workbench-col-plan-type" />
                <col class="schedule-workbench-col-model" />
                <col class="schedule-workbench-col-requirement" />
                <col class="schedule-workbench-col-spec" />
                <col v-if="showAdvancedColumns" class="schedule-workbench-col-front-process" />
                <col v-if="showAdvancedColumns" class="schedule-workbench-col-post-process" />
                <col v-if="showAdvancedColumns" class="schedule-workbench-col-split-plan" />
                <col v-if="showPickedColumn" class="schedule-workbench-col-picked" />
                <col class="schedule-workbench-col-due" />
                <col class="schedule-workbench-col-status" />
                <col
                  v-for="date in dateColumns"
                  :key="`col-${date}`"
                  :style="{ width: `${dateColumnWidth(date)}px` }"
                />
              </colgroup>
              <thead>
                <tr>
                  <th class="schedule-workbench-table__fixed fixed-index">序号</th>
                  <th class="schedule-workbench-table__fixed is-center">计划时间</th>
                  <th class="schedule-workbench-table__fixed second is-center">母批批号</th>
                  <th class="is-center">计划类型</th>
                  <th class="is-center">型号</th>
                  <th>执行要求</th>
                  <th>规格</th>
                  <th v-if="showAdvancedColumns">前置</th>
                  <th v-if="showAdvancedColumns">后加工</th>
                  <th v-if="showAdvancedColumns">拆批</th>
                  <th v-if="showPickedColumn">已配货</th>
                  <th>计划交期</th>
                  <th>完成情况</th>
                  <th
                    v-for="date in dateColumns"
                    :key="date"
                    class="schedule-workbench-table__date"
                  >
                    {{ weekdayLabel(date) }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="waterChangeApplies.length > 0" class="schedule-workbench-water-change-row">
                  <td class="schedule-workbench-table__fixed fixed-index" align="center">水</td>
                  <td class="schedule-workbench-table__fixed is-center">
                    <span class="schedule-workbench__link">湿法换水</span>
                    <small>申请信息</small>
                  </td>
                  <td class="schedule-workbench-table__fixed second is-center">
                    <span class="schedule-workbench__ellipsis">换水申请</span>
                  </td>
                  <td class="schedule-workbench-type-cell is-center">
                    <span>湿法</span>
                  </td>
                  <td class="is-center">-</td>
                  <td class="schedule-workbench-requirement-cell">
                    <Tooltip :title="waterChangeSummaryText">
                      <span class="schedule-workbench-requirement-text">{{ waterChangeSummaryText }}</span>
                    </Tooltip>
                  </td>
                  <td align="center">-</td>
                  <td v-if="showAdvancedColumns" align="center">-</td>
                  <td v-if="showAdvancedColumns" align="center">-</td>
                  <td v-if="showAdvancedColumns" align="center">-</td>
                  <td v-if="showPickedColumn" align="right">-</td>
                  <td>{{ dateRangeText }}</td>
                  <td>
                    <Tag :color="waterChangePendingCount > 0 ? 'orange' : 'green'">
                      {{ waterChangeStatusSummary }}
                    </Tag>
                  </td>
                  <td
                    v-for="date in dateColumns"
                    :key="`water-change-${date}`"
                    :class="[
                      'schedule-workbench-cell',
                      'schedule-workbench-water-change-cell',
                      { 'has-water-change': waterChangeAppliesForDate(date).length > 0 },
                    ]"
                  >
                    <div
                      v-if="waterChangeAppliesForDate(date).length > 0"
                      class="schedule-workbench-water-change-list"
                    >
                      <button
                        v-for="apply in waterChangeAppliesForDate(date)"
                        :key="`${apply.id}-${date}`"
                        type="button"
                        :class="['schedule-workbench-water-change-card', waterChangeStatusClass(apply.status)]"
                        :title="waterChangeCardTitle(apply)"
                        @click.stop="openWaterChangeConfirm(apply)"
                      >
                        <span class="schedule-workbench-water-change-card__head">
                          <b>换水申请</b>
                          <i>{{ waterChangeStatusLabel(apply.status) }}</i>
                        </span>
                        <small>{{ formatDateValue(apply.changeStartDate) }} ~ {{ formatDateValue(apply.changeEndDate) }}</small>
                        <small>{{ apply.areaDesc || '-' }}</small>
                      </button>
                    </div>
                    <span v-else class="schedule-workbench-cell__empty">-</span>
                  </td>
                </tr>
                <tr
                  v-for="(row, rowIndex) in rows"
                  :key="row.planId"
                  :class="{ 'is-cancelled-plan': isCancelledPlan(row) }"
                >
                  <td class="schedule-workbench-table__fixed fixed-index" align="center">
                    {{ rowIndex + 1 }}
                  </td>
                  <td class="schedule-workbench-table__fixed is-center">
                    <a
                      v-if="canOpenPlanDetail"
                      class="schedule-workbench__link"
                      @click="openPlan(row)"
                    >
                      {{ formatDateValue(row.planDate) }}
                    </a>
                    <span v-else>{{ formatDateValue(row.planDate) }}</span>
                    <span v-if="isDraftPlan(row)" class="schedule-workbench-plan-draft-mark">
                      未下达
                    </span>
                    <small>{{ row.planNo }}</small>
                    <span v-if="isCancelledPlan(row)" class="schedule-workbench-plan-void-mark">
                      <IconifyIcon icon="lucide:ban" />
                      作废
                    </span>
                  </td>
                  <td class="schedule-workbench-table__fixed second is-center">
                    <Tooltip :title="row.motherBatchPreviewNo ? `${rowMotherRollNo(row)}（未占号预览；确认下发时锁定批号，历史未占号计划以实际占号结果为准）` : rowMotherRollNo(row)">
                      <span
                        :class="[
                          'schedule-workbench__ellipsis',
                          { 'schedule-workbench__multiline': isDiscretePostPlan(row) },
                        ]"
                      >
                        {{ rowMotherRollNo(row) }}
                      </span>
                    </Tooltip>
                  </td>
                  <td class="schedule-workbench-type-cell is-center">
                    <span>{{ prodTypeLabel(row) }}</span>
                  </td>
                  <td class="schedule-workbench-changeover-cell">
                    <div class="schedule-workbench-changeover-cell__plan">
                      <b>{{ row.modelCode || '-' }}</b>
                    </div>
                    <button
                      v-if="hasChangeoverSummary(row, 'MODEL')"
                      class="schedule-workbench-changeover-cell__summary"
                      type="button"
                      :title="changeoverSummaryTitle(row, 'MODEL')"
                      @click.stop="openChangeoverDetail(row, 'MODEL')"
                    >
                      <span
                        v-for="summary in rowChangeoverSummaries(row, 'MODEL')"
                        :key="`${row.planId}-model-${summary.actualValue}`"
                      >
                        {{ changeoverSummaryText(summary) }}
                      </span>
                    </button>
                  </td>
                  <td class="schedule-workbench-requirement-cell">
                    <Tooltip :title="row.requirement">
                      <span class="schedule-workbench-requirement-text">{{ row.requirement || '-' }}</span>
                    </Tooltip>
                  </td>
                  <td class="schedule-workbench-changeover-cell">
                    <div class="schedule-workbench-changeover-cell__plan">
                      <b>{{ normalizeSizeText(row.sizeSpec) || '-' }}</b>
                    </div>
                    <button
                      v-if="hasChangeoverSummary(row, 'SIZE')"
                      class="schedule-workbench-changeover-cell__summary"
                      type="button"
                      :title="changeoverSummaryTitle(row, 'SIZE')"
                      @click.stop="openChangeoverDetail(row, 'SIZE')"
                    >
                      <span
                        v-for="summary in rowChangeoverSummaries(row, 'SIZE')"
                        :key="`${row.planId}-size-${summary.actualValue}`"
                      >
                        {{ changeoverSummaryText(summary) }}
                      </span>
                    </button>
                  </td>
                  <td v-if="showAdvancedColumns" align="center">
                    <Tooltip :title="row.frontProcessEnabled ? '包含前加工工序' : '未选择前加工工序'">
                      <span :class="['schedule-workbench-process-scope-mark', { 'is-active': row.frontProcessEnabled }]">
                        <IconifyIcon :icon="row.frontProcessEnabled ? 'lucide:badge-check' : 'lucide:minus'" />
                      </span>
                    </Tooltip>
                  </td>
                  <td v-if="showAdvancedColumns" align="center">
                    <Tooltip :title="row.postProcessEnabled ? '包含后加工工序' : '未选择后加工工序'">
                      <span :class="['schedule-workbench-process-scope-mark', { 'is-active': row.postProcessEnabled }]">
                        <IconifyIcon :icon="row.postProcessEnabled ? 'lucide:badge-check' : 'lucide:minus'" />
                      </span>
                    </Tooltip>
                  </td>
                  <td v-if="showAdvancedColumns">
                    <Tooltip :title="row.splitPlanNos">
                      <span class="schedule-workbench__ellipsis">{{ row.splitPlanNos || '-' }}</span>
                    </Tooltip>
                  </td>
                  <td v-if="showPickedColumn" align="right">{{ progressQuantityText(row.pickedQty, 0) }}</td>
                  <td>{{ formatDateValue(row.dueDate) }}</td>
                  <td>
                    <Tag :color="completionTagColor(row)">
                      {{ isCancelledPlan(row) ? '作废取消' : row.completionStatus || '-' }}
                    </Tag>
                  </td>
                  <td
                    v-for="date in dateColumns"
                    :key="`${row.planId}-${date}`"
                    :class="['schedule-workbench-cell', cellClass(cellOf(row, date))]"
                    @click="openCell(row, cellOf(row, date))"
                  >
                    <div v-if="hasDisplayFactContent(row, cellOf(row, date))" class="schedule-workbench-card-list">
                      <div :class="['schedule-workbench-card-stack', { 'is-simple': cardDisplayMode === 'simple' }]">
                        <div
                          v-for="card in visibleProcessCards(row, cellOf(row, date))"
                          :key="card.key"
                          :class="[
                            'schedule-workbench-process-card',
                            processOperationClass(card.operationName),
                            card.statusClass,
                            {
                              'is-simple-mode': cardDisplayMode === 'simple',
                              'is-ng-piece': card.ngPieceFlag,
                              'is-coa-ng': card.coaNgFlag,
                            },
                          ]"
                          @click.stop="handleTableCardClick(row, cellOf(row, date), card)"
                        >
                          <div class="schedule-workbench-process-card__head">
                            <b>{{ card.titleText }}</b>
                            <span class="schedule-workbench-process-card__head-extra">
                              <span class="schedule-workbench-status-text">{{ card.statusText }}</span>
                              <span
                                v-if="cardDisplayMode === 'complex' && card.reportTimeText !== '-'"
                                class="schedule-workbench-report-time"
                              >
                                {{ card.reportTimeText }}
                              </span>
                              <span v-if="card.showCoa" :class="['schedule-workbench-coa-pill', card.coaNgFlag ? 'is-ng' : 'is-ok']">
                                COA{{ card.coaNgFlag ? '异常' : '正常' }}
                              </span>
                            </span>
                          </div>
                          <div v-if="card.rows.length > 0" class="schedule-workbench-process-card__metrics">
                            <div
                              v-for="item in card.rows"
                              :key="`${card.key}-${item.label}`"
                              :class="['schedule-workbench-process-card__metric', { 'is-danger': item.danger }]"
                            >
                              <span>{{ item.label }}</span>
                              <b>{{ item.value }}</b>
                            </div>
                          </div>
                        </div>
                      </div>
                      <span
                        v-if="hiddenProcessCardCount(row, cellOf(row, date)) > 0"
                        class="schedule-workbench-card-more"
                        @click.stop="openCell(row, cellOf(row, date))"
                      >
                        +{{ hiddenProcessCardCount(row, cellOf(row, date)) }} 展开
                      </span>
                    </div>
                    <span v-else class="schedule-workbench-cell__empty">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-else class="schedule-workbench-table-empty" :style="{ minWidth: tableMinWidth }">
              <Empty description="暂无排程数据" />
            </div>
          </div>
        </Spin>
      </section>

      <section class="schedule-workbench__legend">
        <span><i class="schedule-workbench-swatch schedule-workbench-swatch--processing"></i>加工中</span>
        <span><i class="schedule-workbench-swatch schedule-workbench-swatch--completed"></i>已完成</span>
        <span><i class="schedule-workbench-swatch schedule-workbench-swatch--maintenance"></i>产线维修保养</span>
        <span><i class="schedule-workbench-swatch schedule-workbench-swatch--paused"></i>暂停</span>
        <span><i class="schedule-workbench-swatch schedule-workbench-swatch--planned"></i>草稿未下达计划</span>
        <span v-if="cardDisplayMode === 'complex'"><i class="schedule-workbench-swatch schedule-workbench-swatch--coa-ng"></i>COA异常</span>
        <span class="schedule-workbench__legend-note">{{ legendNote }}</span>
      </section>

      <Drawer
        v-model:open="drawerOpen"
        destroy-on-close
        placement="right"
        title="排程执行明细"
        width="520"
      >
        <div v-if="selectedRow" class="schedule-workbench-drawer">
          <div class="schedule-workbench-drawer__summary">
            <div>
              <span>型号</span>
              <b>{{ selectedRow.modelCode || '-' }}</b>
            </div>
            <div>
              <span>母批批号</span>
              <b>{{ rowMotherRollNo(selectedRow) }}</b>
            </div>
            <div>
              <span>日期</span>
              <b>{{ selectedCell?.date || '-' }}</b>
            </div>
          </div>
          <div class="schedule-workbench-drawer__plan">
            计划工序：{{ plannedSummary(selectedCell) || '-' }}
          </div>
          <div class="schedule-workbench-drawer__section">
            <h4>当前日期卡片</h4>
            <div v-if="drawerProcessCards(selectedRow, selectedCell).length" class="schedule-workbench-drawer-card-list">
              <div
                v-for="card in drawerProcessCards(selectedRow, selectedCell)"
                :key="card.key"
                :class="[
                  'schedule-workbench-process-card',
                  'is-drawer',
                  processOperationClass(card.operationName),
                  card.statusClass,
                  { 'is-ng-piece': card.ngPieceFlag, 'is-coa-ng': card.coaNgFlag },
                ]"
                @click="openReportCardDetail(selectedRow, selectedCell, card)"
              >
                <div class="schedule-workbench-process-card__head">
                  <b>{{ card.titleText }}</b>
                  <span class="schedule-workbench-process-card__head-extra">
                    <span class="schedule-workbench-status-text">{{ card.statusText }}</span>
                    <span
                      v-if="card.reportTimeText !== '-'"
                      class="schedule-workbench-report-time"
                    >
                      {{ card.reportTimeText }}
                    </span>
                    <span v-if="card.showCoa" :class="['schedule-workbench-coa-pill', card.coaNgFlag ? 'is-ng' : 'is-ok']">
                      COA{{ card.coaNgFlag ? '异常' : '正常' }}
                    </span>
                  </span>
                </div>
                <div v-if="card.rows.length > 0" class="schedule-workbench-process-card__metrics">
                  <div
                    v-for="item in card.rows"
                    :key="`${card.key}-${item.label}`"
                    :class="['schedule-workbench-process-card__metric', { 'is-danger': item.danger }]"
                  >
                    <span>{{ item.label }}</span>
                    <b>{{ item.value }}</b>
                  </div>
                </div>
              </div>
            </div>
            <Empty v-else description="暂无实际报工" />
          </div>
          <div class="schedule-workbench-drawer__section">
            <h4>本周NG</h4>
            <div class="schedule-workbench-ng-list">
              <span v-for="item in ngSummaries" :key="item.operationName">
                {{ item.operationName || '-' }}：{{ item.ngQty || 0 }}
              </span>
              <span v-if="ngSummaries.length === 0">暂无NG</span>
            </div>
          </div>
        </div>
        <div v-else class="schedule-workbench-drawer">
          <div class="schedule-workbench-drawer__header">
            <b>本周NG</b>
          </div>
          <div class="schedule-workbench-ng-list">
            <span v-for="item in ngSummaries" :key="item.operationName">
              {{ item.operationName || '-' }}：{{ item.ngQty || 0 }}
            </span>
            <span v-if="ngSummaries.length === 0">暂无NG</span>
          </div>
        </div>
      </Drawer>

      <Modal
        v-model:open="reportDetailOpen"
        destroy-on-close
        :footer="null"
        :z-index="1060"
        title="工序报工详情"
        width="720px"
      >
        <div v-if="selectedReportCard" class="schedule-workbench-report-detail">
          <div class="schedule-workbench-report-detail__header">
            <div>
              <span>{{ selectedReportCard.reportTimeText }}</span>
              <b>{{ selectedReportCard.titleText }}</b>
            </div>
            <div class="schedule-workbench-report-detail__badges">
              <Tag :color="selectedReportCard.statusClass === 'is-status-completed' ? 'green' : 'processing'">
                {{ selectedReportCard.statusText }}
              </Tag>
              <span
                v-if="selectedReportCard.showCoa"
                :class="['schedule-workbench-coa-pill', selectedReportCard.coaNgFlag ? 'is-ng' : 'is-ok']"
              >
                COA{{ selectedReportCard.coaNgFlag ? '异常' : '正常' }}
              </span>
            </div>
          </div>
          <section class="schedule-workbench-report-detail__section">
            <div class="schedule-workbench-report-detail__section-title">基础信息</div>
            <dl class="schedule-workbench-report-detail__meta">
              <dt>型号</dt>
              <dd>{{ selectedReportRow?.modelCode || '-' }}</dd>
              <template v-if="shouldShowReportDetailMotherBatch(selectedReportCard)">
                <dt>母批批号</dt>
                <dd>{{ rowMotherRollNo(selectedReportRow) }}</dd>
              </template>
              <template v-if="shouldShowReportDetailSegmentBatch(selectedReportCard)">
                <dt>分段批号</dt>
                <dd>{{ selectedReportCard.segmentBatchNo || '-' }}</dd>
              </template>
              <dt>日期</dt>
              <dd>{{ selectedReportCell?.date || '-' }}</dd>
              <dt>计划号</dt>
              <dd>{{ selectedReportRow?.planNo || '-' }}</dd>
              <template v-if="selectedReportCard.operationName === '粘胶1' || selectedReportCard.operationName === '粘胶2'">
                <dt>粘胶型号</dt>
                <dd>{{ selectedReportCard.glueBoardModel }}</dd>
              </template>
              <template v-if="selectedReportCard.showInspectionResult">
                <dt>{{ selectedReportCard.inspectionResultLabel }}</dt>
                <dd>{{ selectedReportCard.firstInspectionResult }}</dd>
              </template>
            </dl>
          </section>
          <section class="schedule-workbench-report-detail__section">
            <div class="schedule-workbench-report-detail__section-title">报工数据</div>
            <div class="schedule-workbench-report-detail__metrics">
              <div
                v-for="item in selectedReportCard.detailRows"
                :key="`${selectedReportCard.key}-${item.label}`"
                :class="{ 'is-danger': item.danger }"
              >
                <span>{{ item.label }}</span>
                <b>{{ item.value }}</b>
              </div>
            </div>
          </section>
          <div class="schedule-workbench-report-detail__actions">
            <Tooltip title="下达生产指令">
              <Button
                v-access:code="['mes:pp:schedule-workbench:issue-instruction']"
                aria-label="下达生产指令"
                class="schedule-workbench-report-detail__icon-btn"
                shape="circle"
                type="primary"
                @click="openIssueInstruction"
              >
                <IconifyIcon icon="lucide:send" />
              </Button>
            </Tooltip>
            <Tooltip title="查看指令记录">
              <Button
                v-access:code="['mes:pp:schedule-workbench:instruction-record']"
                aria-label="查看指令记录"
                class="schedule-workbench-report-detail__icon-btn"
                shape="circle"
                @click="openInstructionRecords"
              >
                <IconifyIcon icon="lucide:list-checks" />
              </Button>
            </Tooltip>
            <Tooltip title="进入拆批管理">
              <Button
                v-access:code="['mes:pp:schedule-workbench:plan-split']"
                aria-label="进入拆批管理"
                class="schedule-workbench-report-detail__icon-btn"
                shape="circle"
                @click="openPlanSplitFromReport"
              >
                <IconifyIcon icon="ant-design:split-cells-outlined" />
              </Button>
            </Tooltip>
            <Tooltip title="查看工序流转图">
              <Button
                v-access:code="['mes:pp:schedule-workbench:process-flow']"
                aria-label="查看工序流转图"
                class="schedule-workbench-report-detail__icon-btn"
                shape="circle"
                @click="openProcessFlowFromReport"
              >
                <IconifyIcon icon="lucide:workflow" />
              </Button>
            </Tooltip>
            <Tooltip title="关闭窗口">
              <Button
                aria-label="关闭窗口"
                class="schedule-workbench-report-detail__icon-btn"
                shape="circle"
                @click="reportDetailOpen = false"
              >
                <IconifyIcon icon="lucide:x" />
              </Button>
            </Tooltip>
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="changeoverDetailOpen"
        destroy-on-close
        :footer="null"
        :z-index="1070"
        :title="changeoverDetailTitle"
        width="840px"
      >
        <Spin :spinning="changeoverDetailLoading">
          <div class="schedule-workbench-changeover-detail">
            <div class="schedule-workbench-changeover-detail__summary">
              <span>{{ changeoverTypeLabel(changeoverDetailType) }}</span>
              <b>{{ changeoverDetailPlanText }}</b>
              <i>{{ changeoverDetailRow?.planNo || '-' }}</i>
            </div>
            <Empty v-if="!changeoverDetailLoading && changeoverDetailRows.length === 0" description="暂无改型明细" />
            <div v-else class="schedule-workbench-changeover-detail__table-wrap">
              <table class="schedule-workbench-changeover-detail__table">
                <thead>
                  <tr>
                    <th>来源工序</th>
                    <th>片号</th>
                    <th>计划值</th>
                    <th>实际值</th>
                    <th v-if="changeoverDetailType === 'MODEL'">粘胶型号</th>
                    <th>报工时间</th>
                    <th>确认人</th>
                    <th v-if="changeoverDetailType !== 'MODEL'">状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="detail in changeoverDetailRows"
                    :key="`${changeoverDetailType}-${detail.sourceReportId}`"
                  >
                    <td>{{ detail.sourceOperation || '-' }}</td>
                    <td>{{ detail.productionBatchNo || '-' }}</td>
                    <td>{{ detail.planValue || '-' }}</td>
                    <td>{{ changeoverDetailActualText(detail) }}</td>
                    <td v-if="changeoverDetailType === 'MODEL'">{{ detail.glueBoardModel || '-' }}</td>
                    <td>{{ formatDateTimeValue(detail.reportTime) }}</td>
                    <td>{{ detail.reporterName || '-' }}</td>
                    <td v-if="changeoverDetailType !== 'MODEL'">{{ detail.reportStatus || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </Spin>
      </Modal>

      <ProductionInstructionIssueModal
        v-model:open="issueInstructionOpen"
        :context="reportInstructionContext"
        :operation-options="reportInstructionOperationOptions"
        title="下达指令"
        :z-index="1160"
        @success="handleInstructionIssued"
      />
      <ProductionInstructionFeedbackDrawer
        v-model:open="instructionRecordOpen"
        :context="reportInstructionContext"
        title="指令记录"
      />
    </div>
  </Page>
</template>

<style scoped>
.schedule-workbench {
  display: grid;
  box-sizing: border-box;
  grid-template-rows: max-content max-content minmax(0, 1fr) max-content;
  align-content: stretch;
  gap: 6px;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 6px 8px;
  overflow: hidden;
  background: #f3f6fa;
}

.schedule-workbench-banner {
  grid-row: 1;
  min-height: 78px;
  max-height: 90px;
}

.schedule-workbench-action-group {
  flex-wrap: nowrap;
}

.schedule-workbench-action-group .action-tile.is-primary {
  color: #175cd3 !important;
  background: linear-gradient(180deg, #eff8ff 0%, #d1e9ff 100%) !important;
  border-color: #84caff !important;
}

.schedule-workbench-action-group .action-tile:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.schedule-workbench__query {
  grid-row: 2;
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  min-width: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.schedule-workbench__query-grid {
  display: grid;
  grid-template-columns: 86px 240px 78px minmax(260px, 1fr) 54px 150px 54px 150px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.schedule-workbench__query-grid > * {
  min-width: 0;
}

.schedule-workbench__query-grid label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.schedule-workbench__query-grid :deep(.ant-picker),
.schedule-workbench__query-grid :deep(.ant-input),
.schedule-workbench__query-grid :deep(.ant-input-affix-wrapper),
.schedule-workbench__query-grid :deep(.ant-select-selector) {
  box-sizing: border-box;
  width: 100%;
  height: 34px;
  min-height: 34px;
  color: #344054;
  background: #f9fbfd !important;
  border-color: #c6d3df !important;
  border-radius: 0;
  box-shadow: none;
}

.schedule-workbench__query-grid :deep(.ant-input) {
  line-height: 32px;
}

.schedule-workbench__query-grid :deep(.ant-input-affix-wrapper) {
  align-items: center;
  padding-top: 0;
  padding-bottom: 0;
}

.schedule-workbench__query-grid :deep(.ant-input-affix-wrapper .ant-input) {
  height: auto;
  min-height: 0;
  line-height: 20px;
}

.schedule-workbench__query-grid :deep(.ant-select-single) {
  height: 34px;
}

.schedule-workbench__query-grid :deep(.ant-select-selection-item),
.schedule-workbench__query-grid :deep(.ant-select-selection-placeholder) {
  line-height: 32px;
}

.schedule-workbench__query-grid :deep(.ant-picker-input > input::placeholder),
.schedule-workbench__query-grid :deep(.ant-input::placeholder),
.schedule-workbench__query-grid :deep(.ant-select-selection-placeholder) {
  color: #98a2b3;
}

.schedule-workbench__table-host {
  --schedule-card-stack-height: 86px;
  --schedule-row-height: 116px;
  --schedule-table-card-height: 86px;

  position: relative;
  grid-row: 3;
  display: grid;
  grid-template-rows: minmax(0, 1fr);
  gap: 0;
  min-height: 0;
  padding: 0 0 8px;
  overflow: hidden;
  border: 1px solid #d9dee8;
  background: #f5f7fb;
}

.schedule-workbench__table-host.is-simple-mode {
  --schedule-card-stack-height: 54px;
  --schedule-row-height: 90px;
  --schedule-table-card-height: 24px;
}

.schedule-workbench__table-host.is-complex-mode {
  --schedule-card-stack-height: 86px;
  --schedule-row-height: 116px;
  --schedule-table-card-height: 86px;
}

.schedule-workbench__table-host.is-maximized {
  position: fixed;
  inset: 12px;
  z-index: 1000;
  height: auto;
  min-height: 0;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 6px;
  padding: 10px;
  border: 1px solid #8ea4bb;
  box-shadow: 0 18px 44px rgb(15 23 42 / 32%);
}

.schedule-workbench__table-host.is-maximized .schedule-workbench__table-toolbar {
  min-height: 38px;
  padding: 5px 8px;
  background: #eef3f8;
  border: 1px solid #c7d3e1;
  border-bottom-color: #aebdcb;
}

.schedule-workbench__table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 30px;
  padding: 0 2px 4px;
  color: #344054;
  border-bottom: 1px solid #e4e7ec;
}

.schedule-workbench__table-title,
.schedule-workbench__table-tools {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.schedule-workbench__table-tools {
  flex: 0 0 auto;
  flex-wrap: nowrap;
  justify-content: flex-end;
  gap: 6px;
}

.schedule-workbench__table-toolbar b {
  color: #101828;
  font-size: 13px;
  font-weight: 900;
}

.schedule-workbench__table-count,
.schedule-workbench__table-tool-label {
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.schedule-workbench__maximize-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #175cd3;
  font-weight: 800;
  background: #eff8ff;
  border-color: #84caff;
  border-radius: 2px;
}

.schedule-workbench__maximize-btn:hover,
.schedule-workbench__maximize-btn:focus {
  color: #fff;
  background: #175cd3;
  border-color: #175cd3;
}

.schedule-workbench__maximize-btn span {
  color: inherit;
}

.schedule-workbench__table-host :deep(.ant-spin-nested-loading),
.schedule-workbench__table-host :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.schedule-workbench-table-wrap {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: scroll;
  scrollbar-gutter: stable;
}

.schedule-workbench-table-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  height: 100%;
  min-height: 360px;
  background: #f4f7fb;
}

.schedule-workbench-table {
  min-width: 1580px;
  table-layout: fixed;
  width: 100%;
  border-spacing: 0;
  border-collapse: separate;
  color: #1f2937;
  font-size: 12px;
}

.schedule-workbench-col-index {
  width: 46px;
}

.schedule-workbench-col-plan-date {
  width: 112px;
}

.schedule-workbench-col-plan-type {
  width: 76px;
}

.schedule-workbench-col-mother-roll {
  width: 118px;
}

.schedule-workbench-col-model {
  width: 132px;
}

.schedule-workbench-col-requirement {
  width: 120px;
}

.schedule-workbench-col-spec {
  width: 102px;
}

.schedule-workbench-col-front-process,
.schedule-workbench-col-post-process {
  width: 58px;
}

.schedule-workbench-process-scope-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #98a2b3;
  font-size: 14px;
}

.schedule-workbench-process-scope-mark.is-active {
  color: #1677ff;
}

.schedule-workbench-col-split-plan {
  width: 64px;
}

.schedule-workbench-col-wet-roll {
  width: 84px;
}

.schedule-workbench-col-second-grinding {
  width: 72px;
}

.schedule-workbench-col-slitting {
  width: 64px;
}

.schedule-workbench-col-adhesive2 {
  width: 70px;
}

.schedule-workbench-col-picked {
  width: 68px;
}

.schedule-workbench-col-due {
  width: 100px;
}

.schedule-workbench-col-status {
  width: 84px;
}

.schedule-workbench-table th,
.schedule-workbench-table td {
  box-sizing: border-box;
  padding: 4px 6px;
  overflow: hidden;
  vertical-align: middle;
  border-right: 1px solid #dde3ed;
  border-bottom: 1px solid #dde3ed;
  background: #f7f9fc;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: keep-all;
}

.schedule-workbench-table tbody tr {
  height: var(--schedule-row-height);
}

.schedule-workbench-table tbody tr.is-cancelled-plan td {
  background: #fff1f3 !important;
}

.schedule-workbench-table tbody tr.is-cancelled-plan .schedule-workbench-table__fixed {
  background: #fff1f3 !important;
}

.schedule-workbench-table tbody td {
  height: var(--schedule-row-height);
}

.schedule-workbench-table th {
  position: sticky;
  z-index: 4;
  top: 0;
  height: 32px;
  color: #344054;
  font-weight: 600;
  background: #edf1f7;
}

.schedule-workbench-table__fixed {
  position: sticky;
  z-index: 3;
  left: 46px;
  border-left: 1px solid #dde3ed;
  background: #f7f9fc !important;
}

.schedule-workbench-table__fixed.fixed-index {
  left: 0;
  text-align: center;
}

.schedule-workbench-table__fixed.second {
  left: 158px;
}

th.schedule-workbench-table__fixed {
  z-index: 5;
  background: #edf1f7 !important;
}

.schedule-workbench-table__date {
  text-align: center;
}

.schedule-workbench-table .is-center {
  text-align: center;
}

.schedule-workbench__link {
  display: block;
  overflow: hidden;
  color: #175cd3;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-table small {
  display: block;
  overflow: hidden;
  color: #667085;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-plan-void-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  max-width: 72px;
  margin-top: 2px;
  padding: 1px 5px;
  color: #b42318;
  font-size: 10px;
  font-weight: 900;
  line-height: 16px;
  background: #ffe4e8;
  border: 1px solid #fda29b;
}

.schedule-workbench-plan-draft-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  padding: 1px 5px;
  color: #344054;
  font-size: 10px;
  font-weight: 800;
  line-height: 16px;
  vertical-align: 1px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.schedule-workbench__ellipsis {
  display: block;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench__multiline {
  max-height: 44px;
  white-space: pre-line;
}

.schedule-workbench-table td.schedule-workbench-requirement-cell {
  overflow: hidden;
  text-overflow: clip;
  white-space: normal;
  word-break: break-word;
}

.schedule-workbench-requirement-text {
  display: block;
  max-width: 100%;
  max-height: calc(var(--schedule-row-height) - 12px);
  overflow: hidden;
  line-height: 18px;
  white-space: pre-line;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.schedule-workbench-type-cell {
  display: table-cell;
}

.schedule-workbench-type-cell span {
  display: block;
  overflow: hidden;
  margin-bottom: 2px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-cell {
  height: var(--schedule-row-height) !important;
  min-height: var(--schedule-row-height);
  max-height: var(--schedule-row-height);
  padding: 4px !important;
  overflow: hidden !important;
  white-space: normal !important;
  vertical-align: top !important;
  cursor: pointer;
  background: #f4f7fb !important;
}

.schedule-workbench-water-change-row td {
  background: #fff7ed !important;
}

.schedule-workbench-water-change-row .schedule-workbench-table__fixed {
  background: #fff7ed !important;
}

.schedule-workbench-water-change-cell {
  cursor: default;
  background: #fffaf3 !important;
}

.schedule-workbench-water-change-cell.has-water-change {
  background: #fff7ed !important;
}

.schedule-workbench-water-change-list {
  display: grid;
  gap: 4px;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.schedule-workbench-water-change-card {
  display: grid;
  gap: 2px;
  width: 100%;
  min-width: 0;
  padding: 4px 5px;
  color: #9a3412;
  text-align: left;
  background: #fffbeb;
  border: 1px solid #fdba74;
  border-radius: 5px;
  cursor: pointer;
  line-height: 1.2;
}

.schedule-workbench-water-change-card:hover {
  border-color: #f97316;
  box-shadow: inset 0 0 0 1px rgba(249, 115, 22, 0.16);
}

.schedule-workbench-water-change-card.is-approved {
  color: #047857;
  background: #ecfdf5;
  border-color: #6ee7b7;
}

.schedule-workbench-water-change-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  min-width: 0;
}

.schedule-workbench-water-change-card__head b {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-water-change-card__head i {
  flex: 0 0 auto;
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
}

.schedule-workbench-water-change-card small {
  color: inherit;
  font-size: 10px;
  opacity: 0.82;
}

.schedule-workbench-card-list {
  position: relative;
  display: grid;
  height: 100%;
  padding-bottom: 20px;
  min-width: 0;
  overflow: hidden;
}

.schedule-workbench-card-stack {
  display: grid;
  gap: 4px;
  max-height: var(--schedule-card-stack-height);
  min-width: 0;
  overflow: hidden;
}

.schedule-workbench-card-stack.is-simple {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 3px;
}

.schedule-workbench-process-card {
  display: grid;
  gap: 2px;
  min-width: 0;
  padding: 5px 6px;
  border: 1px solid #a5b4fc;
  border-radius: 6px;
  background: #f3f6fa;
  cursor: pointer;
  line-height: 1.25;
}

.schedule-workbench__table-host .schedule-workbench-process-card:not(.is-drawer) {
  max-height: var(--schedule-table-card-height);
  overflow: hidden;
}

.schedule-workbench-process-card.is-simple-mode {
  min-height: 24px;
  padding: 3px 4px;
  border-radius: 4px;
}

.schedule-workbench-process-card.is-simple-mode .schedule-workbench-process-card__head {
  justify-content: center;
}

.schedule-workbench-process-card.is-simple-mode .schedule-workbench-process-card__head b {
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}

.schedule-workbench-process-card.is-simple-mode .schedule-workbench-process-card__head-extra {
  display: none;
}

.schedule-workbench-process-card.is-feeding {
  color: #075985;
  background: #f0f9ff;
  border: 1px solid #7dd3fc;
}

.schedule-workbench-process-card.is-wet {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #6ee7b7;
}

.schedule-workbench-process-card.is-post {
  color: #4338ca;
  background: #eef2ff;
  border: 1px solid #a5b4fc;
}

.schedule-workbench-process-card.is-package {
  color: #854d0e;
  background: #fffbeb;
  border: 1px solid #facc15;
}

.schedule-workbench-process-card.is-ng-piece {
  color: #b42318;
  background: #fff1f3;
  border: 1px solid #f97066;
}

.schedule-workbench-process-card.is-coa-ng {
  color: #93370d;
  background: #fffaeb;
  border: 2px solid #f79009;
  box-shadow: inset 0 0 0 1px #fedf89;
}

.schedule-workbench-process-card.is-coa-ng .schedule-workbench-process-card__head b {
  color: #93370d;
}

.schedule-workbench-process-card.is-status-processing {
  color: #111827;
  background: #fff200;
  border-color: #d7b600;
}

.schedule-workbench-process-card.is-status-completed {
  color: #fff;
  background: #2f8f2f;
  border-color: #1f6f1f;
}

.schedule-workbench-process-card.is-status-maintenance {
  color: #fff;
  background: #3f73d8;
  border-color: #2856ad;
}

.schedule-workbench-process-card.is-status-paused {
  color: #fff;
  background: #f00000;
  border-color: #b80000;
}

.schedule-workbench-process-card.is-status-cancelled {
  color: #fff;
  background: #b42318;
  border-color: #7a271a;
}

.schedule-workbench-process-card.is-status-planned {
  color: #344054;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.schedule-workbench-process-card.is-status-processing .schedule-workbench-process-card__head b,
.schedule-workbench-process-card.is-status-processing .schedule-workbench-process-card__metric,
.schedule-workbench-process-card.is-status-processing .schedule-workbench-process-card__metric span,
.schedule-workbench-process-card.is-status-processing .schedule-workbench-process-card__metric b,
.schedule-workbench-process-card.is-status-processing small {
  color: #111827;
}

.schedule-workbench-process-card.is-status-completed .schedule-workbench-process-card__head b,
.schedule-workbench-process-card.is-status-completed .schedule-workbench-process-card__metric,
.schedule-workbench-process-card.is-status-completed .schedule-workbench-process-card__metric span,
.schedule-workbench-process-card.is-status-completed .schedule-workbench-process-card__metric b,
.schedule-workbench-process-card.is-status-completed small {
  color: #fff;
}

.schedule-workbench-process-card.is-status-maintenance .schedule-workbench-process-card__head b,
.schedule-workbench-process-card.is-status-paused .schedule-workbench-process-card__head b,
.schedule-workbench-process-card.is-status-cancelled .schedule-workbench-process-card__head b {
  color: #fff;
}

.schedule-workbench-process-card.is-status-maintenance .schedule-workbench-process-card__metric,
.schedule-workbench-process-card.is-status-paused .schedule-workbench-process-card__metric,
.schedule-workbench-process-card.is-status-cancelled .schedule-workbench-process-card__metric,
.schedule-workbench-process-card.is-status-maintenance .schedule-workbench-process-card__metric span,
.schedule-workbench-process-card.is-status-paused .schedule-workbench-process-card__metric span,
.schedule-workbench-process-card.is-status-cancelled .schedule-workbench-process-card__metric span,
.schedule-workbench-process-card.is-status-maintenance .schedule-workbench-process-card__metric b,
.schedule-workbench-process-card.is-status-paused .schedule-workbench-process-card__metric b,
.schedule-workbench-process-card.is-status-cancelled .schedule-workbench-process-card__metric b,
.schedule-workbench-process-card.is-status-maintenance small,
.schedule-workbench-process-card.is-status-paused small,
.schedule-workbench-process-card.is-status-cancelled small {
  color: rgba(255, 255, 255, 0.92);
}

.schedule-workbench-process-card.is-ng-piece {
  box-shadow: inset 0 0 0 2px #f04438;
}

.schedule-workbench-process-card.is-coa-ng {
  box-shadow: inset 0 0 0 2px #f79009;
}

.schedule-workbench-process-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  min-width: 0;
}

.schedule-workbench-process-card__head b {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-process-card__head-extra {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
}

.schedule-workbench-status-text {
  flex: 0 0 auto;
  color: currentcolor;
  font-size: 10px;
  font-weight: 900;
  line-height: 16px;
  white-space: nowrap;
}

.schedule-workbench-report-time {
  padding: 0 4px;
  color: #344054;
  font-size: 10px;
  font-weight: 800;
  line-height: 16px;
  background: rgba(243, 246, 250, 0.88);
  border: 1px solid #cbd5e1;
  border-radius: 3px;
}

.schedule-workbench-process-card small {
  display: block;
  overflow: hidden;
  color: #64748b;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-process-card__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  min-width: 0;
}

.schedule-workbench-process-card__metric {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 2px;
  align-items: center;
  min-width: 0;
  color: #344054;
  font-size: 11px;
  line-height: 1.25;
}

.schedule-workbench-process-card:not(.is-simple-mode):not(.is-drawer) .schedule-workbench-process-card__metric {
  min-height: 22px;
  padding: 2px 0;
  border-top: 1px dashed rgba(17, 24, 39, 0.32);
}

.schedule-workbench-process-card.is-drawer .schedule-workbench-process-card__metric {
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 8px;
  min-height: 28px;
  padding: 4px 0;
  border-top: 1px dashed rgba(17, 24, 39, 0.32);
}

.schedule-workbench-process-card__metric span {
  overflow: hidden;
  color: #667085;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-process-card__metric b {
  overflow: visible;
  color: #1f2937;
  font-weight: 900;
  text-align: left;
  white-space: normal;
  word-break: break-word;
}

.schedule-workbench-process-card__metric.is-danger {
  padding: 3px 4px;
  color: #fff;
  background: #d92d20;
  border: 1px solid #b42318;
  border-radius: 4px;
}

.schedule-workbench-process-card .schedule-workbench-process-card__metric.is-danger,
.schedule-workbench-process-card .schedule-workbench-process-card__metric.is-danger span,
.schedule-workbench-process-card .schedule-workbench-process-card__metric.is-danger b {
  color: #fff;
}

.schedule-workbench-process-card.is-status-completed .schedule-workbench-process-card__metric,
.schedule-workbench-process-card.is-status-maintenance .schedule-workbench-process-card__metric,
.schedule-workbench-process-card.is-status-paused .schedule-workbench-process-card__metric {
  border-top-color: rgba(255, 255, 255, 0.48);
}

.schedule-workbench-process-card:not(.is-simple-mode) .schedule-workbench-process-card__metric span {
  opacity: 0.78;
}

.schedule-workbench-process-card:not(.is-simple-mode) .schedule-workbench-process-card__metric b {
  opacity: 1;
}

.schedule-workbench-coa-pill {
  flex: 0 0 auto;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 800;
  border-radius: 999px;
}

.schedule-workbench-coa-pill.is-ok {
  color: #067647;
  background: #dcfae6;
  border: 1px solid #75e0a7;
}

.schedule-workbench-coa-pill.is-ng {
  color: #7a2e0e;
  background: #fef0c7;
  border: 1px solid #f79009;
}

.schedule-workbench-card-more {
  position: absolute;
  right: 2px;
  bottom: 1px;
  display: inline-flex;
  justify-content: center;
  width: fit-content;
  padding: 0 6px;
  color: #175cd3;
  cursor: pointer;
  font-size: 11px;
  font-weight: 900;
  line-height: 18px;
  text-align: center;
  background: #eff8ff;
  border: 1px solid #84caff;
  border-radius: 999px;
}

.schedule-workbench-cell--planned {
  background: #eef4ff !important;
}

.schedule-workbench-cell--done {
  background: #ecfdf3 !important;
}

.schedule-workbench-cell--ng {
  background: #fff1f3 !important;
}

.schedule-workbench-cell--coa-ng {
  background: #fffaeb !important;
}

.schedule-workbench-cell__plan {
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-cell__fact {
  overflow: hidden;
  color: #067647;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-cell__empty {
  color: #98a2b3;
}

.schedule-workbench-progress-value {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 3px;
  max-width: 100%;
}

.schedule-workbench-changeover-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 3px;
  color: #b54708;
  font-size: 11px;
  font-weight: 900;
  line-height: 14px;
  background: #fffaeb;
  border: 1px solid #fdb022;
  border-radius: 3px;
}

.schedule-workbench-changeover-cell {
  text-align: left;
  vertical-align: middle;
}

.schedule-workbench-changeover-cell__plan {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #475467;
  line-height: 18px;
}

.schedule-workbench-changeover-cell__plan span {
  flex: 0 0 auto;
  font-size: 11px;
}

.schedule-workbench-changeover-cell__plan b {
  min-width: 0;
  overflow: hidden;
  color: #1d2939;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-changeover-cell__summary {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 0;
  color: #b54708;
  font-size: 11px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.schedule-workbench-changeover-cell__summary span {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-changeover-cell__summary:hover {
  color: #dc6803;
  text-decoration: underline;
}

.schedule-workbench-changeover-detail {
  display: grid;
  gap: 12px;
}

.schedule-workbench-changeover-detail__summary {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  color: #475467;
  background: #f8fafc;
  border: 1px solid #e4e7ec;
  border-radius: 6px;
}

.schedule-workbench-changeover-detail__summary b {
  color: #1d2939;
  font-size: 14px;
}

.schedule-workbench-changeover-detail__summary i {
  margin-left: auto;
  color: #667085;
  font-style: normal;
}

.schedule-workbench-changeover-detail__table-wrap {
  max-height: 420px;
  overflow: auto;
  border: 1px solid #e4e7ec;
  border-radius: 6px;
}

.schedule-workbench-changeover-detail__table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
  font-size: 12px;
}

.schedule-workbench-changeover-detail__table th,
.schedule-workbench-changeover-detail__table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid #eef2f6;
}

.schedule-workbench-changeover-detail__table th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #344054;
  font-weight: 700;
  background: #f8fafc;
}

.schedule-workbench-changeover-detail__table tr:last-child td {
  border-bottom: 0;
}

.schedule-workbench-drawer {
  display: grid;
  gap: 12px;
}

.schedule-workbench-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #eaecf0;
}

.schedule-workbench-drawer__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.schedule-workbench-drawer__summary div {
  min-width: 0;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
}

.schedule-workbench-drawer__summary span {
  display: block;
  margin-bottom: 3px;
  color: #667085;
  font-size: 12px;
}

.schedule-workbench-drawer__summary b {
  display: block;
  overflow: hidden;
  color: #1f2937;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-drawer__plan {
  padding: 6px 8px;
  color: #475467;
  font-size: 12px;
  background: #f5f7fb;
  border: 1px solid #dbe3ef;
  border-radius: 4px;
}

.schedule-workbench-drawer__section h4 {
  margin: 0 0 8px;
  color: #344054;
  font-size: 13px;
}

.schedule-workbench-fact-table {
  width: 100%;
  border-spacing: 0;
  border-collapse: collapse;
  font-size: 12px;
}

.schedule-workbench-fact-table th,
.schedule-workbench-fact-table td {
  padding: 6px;
  border: 1px solid #dde3ed;
}

.schedule-workbench-fact-table th {
  background: #f2f4f7;
}

.schedule-workbench-drawer-card-list {
  display: grid;
  gap: 8px;
}

.schedule-workbench-process-card.is-drawer {
  padding: 8px;
}

.schedule-workbench-process-card.is-drawer .schedule-workbench-process-card__metrics {
  grid-template-columns: minmax(0, 1fr);
  font-size: 12px;
}

.schedule-workbench-process-card.is-drawer .schedule-workbench-process-card__metric b {
  text-align: right;
  white-space: normal;
  word-break: break-word;
}

.schedule-workbench-report-detail {
  display: grid;
  gap: 10px;
  color: #1f2937;
}

.schedule-workbench-report-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 54px;
  padding: 10px 12px;
  background: #eef3f8;
  border: 1px solid #c7d3e1;
}

.schedule-workbench-report-detail__header > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.schedule-workbench-report-detail__header span:first-child {
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.schedule-workbench-report-detail__header b {
  overflow: hidden;
  color: #0f172a;
  font-size: 16px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-workbench-report-detail__badges {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  justify-content: flex-end;
}

.schedule-workbench-report-detail__section {
  border: 1px solid #cfd8e3;
  background: #f8fafc;
}

.schedule-workbench-report-detail__section-title {
  height: 28px;
  padding: 5px 10px;
  color: #344054;
  font-size: 12px;
  font-weight: 900;
  line-height: 18px;
  background: #e8eef5;
  border-bottom: 1px solid #cfd8e3;
}

.schedule-workbench-report-detail__meta {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px minmax(0, 1fr);
  gap: 0;
  margin: 0;
  font-size: 12px;
}

.schedule-workbench-report-detail__meta dt {
  min-height: 32px;
  padding: 7px 8px;
  color: #475467;
  font-weight: 800;
  text-align: right;
  background: #edf2f7;
  border-right: 1px solid #d6dee9;
  border-bottom: 1px solid #d6dee9;
}

.schedule-workbench-report-detail__meta dd {
  min-width: 0;
  margin: 0;
  min-height: 32px;
  padding: 7px 8px;
  color: #111827;
  word-break: break-word;
  background: #f8fafc;
  border-right: 1px solid #d6dee9;
  border-bottom: 1px solid #d6dee9;
}

.schedule-workbench-report-detail__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
}

.schedule-workbench-report-detail__metrics div {
  min-width: 0;
  padding: 8px 10px;
  background: #f8fafc;
  border-right: 1px solid #d6dee9;
  border-bottom: 1px solid #d6dee9;
}

.schedule-workbench-report-detail__metrics div.is-danger {
  color: #fff;
  background: #b42318;
  border-color: #b42318;
}

.schedule-workbench-report-detail__metrics span {
  display: block;
  margin-bottom: 3px;
  color: #667085;
  font-size: 12px;
}

.schedule-workbench-report-detail__metrics b {
  color: #1f2937;
  font-size: 13px;
  word-break: break-word;
}

.schedule-workbench-report-detail__metrics div.is-danger span,
.schedule-workbench-report-detail__metrics div.is-danger b {
  color: #fff;
}

.schedule-workbench-report-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  padding: 10px 0 0;
  border-top: 1px solid #d6dee9;
}

.schedule-workbench-report-detail__actions :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.schedule-workbench-report-detail__icon-btn {
  width: 34px;
  min-width: 34px;
  height: 34px;
  padding: 0;
  font-size: 16px;
}

.schedule-workbench-ng-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.schedule-workbench-ng-list span {
  padding: 3px 8px;
  border: 1px solid #fecdca;
  border-radius: 2px;
  color: #b42318;
  background: #fffbfa;
  font-size: 12px;
}

.schedule-workbench__legend {
  grid-row: 4;
  align-self: end;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  min-height: 32px;
  padding: 6px 10px;
  border: 1px solid #d9dee8;
  background: #f5f7fb;
  color: #475467;
  font-size: 12px;
}

.schedule-workbench__legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.schedule-workbench__legend-note {
  margin-left: auto;
  color: #667085;
}

.schedule-workbench-swatch {
  display: inline-block;
  width: 36px;
  height: 14px;
  border: 1px solid #d0d5dd;
  border-radius: 0;
}

.schedule-workbench-swatch--processing {
  background: #fff200;
  border-color: #d7b600;
}

.schedule-workbench-swatch--completed {
  background: #2f8f2f;
  border-color: #1f6f1f;
}

.schedule-workbench-swatch--maintenance {
  background: #3f73d8;
  border-color: #2856ad;
}

.schedule-workbench-swatch--paused {
  background: #f00000;
  border-color: #b80000;
}

.schedule-workbench-swatch--planned {
  background: #f8fafc;
  border-color: #cbd5e1;
}

.schedule-workbench-swatch--cancelled {
  background: #b42318;
  border-color: #7a271a;
}

.schedule-workbench-swatch--ng {
  background: #fff1f3;
  border-color: #f97066;
}

.schedule-workbench-swatch--coa-ng {
  background: #fffaeb;
  border: 2px solid #f79009;
}

@media (max-width: 1320px) {
  .schedule-workbench__query {
    grid-template-columns: 1fr;
  }

  .schedule-workbench__query-grid {
    grid-template-columns: 72px minmax(0, 1fr) 64px minmax(0, 1fr) 72px minmax(0, 1fr);
  }

}

@media (max-width: 820px) {
  .schedule-workbench__query-grid {
    grid-template-columns: 1fr;
  }

  .schedule-workbench__query-grid label {
    justify-content: flex-start;
  }
}
</style>
