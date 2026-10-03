<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';
import type { MesHcSliceAdjustApi } from '#/api/mes/hc/execution/slice-adjust';

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import { Button, Input, Modal as AModal, Segmented, Table as ATable, Tabs, Tag, Textarea, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getSliceAdjustCandidateList,
  getSliceAdjustRecordList,
  getSliceAdjustTraceList,
  renameSliceAdjustNo,
  swapSliceAdjustNo,
} from '#/api/mes/hc/execution/slice-adjust';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesHcExecutionSliceAdjust' });

type Candidate = MesHcSliceAdjustApi.Candidate;
type TraceRow = MesHcSliceAdjustApi.TraceRow;
type AuditRecord = MesHcSliceAdjustApi.AuditRecord;
type AdjustMode = 'rename' | 'swap';
type SelectSide = 'left' | 'right';
type SwapAnimationState = 'done' | 'idle' | 'running';

interface TraceMergeDetail {
  processName: string;
  resultText: string;
  sourceId?: number;
  sourceName: string;
  statusText: string;
}

interface TraceDisplayRow extends TraceRow {
  mergedCount?: number;
  mergedDetails?: TraceMergeDetail[];
  mergedReportTimeText?: string;
  mergedResultTexts?: string[];
  mergedStatusTexts?: string[];
}

interface TraceAlignRow {
  key: string;
  left?: TraceDisplayRow;
  right?: TraceDisplayRow;
  step: number;
}

interface ProcessDisplayRecord {
  lastProcessCode?: string;
  lastProcessName?: string;
  processCode?: string;
  processName?: string;
  sourceTable?: string;
}

const userStore = useUserStore();
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const activeTab = ref('workbench');
const candidateLoading = ref(false);
const traceLoading = ref(false);
const auditLoading = ref(false);
const swapping = ref(false);
const candidateVisible = ref(false);
const adjustMode = ref<AdjustMode>('swap');
const selectingSide = ref<SelectSide>('left');
const candidates = ref<Candidate[]>([]);
const traceRows = ref<TraceRow[]>([]);
const auditRecords = ref<AuditRecord[]>([]);
const leftCandidate = ref<Candidate | null>(null);
const rightCandidate = ref<Candidate | null>(null);
const lastSwapResult = ref<MesHcSliceAdjustApi.SwapResp | null>(null);
const lastResultMode = ref<AdjustMode>('swap');
const swapAnimationState = ref<SwapAnimationState>('idle');
let currentTimer: ReturnType<typeof setInterval> | null = null;
let swapAnimationTimer: ReturnType<typeof setTimeout> | null = null;

const queryParams = reactive({
  keyword: '',
  segmentBatchNo: '',
});

const adjustForm = reactive({
  reason: '',
  targetSliceNo: '',
});

const auditQuery = reactive({
  keyword: '',
});

const candidateColumns: TableColumnsType<Candidate> = [
  { dataIndex: 'sliceNo', key: 'sliceNo', title: '片号', width: 160 },
  { dataIndex: 'segmentBatchNo', key: 'segmentBatchNo', title: '分段/母批', width: 150 },
  { dataIndex: 'lastProcessName', key: 'lastProcessName', title: '最后工序', width: 120 },
  { dataIndex: 'statusText', key: 'statusText', title: '状态/结果', width: 140 },
  { dataIndex: 'lastReportTime', key: 'lastReportTime', title: '最后时间', width: 150 },
  { fixed: 'right' as const, key: 'action', title: '操作', width: 86 },
];

const traceColumns: TableColumnsType<TraceAlignRow> = [
  { dataIndex: 'step', key: 'step', title: '序', width: 56 },
  { key: 'left', title: '左侧相关单据', width: 360 },
  { key: 'right', title: '右侧相关单据', width: 360 },
];

const auditColumns: TableColumnsType<AuditRecord> = [
  { dataIndex: 'adjustNo', fixed: 'left' as const, key: 'adjustNo', title: '调账单号', width: 180 },
  { dataIndex: 'segmentBatchNo', key: 'segmentBatchNo', title: '分段', width: 150 },
  { key: 'slicePair', title: '片号', width: 260 },
  { key: 'processPair', title: '最后工序', width: 220 },
  { dataIndex: 'affectedRows', key: 'affectedRows', title: '影响行数', width: 90 },
  { dataIndex: 'operatorName', key: 'operatorName', title: '操作人', width: 100 },
  { dataIndex: 'adjustTime', key: 'adjustTime', title: '调账时间', width: 150 },
  { dataIndex: 'adjustReason', key: 'adjustReason', title: '原因', width: 260 },
];

const adjustModeOptions: Array<{ label: string; value: AdjustMode }> = [
  { label: '交换片号', value: 'swap' },
  { label: '直接修改片号', value: 'rename' },
];

