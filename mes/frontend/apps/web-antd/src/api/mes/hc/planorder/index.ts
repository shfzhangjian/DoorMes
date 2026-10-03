import type { PageParam, PageResult } from '@vben/request';
import type { MesHcInvStockApi } from '#/api/mes/hc/inv-stock';

import { requestClient } from '#/api/request';

const PROCESS_PIVOT_REQUEST_TIMEOUT = 60_000;

export namespace MesHcPlanOrderApi {
  export interface Operation {
    id?: number;
    opSeq?: number;
    opCode?: string;
    opName?: string;
    routeOperationId?: number;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    yieldRate?: number;
    requiredQty?: number;
    lockedQty?: number;
    dispatchQty?: number;
    unitId?: number;
    unitCode?: string;
    unitName?: string;
    uom?: string;
    instructionText?: string;
    hasLock?: boolean;
    operationStatus?: string;
    finishTime?: string;
    finishRemark?: string;
    splitMark?: string;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    sourceOperationCode?: string;
    sourceOperationName?: string;
    pauseScope?: string;
    pauseStartDate?: string;
    pauseEndDate?: string;
    pauseRemark?: string;
    cancelReason?: string;
    statusOperatorId?: number;
    statusOperatorName?: string;
    statusOperateTime?: string;
    statusDateMarksJson?: string;
    reportQtyByDate?: Record<string, number>;
    latestReportDate?: string;
    latestStartTime?: string;
    latestEndTime?: string;
    latestRemark?: string;
    latestRecorderName?: string;
    latestConfirmerName?: string;
    sort?: number;
    createTime?: string;
    updateTime?: string;
  }

  export interface InventoryLock {
    id?: number;
    planOperationId?: number;
    targetPlanNo?: string;
    targetOpCode?: string;
    targetOpName?: string;
    lockType?: string;
    stockId?: number;
    stockType?: string;
    sourceType?: string;
    sourceTable?: string;
    sourceId?: number;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    sourceBatchNo?: string;
    opSeq?: number;
    opCode?: string;
    opName?: string;
    segmentCode?: string;
    segmentName?: string;
    thickness?: number;
    lotNo?: string;
    batchNo?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    modelNo?: string;
    recipeCode?: string;
    sizeSpec?: string;
    productionDate?: string;
    expiryDate?: string;
    remainMonths?: number;
    locationId?: number;
    locationCode?: string;
    locationName?: string;
    ownerId?: number;
    ownerCode?: string;
    ownerName?: string;
    availableQty?: number;
    shareableQty?: number;
    lockQty?: number;
    consumedQty?: number;
    releasedQty?: number;
    remainingQty?: number;
    consumeReportId?: number;
    consumeTime?: string;
    releaseTime?: string;
    releaseReason?: string;
    lockTxnNo?: string;
    consumeTxnNo?: string;
    releaseTxnNo?: string;
    unitId?: number;
    unitCode?: string;
    unitName?: string;
    uom?: string;
    fifoRank?: number;
    lockStatus?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface PlanOrder {
    id?: number;
    planNo?: string;
    planDate?: string;
    planMode?: string;
    planStatus?: string;
    sourceType?: string;
    salesOrderId?: number;
    salesOrderNo?: string;
    salesOrderErpNo?: string;
    salesOrderLineNo?: string;
    customerId?: number;
    customerName?: string;
    orderDueQty?: number;
    orderDueUnitId?: number;
    orderDueUnitCode?: string;
    orderDueUnitName?: string;
    salesOrderDeliveryDate?: string;
    productionStartDate?: string;
    productionEndDate?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    motherMaterialId?: number;
    motherMaterialCode?: string;
    motherMaterialName?: string;
    categoryId?: number;
    categoryCode?: string;
    categoryName?: string;
    prodType?: string;
    prodTypeName?: string;
    modelId?: number;
    modelName?: string;
    modelCode?: string;
    motherModelId?: number;
    motherModelName?: string;
    motherModelCode?: string;
    recipeId?: number;
    recipeCode?: string;
    recipeName?: string;
    bomId?: number;
    bomVersion?: string;
    routeId?: number;
    routeCode?: string;
    routeName?: string;
    routeVersion?: string;
    sizeSpec?: string;
    sizeName?: string;
    targetQty?: number;
    targetUnitId?: number;
    targetUnitCode?: string;
    targetUnitName?: string;
    targetUom?: string;
    fgDeductQty?: number;
    netPlanQty?: number;
    operationCount?: number;
    totalLockQty?: number;
    frontProcessFlag?: boolean;
    postProcessFlag?: boolean;
    inventorySourceBatchNos?: string;
    plannedAt?: string;
    releasedAt?: string;
    statusOperatorId?: number;
    statusOperatorName?: string;
    statusOperateTime?: string;
    statusRemark?: string;
    batchRuleId?: number;
    batchRuleCode?: string;
    batchRuleVersion?: number;
    batchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    productionBatchRuleId?: number;
    productionBatchRuleCode?: string;
    productionBatchRuleVersion?: number;
    productionBatchContextJson?: string;
    batchStatus?: string;
    batchGeneratedTime?: string;
    remark?: string;
    routeSnapshotJson?: string;
    salesOrderSnapshotJson?: string;
    creator?: string;
    createTime?: string;
    updater?: string;
    updateTime?: string;
    operations?: Operation[];
    inventoryLocks?: InventoryLock[];
  }

