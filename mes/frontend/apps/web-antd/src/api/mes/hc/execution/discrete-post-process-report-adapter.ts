import type { MesHcDiscretePostProcessApi } from './discrete-post-process';

type AnyRecord = Record<string, any>;

export type DiscreteOperationCode = 'WC-ADH2' | 'WC-CUT' | 'WC-GROOVE';

export function normalizeDiscreteTaskStatus(status?: string) {
  const normalized = String(status || '').toUpperCase();
  if (['COMPLETED', 'CONSUMED', 'FINISHED'].includes(normalized)) return 'FINISHED';
  if (['IN_PROGRESS', 'PROCESSING', 'RUNNING', 'STARTED'].includes(normalized)) return 'RUNNING';
  if (['CANCELLED', 'CANCELED'].includes(normalized)) return 'CANCELLED';
  if (['PAUSED', 'SUSPENDED'].includes(normalized)) return 'PAUSED';
  return 'PENDING';
}

export function mapDiscreteTaskToConsoleTask(
  row: MesHcDiscretePostProcessApi.TaskItem,
  fallbackOpName: string,
) {
  const pendingCount = Number(row.pendingCount || 0);
  const sourceCount = Number(row.sourceCount || row.requiredQty || 0);
  const finishedCount = Number(row.finishedCount || 0);
  const status = pendingCount > 0
    ? normalizeDiscreteTaskStatus(row.operationStatus || row.planStatus)
    : 'FINISHED';

  return {
    availableSourceLength: pendingCount || sourceCount,
    batchNo: row.firstSourceBatchNo || row.planNo,
    confirmedSourceCount: finishedCount,
    equipmentCode: row.equipmentCode,
    equipmentId: row.equipmentId,
    equipmentName: row.equipmentName,
    id: `DISCRETE-${row.planOperationId}`,
    materialCode: row.materialCode,
    modelCode: row.modelCode,
    parentProductionBatchNo: row.firstSourceBatchNo || row.planNo,
    planId: row.planId,
    planNo: row.planNo,
    planOperationId: row.planOperationId,
    process: row.opName || fallbackOpName,
    productionBatchNo: row.firstSourceBatchNo || row.planNo,
    reportMode: 'DISCRETE_POST_PROCESS',
    requirements: `离散后加工：共 ${sourceCount} 片，待报 ${pendingCount} 片，已报 ${finishedCount} 片`,
    sourceBatchNo: row.firstSourceBatchNo,
    sourceProductionBatchNo: row.firstSourceBatchNo,
    spec: row.sizeSpec,
    status,
    workCenterCode: row.workCenterCode,
    workCenterId: row.workCenterId,
    workCenterName: row.workCenterName || row.opName || fallbackOpName,
  };
}

export function mapDiscreteSourceToConsoleSource(
  row: MesHcDiscretePostProcessApi.SourceItem,
  fallbackOpName: string,
) {
  const pieceNo = row.pieceNo || row.batchNo || row.sourceBatchNo || '';
  const parentBatchNo = row.sourceParentBatchNo || row.sourceBatchNo || pieceNo;
  const remainingQty = Number(row.remainingQty ?? row.lockQty ?? 1);
  const consumed = String(row.lockStatus || '').toUpperCase() === 'CONSUMED';

  return {
    actualSizeRule: row.sizeSpec,
    actualSizeSuffix: undefined,
    confirmStatus: consumed ? 'CONFIRMED' : 'PENDING',
    confirmedBatchNo: consumed ? pieceNo : undefined,
    downstreamStatus: consumed ? '已报工' : '未报工',
    discretePostProcess: true,
    extraJson: JSON.stringify({
      reportMode: 'DISCRETE_POST_PROCESS',
      sourceLockId: row.lockId,
      sourcePlanNo: row.sourcePlanNo,
      sourceParentBatchNo: row.sourceParentBatchNo,
    }),
    grindingPlanId: row.planId,
    grindingPlanNo: row.planNo,
    grindingPlanOperationId: row.planOperationId,
    grindingSecondDetailId: row.lockId,
    motherBatchNo: parentBatchNo,
    modelCode: row.modelNo,
    operationCode: row.opCode,
    operationName: row.opName,
    outputLength: remainingQty,
    parentProductionBatchNo: parentBatchNo,
    processLength: remainingQty,
    processName: row.opName || fallbackOpName,
    productionBatchNo: pieceNo,
    productQualityStatus: row.inspectionResult || '合格',
    reportProcess: row.opName || fallbackOpName,
    rowUid: `DISCRETE-LOCK-${row.lockId}`,
    selfCheck: row.inspectionResult,
    sourceBatchNo: row.sourceBatchNo || pieceNo,
    sourceOriginalPlanNo: row.sourcePlanNo,
    sourceMenuCode: 'DISCRETE_POST_PROCESS',
    sourceProductionBatchNo: pieceNo,
  };
}