const affectedColumns = computed(() => lastSwapResult.value?.affectedColumns || []);
const sortedCandidates = computed(() => [...candidates.value].sort(compareCandidate));
const selectableCandidates = computed(() => sortedCandidates.value.filter((item) => !isExcludedCandidateProcess(item)));
const leftTraceRows = computed(() => buildTraceDisplayRows(traceRows.value.filter((item) => item.side === 'left')));
const rightTraceRows = computed(() => buildTraceDisplayRows(traceRows.value.filter((item) => item.side === 'right')));
const alignedTraceRows = computed<TraceAlignRow[]>(() => {
  const rowCount = Math.max(leftTraceRows.value.length, rightTraceRows.value.length);
  return Array.from({ length: rowCount }, (_, index) => ({
    key: `trace-${index}`,
    left: leftTraceRows.value[index],
    right: rightTraceRows.value[index],
    step: index + 1,
  }));
});
const selectedSegmentBatchNo = computed(() =>
  normalizeSegmentBatchNo(leftCandidate.value?.segmentBatchNo || (adjustMode.value === 'swap' ? rightCandidate.value?.segmentBatchNo : '') || queryParams.segmentBatchNo),
);
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统');
const renameTargetSliceNo = computed(() => adjustForm.targetSliceNo.trim());
const traceTargetSliceNo = computed(() => adjustMode.value === 'rename' ? renameTargetSliceNo.value : rightCandidate.value?.sliceNo);
const canSubmit = computed(() => {
  if (!leftCandidate.value?.sliceNo || !adjustForm.reason.trim()) return false;
  if (adjustMode.value === 'rename') {
    return !!renameTargetSliceNo.value && leftCandidate.value.sliceNo !== renameTargetSliceNo.value;
  }
  return !!rightCandidate.value?.sliceNo && leftCandidate.value.sliceNo !== rightCandidate.value.sliceNo;
});
const traceRefreshDisabled = computed(() => !leftCandidate.value?.sliceNo || !traceTargetSliceNo.value);
const submitButtonText = computed(() => adjustMode.value === 'rename' ? '确认修改片号' : '确认调账');
const submitButtonIcon = computed(() => adjustMode.value === 'rename' ? 'lucide:edit-3' : 'lucide:badge-check');
const submitStatusText = computed(() => {
  if (!leftCandidate.value?.sliceNo) return adjustMode.value === 'rename' ? '请选择原片号' : '请选择左右片号';
  if (adjustMode.value === 'rename' && !renameTargetSliceNo.value) return '请输入新片号';
  if (adjustMode.value === 'swap' && !rightCandidate.value?.sliceNo) return '请选择左右片号';
  if (adjustMode.value === 'rename' && leftCandidate.value.sliceNo === renameTargetSliceNo.value) return '原片号和新片号不能相同';
  if (adjustMode.value === 'swap' && leftCandidate.value.sliceNo === rightCandidate.value?.sliceNo) return '两个片号不能相同';
  if (!adjustForm.reason.trim()) return '请填写调账原因';
  return selectedSegmentBatchNo.value || (adjustMode.value === 'rename' ? '待确认修改' : '待确认调账');
});
const traceTableScroll = { x: 780, y: 360 };

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function resultColor(value?: string) {
  const text = String(value || '').toUpperCase();
  if (text.includes('NG') || text.includes('FAIL') || text.includes('REJECT') || text.includes('不合格') || text.includes('失败') || text.includes('拒绝') || text.includes('异常')) return 'red';
  if (text.includes('OK') || text.includes('PASS') || text.includes('NORMAL') || text.includes('CONFIRMED') || text.includes('COMPLETE') || text.includes('FINISHED') || text.includes('合格') || text.includes('通过') || text.includes('正常') || text.includes('确认') || text.includes('完成') || text.includes('完工')) return 'green';
  if (text.includes('SUBMITTED') || text.includes('LOCK') || text.includes('提交') || text.includes('锁定')) return 'blue';
  return 'default';
}

const STATUS_TEXT_MAP: Record<string, string> = {
  ABNORMAL: '异常',
  AVAILABLE: '可用',
  CANCELLED: '已取消',
  CLOSED: '已关闭',
  COMPLETE: '已完成',
  COMPLETED: '已完成',
  CONFIRMED: '已确认',
  CREATED: '已创建',
  DONE: '已完成',
  DRAFT: '草稿',
  FAIL: '失败',
  FAILED: '失败',
  FINISHED: '已完成',
  FREEZE: '冻结',
  FROZEN: '已冻结',
  HOLD: '冻结',
  INBOUNDED: '已入库',
  INBOUND_LOCKE: '入库锁定',
  INBOUND_LOCKED: '入库锁定',
  INPUTTED: '已录入',
  LOCK: '锁定',
  LOCKED: '已锁定',
  MATCHED: '已匹配',
  MISMATCH: '不匹配',
  NG: '不合格',
  NORMAL: '正常',
  OK: '合格',
  OUTBOUND_LOCKE: '出库锁定',
  OUTBOUND_LOCKED: '出库锁定',
  PASS: '通过',
  PASSED: '通过',
  PENDING: '待处理',
  PROCESSING: '处理中',
  QUALIFIED: '合格',
  REJECT: '拒绝',
  REJECTED: '已拒绝',
  RELEASED: '已放行',
  SCANNED: '已扫码',
  SUBMITTED: '已提交',
  TODO: '待处理',
  UNQUALIFIED: '不合格',
  WAITING: '待处理',
};

const FINISHED_STOCK_STATUS_TEXT_MAP: Record<string, string> = {
  ALLOCATED: '出库锁定',
  AVAILABLE: '已入库',
  INBOUNDED: '已入库',
  INBOUND_LOCKE: '入库锁定',
  INBOUND_LOCKED: '入库锁定',
  OUTBOUND_LOCKE: '出库锁定',
  OUTBOUND_LOCKED: '出库锁定',
  SHIPPED: '已出库',
  WAIT_INBOUND: '待入库',
};

const QUALITY_STATUS_TEXT_MAP: Record<string, string> = {
  ABNORMAL: '质量异常',
  LOCKED: '已锁定',
  NORMAL: '正常',
  QUALITY_ABNORMAL: '质量异常',
  WAITING: '待检',
};

const INSPECTION_SOURCE_TEXT_MAP: Record<string, string> = {
  mes_qms_fqc_item: 'FQC检验项',
  mes_qms_fqc_order: 'FQC检验单',
  mes_qms_fqc_sample: 'FQC样本',
  mes_qms_fqc_scan_record: 'FQC扫码',
  mes_qms_fqc_submission_detail: 'FQC送检',
  mes_sfc_cut_round_inspection_detail: '裁切检验',
};

const PROCESS_SORT_MAP: Record<string, number> = {
  SLITTING: 10,
  PRESS_SLOT: 20,
  ADHESIVE: 30,
  ADHESIVE2: 35,
  CUT_ROUND: 40,
  CUT_ROUND_FQC: 45,
  FQC_SUBMISSION: 50,
  FQC_ITEM: 51,
  FQC_SAMPLE: 52,
  FQC_ORDER: 53,
  FQC_SCAN: 54,
  PACKAGING: 60,
  FG_STOCK: 70,
  SHIPPING_PICK: 80,
  FG_OUTBOUND: 90,
  OPERATION_REPORT: 999,
};

function isFinishedStockProcess(record?: null | ProcessDisplayRecord) {
  if (!record) return false;
  const code = String(record.lastProcessCode || record.processCode || '').toUpperCase();
  return code === 'FG_STOCK' || record.sourceTable === 'mes_inv_finished_stock';
}

function translateStatus(value?: string, record?: null | ProcessDisplayRecord) {
  const text = String(value || '').trim();
  if (!text) return '-';
  const normalized = text.toUpperCase();
  if (isFinishedStockProcess(record) && FINISHED_STOCK_STATUS_TEXT_MAP[normalized]) {
    return FINISHED_STOCK_STATUS_TEXT_MAP[normalized];
  }
  if (QUALITY_STATUS_TEXT_MAP[normalized]) {
    return QUALITY_STATUS_TEXT_MAP[normalized];
  }
  return STATUS_TEXT_MAP[normalized] || text;
}