  export interface BatchPreviewReq {
    id?: number;
    planDate?: string;
    productionStartDate?: string;
    materialCode?: string;
    categoryCode?: string;
    prodType?: string;
    modelCode?: string;
    motherModelCode?: string;
    batchRuleId?: number;
    batchRuleCode?: string;
    useBoundRule?: boolean;
    opCode?: string;
    opName?: string;
    workCenterId?: number;
  }

  export interface BatchPreviewResp {
    batchNo?: string;
    ruleId?: number;
    ruleCode?: string;
    ruleName?: string;
    ruleVersion?: number;
    currentSeq?: number;
    nextSeq?: number;
    maxUsedSeq?: number;
    maxPlannedSeq?: number;
    warning?: string;
  }

  export interface ProcessPivotPiece {
    actualModelCode?: string;
    actualSizeSpec?: string;
    coaFlag?: boolean;
    defectFlag?: boolean;
    lastReportTime?: string;
    outputActualSizeSpec?: string;
    outputBatchNo?: string;
    pieceNo?: string;
    remark?: string;
    reportConfirmed?: boolean;
    sourceBatchNo?: string;
    stageCode?: string;
    status?: 'DEFECT' | 'DONE' | 'PENDING' | string;
  }

  export interface ProcessPivotInspection {
    defectSummary?: string;
    inspectionId?: number;
    inspectionNgQty?: number;
    inspectionNo?: string;
    inspectionQty?: number;
    inspectionTime?: string;
    inspectionType?: string;
    judgment?: string;
    productBatchNo?: string;
    remark?: string;
    sourceType?: string;
    stageCode?: string;
    status?: string;
  }

  export interface QtimeInfo {
    elapsedMinutes?: number;
    message?: string;
    modelPrefix?: string;
    ruleType?: string;
    sourceEndTime?: string;
    standardMinutes?: number;
    status?: 'MISSING_SOURCE_TIME' | 'NORMAL' | 'NO_RULE' | 'TIMEOUT' | string;
    targetStarted?: boolean;
    targetStartTime?: string;
    timeout?: boolean;
  }

  export interface ProcessPivotStage {
    stageCode?: string;
    stageName?: string;
    stageStatus?: 'FINISHED' | 'NOT_STARTED' | 'PENDING' | 'RUNNING' | string;
    sourceBatchNos?: string;
    outputBatchNos?: string;
    inputQty?: number;
    reportQty?: number;
    doneQty?: number;
    pendingQty?: number;
    pendingUnit?: string;
    defectQty?: number;
    inspectionQty?: number;
    inspectionNgQty?: number;
    confirmedQty?: number;
    lengthQty?: number;
    lengthUnit?: string;
    processLength?: number;
    reportUnit?: string;
    startPosition?: number;
    lastReportTime?: string;
    qtime?: QtimeInfo;
    pieceDetails?: ProcessPivotPiece[];
    inspectionDetails?: ProcessPivotInspection[];
    remark?: string;
  }

