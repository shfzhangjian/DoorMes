import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcCutRoundConsoleApi {
  export interface QtimeInfo {
    elapsedMinutes?: number;
    message?: string;
    sourceEndTime?: string;
    standardMinutes?: number;
    status?: string;
    targetStarted?: boolean;
    targetStartTime?: string;
    timeout?: boolean;
  }

  export interface TaskQuery {
    equipmentCode?: string;
    equipmentId?: number;
    taskStatus?: 'ALL' | 'CANCELLED' | 'COMPLETED' | 'FINISHED' | 'IN_PROGRESS' | 'PAUSED' | 'PENDING' | 'RUNNING' | 'UNFINISHED';
    taskKeyword?: string;
    productKeyword?: string;
    motherMaterialKeyword?: string;
    motherModelKeyword?: string;
    productionDate?: string;
  }

  export interface TaskItem {
    availableSourceLength?: number;
    batchNo?: string;
    categoryCode?: string;
    categoryName?: string;
    confirmedSourceCount?: number;
    endTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    id?: string;
    materialCode?: string;
    modelCode?: string;
    motherMaterialCode?: string;
    motherModelCode?: string;
    parentProductionBatchNo?: string;
    planId: number;
    planNo?: string;
    planOperationId: number;
    process?: string;
    productionBatchNo?: string;
    requirements?: string;
    sourceBatchNo?: string;
    sourceProductionBatchNo?: string;
    spec?: string;
    startTime?: string;
    status?: string;
    workCenterId?: number;
    workCenterName?: string;
  }

  export interface PassWorkItem {
    actualValue?: string;
    actualValue2?: string;
    category?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    item?: string;
    itemSeq?: number;
    node?: string;
    remark?: string;
    standard?: string;
    status?: string;
    valueMode?: string;
  }

  export interface PassWorkRecord {
    canConfirm?: boolean;
    canFill?: boolean;
    canView?: boolean;
    confirmRemark?: string;
    confirmer?: string;
    confirmerTime?: string;
    details?: PassWorkItem[];
    formCode?: string;
    formId?: number;
    formRemark?: string;
    headerDataJson?: string;
    id?: string;
    inspectionResult?: string;
    name?: string;
    presetDetails?: PassWorkItem[];
    presetHeaderDataJson?: string;
    recordId?: number;
    recorder?: string;
    recorderTime?: string;
    result?: string;
    schemaJson?: string;
    status?: string;
    timing?: string;
  }

  export interface PassWorkSaveReq {
    confirmRemark?: string;
    confirmer?: string;
    confirmerTime?: string;
    details?: PassWorkItem[];
    formCode: string;
    formRemark?: string;
    inspectionResult?: string;
    planId: number;
    planOperationId: number;
    recordId?: number;
    recordDate?: string;
    recorder?: string;
    recorderTime?: string;
    result?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
  }

  export interface SourceItem {
    actualSizeRule?: string;
    actualSizeSuffix?: string;
    coaFlag?: boolean;
    confirmStatus?: string;
    confirmTime?: string;
    confirmedBatchNo?: string;
    defectCode?: string;
    downstreamStatus?: string;
    extraJson?: string;
    grindingPlanId?: number;
    grindingPlanNo?: string;
    grindingPlanOperationId?: number;
    grindingSecondDetailId: number;
    inspectionResult?: string;
    lossLength?: number;
    motherBatchNo?: string;
    modelCode?: string;
    napSampleLength?: number;
    operationCode?: string;
    operationName?: string;
    outputLength?: number;
    parentProductionBatchNo?: string;
    processName?: string;
    processStage?: string;
    processLength?: number;
    productionBatchNo?: string;
    productQualityStatus?: string;
    qualityLockReason?: string;
    reportProcess?: string;
    rowUid?: string;
    segmentMark?: string;
    selfCheck?: string;
    sourceBatchNo?: string;
    sourceMenuCode?: string;
    sourceProductionBatchNo?: string;
    qtime?: QtimeInfo;
  }

  export interface CheckItem {
    abnormalRemark?: string;
    actualValue?: string;
    checkResult?: string;
    id?: number;
    itemCategory?: string;
    itemName?: string;
    sortNo?: number;
    standardValue?: string;
  }

  export interface ReportItem {
    upstreamSampleLockReason?: string;
    actualSizeRule?: string;
    actualSizeSuffix?: string;
    checkItems?: CheckItem[];
    confirmerName?: string;
    confirmerTime?: string;
    defectCode?: string;
    endTime?: string;
    extraJson?: string;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardStartPosition?: number;
    glueBoardUsageId?: number;
    glueBoardUseLength?: number;
    aqcStatus?: string;
    aqcTaskId?: number;
    fqcOrderId?: number;
    fqcNo?: string;
    fqcStatus?: string;
    fqcJudgment?: string;
    inspectionRemark?: string;
    inspectionResult?: string;
    inspectionStatus?: string;
    inspectionTaskId?: number;
    inspectionTaskNo?: string;
    inspectionTime?: string;
    inspectorName?: string;
    productQualityStatus?: string;
    qualityLockReason?: string;
    qualityRiskFlag?: 'ADHESIVE2_NG' | 'BOTH_NG' | 'CUT_ROUND_NG' | 'NONE' | string;
    qualityRiskSnapshotJson?: string;
    id?: number;
    inputLength?: number;
    startPosition?: number;
    endPosition?: number;
    lossLength?: number;
    materialCode?: string;
    modelCode?: string;
    napSampleLength?: number;
    outputLength?: number;
    parentProductionBatchNo?: string;
    planId: number;
    planNo?: string;
    planOperationId: number;
    productionBatchNo?: string;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    reportDate?: string;
    reportStatus?: string;
    selfCheck?: string;
    sourceBatchNo?: string;
    sourceGrindingSecondDetailId?: number;
    sourceProductionBatchNo?: string;
    sourceType?: string;
    startTime?: string;
  }

  export interface ReportQuery {
    scanConfirmDate?: string;
  }

  export interface ChangeoverInspection {
    checkItems?: CheckItem[];
    createTime?: string;
    currentPlanNo?: string;
    detailItemsJson?: string;
    extraJson?: string;
    feedbackRemark?: string;
    feedbackResult?: string;
    feedbackTime?: string;
    headerDataJson?: string;
    id?: number;
    inspectionStatus?: string;
    inspectionStatusName?: string;
    motherSegmentBatchNo?: string;
    planId: number;
    planNo?: string;
    planOperationId: number;
    pressSlotSliceNo?: string;
    previousModelCode?: string;
    productionMaterialCode?: string;
    productionModelCode?: string;
    recordTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    recorderName?: string;
    remark?: string;
    sourceSlittingSliceId?: number;
    submitTime?: string;
    updateTime?: string;
  }

  export interface CutRoundInspectionTaskDetail {
    cutRoundReportId?: number;
    id?: number;
    inspectionResult?: string;
    inspectionTime?: string;
    inspectorName?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    parentProductionBatchNo?: string;
    productionBatchNo?: string;
    qualityRiskFlag?: 'ADHESIVE2_NG' | 'BOTH_NG' | 'CUT_ROUND_NG' | 'NONE' | string;
    qualityRiskSnapshotJson?: string;
    fqcOrderId?: number;
    fqcNo?: string;
    fqcStatus?: string;
    fqcJudgment?: string;
    remark?: string;
    seqNo?: number;
    sizeRule?: string;
    taskId?: number;
  }

  export interface CutRoundInspectionTask {
    detailCount?: number;
    details?: CutRoundInspectionTaskDetail[];
    expectedFinishDate?: string;
    id?: number;
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    priorityLevel?: string;
    receiveLocation?: string;
    receiverName?: string;
    remark?: string;
    reportDate?: string;
    reportProcess?: string;
    reportTime?: string;
    reporterName?: string;
    taskNo?: string;
    taskStatus?: string;
    fqcOrderId?: number;
    fqcNo?: string;
    fqcStatus?: string;
    fqcJudgment?: string;
  }

  export interface CreateCutRoundInspectionTaskReq {
    expectedFinishDate?: string;
    planId: number;
    planOperationId: number;
    priorityLevel?: string;
    receiveLocation?: string;
    receiverName?: string;
    remark?: string;
    reportDate?: string;
    reportIds: number[];
    reportProcess?: string;
    reportTime?: string;
    reporterName?: string;
  }

  export interface SaveReportReq extends Omit<ReportItem, 'planNo' | 'reportStatus'> {
    sourceGrindingSecondDetailId: number;
  }

  export type SaveAndConfirmReportReq = Omit<ConfirmReportReq, 'id'> & SaveReportReq;

  export interface GlueBoardUsage {
    aqcSampleLength?: number;
    availableLength?: number;
    availableStartPosition?: number;
    consumedLength?: number;
    lossLength?: number;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardStockId?: number;
    id?: number;
    stockMeasureMode?: string;
    latestAqcTask?: AqcTask;
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    qualityLockReason?: string;
    qualityLockStartPosition?: number;
    qualityStatus?: string;
    receiveLength?: number;
    receiveCount?: number;
    receiveStartPosition?: number;
    recordDate?: string;
    recorderId?: number;
    recorderName?: string;
    recorderTime?: string;
    returnedLength?: number;
    returnedCount?: number;
    returnedStartPosition?: number;
    consumedCount?: number;
    lossCount?: number;
    availableCount?: number;
    lifetimeMode?: string;
    lifetimeLimitLength?: number;
    lifetimeLimitCount?: number;
    lifeUsedLength?: number;
    lifeUsedCount?: number;
    usageStatus?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface SaveGlueBoardUsageReq {
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    glueBoardBatchNo: string;
    glueBoardMaterialCode: string;
    glueBoardStockId?: number;
    id?: number;
    planId: number;
    planOperationId: number;
    receiveCount?: number;
    receiveLength?: number;
    receiveStartPosition?: number;
    recordDate?: string;
    recorderId?: number;
    recorderName?: string;
    recorderTime?: string;
    stockMeasureMode?: string;
  }

  export interface GlueBoardStock {
    accessoryCategory?: string;
    accessoryCategoryName?: string;
    availableCount?: number;
    availableLength?: number;
    availableStartPosition?: number;
    createTime?: string;
    edgeWarehouseCode?: string;
    edgeWarehouseName?: string;
    erpTransferMessage?: string;
    erpTransferNo?: string;
    erpTransferStatus?: string;
    erpTransferTime?: string;
    extraJson?: string;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardMaterialName?: string;
    id?: number;
    printCount?: number;
    printTime?: string;
    qualityStatus?: string;
    receiveDate?: string;
    receiveCount?: number;
    receiveLength?: number;
    receiveStartPosition?: number;
    receiveTime?: string;
    receiverId?: number;
    receiverName?: string;
    remark?: string;
    sourceWarehouseCode?: string;
    sourceWarehouseName?: string;
    stockStatus?: string;
    stockMeasureMode?: string;
    lossLength?: number;
    lossCount?: number;
    lifetimeMode?: string;
    lifetimeLimitLength?: number;
    lifetimeLimitCount?: number;
    lifeUsedLength?: number;
    lifeUsedCount?: number;
    transferQty?: number;
    transferUnit?: string;
    updateTime?: string;
    unpackQty?: number;
    unpackUnit?: string;
    usedLength?: number;
    usedCount?: number;
  }

  export interface SaveGlueBoardStockReq {
    accessoryCategory?: string;
    accessoryCategoryName?: string;
    availableCount?: number;
    availableLength?: number;
    availableStartPosition?: number;
    edgeWarehouseCode?: string;
    edgeWarehouseName?: string;
    erpTransferNo?: string;
    glueBoardBatchNo: string;
    glueBoardMaterialCode: string;
    glueBoardMaterialName?: string;
    receiveDate?: string;
    receiveCount?: number;
    receiveLength?: number;
    receiveStartPosition?: number;
    receiveTime?: string;
    receiverId?: number;
    receiverName?: string;
    remark?: string;
    sourceWarehouseCode?: string;
    sourceWarehouseName?: string;
    stockMeasureMode?: string;
    lifetimeMode?: string;
    lifetimeLimitLength?: number;
    lifetimeLimitCount?: number;
    lifeUsedLength?: number;
    lifeUsedCount?: number;
    transferQty?: number;
    transferUnit?: string;
    unpackQty?: number;
    unpackUnit?: string;
  }

  export interface SaveGlueBoardLossReq {
    endPosition?: number;
    glueBoardUsageId: number;
    lossLength: number;
    lossReason?: string;
    startPosition: number;
  }

  export interface AqcTask {
    adhesiveReportId?: number;
    feedbackRemark?: string;
    feedbackResult?: string;
    feedbackTime?: string;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardUsageId?: number;
    id?: number;
    lockStartPosition?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    recordDate?: string;
    sampleLength?: number;
    sampleStartPosition?: number;
    submitterId?: number;
    submitterName?: string;
    submitTime?: string;
    taskStatus?: string;
    taskType?: string;
  }

  export interface SaveAqcTaskReq {
    adhesiveReportId?: number;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardUsageId?: number;
    id?: number;
    planId: number;
    planOperationId: number;
    recordDate?: string;
    sampleLength: number;
    sampleStartPosition: number;
    submitterId?: number;
    submitterName?: string;
    submitTime?: string;
    taskType: 'DAILY_GLUE_BOARD' | 'REPORT_FIRST_INSPECTION';
  }

  export interface FeedbackAqcTaskReq {
    feedbackRemark?: string;
    feedbackResult: string;
    feedbackTime?: string;
    id: number;
    lockStartPosition?: number;
  }

  export interface ReplaceConsumableReq {
    ledgerId?: number;
    requestKey?: string;
    batchNo?: string;
    consumableType: 'CUTTING_BLADE' | 'CUTTING_FELT' | string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    materialCode?: string;
    materialName?: string;
    operatorId?: number;
    operatorName?: string;
    planId?: number;
    planNo?: string;
    planOperationId: number;
    initialUseCount?: number;
    replaceQuantity?: number;
    replaceReason?: string;
    replaceTime?: string;
    stockId?: number;
  }

  export interface StartWorkOrderReq {
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    planId: number;
    planOperationId: number;
    recorderName?: string;
    recorderTime?: string;
    reportDate?: string;
    startTime?: string;
  }

  export interface SwitchEquipmentReq {
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    planId?: number;
    planOperationId?: number;
    switchReason?: string;
  }

  export interface CompleteWorkOrderReq {
    confirmerName?: string;
    confirmerTime?: string;
    endTime?: string;
    planId: number;
    planOperationId: number;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    reportDate?: string;
  }

  export interface CompleteSegmentReq extends CompleteWorkOrderReq {
    sourceProductionBatchNo?: string;
  }

  export interface ConfirmReportReq {
    actualSizeRule?: string;
    confirmerName?: string;
    confirmerTime?: string;
    glueBoardModel?: string;
    id: number;
    scannedBatchNo: string;
  }

  export interface PrintReportReq {
    id: number;
    printCount?: number;
    printStatus?: string;
    printTime?: string;
  }

  export interface CorrectAbnormalCategoryReq {
    category: string;
    id: number;
    reason: string;
  }

  export interface IntermediateDetail {
    id?: number;
    pressSlotReportId?: number;
    samplePosition?: string;
    samplePositionName?: string;
    seq?: number;
    sliceBatchNo?: string;
    sourceSlittingSliceId?: number;
    thickness1?: number;
    thickness2?: number;
    thickness3?: number;
    thickness4?: number;
    thickness5?: number;
    thickness6?: number;
    thickness7?: number;
    thickness8?: number;
    thickness9?: number;
    thickness10?: number;
    widthMm?: number;
    // 兼容旧中间品字段。
    leftThickness?: number;
    lengthMark?: number;
    remark?: string;
    rightThickness?: number;
    sortNo?: number;
  }

  export interface IntermediateRecord {
    adhesiveReportId?: number;
    batchNo?: string;
    confirmerName?: string;
    details?: IntermediateDetail[];
    endSliceNo?: string;
    extraJson?: string;
    firstSampleSliceNo?: string;
    firstSlotDepthAvg?: number;
    firstSlotDepthMax?: number;
    firstSlotDepthMin?: number;
    firstSlotDepthXAvg?: number;
    firstSlotDepthXMax?: number;
    firstSlotDepthXMin?: number;
    firstSlotDepthYAvg?: number;
    firstSlotDepthYMax?: number;
    firstSlotDepthYMin?: number;
    frontSliceNo?: string;
    id?: number;
    inputQty?: number;
    materialCode?: string;
    middleSliceNo?: string;
    modelCode?: string;
    outputQty?: number;
    planId: number;
    planNo?: string;
    planOperationId: number;
    processLength?: number;
    productionDate?: string;
    recordDate?: string;
    recorderName?: string;
    recordStatus?: string;
    remark?: string;
    widthEnd?: number;
    widthMiddle?: number;
    widthStart?: number;
  }
}

const CUT_ROUND_BASE = '/mes/hc/execution/formula-report/cut-round';

export function getCutRoundConsoleTaskList(params: MesHcCutRoundConsoleApi.TaskQuery) {
  return requestClient.get<MesHcCutRoundConsoleApi.TaskItem[]>(`${CUT_ROUND_BASE}/task-list`, { params });
}

export function getCutRoundConsolePassWorkList(
  planId: number,
  planOperationId: number,
  params?: { equipmentId?: number; recordDate?: string },
) {
  return requestClient.get<MesHcCutRoundConsoleApi.PassWorkRecord[]>(`${CUT_ROUND_BASE}/pass-work/list`, {
    params: { planId, planOperationId, ...params },
  });
}

export function startCutRoundConsoleWorkOrder(data: MesHcCutRoundConsoleApi.StartWorkOrderReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/start`, data);
}

export function switchCutRoundConsoleWorkOrderEquipment(data: MesHcCutRoundConsoleApi.SwitchEquipmentReq) {
  return requestClient.post<boolean>(`${CUT_ROUND_BASE}/switch-equipment`, data);
}

export function saveCutRoundConsolePassWork(data: MesHcCutRoundConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/pass-work/save`, data);
}

