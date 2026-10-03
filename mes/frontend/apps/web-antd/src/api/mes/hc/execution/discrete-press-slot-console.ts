import type { MesHcAdhesiveConsoleApi } from './press-slot-console';

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

export * from './press-slot-console';

let currentPlanOperationId: number | undefined;

export function getAdhesiveConsoleTaskList(params: MesHcAdhesiveConsoleApi.TaskQuery) {
  return getDiscretePostProcessTaskList({
    keyword: params.taskKeyword || params.productKeyword || undefined,
    opCode: 'WC-GROOVE',
    taskStatus: params.taskStatus,
  }).then((rows) => rows.map((row) => mapDiscreteTaskToConsoleTask(row, '压槽') as MesHcAdhesiveConsoleApi.TaskItem));
}

export function getAdhesiveConsoleSourceList(_planId: number, planOperationId: number) {
  currentPlanOperationId = planOperationId;
  return getDiscretePostProcessSourceList({
    planOperationId,
    taskStatus: 'ALL',
  }).then((rows) => rows.map((row) => mapDiscreteSourceToConsoleSource(row, '压槽') as MesHcAdhesiveConsoleApi.SourceItem));
}

export function scanAdhesiveConsoleSource(batchNo: string, _includeAbnormal = false) {
  const scanByOp = () => scanDiscretePostProcessSourceByOp({
    pieceNo: batchNo,
    targetOpCode: 'WC-GROOVE',
  });
  const request = currentPlanOperationId
    ? scanDiscretePostProcessSource({
        pieceNo: batchNo,
        planOperationId: Number(currentPlanOperationId),
      }).catch(scanByOp)
    : scanByOp();
  return request.then((row) => {
    currentPlanOperationId = row.planOperationId || currentPlanOperationId;
    return mapDiscreteSourceToConsoleSource(row, '压槽') as MesHcAdhesiveConsoleApi.SourceItem;
  });
}

export function saveAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.SaveReportReq) {
  return confirmDiscretePostProcessReport(buildDiscreteReportReq(data)).then((row) => row.consumeReportId || row.lockId);
}

export function saveAndConfirmPressSlotReport(
  data: MesHcAdhesiveConsoleApi.SaveReportReq & Omit<MesHcAdhesiveConsoleApi.ConfirmReportReq, 'id'>,
) {
  return confirmDiscretePostProcessReport(buildDiscreteReportReq(data)).then((row) => row.consumeReportId || row.lockId);
}

export async function getPressSlotFaiSummary(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  const rows = await getPressSlotFaiList(params);
  return rows[0] || ({ allowReportSubmit: false, displayText: '待送检', faiJudgment: 'PENDING', faiStatus: 'PENDING', records: rows } as MesHcAdhesiveConsoleApi.FaiSummary);
}

export async function getPressSlotFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  const rows = await getDiscretePostProcessInspectionTaskList({ planOperationId: params.planOperationId });
  return sortFaiRows(
    rows
      .filter((row) => !isPressSlotProcessInspectionType(row.inspectionType))
      .map((row) => mapDiscreteInspectionToFaiSummary(row, {
        defaultInspectionType: 'FIRST_INSPECTION',
        planId: params.planId,
        planOperationId: params.planOperationId,
      }) as MesHcAdhesiveConsoleApi.FaiSummary),
  );
}

export async function getPressSlotProcessCheckFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  const rows = await getDiscretePostProcessInspectionTaskList({ planOperationId: params.planOperationId });
  return sortFaiRows(
    rows
      .filter((row) => isPressSlotProcessInspectionType(row.inspectionType))
      .map((row) => mapDiscreteInspectionToFaiSummary(row, {
        defaultInspectionType: row.inspectionType || 'PROCESS_CHECK',
        inspectionScene: normalizePressSlotInspectionScene(row.inspectionType),
        planId: params.planId,
        planOperationId: params.planOperationId,
      }) as MesHcAdhesiveConsoleApi.FaiSummary),
  );
}

