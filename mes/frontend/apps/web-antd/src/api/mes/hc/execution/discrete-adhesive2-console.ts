import type { MesHcAdhesiveConsoleApi } from './adhesive2-console';

import {
  getAdhesive2SourcePressSlotFaiList as getBaseAdhesive2SourcePressSlotFaiList,
  getAdhesive2SourcePressSlotProcessCheckFaiList as getBaseAdhesive2SourcePressSlotProcessCheckFaiList,
} from './adhesive2-console';
import {
  confirmDiscretePostProcessReport,
  createDiscretePostProcessInspectionTask,
  getDiscretePostProcessInspectionTaskList,
  getDiscretePostProcessSourceList,
  getDiscretePostProcessTaskList,
  scanDiscretePostProcessSource,
  scanDiscretePostProcessSourceByOp,
} from './discrete-post-process';
import {
  buildDiscreteReportReq,
  mapDiscreteInspectionToFaiSummary,
  mapDiscreteSourceToConsoleSource,
  mapDiscreteTaskToConsoleTask,
} from './discrete-post-process-report-adapter';

export * from './adhesive2-console';

let currentPlanOperationId: number | undefined;

export function getAdhesiveConsoleTaskList(params: MesHcAdhesiveConsoleApi.TaskQuery) {
  return getDiscretePostProcessTaskList({
    keyword: params.taskKeyword || params.productKeyword || undefined,
    opCode: 'WC-ADH2',
    taskStatus: params.taskStatus,
  }).then((rows) => rows.map((row) => mapDiscreteTaskToConsoleTask(row, '粘胶2') as MesHcAdhesiveConsoleApi.TaskItem));
}

export function getAdhesiveConsoleSourceList(_planId: number, planOperationId: number, _sourceBatchNo?: string) {
  currentPlanOperationId = planOperationId;
  return getDiscretePostProcessSourceList({
    planOperationId,
    taskStatus: 'ALL',
  }).then((rows) => rows.map((row) => mapDiscreteSourceToConsoleSource(row, '粘胶2') as MesHcAdhesiveConsoleApi.SourceItem));
}

export function scanAdhesiveConsoleSource(batchNo: string) {
  const scanByOp = () => scanDiscretePostProcessSourceByOp({
    pieceNo: batchNo,
    targetOpCode: 'WC-ADH2',
  });
  const request = currentPlanOperationId
    ? scanDiscretePostProcessSource({
        pieceNo: batchNo,
        planOperationId: Number(currentPlanOperationId),
      }).catch(scanByOp)
    : scanByOp();
  return request.then((row) => {
    currentPlanOperationId = row.planOperationId || currentPlanOperationId;
    return mapDiscreteSourceToConsoleSource(row, '粘胶2') as MesHcAdhesiveConsoleApi.SourceItem;
  });
}

export function saveAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.SaveReportReq) {
  return confirmDiscretePostProcessReport(buildDiscreteReportReq(data)).then((row) => row.consumeReportId || row.lockId);
}

export function saveAndConfirmAdhesiveConsoleReport(
  data: MesHcAdhesiveConsoleApi.SaveReportReq & Omit<MesHcAdhesiveConsoleApi.ConfirmReportReq, 'id'>,
) {
  return confirmDiscretePostProcessReport(buildDiscreteReportReq(data)).then((row) => row.consumeReportId || row.lockId);
}

export async function getAdhesive2FaiSummary(params: { planId: number; planOperationId: number }) {
  const rows = await getAdhesive2CoaFaiList(params);
  return rows[0] || ({ allowReportSubmit: false, displayText: '待送检', faiJudgment: 'PENDING', faiStatus: 'PENDING' } as MesHcAdhesiveConsoleApi.FaiSummary);
}

export async function getAdhesive2CoaFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return getDiscreteAdhesive2FaiRows(params.planOperationId, (type) => type === 'COA', 'COA');
}

export async function getAdhesive2ProcessCheckFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return getDiscreteAdhesive2FaiRows(params.planOperationId, (type) => type === 'PROCESS_CHECK', 'PROCESS_CHECK');
}

export async function getAdhesive2SourcePressSlotFaiList(params: MesHcAdhesiveConsoleApi.SourcePressSlotFaiQuery) {
  const rows = await getDiscretePressSlotSourceFaiRows(params.sourcePlanOperationId, (type) => !['ABNORMAL_RELEASE', 'PROCESS_CHECK'].includes(type), 'FIRST_INSPECTION');
  if (rows.length) return rows;
  return getBaseAdhesive2SourcePressSlotFaiList(params);
}

export async function getAdhesive2SourcePressSlotProcessCheckFaiList(params: MesHcAdhesiveConsoleApi.SourcePressSlotFaiQuery) {
  const rows = await getDiscretePressSlotSourceFaiRows(params.sourcePlanOperationId, (type) => ['ABNORMAL_RELEASE', 'PROCESS_CHECK'].includes(type), 'PROCESS_CHECK');
  if (rows.length) return rows;
  return getBaseAdhesive2SourcePressSlotProcessCheckFaiList(params);
}

export async function applyAdhesive2Fai(data: MesHcAdhesiveConsoleApi.FaiApplyReq) {
  const sourceReportNo = getOptionalText(data, 'sourceReportNo');
  return createAdhesive2InspectionFromPiece({
    inspectionScene: data.inspectionScene || 'COA',
    pieceNo: data.productBatchNo || sourceReportNo,
    planId: data.planId,
    planOperationId: data.planOperationId,
    remark: data.remark,
    sourceReportId: data.sourceReportId,
    sourceReportNo,
  });
}