export function confirmCutRoundConsolePassWork(data: MesHcCutRoundConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/pass-work/confirm`, data);
}

export function scanCutRoundConsoleSource(batchNo: string) {
  return requestClient.get<MesHcCutRoundConsoleApi.SourceItem>(`${CUT_ROUND_BASE}/source/scan`, {
    params: { batchNo },
  });
}

export function getCutRoundConsoleSourceList(planId: number, planOperationId: number, sourceBatchNo?: string) {
  return requestClient.get<MesHcCutRoundConsoleApi.SourceItem[]>(`${CUT_ROUND_BASE}/source/list`, {
    params: { planId, planOperationId, sourceBatchNo: sourceBatchNo || undefined },
  });
}

export function getCutRoundConsoleCheckTemplate(modelCode?: string) {
  return requestClient.get<MesHcCutRoundConsoleApi.CheckItem[]>(`${CUT_ROUND_BASE}/check-template`, {
    params: { modelCode },
  });
}

export function getCutRoundConsoleReportList(
  planOperationId: number,
  query?: MesHcCutRoundConsoleApi.ReportQuery,
) {
  return requestClient.get<MesHcCutRoundConsoleApi.ReportItem[]>(`${CUT_ROUND_BASE}/report/list`, {
    params: { planOperationId, ...query },
  });
}

export function findCutRoundReportByBatchNo(batchNo: string, planNo?: string) {
  return requestClient.get<MesHcCutRoundConsoleApi.ReportItem | null>(`${CUT_ROUND_BASE}/report/get-by-batch`, {
    params: { batchNo, planNo },
  });
}

export function getCutRoundInspectionTasks(planOperationId: number) {
  return requestClient.get<MesHcCutRoundConsoleApi.CutRoundInspectionTask[]>(`${CUT_ROUND_BASE}/inspection-task/list`, {
    params: { planOperationId },
  });
}

export function createCutRoundInspectionTask(data: MesHcCutRoundConsoleApi.CreateCutRoundInspectionTaskReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/inspection-task/create`, data);
}