export function mapDiscreteInspectionToFaiSummary(
  row: MesHcDiscretePostProcessApi.SourceItem,
  options: {
    defaultInspectionType?: string;
    inspectionScene?: string;
    planId?: number;
    planOperationId?: number;
    remark?: string;
    sourceReportId?: number;
    sourceReportNo?: string;
  } = {},
) {
  const status = normalizeDiscreteInspectionStatus(row.inspectionStatus);
  const judgment = normalizeDiscreteInspectionJudgment(row.inspectionResult);
  const productBatchNo = row.pieceNo || row.batchNo || row.sourceBatchNo || '';
  const inspectionType = row.inspectionType || options.defaultInspectionType || options.inspectionScene || 'PROCESS';
  const applyTime = row.inspectionReportTime || row.inspectionTime || row.consumeTime || '';
  return {
    allowReportSubmit: Boolean(row.inspectionTaskId),
    displayText: resolveDiscreteInspectionDisplayText(status, judgment),
    faiApplyTime: applyTime,
    faiId: row.inspectionTaskId,
    faiJudgment: judgment,
    faiNo: row.inspectionTaskNo,
    faiStatus: status,
    inspectionDate: applyTime ? String(applyTime).slice(0, 10) : undefined,
    inspectionScene: options.inspectionScene || normalizeDiscreteInspectionScene(inspectionType),
    inspectionScopeBatchNo: row.sourceParentBatchNo || row.sourceBatchNo || '多批号加工',
    materialCode: row.materialCode,
    materialName: row.materialName,
    productBatchNo,
    productModel: row.modelNo,
    remark: options.remark || row.remark,
    sourceReportId: options.sourceReportId || row.consumeReportId,
    sourceReportNo: options.sourceReportNo || productBatchNo,
    specification: row.sizeSpec,
    todayInspected: Boolean(row.inspectionTaskId),
  };
}

export function buildDiscreteReportReq(data: AnyRecord) {
  const inputQty = toNumber(data.inputLength ?? data.processLength ?? data.sourceLockQty ?? 1);
  const selfCheck = String(data.selfCheck || data.reportResult || 'OK').toUpperCase();
  const outputQty = selfCheck === 'NG'
    ? 0
    : toNumber(data.outputLength ?? Math.max(inputQty - toNumber(data.lossLength), 0));

  return {
    actualSizeRule: data.actualSizeRule,
    actualSizeSuffix: data.actualSizeSuffix,
    confirmerName: data.confirmerName,
    defectCode: data.defectCode,
    inputQty,
    lockId: toNumber(data.sourceGrindingSecondDetailId || data.sourcePressSlotReportId || data.sourcePlanLockId),
    lossQty: toNumber(data.lossLength),
    outputQty,
    pieceNo: data.sourceProductionBatchNo || data.productionBatchNo || data.scannedBatchNo,
    planOperationId: toNumber(data.planOperationId),
    recorderName: data.recorderName,
    remark: data.remark,
    reportResult: selfCheck === 'NG' ? 'NG' : 'OK',
    selfCheck: selfCheck === 'NG' ? 'NG' : 'OK',
  };
}

function normalizeDiscreteInspectionScene(inspectionType?: string) {
  const normalized = String(inspectionType || '').trim().toUpperCase();
  if (normalized.includes('PROCESS_CHECK')) return 'PROCESS_CHECK';
  if (normalized.includes('ABNORMAL_RELEASE')) return 'ABNORMAL_RELEASE';
  if (normalized.includes('COA')) return 'COA';
  return undefined;
}

function normalizeDiscreteInspectionStatus(status?: string) {
  const normalized = String(status || '').trim().toUpperCase();
  if (['WAIT_INSPECTION', 'WAITING_QA', 'WAIT_QA', 'PENDING'].includes(normalized)) return 'PENDING';
  if (['QUALIFIED', 'COMPLETED', 'OK'].includes(normalized)) return 'COMPLETED';
  if (['INSPECTING', 'IN_PROGRESS', 'PROCESSING'].includes(normalized)) return 'INSPECTING';
  if (['CANCELLED', 'CANCELED'].includes(normalized)) return 'CANCELED';
  if (['REJECTED', 'NG', 'ABNORMAL'].includes(normalized)) return 'REJECTED';
  return normalized || 'PENDING';
}

function normalizeDiscreteInspectionJudgment(result?: string) {
  const normalized = String(result || '').trim().toUpperCase();
  if (['OK', 'QUALIFIED', '合格'].includes(normalized)) return 'OK';
  if (['NG', 'ABNORMAL', 'REJECTED', '不合格'].includes(normalized)) return 'NG';
  return 'PENDING';
}

function resolveDiscreteInspectionDisplayText(status?: string, judgment?: string) {
  if (status === 'COMPLETED' && judgment === 'OK') return '已完成';
  if (status === 'REJECTED' || judgment === 'NG') return '已驳回';
  if (status === 'INSPECTING') return '检测中';
  if (status === 'CANCELED') return '已取消';
  return '待检测';
}

function toNumber(value: unknown) {
  const parsed = Number(value ?? 0);
  return Number.isFinite(parsed) ? parsed : 0;
}
