import { requestClient } from '#/api/request';

const BASE_URL = '/mes/hc/execution/discrete-post-process';

export namespace MesHcDiscretePostProcessApi {
  export type OperationCode = 'WC-ADH2' | 'WC-CUT' | 'WC-GROOVE' | string;
  export type TaskStatus = 'ALL' | 'COMPLETED' | 'FINISHED' | 'PENDING' | 'UNFINISHED' | string;

  export interface CandidateQuery {
    keyword?: string;
    limit?: number;
    locationKeyword?: string;
    materialCode?: string;
    modelNo?: string;
    ngStatuses?: string[];
    qualityStatus?: string;
    reportDate?: string;
    sourceOpCode?: string;
    sourcePool?: 'ALL' | 'NG' | 'WIP' | string;
    sourceTypes?: string[];
    specSize?: string;
    targetOpCode?: OperationCode;
  }

  export interface CandidateItem {
    availableQty?: number;
    batchNo?: string;
    bizStatus?: string;
    candidateKey?: string;
    candidateStatus?: string;
    candidateType?: 'LOCK' | 'STOCK' | string;
    expiryDate?: string;
    frozenQty?: number;
    lastTxnTime?: string;
    locationCode?: string;
    locationName?: string;
    materialCode?: string;
    materialId?: number;
    materialName?: string;
    modelNo?: string;
    ngPieceId?: number;
    ngStatus?: string;
    ngStatusText?: string;
    onHandQty?: number;
    opCode?: string;
    opName?: string;
    opSeq?: number;
    planLockedQty?: number;
    productionDate?: string;
    qualityStatus?: string;
    recipeCode?: string;
    recipeName?: string;
    reportTime?: string;
    segmentCode?: string;
    segmentName?: string;
    shareableQty?: number;
    sourceBatchNo?: string;
    sourceId?: number;
    sourceLockId?: number;
    sourceLockOperationId?: number;
    sourceLockPlanId?: number;
    sourceLockPlanNo?: string;
    sourceParentBatchNo?: string;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    sourceReportId?: number;
    sourceTable?: string;
    sourceType?: string;
    specSize?: string;
    stockId?: number;
    stockType?: string;
    thickness?: number;
    uom?: string;
  }

  export interface CreatePlanReq {
    operationCodes: OperationCode[];
    executionRequirement?: string;
    ngPieceIds?: number[];
    planModel?: string;
    releaseNow?: boolean;
    remark?: string;
    sourceLockIds?: number[];
    stockIds?: number[];
  }

  export interface TaskQuery {
    keyword?: string;
    opCode: OperationCode;
    taskStatus?: TaskStatus;
  }

  export interface TaskItem {
    finishedCount?: number;
    firstSourceBatchNo?: string;
    lastReportTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    opCode?: string;
    opName?: string;
    opSeq?: number;
    operationStatus?: string;
    pendingCount?: number;
    planId: number;
    planMode?: string;
    planNo?: string;
    planOperationId: number;
    planStatus?: string;
    releasedCount?: number;
    requiredQty?: number;
    sizeSpec?: string;
    sourceCount?: number;
    sourcePlanNos?: string;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
  }

  export interface SourceItem {
    batchNo?: string;
    consumeReportId?: number;
    consumeTime?: string;
    consumeTxnNo?: string;
    consumedQty?: number;
    inspectionResult?: string;
    inspectionReportTime?: string;
    inspectionReporterName?: string;
    inspectionStatus?: string;
    inspectionTaskId?: number;
    inspectionTaskNo?: string;
    inspectionTime?: string;
    inspectionType?: string;
    lockId: number;
    lockQty?: number;
    lockStatus?: string;
    materialCode?: string;
    materialId?: number;
    materialName?: string;
    modelNo?: string;
    opCode?: string;
    opName?: string;
    outputStockBatchNo?: string;
    outputStockId?: number;
    outputStockPostStatus?: string;
    pieceNo?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    recipeCode?: string;
    releasedQty?: number;
    remainingQty?: number;
    remark?: string;
    sizeSpec?: string;
    sourceBatchNo?: string;
    sourceId?: number;
    sourceParentBatchNo?: string;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    sourceTable?: string;
    sourceType?: string;
    sourceReportId?: number;
    stockId?: number;
    ngPieceId?: number;
    ngStatus?: string;
    ngStatusText?: string;
    currentWarehouseCode?: string;
    currentWarehouseName?: string;
    currentLocationCode?: string;
    currentLocationName?: string;
    stockType?: string;
    targetOpCode?: string;
    targetOpName?: string;
  }

  export interface ReportReq {
    actualSizeRule?: string;
    actualSizeSuffix?: string;
    confirmerName?: string;
    defectCode?: string;
    inputQty?: number;
    lockId?: number;
    lossQty?: number;
    outputQty?: number;
    pieceNo?: string;
    planOperationId: number;
    recorderName?: string;
    remark?: string;
    reportResult?: 'NG' | 'OK' | string;
    selfCheck?: 'NG' | 'OK' | string;
  }

  export interface InspectionReq {
    inspectionType?: string;
    lockId: number;
    remark?: string;
  }

  export interface InspectionResp {
    id?: number;
    inspectionStatus?: string;
    inspectionTaskNo?: string;
  }
}

export function getDiscretePostProcessCandidates(params: MesHcDiscretePostProcessApi.CandidateQuery) {
  return requestClient.get<MesHcDiscretePostProcessApi.CandidateItem[]>(`${BASE_URL}/candidate/list`, { params });
}

export function createDiscretePostProcessPlan(data: MesHcDiscretePostProcessApi.CreatePlanReq) {
  return requestClient.post<number>(`${BASE_URL}/plan/create`, data);
}

export function getDiscretePostProcessTaskList(params: MesHcDiscretePostProcessApi.TaskQuery) {
  return requestClient.get<MesHcDiscretePostProcessApi.TaskItem[]>(`${BASE_URL}/task-list`, { params });
}

export function getDiscretePostProcessSourceList(params: {
  keyword?: string;
  planOperationId: number;
  taskStatus?: MesHcDiscretePostProcessApi.TaskStatus;
}) {
  return requestClient.get<MesHcDiscretePostProcessApi.SourceItem[]>(`${BASE_URL}/source/list`, { params });
}

export function getDiscretePostProcessInspectionTaskList(params: {
  inspectionType?: string;
  keyword?: string;
  planOperationId: number;
}) {
  return requestClient.get<MesHcDiscretePostProcessApi.SourceItem[]>(`${BASE_URL}/inspection-task/list`, { params });
}

export function scanDiscretePostProcessSource(params: { pieceNo: string; planOperationId: number }) {
  return requestClient.get<MesHcDiscretePostProcessApi.SourceItem>(`${BASE_URL}/source/scan`, { params });
}

export function scanDiscretePostProcessSourceByOp(params: {
  pieceNo: string;
  targetOpCode: MesHcDiscretePostProcessApi.OperationCode;
}) {
  return requestClient.get<MesHcDiscretePostProcessApi.SourceItem>(`${BASE_URL}/source/scan-by-op`, { params });
}

export function confirmDiscretePostProcessReport(data: MesHcDiscretePostProcessApi.ReportReq) {
  return requestClient.post<MesHcDiscretePostProcessApi.SourceItem>(`${BASE_URL}/report/confirm`, data);
}

export function createDiscretePostProcessInspectionTask(data: MesHcDiscretePostProcessApi.InspectionReq) {
  return requestClient.post<MesHcDiscretePostProcessApi.InspectionResp>(`${BASE_URL}/inspection-task/create`, data);
}