export function getCutRoundConsoleGlueBoardUsageList(planOperationId: number) {
  return requestClient.get<MesHcCutRoundConsoleApi.GlueBoardUsage[]>(`${CUT_ROUND_BASE}/glue-board/list`, {
    params: { planOperationId },
  });
}

export function getCutRoundConsoleGlueBoardUsagePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcCutRoundConsoleApi.GlueBoardUsage>>(`${CUT_ROUND_BASE}/glue-board/page`, {
    params,
  });
}

export function getCutRoundConsoleGlueBoardStockPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcCutRoundConsoleApi.GlueBoardStock>>(
    `${CUT_ROUND_BASE}/glue-board-stock/page`,
    { params },
  );
}

export function getCutRoundConsoleGlueBoardStockByBatch(glueBoardBatchNo: string, consumableType = 'CUTTING_BLADE') {
  return requestClient.get<MesHcCutRoundConsoleApi.GlueBoardStock | null>(
    `${CUT_ROUND_BASE}/consumable/stock/get-by-batch`,
    { params: { batchNo: glueBoardBatchNo, consumableType } },
  );
}

export function createCutRoundConsoleGlueBoardStock(data: MesHcCutRoundConsoleApi.SaveGlueBoardStockReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/glue-board-stock/create`, data);
}

export function markCutRoundConsoleGlueBoardStockPrinted(id: number) {
  return requestClient.post<MesHcCutRoundConsoleApi.GlueBoardStock>(
    `${CUT_ROUND_BASE}/glue-board-stock/mark-printed?id=${id}`,
  );
}

export function getCutRoundConsoleCurrentGlueBoardUsage(planOperationId: number, equipmentId?: number) {
  return requestClient.get<any[]>(`${CUT_ROUND_BASE}/consumable/status`, {
    params: { equipmentId, planOperationId },
  });
}

export function saveCutRoundConsoleGlueBoardUsage(data: MesHcCutRoundConsoleApi.SaveGlueBoardUsageReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/glue-board/save`, data);
}