function displayProcessName(record?: null | ProcessDisplayRecord) {
  if (!record) return '-';
  const name = String(record.lastProcessName || record.processName || '').trim();
  if (isInspectionProcess(record)) {
    return '成品检验';
  }
  return name || '-';
}

function originalProcessName(record?: null | ProcessDisplayRecord) {
  if (!record) return '-';
  const name = String(record.lastProcessName || record.processName || '').trim();
  const code = String(record.lastProcessCode || record.processCode || '').trim();
  return name || code || '-';
}

function sourceDisplayName(row?: TraceRow) {
  const sourceTable = String(row?.sourceTable || '').trim();
  if (!sourceTable) return originalProcessName(row);
  return INSPECTION_SOURCE_TEXT_MAP[sourceTable] || originalProcessName(row);
}

function isInspectionProcess(record?: null | ProcessDisplayRecord) {
  if (!record) return false;
  const code = String(record.lastProcessCode || record.processCode || '').toUpperCase();
  const name = String(record.lastProcessName || record.processName || '').trim();
  const upperName = name.toUpperCase();
  return code.includes('FQC')
    || code.includes('INSPECTION')
    || upperName.includes('FQC')
    || upperName.includes('INSPECTION')
    || name.includes('检验')
    || name.includes('送检');
}

function isWetOrGrindingProcess(record?: null | ProcessDisplayRecord) {
  if (!record) return false;
  const code = String(record.lastProcessCode || record.processCode || '').toUpperCase();
  const name = String(record.lastProcessName || record.processName || '').trim();
  return code.includes('WET')
    || code.includes('GRIND')
    || name.includes('湿法')
    || name.includes('磨皮')
    || name.includes('粗磨')
    || name.includes('精磨')
    || name.includes('研磨')
    || name.includes('磨削')
    || name.includes('磨边');
}

function isExcludedCandidateProcess(record?: null | ProcessDisplayRecord) {
  return isInspectionProcess(record) || isWetOrGrindingProcess(record);
}

function processSortValue(record: ProcessDisplayRecord) {
  const code = String(record.lastProcessCode || record.processCode || '').toUpperCase();
  if (PROCESS_SORT_MAP[code] != null) {
    return PROCESS_SORT_MAP[code];
  }
  const name = String(record.lastProcessName || record.processName || '');
  if (name.includes('分切')) return 10;
  if (name.includes('压槽')) return 20;
  if (name.includes('粘胶2')) return 35;
  if (name.includes('粘胶')) return 30;
  if (name.includes('裁切检验')) return 45;
  if (name.includes('裁切')) return 40;
  if (name.includes('FQC') || name.includes('成品检验')) return 50;
  if (name.includes('包装')) return 60;
  if (name.includes('库存')) return 70;
  if (name.includes('发货')) return 80;
  if (name.includes('出库')) return 90;
  return 999;
}

function compareText(left?: string, right?: string) {
  return String(left || '').localeCompare(String(right || ''), 'zh-Hans-CN');
}

function compareCandidate(left: Candidate, right: Candidate) {
  const processDiff = processSortValue(left) - processSortValue(right);
  if (processDiff !== 0) return processDiff;
  return compareText(normalizeSegmentBatchNo(left.segmentBatchNo), normalizeSegmentBatchNo(right.segmentBatchNo))
    || compareText(left.sliceNo, right.sliceNo)
    || compareText(left.lastReportTime, right.lastReportTime);
}

function compareTrace(left: TraceRow, right: TraceRow) {
  const processDiff = processSortValue(left) - processSortValue(right);
  if (processDiff !== 0) return processDiff;
  return compareText(left.reportTime, right.reportTime)
    || compareText(left.sourceTable, right.sourceTable)
    || Number(left.sourceId || 0) - Number(right.sourceId || 0);
}

function uniqueTraceTextList(rows: TraceRow[], field: 'resultText' | 'statusText') {
  return Array.from(
    new Set(rows.map((item) => translateStatus(item[field], item)).filter((item) => item && item !== '-')),
  );
}

function buildMergedInspectionDetails(rows: TraceRow[]): TraceMergeDetail[] {
  return rows.map((item) => ({
    processName: originalProcessName(item),
    resultText: translateStatus(item.resultText, item),
    sourceId: item.sourceId,
    sourceName: sourceDisplayName(item),
    statusText: translateStatus(item.statusText, item),
  }));
}

function buildTraceDisplayRows(rows: TraceRow[]): TraceDisplayRow[] {
  const sortedRows = [...rows].sort(compareTrace);
  const inspectionRows = sortedRows.filter((item) => isInspectionProcess(item));
  if (inspectionRows.length === 0) {
    return sortedRows;
  }
  const normalRows = sortedRows.filter((item) => !isInspectionProcess(item));
  const orderedInspectionRows = [...inspectionRows].sort(compareTrace);
  const firstInspectionRow = orderedInspectionRows[0]!;
  const lastInspectionRow = orderedInspectionRows[orderedInspectionRows.length - 1]!;
  const firstTime = formatDateTime(firstInspectionRow.reportTime);
  const lastTime = formatDateTime(lastInspectionRow.reportTime);
  const mergedInspectionRow: TraceDisplayRow = {
    ...lastInspectionRow,
    mergedCount: inspectionRows.length,
    mergedDetails: buildMergedInspectionDetails(orderedInspectionRows),
    mergedReportTimeText: firstTime !== lastTime ? `${firstTime} ~ ${lastTime}` : lastTime,
    mergedResultTexts: uniqueTraceTextList(orderedInspectionRows, 'resultText'),
    mergedStatusTexts: uniqueTraceTextList(orderedInspectionRows, 'statusText'),
    processCode: 'FQC_MERGED',
    processName: '成品检验',
    sourceId: undefined,
    sourceTable: 'merged_inspection',
  };
  return [...normalRows, mergedInspectionRow].sort(compareTrace);
}

function traceCell(record: TraceAlignRow, key: unknown) {
  return key === 'left' ? record.left : record.right;
}

function traceStatusTags(row?: TraceDisplayRow) {
  if (!row) return [];
  if (row.mergedStatusTexts?.length) return row.mergedStatusTexts;
  const text = translateStatus(row.statusText, row);
  return text === '-' ? [] : [text];
}

function traceResultTags(row?: TraceDisplayRow) {
  if (!row) return [];
  if (row.mergedResultTexts?.length) return row.mergedResultTexts;
  const text = translateStatus(row.resultText, row);
  return text === '-' ? [] : [text];
}