  export interface ProcessPivotRow {
    id?: number;
    planNo?: string;
    planMode?: string;
    sourceType?: string;
    postProcessFlag?: boolean;
    planNoTagText?: string;
    planDate?: string;
    planStatus?: string;
    productionStartDate?: string;
    productionEndDate?: string;
    materialCode?: string;
    materialName?: string;
    motherMaterialCode?: string;
    motherMaterialName?: string;
    motherModelCode?: string;
    motherModelName?: string;
    modelCode?: string;
    modelName?: string;
    sizeSpec?: string;
    sizeName?: string;
    targetQty?: number;
    netPlanQty?: number;
    targetUom?: string;
    batchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    inventorySourceBatchNos?: string;
    operationStageCodes?: string[];
    motherRollBatchNo?: string;
    segmentBatchNo?: string;
    pivotRowKey?: string;
    planMergeKey?: string;
    segmentMergeKey?: string;
    modelSizeMergeKey?: string;
    actualModelCode?: string;
    actualSizeSpec?: string;
    variationStartStageCode?: string;
    variationStartStageName?: string;
    stageMergeKeys?: Record<string, string>;
    totalDefectQty?: number;
    latestReportTime?: string;
    stages?: Record<string, ProcessPivotStage>;
  }

  export interface PageReqVO extends PageParam {
    planNo?: string;
    keyword?: string;
    planStatuses?: string[];
    planMode?: string;
    sourceType?: string;
    salesOrderNo?: string;
    materialKeyword?: string;
    motherMaterialCode?: string;
    motherModelCode?: string;
    motherRollBatchNo?: string;
    prodType?: string;
    categoryCode?: string;
    modelCode?: string;
    sizeSpec?: string;
    recipeCode?: string;
    routeKeyword?: string;
    planDateStart?: string;
    planDateEnd?: string;
    productionStartDateStart?: string;
    productionStartDateEnd?: string;
    productionEndDateStart?: string;
    productionEndDateEnd?: string;
    createTimeStart?: string;
    createTimeEnd?: string;
  }

  export interface ProcessPivotPageReqVO extends PageReqVO {
    exportBaseColumns?: string[];
    exportStageColumns?: string[];
    motherSegmentBatchNo?: string;
  }

  export interface StaticOption {
    id?: number;
    code: string;
    name: string;
  }

  export interface StaticOptions {
    prodTypes: StaticOption[];
    materialCategories: StaticOption[];
    sizeSpecs: StaticOption[];
  }

  export interface OperationStatusReq {
    operationIds: number[];
    actionType: 'CANCEL' | 'FINISH' | 'PAUSE' | 'RESUME';
    pauseScope?: 'ALL' | 'DATE_RANGE';
    pauseStartDate?: string;
    pauseEndDate?: string;
    statusDates?: string[];
    resumeDate?: string;
    finishTime?: string;
    reasonRemark?: string;
  }

  export interface PlanStatusReq {
    id: number;
    planStatus: 'CANCELLED' | 'CLOSED' | 'DRAFT' | 'PAUSED' | 'RELEASED';
    reasonRemark: string;
  }

  export interface InventoryLockReleaseReq {
    lockId: number;
    releaseQty: number;
    releaseReason: string;
  }

  export interface WipCandidate extends MesHcInvStockApi.Stock {
    shareableQty?: number;
    stockStatus?: string;
    lockedQty?: number;
    consumedQty?: number;
    releasedQty?: number;
    lockRemainingQty?: number;
    activeLockId?: number;
    activeLockStatus?: string;
    activeLockTargetPlanNo?: string;
    activeLockTargetOpName?: string;
    activeLockRemainingQty?: number;
    txnSummary?: string;
  }

  export interface WipCandidatePageReq extends PageParam {
    targetPlanId?: number;
    targetPlanNo?: string;
    targetOperationId?: number;
    targetOpSeq?: number;
    targetOpCode?: string;
    sourceOpSeq?: number;
    sourcePlanNo?: string;
    batchNo?: string;
    sourceBatchNo?: string;
    sourceParentBatchNo?: string;
    materialCode?: string;
    modelNo?: string;
    qualityStatus?: string;
    keyword?: string;
  }