export function reportCutRoundConsoleGlueBoardLoss(data: MesHcCutRoundConsoleApi.SaveGlueBoardLossReq) {
  return requestClient.post<MesHcCutRoundConsoleApi.GlueBoardUsage>(`${CUT_ROUND_BASE}/glue-board/loss`, data);
}

export function returnCutRoundConsoleGlueBoardUsage(id: number) {
  return requestClient.post<MesHcCutRoundConsoleApi.GlueBoardUsage>(`${CUT_ROUND_BASE}/glue-board/return?id=${id}`);
}

export function replaceCutRoundConsumable(data: MesHcCutRoundConsoleApi.ReplaceConsumableReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/consumable/replace`, data);
}

export function submitCutRoundConsoleAqcTask(data: MesHcCutRoundConsoleApi.SaveAqcTaskReq) {
  return requestClient.post<MesHcCutRoundConsoleApi.AqcTask>(`${CUT_ROUND_BASE}/aqc/submit`, data);
}

export function feedbackCutRoundConsoleAqcTask(data: MesHcCutRoundConsoleApi.FeedbackAqcTaskReq) {
  return requestClient.post<MesHcCutRoundConsoleApi.AqcTask>(`${CUT_ROUND_BASE}/aqc/feedback`, data);
}

export function saveCutRoundConsoleReport(data: MesHcCutRoundConsoleApi.SaveReportReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/report/save`, data);
}