function traceTimeText(row?: TraceDisplayRow) {
  if (!row) return '-';
  return row.mergedReportTimeText || formatDateTime(row.reportTime);
}

function normalizeSegmentBatchNo(value?: string) {
  return String(value || '').trim().replace(/-J\d+$/i, '');
}

function rowKey(row: Candidate) {
  return `${normalizeSegmentBatchNo(row.segmentBatchNo) || 'none'}__${row.sliceNo || ''}`;
}

function candidateRowEvents(record: Candidate) {
  const handleDblclick = () => chooseCandidate(record);
  return {
    onDblClick: handleDblclick,
    onDblclick: handleDblclick,
    style: { cursor: 'pointer' },
    title: '双击选择片号',
  };
}

async function loadCandidates() {
  candidateLoading.value = true;
  try {
    candidates.value = await getSliceAdjustCandidateList({
      keyword: queryParams.keyword.trim(),
      segmentBatchNo: normalizeSegmentBatchNo(queryParams.segmentBatchNo),
    });
  } finally {
    candidateLoading.value = false;
  }
}

async function loadTraceRows() {
  const leftSliceNo = leftCandidate.value?.sliceNo;
  const rightSliceNo = traceTargetSliceNo.value;
  if (!leftSliceNo || !rightSliceNo) {
    traceRows.value = [];
    return;
  }
  traceLoading.value = true;
  try {
    traceRows.value = await getSliceAdjustTraceList({
      leftSliceNo,
      rightSliceNo,
      segmentBatchNo: selectedSegmentBatchNo.value,
    });
  } finally {
    traceLoading.value = false;
  }
}

async function loadAuditRecords() {
  auditLoading.value = true;
  try {
    auditRecords.value = await getSliceAdjustRecordList({
      keyword: auditQuery.keyword.trim(),
    });
  } finally {
    auditLoading.value = false;
  }
}

function handleTabChange(activeKey: number | string) {
  if (String(activeKey) === 'audit') {
    void loadAuditRecords();
  }
}

function handleAdjustModeChange() {
  resetSwapAnimation();
  lastSwapResult.value = null;
  if (adjustMode.value === 'rename') {
    rightCandidate.value = null;
  }
  void loadTraceRows();
}

function resetSwapAnimation() {
  if (swapAnimationTimer) {
    clearTimeout(swapAnimationTimer);
    swapAnimationTimer = null;
  }
  swapAnimationState.value = 'idle';
}

function startSwapAnimation() {
  resetSwapAnimation();
  swapAnimationState.value = 'running';
  swapAnimationTimer = setTimeout(() => {
    swapAnimationState.value = 'done';
    swapAnimationTimer = null;
  }, 900);
}

function traceRowKey(row?: TraceDisplayRow) {
  return `${row?.side || 'none'}__${row?.sourceTable || 'table'}__${row?.sourceId || row?.reportTime || row?.sliceNo || ''}`;
}

async function openCandidateModal(side: SelectSide) {
  selectingSide.value = side;
  candidateVisible.value = true;
  await loadCandidates();
}

function chooseCandidate(row: Candidate) {
  const other = selectingSide.value === 'left' ? rightCandidate.value : leftCandidate.value;
  const otherSegment = normalizeSegmentBatchNo(other?.segmentBatchNo);
  const rowSegment = normalizeSegmentBatchNo(row.segmentBatchNo);
  if (adjustMode.value === 'swap' && otherSegment && rowSegment && otherSegment !== rowSegment) {
    message.warning('两个片号分段不一致，请确认后重新选择');
    return;
  }
  if (selectingSide.value === 'left') {
    leftCandidate.value = row;
  } else {
    rightCandidate.value = row;
  }
  resetSwapAnimation();
  lastSwapResult.value = null;
  if (!queryParams.segmentBatchNo && rowSegment) {
    queryParams.segmentBatchNo = rowSegment;
  }
  candidateVisible.value = false;
  void loadTraceRows();
}

function clearCandidate(side: SelectSide) {
  if (side === 'left') {
    leftCandidate.value = null;
  } else {
    rightCandidate.value = null;
  }
  resetSwapAnimation();
  lastSwapResult.value = null;
  void loadTraceRows();
}

function submitSwap() {
  if (!leftCandidate.value?.sliceNo || !rightCandidate.value?.sliceNo) {
    message.warning('请先选择两个片号');
    return;
  }
  if (leftCandidate.value.sliceNo === rightCandidate.value.sliceNo) {
    message.warning('两个片号不能相同');
    return;
  }
  if (!adjustForm.reason.trim()) {
    message.warning('请填写调账原因');
    return;
  }
  AModal.confirm({
    content: `确认调换片号 ${leftCandidate.value.sliceNo} 与 ${rightCandidate.value.sliceNo} 的全流程关联数据？`,
    okButtonProps: { danger: true },
    okText: '确认调账',
    onOk: async () => {
      swapping.value = true;
      try {
        lastSwapResult.value = await swapSliceAdjustNo({
          leftSliceNo: leftCandidate.value!.sliceNo!,
          operatorName: currentUserName.value,
          reason: adjustForm.reason.trim(),
          rightSliceNo: rightCandidate.value!.sliceNo!,
          segmentBatchNo: selectedSegmentBatchNo.value,
        });
        lastResultMode.value = 'swap';
        startSwapAnimation();
        message.success(lastSwapResult.value?.message || '片号调账完成');
        await Promise.all([loadCandidates(), loadTraceRows(), loadAuditRecords()]);
      } finally {
        swapping.value = false;
      }
    },
    title: '确认片号调账',
  });
}

function submitRename() {
  if (!leftCandidate.value?.sliceNo) {
    message.warning('请先选择原片号');
    return;
  }
  if (!renameTargetSliceNo.value) {
    message.warning('请输入新片号');
    return;
  }
  if (leftCandidate.value.sliceNo === renameTargetSliceNo.value) {
    message.warning('原片号和新片号不能相同');
    return;
  }
  if (!adjustForm.reason.trim()) {
    message.warning('请填写调账原因');
    return;
  }
  AModal.confirm({
    content: `确认将片号 ${leftCandidate.value.sliceNo} 直接修改为 ${renameTargetSliceNo.value}？`,
    okButtonProps: { danger: true },
    okText: '确认修改',
    onOk: async () => {
      swapping.value = true;
      try {
        lastSwapResult.value = await renameSliceAdjustNo({
          operatorName: currentUserName.value,
          reason: adjustForm.reason.trim(),
          segmentBatchNo: selectedSegmentBatchNo.value,
          sourceSliceNo: leftCandidate.value!.sliceNo!,
          targetSliceNo: renameTargetSliceNo.value,
        });
        lastResultMode.value = 'rename';
        adjustForm.targetSliceNo = lastSwapResult.value?.rightSliceNo || renameTargetSliceNo.value;
        startSwapAnimation();
        message.success(lastSwapResult.value?.message || '片号修改完成');
        await Promise.all([loadCandidates(), loadTraceRows(), loadAuditRecords()]);
      } finally {
        swapping.value = false;
      }
    },
    title: '确认直接修改片号',
  });
}