  export interface PlanStatusLog {
    id?: number;
    planId?: number;
    planNo?: string;
    actionType?: string;
    fromStatus?: string;
    toStatus?: string;
    reasonRemark?: string;
    operatorId?: number;
    operatorName?: string;
    operateTime?: string;
  }
}

export async function getPlanOrderPage(params: MesHcPlanOrderApi.PageReqVO) {
  return requestClient.get<PageResult<MesHcPlanOrderApi.PlanOrder>>(
    '/mes/hc/plan/plan-order/page',
    { params },
  );
}

export async function getPlanProcessPivotPage(
  params: MesHcPlanOrderApi.ProcessPivotPageReqVO,
) {
  return requestClient.get<PageResult<MesHcPlanOrderApi.ProcessPivotRow>>(
    '/mes/hc/plan/plan-order/process-pivot/page',
    { params, timeout: PROCESS_PIVOT_REQUEST_TIMEOUT },
  );
}

export async function exportPlanProcessPivot(
  params: MesHcPlanOrderApi.ProcessPivotPageReqVO,
) {
  return requestClient.download(
    '/mes/hc/plan/plan-order/process-pivot/export-excel',
    { params, timeout: PROCESS_PIVOT_REQUEST_TIMEOUT },
  );
}

export async function getPlanOrder(id: number) {
  return requestClient.get<MesHcPlanOrderApi.PlanOrder>(
    `/mes/hc/plan/plan-order/get?id=${id}`,
  );
}

export async function getPlanOrderDetail(id: number) {
  return requestClient.get<MesHcPlanOrderApi.PlanOrder>(
    `/mes/hc/plan/plan-order/get-detail?id=${id}`,
  );
}

export async function getPlanOrderStaticOptions() {
  return requestClient.get<MesHcPlanOrderApi.StaticOptions>(
    '/mes/hc/plan/plan-order/static-options',
  );
}

export async function previewPlanRootBatchNo(
  params: MesHcPlanOrderApi.BatchPreviewReq,
) {
  return requestClient.get<MesHcPlanOrderApi.BatchPreviewResp>(
    '/mes/hc/plan/plan-order/batch-no/preview',
    { params },
  );
}

export async function createPlanOrder(data: MesHcPlanOrderApi.PlanOrder) {
  return requestClient.post<number>('/mes/hc/plan/plan-order/create', data);
}

export async function updatePlanOrder(data: MesHcPlanOrderApi.PlanOrder) {
  return requestClient.put<boolean>('/mes/hc/plan/plan-order/update', data);
}

export async function updatePlanOperationStatus(
  data: MesHcPlanOrderApi.OperationStatusReq,
) {
  return requestClient.put<boolean>(
    '/mes/hc/plan/plan-order/operation-status',
    data,
  );
}

export async function updatePlanStatus(data: MesHcPlanOrderApi.PlanStatusReq) {
  return requestClient.put<boolean>('/mes/hc/plan/plan-order/status', data);
}

export async function withdrawPlanOrder(id: number) {
  return requestClient.put<boolean>(
    `/mes/hc/plan/plan-order/withdraw?id=${id}`,
  );
}

export async function getPlanWipCandidatePage(
  params: MesHcPlanOrderApi.WipCandidatePageReq,
) {
  return requestClient.get<PageResult<MesHcPlanOrderApi.WipCandidate>>(
    '/mes/hc/plan/plan-order/wip-candidate/page',
    { params },
  );
}

export async function releasePlanInventoryLock(
  data: MesHcPlanOrderApi.InventoryLockReleaseReq,
) {
  return requestClient.put<boolean>(
    '/mes/hc/plan/plan-order/inventory-lock/release',
    data,
  );
}

export async function getPlanStatusLogList(planId: number) {
  return requestClient.get<MesHcPlanOrderApi.PlanStatusLog[]>(
    `/mes/hc/plan/plan-order/status-log/list?planId=${planId}`,
  );
}

export async function deletePlanOrder(id: number) {
  return requestClient.delete<boolean>(
    `/mes/hc/plan/plan-order/delete?id=${id}`,
  );
}

export async function deletePlanOrderList(ids: number[]) {
  return requestClient.delete<boolean>(
    `/mes/hc/plan/plan-order/delete-list?ids=${ids.join(',')}`,
  );
}

export async function destroyPlanOrder(id: number) {
  return requestClient.delete<boolean>(
    `/mes/hc/plan/plan-order/destroy?id=${id}`,
  );
}

export async function exportPlanOrder(params: Record<string, any>) {
  return requestClient.download('/mes/hc/plan/plan-order/export-excel', {
    params,
  });
}