export async function applyPressSlotFai(data: MesHcAdhesiveConsoleApi.FaiApplyReq) {
  const inspectionType = resolvePressSlotInspectionType(data);
  const pieceNo = String(data.productBatchNo || data.sourceReportNo || '').trim();
  if (!pieceNo) {
    throw new Error('送检片号不能为空');
  }
  const source = await scanDiscretePostProcessSource({
    pieceNo,
    planOperationId: Number(data.planOperationId),
  });
  const task = await createDiscretePostProcessInspectionTask({
    inspectionType,
    lockId: source.lockId,
    remark: data.remark,
  });
  return mapDiscreteInspectionToFaiSummary(
    {
      ...source,
      inspectionStatus: task.inspectionStatus,
      inspectionTaskId: task.id,
      inspectionTaskNo: task.inspectionTaskNo,
      inspectionType,
    },
    {
      inspectionScene: data.inspectionScene,
      planId: data.planId,
      planOperationId: data.planOperationId,
      remark: data.remark,
      sourceReportId: data.sourceReportId,
      sourceReportNo: data.sourceReportNo,
    },
  ) as MesHcAdhesiveConsoleApi.FaiSummary;
}

export async function validatePressSlotFaiScan(params: {
  motherBatchNo?: string;
  planId: number;
  planOperationId: number;
  productionBatchNo?: string;
  reportType?: string;
}) {
  const pieceNo = String(params.productionBatchNo || '').trim();
  if (!pieceNo) {
    return {
      allowScan: false,
      warningMessage: '请扫描当前离散压槽计划下的片号',
    } as MesHcAdhesiveConsoleApi.PressSlotScanGateResp;
  }
  try {
    await scanDiscretePostProcessSource({
      pieceNo,
      planOperationId: Number(params.planOperationId),
    });
    return {
      allowScan: true,
    } as MesHcAdhesiveConsoleApi.PressSlotScanGateResp;
  } catch (error) {
    const message = error instanceof Error ? error.message : '';
    return {
      allowScan: false,
      warningMessage: message || `当前离散压槽计划下未找到片号：${pieceNo}`,
    } as MesHcAdhesiveConsoleApi.PressSlotScanGateResp;
  }
}

export function getPressSlotActiveAbnormalLock(_params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return Promise.resolve(null as MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord | null);
}

export function getPressSlotAbnormalLockList(_params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return Promise.resolve([] as MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord[]);
}

function resolvePressSlotInspectionType(data: MesHcAdhesiveConsoleApi.FaiApplyReq) {
  const scene = String(data.inspectionScene || '').trim().toUpperCase();
  if (scene === 'ABNORMAL_RELEASE') return 'ABNORMAL_RELEASE';
  if (scene === 'PROCESS_CHECK') return 'PROCESS_CHECK';
  return 'FIRST_INSPECTION';
}

function isPressSlotProcessInspectionType(inspectionType?: string) {
  const normalized = String(inspectionType || '').trim().toUpperCase();
  return ['ABNORMAL_RELEASE', 'PROCESS_CHECK'].includes(normalized);
}

function normalizePressSlotInspectionScene(inspectionType?: string) {
  const normalized = String(inspectionType || '').trim().toUpperCase();
  if (normalized === 'ABNORMAL_RELEASE') return 'ABNORMAL_RELEASE';
  if (normalized === 'PROCESS_CHECK') return 'PROCESS_CHECK';
  return undefined;
}

function sortFaiRows<T extends MesHcAdhesiveConsoleApi.FaiSummary>(rows: T[]) {
  return rows.sort((a, b) => {
    const timeDiff = new Date(String(b.faiApplyTime || 0)).getTime() - new Date(String(a.faiApplyTime || 0)).getTime();
    if (Number.isFinite(timeDiff) && timeDiff !== 0) return timeDiff;
    return Number(b.faiId || 0) - Number(a.faiId || 0);
  });
}
