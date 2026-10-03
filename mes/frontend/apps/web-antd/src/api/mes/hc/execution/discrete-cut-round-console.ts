import type { MesHcCutRoundConsoleApi } from './cut-round-console';

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
  mapDiscreteSourceToConsoleSource,
  mapDiscreteTaskToConsoleTask,
} from './discrete-post-process-report-adapter';

export * from './cut-round-console';

let currentPlanOperationId: number | undefined;

export function getCutRoundConsoleTaskList(params: MesHcCutRoundConsoleApi.TaskQuery) {
  return getDiscretePostProcessTaskList({
    keyword: params.taskKeyword || params.productKeyword || undefined,
    opCode: 'WC-CUT',
    taskStatus: params.taskStatus,
  }).then((rows) => rows.map((row) => mapDiscreteTaskToConsoleTask(row, '裁切') as MesHcCutRoundConsoleApi.TaskItem));
}

export function getCutRoundConsoleSourceList(_planId: number, planOperationId: number, _sourceBatchNo?: string) {
  currentPlanOperationId = planOperationId;
  return getDiscretePostProcessSourceList({
    planOperationId,
    taskStatus: 'ALL',
  }).then((rows) => rows.map((row) => mapDiscreteSourceToConsoleSource(row, '裁切') as MesHcCutRoundConsoleApi.SourceItem));
}

export function scanCutRoundConsoleSource(batchNo: string) {
  const scanByOp = () => scanDiscretePostProcessSourceByOp({
    pieceNo: batchNo,
    targetOpCode: 'WC-CUT',
  });
  const request = currentPlanOperationId
    ? scanDiscretePostProcessSource({
        pieceNo: batchNo,
        planOperationId: Number(currentPlanOperationId),
      }).catch(scanByOp)
    : scanByOp();
  return request.then((row) => {
    currentPlanOperationId = row.planOperationId || currentPlanOperationId;
    return mapDiscreteSourceToConsoleSource(row, '裁切') as MesHcCutRoundConsoleApi.SourceItem;
  });
}

export function saveCutRoundConsoleReport(data: MesHcCutRoundConsoleApi.SaveReportReq) {
  return confirmDiscretePostProcessReport(buildDiscreteReportReq(data)).then((row) => row.consumeReportId || row.lockId);
}

export function saveAndConfirmCutRoundConsoleReport(
  data: MesHcCutRoundConsoleApi.SaveAndConfirmReportReq,
) {
  return confirmDiscretePostProcessReport(buildDiscreteReportReq(data)).then((row) => row.consumeReportId || row.lockId);
}

export async function getCutRoundInspectionTasks(planOperationId: number) {
  const rows = await getDiscretePostProcessInspectionTaskList({
    inspectionType: 'FQC',
    planOperationId,
  });
  return rows.map((row, index) => mapDiscreteCutRoundInspectionTask(row, index));
}

export async function createCutRoundInspectionTask(data: MesHcCutRoundConsoleApi.CreateCutRoundInspectionTaskReq) {
  const sourceRows = await getDiscretePostProcessSourceList({
    planOperationId: data.planOperationId,
    taskStatus: 'ALL',
  });
  const reportIds = new Set((data.reportIds || []).map((id) => Number(id)).filter(Boolean));
  const targetRows = sourceRows.filter((row) => reportIds.has(Number(row.consumeReportId || 0)));
  if (!targetRows.length) {
    throw new Error('未找到可送检的离散裁切报工片号');
  }
  const tasks = [];
  for (const row of targetRows) {
    tasks.push(await createDiscretePostProcessInspectionTask({
      inspectionType: 'FQC',
      lockId: row.lockId,
      remark: data.remark || '离散裁切产品报检',
    }));
  }
  return tasks[0]?.id || 0;
}

function mapDiscreteCutRoundInspectionTask(
  row: Awaited<ReturnType<typeof getDiscretePostProcessInspectionTaskList>>[number],
  index: number,
): MesHcCutRoundConsoleApi.CutRoundInspectionTask {
  const pieceNo = row.pieceNo || row.batchNo || row.sourceBatchNo || '';
  const taskStatus = normalizeCutRoundInspectionStatus(row.inspectionStatus);
  const detail: MesHcCutRoundConsoleApi.CutRoundInspectionTaskDetail = {
    cutRoundReportId: row.consumeReportId,
    fqcJudgment: normalizeCutRoundInspectionJudgment(row.inspectionResult),
    fqcNo: row.inspectionTaskNo,
    fqcStatus: taskStatus,
    id: row.inspectionTaskId,
    inspectionResult: row.inspectionResult || 'PENDING',
    inspectionTime: row.inspectionTime,
    materialCode: row.materialCode,
    materialName: row.materialName,
    modelCode: row.modelNo,
    parentProductionBatchNo: row.sourceParentBatchNo || row.sourceBatchNo,
    productionBatchNo: pieceNo,
    remark: row.remark,
    seqNo: index + 1,
    sizeRule: row.sizeSpec,
    taskId: row.inspectionTaskId,
  };
  return {
    detailCount: 1,
    details: [detail],
    fqcJudgment: detail.fqcJudgment,
    fqcNo: row.inspectionTaskNo,
    fqcStatus: taskStatus,
    id: row.inspectionTaskId,
    operationCode: row.opCode,
    operationName: row.opName || '裁切',
    planId: row.planId,
    planNo: row.planNo,
    planOperationId: row.planOperationId,
    remark: row.remark,
    reportDate: row.inspectionReportTime ? String(row.inspectionReportTime).slice(0, 10) : undefined,
    reportProcess: row.opName || '裁切',
    reportTime: row.inspectionReportTime,
    reporterName: row.inspectionReporterName,
    taskNo: row.inspectionTaskNo,
    taskStatus,
  };
}

function normalizeCutRoundInspectionStatus(status?: string) {
  const normalized = String(status || '').trim().toUpperCase();
  if (['WAIT_INSPECTION', 'WAITING_QA', 'WAIT_QA'].includes(normalized)) return 'PENDING';
  if (['QUALIFIED', 'OK', 'PASSED'].includes(normalized)) return 'COMPLETED';
  if (['INSPECTING', 'IN_PROGRESS', 'PROCESSING'].includes(normalized)) return 'INSPECTING';
  if (['NG', 'FAILED', 'REJECTED'].includes(normalized)) return 'REJECTED';
  return normalized || 'PENDING';
}

function normalizeCutRoundInspectionJudgment(result?: string) {
  const normalized = String(result || '').trim().toUpperCase();
  if (['OK', 'PASS', 'PASSED', 'QUALIFIED'].includes(normalized)) return 'OK';
  if (['NG', 'FAIL', 'FAILED', 'REJECTED'].includes(normalized)) return 'NG';
  return 'PENDING';
}