function submitAdjust() {
  if (adjustMode.value === 'rename') {
    submitRename();
    return;
  }
  submitSwap();
}

onMounted(() => {
  currentTimer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  void loadCandidates();
});

onBeforeUnmount(() => {
  if (currentTimer) clearInterval(currentTimer);
  if (swapAnimationTimer) clearTimeout(swapAnimationTimer);
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console slice-adjust-console">
      <section class="prototype-banner slice-adjust-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:shuffle" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">片号调账</h2>
            <Tag color="processing" class="console-title-tag">车间执行</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">候选</span>
              <span class="console-meta-value">{{ selectableCandidates.length }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">分段</span>
              <span class="console-meta-value">{{ selectedSegmentBatchNo || '-' }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">操作人</span>
              <span class="console-meta-value">{{ currentUserName }}</span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateTime.slice(0, 10) }}</div>
          <strong>{{ currentDateTime.slice(11) }}</strong>
        </div>
        <div class="console-action-group slice-adjust-action-group">
          <button class="action-tile" type="button" @click="submitAdjust">
            <IconifyIcon :icon="submitButtonIcon" />
            <span>{{ adjustMode === 'rename' ? '改片号' : '调账' }}</span>
          </button>
        </div>
      </section>

      <Tabs v-model:active-key="activeTab" class="slice-adjust-tabs" @change="handleTabChange">
        <Tabs.TabPane key="workbench" tab="调账工作台">
          <main class="slice-adjust-body">
            <section class="slice-adjust-work-area">
              <section class="slice-adjust-mode-panel">
                <span>调账方式</span>
                <Segmented
                  v-model:value="adjustMode"
                  :options="adjustModeOptions"
                  size="small"
                  @change="handleAdjustModeChange"
                />
              </section>
              <section class="slice-adjust-workbench">
                <div class="slice-adjust-side">
                  <div class="slice-adjust-side-header">
                    <span>{{ adjustMode === 'rename' ? '原片号' : '左侧片号' }}</span>
                    <Button size="small" type="primary" @click="openCandidateModal('left')">
                      <template #icon><IconifyIcon icon="lucide:search" /></template>
                      选择
                    </Button>
                  </div>
                  <div class="slice-adjust-slice-no">{{ leftCandidate?.sliceNo || '-' }}</div>
                  <div class="slice-adjust-info-grid">
                    <span>分段</span><b>{{ normalizeSegmentBatchNo(leftCandidate?.segmentBatchNo) || '-' }}</b>
                    <span>最后工序</span><b>{{ displayProcessName(leftCandidate) }}</b>
                    <span>状态</span><b>{{ translateStatus(leftCandidate?.statusText, leftCandidate) }}</b>
                    <span>结果</span><b>{{ translateStatus(leftCandidate?.resultText, leftCandidate) }}</b>
                  </div>
                  <Button v-if="leftCandidate" block @click="clearCandidate('left')">清空左侧</Button>
                </div>

                <div class="slice-adjust-swap-mark" :class="`is-${swapAnimationState}`">
                  <IconifyIcon v-if="swapAnimationState === 'done'" icon="lucide:check-circle-2" />
                  <IconifyIcon v-else :icon="adjustMode === 'rename' ? 'lucide:arrow-right' : 'lucide:arrow-left-right'" />
                </div>

                <div v-if="adjustMode === 'swap'" class="slice-adjust-side">
                  <div class="slice-adjust-side-header">
                    <span>右侧片号</span>
                    <Button size="small" type="primary" @click="openCandidateModal('right')">
                      <template #icon><IconifyIcon icon="lucide:search" /></template>
                      选择
                    </Button>
                  </div>
                  <div class="slice-adjust-slice-no">{{ rightCandidate?.sliceNo || '-' }}</div>
                  <div class="slice-adjust-info-grid">
                    <span>分段</span><b>{{ normalizeSegmentBatchNo(rightCandidate?.segmentBatchNo) || '-' }}</b>
                    <span>最后工序</span><b>{{ displayProcessName(rightCandidate) }}</b>
                    <span>状态</span><b>{{ translateStatus(rightCandidate?.statusText, rightCandidate) }}</b>
                    <span>结果</span><b>{{ translateStatus(rightCandidate?.resultText, rightCandidate) }}</b>
                  </div>
                  <Button v-if="rightCandidate" block @click="clearCandidate('right')">清空右侧</Button>
                </div>
                <div v-else class="slice-adjust-side">
                  <div class="slice-adjust-side-header">
                    <span>新片号</span>
                  </div>
                  <Input
                    v-model:value="adjustForm.targetSliceNo"
                    allow-clear
                    class="slice-adjust-target-input"
                    placeholder="输入新片号"
                    @press-enter="loadTraceRows"
                  />
                  <div class="slice-adjust-info-grid">
                    <span>分段</span><b>{{ selectedSegmentBatchNo || '-' }}</b>
                    <span>原片号</span><b>{{ leftCandidate?.sliceNo || '-' }}</b>
                    <span>新片号</span><b>{{ renameTargetSliceNo || '-' }}</b>
                    <span>状态</span><b>{{ renameTargetSliceNo ? '待确认' : '-' }}</b>
                  </div>
                </div>
              </section>

              <section class="slice-adjust-trace-panel">
                <div class="slice-adjust-section-head">
                  <div>
                    <strong>相关单据对齐</strong>
                    <span>{{ leftTraceRows.length }} / {{ rightTraceRows.length }}</span>
                  </div>
                  <Button size="small" :disabled="traceRefreshDisabled" :loading="traceLoading" @click="loadTraceRows">
                    <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
                    刷新
                  </Button>
                </div>
                <ATable
                  class="slice-adjust-trace-table"
                  :columns="traceColumns"
                  :data-source="alignedTraceRows"
                  :loading="traceLoading"
                  :pagination="false"
                  row-key="key"
                  size="small"
                  :scroll="traceTableScroll"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'left' || column.key === 'right'">
                      <div
                        v-if="traceCell(record, column.key)"
                        :key="traceRowKey(traceCell(record, column.key))"
                        class="slice-adjust-doc-card"
                      >
                        <div class="slice-adjust-doc-head">
                          <Tag color="blue">
                            {{ displayProcessName(traceCell(record, column.key)) }}
                            <span v-if="(traceCell(record, column.key)?.mergedCount || 0) > 1">
                              {{ traceCell(record, column.key)?.mergedCount }}条
                            </span>
                          </Tag>
                          <span>{{ traceTimeText(traceCell(record, column.key)) }}</span>
                        </div>
                        <b>{{ traceCell(record, column.key)?.sliceNo || '-' }}</b>
                        <div class="slice-adjust-doc-tags">
                          <Tag
                            v-for="status in traceStatusTags(traceCell(record, column.key))"
                            :key="`status-${status}`"
                            :color="resultColor(status)"
                          >
                            {{ status }}
                          </Tag>
                          <Tag
                            v-for="result in traceResultTags(traceCell(record, column.key))"
                            :key="`result-${result}`"
                            :color="resultColor(result)"
                          >
                            {{ result }}
                          </Tag>
                        </div>
                        <div
                          v-if="traceCell(record, column.key)?.mergedDetails?.length"
                          class="slice-adjust-doc-merge-list"
                        >
                          <span
                            v-for="detail in traceCell(record, column.key)?.mergedDetails"
                            :key="`${detail.sourceName}-${detail.sourceId || detail.processName}`"
                          >
                            <b>{{ detail.sourceName }}</b>
                            <em v-if="detail.sourceId">ID {{ detail.sourceId }}</em>
                            <i v-if="detail.statusText !== '-'">{{ detail.statusText }}</i>
                            <i v-if="detail.resultText !== '-'">{{ detail.resultText }}</i>
                          </span>
                        </div>
                      </div>
                      <div v-else class="slice-adjust-doc-card slice-adjust-doc-card--empty">
                        <IconifyIcon icon="lucide:file-minus-2" />
                        <span>无对应单据</span>
                      </div>
                    </template>
                  </template>
                </ATable>
              </section>
            </section>

            <section class="slice-adjust-submit-panel">
              <div class="slice-adjust-submit-head">
                <div>
                  <strong>确认调账</strong>
                  <span>{{ submitStatusText }}</span>
                </div>
              </div>
              <Textarea
                v-model:value="adjustForm.reason"
                :maxlength="500"
                show-count
                :rows="4"
                placeholder="请输入调账原因"
              />
              <div class="slice-adjust-submit-action">
                <Button block type="primary" danger :disabled="!canSubmit" :loading="swapping" size="large" @click="submitAdjust">
                  <template #icon><IconifyIcon :icon="submitButtonIcon" /></template>
                  {{ submitButtonText }}
                </Button>
              </div>
              <div v-if="lastSwapResult" class="slice-adjust-result">
                <div class="slice-adjust-result-done">
                  <IconifyIcon icon="lucide:check-circle-2" />
                  <span>调账已完成</span>
                </div>
                <div class="slice-adjust-result-main">
                  <span>单号</span><b>{{ lastSwapResult.adjustNo }}</b>
                  <span>影响行数</span><b>{{ lastSwapResult.affectedRows || 0 }}</b>
                  <span>片号</span><b>{{ lastSwapResult.leftSliceNo }} {{ lastResultMode === 'rename' ? '→' : '↔' }} {{ lastSwapResult.rightSliceNo }}</b>
                </div>
                <div v-if="affectedColumns.length > 0" class="slice-adjust-affected-list">
                  <span v-for="item in affectedColumns" :key="`${item.tableName}.${item.columnName}`">
                    {{ item.columnComment }} {{ item.affectedRows || 0 }}
                  </span>
                </div>
              </div>
            </section>
          </main>
        </Tabs.TabPane>
        <Tabs.TabPane key="audit" tab="调账记录">
          <section class="slice-adjust-audit-panel">
            <div class="slice-adjust-audit-toolbar">
              <Input
                v-model:value="auditQuery.keyword"
                allow-clear
                placeholder="调账单号 / 片号 / 分段 / 操作人"
                @press-enter="loadAuditRecords"
              />
              <Button type="primary" :loading="auditLoading" @click="loadAuditRecords">
                <template #icon><IconifyIcon icon="lucide:search" /></template>
                查询
              </Button>
            </div>
            <ATable
              class="slice-adjust-audit-table"
              :columns="auditColumns"
              :data-source="auditRecords"
              :loading="auditLoading"
              :pagination="{ pageSize: 12, showSizeChanger: false }"
              row-key="id"
              size="small"
              :scroll="{ x: 1410, y: 390 }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'segmentBatchNo'">
                  {{ normalizeSegmentBatchNo(record.segmentBatchNo) || '-' }}
                </template>
                <template v-else-if="column.key === 'slicePair'">
                  <span class="slice-adjust-audit-pair">
                    <b>{{ record.leftSliceNo || '-' }}</b>
                    <IconifyIcon :icon="record.adjustType === 'FULL_PROCESS_SLICE_RENAME' ? 'lucide:arrow-right' : 'lucide:arrow-left-right'" />
                    <b>{{ record.rightSliceNo || '-' }}</b>
                  </span>
                </template>
                <template v-else-if="column.key === 'processPair'">
                  {{ displayProcessName({ processName: record.leftLastProcessName }) }}
                  <span class="slice-adjust-audit-arrow">{{ record.adjustType === 'FULL_PROCESS_SLICE_RENAME' ? '→' : '↔' }}</span>
                  {{ displayProcessName({ processName: record.rightLastProcessName }) }}
                </template>
                <template v-else-if="column.key === 'adjustTime'">
                  {{ formatDateTime(record.adjustTime) }}
                </template>
              </template>
            </ATable>
          </section>
        </Tabs.TabPane>
      </Tabs>

      <AModal
        v-model:open="candidateVisible"
        :footer="null"
        :title="selectingSide === 'left' ? '选择左侧片号' : '选择右侧片号'"
        :body-style="{ height: '590px', overflow: 'hidden', padding: '12px 16px' }"
        width="1120px"
        wrap-class-name="slice-adjust-candidate-modal"
      >
        <div class="slice-adjust-modal-toolbar">
          <Input
            v-model:value="queryParams.segmentBatchNo"
            allow-clear
            placeholder="分段 / 母批"
            @press-enter="loadCandidates"
          />
          <Input
            v-model:value="queryParams.keyword"
            allow-clear
            placeholder="片号 / 工序"
            @press-enter="loadCandidates"
          />
          <Button type="primary" :loading="candidateLoading" @click="loadCandidates">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
        </div>
        <ATable
          :columns="candidateColumns"
          :custom-row="candidateRowEvents"
          :data-source="selectableCandidates"
          :loading="candidateLoading"
          :pagination="{ pageSize: 12, showSizeChanger: false }"
          :row-key="rowKey"
          size="small"
          :scroll="{ x: 820, y: 420 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sliceNo'">
              <b class="slice-adjust-table-slice" @dblclick.stop="chooseCandidate(record)">{{ record.sliceNo || '-' }}</b>
            </template>
            <template v-else-if="column.key === 'segmentBatchNo'">
              <span @dblclick.stop="chooseCandidate(record)">
                {{ normalizeSegmentBatchNo(record.segmentBatchNo) || '-' }}
              </span>
            </template>
            <template v-else-if="column.key === 'lastProcessName'">
              <Tag color="blue" @dblclick.stop="chooseCandidate(record)">{{ displayProcessName(record) }}</Tag>
            </template>
            <template v-else-if="column.key === 'statusText'">
              <Tag v-if="record.statusText" :color="resultColor(record.statusText)" @dblclick.stop="chooseCandidate(record)">
                {{ translateStatus(record.statusText, record) }}
              </Tag>
              <Tag v-if="record.resultText" :color="resultColor(record.resultText)" @dblclick.stop="chooseCandidate(record)">
                {{ translateStatus(record.resultText, record) }}
              </Tag>
              <span v-if="!record.statusText && !record.resultText">-</span>
            </template>
            <template v-else-if="column.key === 'lastReportTime'">
              <span @dblclick.stop="chooseCandidate(record)">{{ formatDateTime(record.lastReportTime) }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <Button size="small" type="link" @click="chooseCandidate(record)">选择</Button>
            </template>
          </template>
        </ATable>
      </AModal>
    </div>
  </Page>
</template>

<style scoped>
.slice-adjust-console {
  grid-template-rows: max-content minmax(0, 1fr);
}

.slice-adjust-action-group {
  min-width: 150px;
}

.slice-adjust-tabs {
  display: flex;
  align-self: stretch;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

:global(.slice-adjust-tabs > .ant-tabs-nav) {
  padding: 0 10px;
  margin: 0 0 8px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
}

:global(.slice-adjust-tabs > .ant-tabs-content-holder),
:global(.slice-adjust-tabs > .ant-tabs-content-holder > .ant-tabs-content),
:global(.slice-adjust-tabs > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane) {
  min-height: 0;
  overflow: hidden;
}

:global(.slice-adjust-tabs > .ant-tabs-content-holder) {
  flex: 1;
  height: 100%;
}

:global(.slice-adjust-tabs > .ant-tabs-content-holder > .ant-tabs-content) {
  height: 100%;
}

:global(.slice-adjust-tabs > .ant-tabs-content-holder > .ant-tabs-content > .ant-tabs-tabpane) {
  height: 100%;
}

.slice-adjust-modal-toolbar {
  display: grid;
  grid-template-columns: 180px minmax(240px, 1fr) max-content;
  gap: 8px;
  align-items: center;
}

.slice-adjust-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 10px;
  align-items: stretch;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.slice-adjust-work-area {
  display: grid;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.slice-adjust-mode-panel {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.slice-adjust-mode-panel > span {
  color: #0f172a;
  font-size: 12px;
  font-weight: 900;
}

.slice-adjust-workbench {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 72px minmax(0, 1fr);
  gap: 10px;
  min-height: 0;
}

.slice-adjust-side,
.slice-adjust-submit-panel {
  min-width: 0;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.slice-adjust-side-header,
.slice-adjust-submit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  color: #0f172a;
  font-weight: 900;
}

.slice-adjust-submit-head > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.slice-adjust-submit-head span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-adjust-slice-no {
  min-height: 46px;
  padding: 8px 10px;
  margin-bottom: 12px;
  overflow: hidden;
  color: #0f172a;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 24px;
  font-weight: 950;
  line-height: 30px;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
}

.slice-adjust-target-input {
  height: 46px;
  margin-bottom: 12px;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 20px;
  font-weight: 900;
}

.slice-adjust-info-grid {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 8px 10px;
  margin-bottom: 14px;
}

.slice-adjust-info-grid span {
  color: #64748b;
  font-weight: 800;
}

.slice-adjust-info-grid b {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-adjust-swap-mark {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  color: #0284c7;
  font-size: 32px;
}

.slice-adjust-swap-mark::before,
.slice-adjust-swap-mark::after {
  position: absolute;
  width: 28px;
  height: 2px;
  content: '';
  background: #38bdf8;
  opacity: 0;
}

.slice-adjust-swap-mark.is-running {
  color: #0ea5e9;
  animation: slice-adjust-swap-pulse 0.9s ease-in-out;
}

.slice-adjust-swap-mark.is-running::before {
  animation: slice-adjust-swap-line-left 0.9s ease-in-out;
}

.slice-adjust-swap-mark.is-running::after {
  animation: slice-adjust-swap-line-right 0.9s ease-in-out;
}

.slice-adjust-swap-mark.is-done {
  color: #16a34a;
}

.slice-adjust-submit-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  overflow: hidden;
}

.slice-adjust-submit-action {
  padding-top: 2px;
}

:global(.slice-adjust-submit-action .ant-btn) {
  height: 40px;
  font-weight: 900;
}

:global(.slice-adjust-submit-action .ant-btn[disabled]) {
  color: #64748b !important;
  background: #e2e8f0 !important;
  border-color: #cbd5e1 !important;
  opacity: 1;
}

.slice-adjust-result {
  display: grid;
  gap: 8px;
  min-height: 0;
  padding-top: 10px;
  border-top: 1px solid #cbd5e1;
}

.slice-adjust-result-done {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #15803d;
  font-weight: 950;
}

.slice-adjust-result-done svg {
  font-size: 18px;
}

.slice-adjust-result-main {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr);
  gap: 7px 10px;
}

.slice-adjust-result-main span {
  color: #64748b;
  font-weight: 800;
}

.slice-adjust-result-main b {
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-adjust-affected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 180px;
  overflow: auto;
}

.slice-adjust-affected-list span {
  padding: 3px 8px;
  color: #075985;
  font-size: 12px;
  font-weight: 800;
  background: #e0f2fe;
  border: 1px solid #bae6fd;
}

.slice-adjust-modal-toolbar {
  grid-template-columns: 180px minmax(240px, 1fr) max-content;
  margin-bottom: 10px;
}

.slice-adjust-table-slice {
  font-family: Consolas, 'Microsoft YaHei', monospace;
}

.slice-adjust-trace-panel,
.slice-adjust-audit-panel {
  min-width: 0;
  min-height: 0;
  padding: 12px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
}

.slice-adjust-trace-panel,
.slice-adjust-audit-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slice-adjust-audit-panel {
  height: 100%;
}

.slice-adjust-section-head,
.slice-adjust-audit-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.slice-adjust-section-head > div {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.slice-adjust-section-head strong {
  color: #0f172a;
  font-weight: 950;
}

.slice-adjust-section-head span {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.slice-adjust-doc-card {
  position: relative;
  display: grid;
  gap: 6px;
  min-width: 0;
  min-height: 86px;
  padding: 8px 10px 8px 12px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.slice-adjust-doc-card::before {
  position: absolute;
  top: 8px;
  bottom: 8px;
  left: 0;
  width: 3px;
  content: '';
  background: #0ea5e9;
  border-radius: 0 2px 2px 0;
}

.slice-adjust-doc-card--empty {
  min-height: 86px;
  place-items: center;
  color: #94a3b8;
  background: #f8fafc;
  border-style: dashed;
  box-shadow: none;
}

.slice-adjust-doc-card--empty::before {
  background: #cbd5e1;
}

.slice-adjust-doc-card--empty svg {
  font-size: 18px;
}

.slice-adjust-doc-card--empty span {
  font-size: 12px;
  font-weight: 800;
}

.slice-adjust-doc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.slice-adjust-doc-head span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-adjust-doc-card b {
  overflow: hidden;
  color: #0f172a;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 14px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-adjust-trace-table {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:global(.slice-adjust-trace-table .ant-spin-nested-loading),
:global(.slice-adjust-trace-table .ant-spin-container) {
  height: 100%;
  min-height: 0;
}

:global(.slice-adjust-trace-table .ant-spin-container) {
  display: flex;
  flex-direction: column;
}

:global(.slice-adjust-trace-table .ant-table) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #ffffff;
}

:global(.slice-adjust-trace-table .ant-table-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

:global(.slice-adjust-trace-table .ant-table-cell) {
  padding-top: 6px !important;
  padding-bottom: 6px !important;
}

:global(.slice-adjust-trace-table .ant-table-body) {
  flex: 1;
  height: 100%;
  min-height: 0;
  max-height: none !important;
  background: #ffffff;
}

:global(.slice-adjust-trace-table .ant-table-body > table),
:global(.slice-adjust-trace-table .ant-table-tbody) {
  height: 100%;
}

:global(.slice-adjust-trace-table .ant-table-placeholder) {
  height: 100%;
}

:global(.slice-adjust-trace-table .ant-table-placeholder > td) {
  height: 100%;
  padding: 0 !important;
}

:global(.slice-adjust-trace-table .ant-empty-normal) {
  display: flex;
  height: 100%;
  min-height: 280px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 0;
}

.slice-adjust-doc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.slice-adjust-doc-merge-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-height: 48px;
  overflow: auto;
}

.slice-adjust-doc-merge-list span {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: 22px;
  gap: 4px;
  padding: 2px 6px;
  color: #334155;
  font-size: 12px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 4px;
}

.slice-adjust-doc-merge-list b {
  overflow: hidden;
  max-width: 76px;
  color: #0f172a;
  font-family: inherit;
  font-size: 12px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-adjust-doc-merge-list em,
.slice-adjust-doc-merge-list i {
  color: #64748b;
  font-style: normal;
  white-space: nowrap;
}

.slice-adjust-doc-merge-list i {
  color: #075985;
}

.slice-adjust-empty-cell {
  color: #94a3b8;
}

.slice-adjust-audit-toolbar {
  justify-content: flex-start;
  flex: 0 0 auto;
}

:global(.slice-adjust-audit-toolbar .ant-input-affix-wrapper) {
  max-width: 360px;
}

.slice-adjust-audit-table {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:global(.slice-adjust-audit-table .ant-spin-nested-loading),
:global(.slice-adjust-audit-table .ant-spin-container) {
  height: 100%;
  min-height: 0;
}

:global(.slice-adjust-audit-table .ant-spin-container) {
  display: flex;
  flex-direction: column;
}

:global(.slice-adjust-audit-table .ant-table) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:global(.slice-adjust-audit-table .ant-pagination) {
  flex: 0 0 auto;
  margin: 10px 0 0 !important;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
}

:global(.slice-adjust-audit-table .ant-table-cell) {
  padding-top: 7px !important;
  padding-bottom: 7px !important;
}

.slice-adjust-audit-pair {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  font-family: Consolas, 'Microsoft YaHei', monospace;
}

.slice-adjust-audit-pair b {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.slice-adjust-audit-arrow {
  margin: 0 6px;
  color: #64748b;
}

:global(.slice-adjust-candidate-modal .ant-modal-body) {
  display: flex;
  flex-direction: column;
}

:global(.slice-adjust-candidate-modal .ant-table-wrapper) {
  min-height: 0;
}

:global(.slice-adjust-candidate-modal .ant-table-row) {
  cursor: pointer;
}

:global(.slice-adjust-candidate-modal .ant-table-row:hover td) {
  background: #e0f2fe;
}

@keyframes slice-adjust-swap-pulse {
  0% {
    transform: scale(1) rotate(0deg);
  }
  45% {
    transform: scale(1.22) rotate(180deg);
  }
  100% {
    transform: scale(1) rotate(360deg);
  }
}

@keyframes slice-adjust-swap-line-left {
  0% {
    opacity: 0;
    transform: translateX(-28px);
  }
  35%,
  65% {
    opacity: 0.7;
  }
  100% {
    opacity: 0;
    transform: translateX(28px);
  }
}

@keyframes slice-adjust-swap-line-right {
  0% {
    opacity: 0;
    transform: translateX(28px);
  }
  35%,
  65% {
    opacity: 0.7;
  }
  100% {
    opacity: 0;
    transform: translateX(-28px);
  }
}

@media (max-width: 1100px) {
  .slice-adjust-body,
  .slice-adjust-workbench {
    grid-template-columns: 1fr;
  }

  .slice-adjust-swap-mark {
    min-height: 44px;
  }
}
</style>