export async function applyAdhesive2PostConfirmCoa(data: MesHcAdhesiveConsoleApi.ConfirmReportReq) {
  if (!currentPlanOperationId) {
    throw new Error('请先加载粘胶2离散计划后再送检');
  }
  const pieceNo = String(data.scannedBatchNo || '').trim();
  if (!pieceNo) {
    throw new Error('COA送检片号不能为空');
  }
  const rows = await getDiscretePostProcessSourceList({
    planOperationId: Number(currentPlanOperationId),
    taskStatus: 'ALL',
  });
  const matched = rows.find((row) => Number(row.consumeReportId || 0) === Number(data.id || 0))
    || rows.find((row) => isSameText(row.pieceNo || row.batchNo || row.sourceBatchNo, pieceNo));
  if (!matched) {
    throw new Error(`当前离散粘胶2计划下未找到片号：${pieceNo}`);
  }
  return createAdhesive2InspectionFromSource(matched, {
    inspectionScene: 'COA',
    pieceNo,
    planId: matched.planId,
    planOperationId: matched.planOperationId,
    remark: '粘胶2离散COA送检',
    sourceReportId: data.id,
    sourceReportNo: pieceNo,
  });
}

async function createAdhesive2InspectionFromPiece(options: {
  inspectionScene?: string;
  pieceNo?: string;
  planId?: number;
  planOperationId: number;
  remark?: string;
  sourceReportId?: number;
  sourceReportNo?: string;
}) {
  const pieceNo = String(options.pieceNo || '').trim();
  if (!pieceNo) {
    throw new Error('送检片号不能为空');
  }
  const source = await scanDiscretePostProcessSource({
    pieceNo,
    planOperationId: Number(options.planOperationId),
  });
  return createAdhesive2InspectionFromSource(source, { ...options, pieceNo });
}

async function createAdhesive2InspectionFromSource(
  source: Awaited<ReturnType<typeof scanDiscretePostProcessSource>>,
  options: {
    inspectionScene?: string;
    pieceNo?: string;
    planId?: number;
    planOperationId?: number;
    remark?: string;
    sourceReportId?: number;
    sourceReportNo?: string;
  },
) {
  const inspectionType = normalizeAdhesive2InspectionType(options.inspectionScene);
  const task = await createDiscretePostProcessInspectionTask({
    inspectionType,
    lockId: source.lockId,
    remark: options.remark,
  });
  return mapDiscreteInspectionToFaiSummary(
    {
      ...source,
      inspectionStatus: task.inspectionStatus,
      inspectionTaskId: task.id,
      inspectionTaskNo: task.inspectionTaskNo,
      inspectionType,
      pieceNo: options.pieceNo || source.pieceNo,
    },
    {
      defaultInspectionType: inspectionType,
      inspectionScene: options.inspectionScene || inspectionType,
      planId: options.planId,
      planOperationId: options.planOperationId,
      remark: options.remark,
      sourceReportId: options.sourceReportId,
      sourceReportNo: options.sourceReportNo || options.pieceNo,
    },
  ) as MesHcAdhesiveConsoleApi.FaiSummary;
}

async function getDiscreteAdhesive2FaiRows(
  planOperationId: number,
  predicate: (inspectionType: string) => boolean,
  defaultInspectionType: string,
) {
  const rows = await getDiscretePostProcessInspectionTaskList({ planOperationId });
  return sortFaiRows(
    rows
      .filter((row) => predicate(normalizeInspectionType(row.inspectionType || defaultInspectionType)))
      .map((row) => mapDiscreteInspectionToFaiSummary(row, {
        defaultInspectionType,
        inspectionScene: normalizeInspectionType(row.inspectionType || defaultInspectionType),
      }) as MesHcAdhesiveConsoleApi.FaiSummary),
  );
}

async function getDiscretePressSlotSourceFaiRows(
  planOperationId: number,
  predicate: (inspectionType: string) => boolean,
  defaultInspectionType: string,
) {
  if (!planOperationId) return [];
  const rows = await getDiscretePostProcessInspectionTaskList({ planOperationId });
  return sortFaiRows(
    rows
      .filter((row) => predicate(normalizeInspectionType(row.inspectionType || defaultInspectionType)))
      .map((row) => mapDiscreteInspectionToFaiSummary(row, {
        defaultInspectionType,
        inspectionScene: normalizeInspectionType(row.inspectionType || defaultInspectionType),
      }) as MesHcAdhesiveConsoleApi.FaiSummary),
  );
}

function normalizeAdhesive2InspectionType(scene?: string) {
  const normalized = normalizeInspectionType(scene || 'COA');
  if (normalized === 'PROCESS_CHECK') return 'PROCESS_CHECK';
  return 'COA';
}

function normalizeInspectionType(type?: string) {
  return String(type || '').trim().toUpperCase();
}

function isSameText(a?: string, b?: string) {
  return String(a || '').trim().toUpperCase() === String(b || '').trim().toUpperCase();
}

function getOptionalText(record: unknown, key: string) {
  if (!record || typeof record !== 'object') return undefined;
  const value = (record as Record<string, unknown>)[key];
  return value == null ? undefined : String(value);
}

function sortFaiRows<T extends MesHcAdhesiveConsoleApi.FaiSummary>(rows: T[]) {
  return rows.sort((a, b) => {
    const timeDiff = new Date(String(b.faiApplyTime || 0)).getTime() - new Date(String(a.faiApplyTime || 0)).getTime();
    if (Number.isFinite(timeDiff) && timeDiff !== 0) return timeDiff;
    return Number(b.faiId || 0) - Number(a.faiId || 0);
  });
}