export function saveAndConfirmCutRoundConsoleReport(
  data: MesHcCutRoundConsoleApi.SaveAndConfirmReportReq,
) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/report/save-confirm`, data);
}

export function completeCutRoundConsoleWorkOrder(data: MesHcCutRoundConsoleApi.CompleteWorkOrderReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/submit`, data);
}

export function completeCutRoundConsoleSegment(data: MesHcCutRoundConsoleApi.CompleteSegmentReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/segment/complete`, data);
}

export function confirmCutRoundConsoleReport(data: MesHcCutRoundConsoleApi.ConfirmReportReq) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/report/confirm`, data);
}

export function markCutRoundConsoleReportPrinted(data: MesHcCutRoundConsoleApi.PrintReportReq) {
  return requestClient.post<boolean>(`${CUT_ROUND_BASE}/report/mark-printed`, data);
}

export function correctCutRoundConsoleReportAbnormalCategory(
  data: MesHcCutRoundConsoleApi.CorrectAbnormalCategoryReq,
) {
  return requestClient.post<MesHcCutRoundConsoleApi.ReportItem>(
    `${CUT_ROUND_BASE}/report/abnormal-category/correct`,
    data,
  );
}

export function getCutRoundConsoleIntermediate(params: {
  adhesiveReportId?: number;
  planId: number;
  planOperationId: number;
  recordDate?: string;
}) {
  return Promise.resolve({
    details: [],
    inputQty: 0,
    outputQty: 0,
    planId: params.planId,
    planOperationId: params.planOperationId,
    recordDate: params.recordDate,
    recordStatus: 'DRAFT',
  } as MesHcCutRoundConsoleApi.IntermediateRecord);
}

export function saveCutRoundConsoleIntermediate(data: MesHcCutRoundConsoleApi.IntermediateRecord) {
  return Promise.resolve(data.id || 0);
}

export function getCutRoundChangeoverLatest(planOperationId: number) {
  return requestClient.get<MesHcCutRoundConsoleApi.ChangeoverInspection | null>(`${CUT_ROUND_BASE}/changeover/latest`, {
    params: { planOperationId },
  });
}

export function getCutRoundChangeoverList(planOperationId: number, motherSegmentBatchNo?: string) {
  return requestClient.get<MesHcCutRoundConsoleApi.ChangeoverInspection[]>(`${CUT_ROUND_BASE}/changeover/list`, {
    params: { motherSegmentBatchNo, planOperationId },
  });
}

export function saveCutRoundChangeover(data: MesHcCutRoundConsoleApi.ChangeoverInspection) {
  return requestClient.post<number>(`${CUT_ROUND_BASE}/changeover/save`, data);
}

export function deleteCutRoundChangeover(recordId: number, planOperationId: number) {
  return requestClient.delete<boolean>(
    `${CUT_ROUND_BASE}/changeover/delete?recordId=${recordId}&planOperationId=${planOperationId}`,
  );
}
