import type { PageParam, PageResult } from '@vben/request';
import type { MesHcStationRecordApi } from '#/api/mes/hc/stationrecord';

import { requestClient } from '#/api/request';

export namespace MesHcAdhesiveConsoleApi {
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

  export interface TaskQuery {
    taskStatus?: 'ALL' | 'CANCELLED' | 'COMPLETED' | 'FINISHED' | 'IN_PROGRESS' | 'PAUSED' | 'PENDING' | 'RUNNING' | 'UNFINISHED';
    taskKeyword?: string;
    productKeyword?: string;
    motherMaterialKeyword?: string;
    motherModelKeyword?: string;
    equipmentCode?: string;
    equipmentId?: number;
    productionDate?: string;
  }

  export interface TaskItem {
    availableSourceLength?: number;
    batchNo?: string;
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
    qtime?: QtimeInfo;
    requirements?: string;
    sourceBatchNo?: string;
    sourceDetailIds?: string;
    sourceMode?: string;
    sourceModeName?: string;
    sourceMotherBatchNo?: string;
    sourcePlanNo?: string;
    sourceProductionBatchNo?: string;
    sourceSegmentMarks?: string;
    inventoryLockIds?: string;
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
    coaFlag?: boolean | number | string;
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
    inventoryLockId?: number;
    lossLength?: number;
    motherBatchNo?: string;
    napSampleLength?: number;
    operationCode?: string;
    operationName?: string;
    outputLength?: number;
    parentProductionBatchNo?: string;
    processName?: string;
    processStage?: string;
    productQualityStatus?: string;
    processLength?: number;
    productionBatchNo?: string;
    qtime?: QtimeInfo;
    qualityLockReason?: string;
    reportProcess?: string;
    rowUid?: string;
    segmentEndOperatorId?: number;
    segmentEndOperatorName?: string;
    segmentEndTime?: string;
    segmentMark?: string;
    segmentStartOperatorId?: number;
    segmentStartOperatorName?: string;
    segmentStartTime?: string;
    segmentTimingId?: number;
    selfCheck?: string;
    sourceMenuCode?: string;
    sourceBatchNo?: string;
    sourceProductionBatchNo?: string;
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

  export interface AbnormalPositionItem {
    abnormalLength?: number;
    batchNo?: string;
    createTime?: string;
    id?: number;
    operationCode?: string;
    operationName?: string;
    operationReportId?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    positionText?: string;
    processStage?: string;
    productionBatchNo?: string;
    remark?: string;
    sortOrder?: number;
    sourceDetailId?: number;
    sourceMenuCode?: string;
    sourcePlanNo?: string;
    sourceRowUid?: string;
  }

  export interface AbnormalPositionSaveReq {
    abnormalLength?: number;
    positionText: string;
    remark?: string;
    sortOrder?: number;
  }

  export interface ReportItem {
    abnormalPositions?: AbnormalPositionItem[];
    checkItems?: CheckItem[];
    confirmerName?: string;
    confirmerTime?: string;
    defectCode?: string;
    downstreamFeedbackAbnormal?: boolean;
    downstreamFeedbackProcessCode?: string;
    downstreamFeedbackProcessName?: string;
    downstreamFeedbackReason?: string;
    downstreamFeedbackReportId?: number;
    editBlockedReason?: string;
    endTime?: string;
    extraJson?: string;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardStartPosition?: number;
    glueBoardUsageId?: number;
    glueBoardUseLength?: number;
    aqcStatus?: string;
    aqcTaskId?: number;
    faiApplyTime?: string;
    faiId?: number;
    faiJudgment?: string;
    faiNo?: string;
    faiRejectReason?: string;
    faiReturnTime?: string;
    sampleLength?: number;
    faiStandardId?: number;
    faiStandardNo?: string;
    faiStatus?: string;
    productQualityStatus?: string;
    qualityLockReason?: string;
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

  export interface SaveReportReq extends Omit<ReportItem, 'abnormalPositions' | 'planNo' | 'reportStatus'> {
    abnormalPositions?: AbnormalPositionSaveReq[];
    sourceGrindingSecondDetailId?: number;
  }

  export interface ReportTimeUpdateReq {
    endTime?: string;
    id: number;
    reportDate?: string;
    startTime?: string;
  }

  export interface StatisticsDataReviseReq {
    id: number;
    inputLength?: number;
    lossLength?: number;
    outputLength?: number;
    reason: string;
  }

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
    glueBoardModel?: string;
    glueBoardStockId?: number;
    id?: number;
    inspectionSubmitTime?: string;
    stockMeasureMode?: string;
    latestInspectionId?: number;
    latestInspectionNo?: string;
    latestInspectionResult?: string;
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
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planOperationId?: number;
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
    glueBoardModel?: string;
    id?: number;
    toolingLedgerId?: number;
    inspectionSubmitTime?: string;
    latestInspectionId?: number;
    latestInspectionNo?: string;
    latestInspectionResult?: string;
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

  export interface GlueBoardStockImportResp {
    clearedStockCount?: number;
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    successCount?: number;
    totalRows?: number;
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
    glueBoardModel?: string;
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

  export interface FaiApplyReq {
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardUsageId?: number;
    planId: number;
    planOperationId: number;
    remark?: string;
    sampleLength?: number;
    sourceGrindingSecondDetailId?: number;
    standardMatchMode?: string;
    submitterName?: string;
    triggerReason?: string;
  }

  export interface FaiSummary {
    allowReportSubmit?: boolean;
    displayText?: string;
    faiApplyTime?: string;
    faiId?: number;
    faiJudgment?: string;
    faiNo?: string;
    faiRejectReason?: string;
    faiReturnTime?: string;
    faiStandardId?: number;
    faiStandardNo?: string;
    faiStandardVersion?: string;
    faiStatus?: string;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardUsageId?: number;
    productBatchNo?: string;
    sampleLength?: number;
    sampleStartPosition?: number;
    sourceGrindingSecondDetailId?: number;
    sourceReportNo?: string;
  }

  export interface FeedbackAqcTaskReq {
    feedbackRemark?: string;
    feedbackResult: string;
    feedbackTime?: string;
    id: number;
    lockStartPosition?: number;
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
    planId: number;
    planOperationId: number;
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
    sourceGrindingSecondDetailId?: number;
    sourceProductionBatchNo?: string;
  }

  export interface SegmentTimingStampReq {
    action: 'END' | 'START';
    operatorId?: number;
    operatorName?: string;
    planId: number;
    planOperationId: number;
    sourceGrindingSecondDetailId: number;
  }

  export interface ConfirmReportReq {
    confirmerName?: string;
    confirmerTime?: string;
    id: number;
    scannedBatchNo: string;
  }

  export interface PrintReportReq {
    id: number;
    printCount?: number;
    printStatus?: string;
    printTime?: string;
  }

  export interface IntermediateDetail {
    id?: number;
    leftThickness?: number;
    lengthMark?: number;
    remark?: string;
    rightThickness?: number;
    sortNo?: number;
  }

  export interface IntermediateRecord {
    stationFormId?: number;
    stationFormCode?: string;
    stationFormName?: string;
    thicknessLabels?: string[];
    formNotes?: Record<string, string>;
    adhesiveReportId?: number;
    batchNo?: string;
    confirmerName?: string;
    confirmerTime?: string;
    details?: IntermediateDetail[];
    extraJson?: string;
    id?: number;
    materialCode?: string;
    modelCode?: string;
    planId: number;
    planNo?: string;
    planOperationId: number;
    processLength?: number;
    productWidthMm?: number;
    productionDate?: string;
    recordTime?: string;
    recorderName?: string;
    recordStatus?: string;
    remark?: string;
    widthEnd?: number;
    widthMiddle?: number;
    widthStart?: number;
  }
}

const ADHESIVE_BASE = '/mes/hc/execution/formula-report/adhesive';

export function getAdhesiveConsoleTaskList(params: MesHcAdhesiveConsoleApi.TaskQuery) {
  return requestClient.get<MesHcAdhesiveConsoleApi.TaskItem[]>(`${ADHESIVE_BASE}/task-list`, { params });
}

export function getAdhesiveConsolePassWorkList(
  planId: number,
  planOperationId: number,
  params?: { equipmentId?: number; recordDate?: string },
) {
  return requestClient.get<MesHcAdhesiveConsoleApi.PassWorkRecord[]>(`${ADHESIVE_BASE}/pass-work/list`, {
    params: { planId, planOperationId, ...params },
  });
}

export function startAdhesiveConsoleWorkOrder(data: MesHcAdhesiveConsoleApi.StartWorkOrderReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/start`, data);
}

export function switchAdhesiveConsoleWorkOrderEquipment(data: MesHcAdhesiveConsoleApi.SwitchEquipmentReq) {
  return requestClient.post<boolean>(`${ADHESIVE_BASE}/switch-equipment`, data);
}

export function saveAdhesiveConsolePassWork(data: MesHcAdhesiveConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/pass-work/save`, data);
}

export function confirmAdhesiveConsolePassWork(data: MesHcAdhesiveConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/pass-work/confirm`, data);
}

export function scanAdhesiveConsoleSource(batchNo: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.SourceItem>(`${ADHESIVE_BASE}/source/scan`, {
    params: { batchNo },
  });
}

export function getAdhesiveConsoleSourceList(planId: number, planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.SourceItem[]>(`${ADHESIVE_BASE}/source/list`, {
    params: { planId, planOperationId },
  });
}

export function stampAdhesiveConsoleSegmentTiming(data: MesHcAdhesiveConsoleApi.SegmentTimingStampReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/segment-timing/stamp`, data);
}

export function getAdhesiveConsoleCheckTemplate() {
  return requestClient.get<MesHcAdhesiveConsoleApi.CheckItem[]>(`${ADHESIVE_BASE}/check-template`);
}

export function getAdhesiveConsoleReportList(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ReportItem[]>(`${ADHESIVE_BASE}/report/list`, {
    params: { planOperationId },
  });
}

export function getAdhesiveConsoleGlueBoardUsageList(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.GlueBoardUsage[]>(`${ADHESIVE_BASE}/glue-board/list`, {
    params: { planOperationId },
  });
}

export function getAdhesiveConsoleGlueBoardUsagePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcAdhesiveConsoleApi.GlueBoardUsage>>(`${ADHESIVE_BASE}/glue-board/page`, {
    params,
  });
}

export function getAdhesiveConsoleGlueBoardStockPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcAdhesiveConsoleApi.GlueBoardStock>>(
    `${ADHESIVE_BASE}/glue-board-stock/page`,
    { params },
  );
}

export function exportAdhesiveConsoleGlueBoardStock(params: Record<string, any>) {
  return requestClient.download(`${ADHESIVE_BASE}/glue-board-stock/export-excel`, { params });
}

export function importAdhesiveConsoleGlueBoardStock(file: File, confirmClear = true) {
  return requestClient.upload<MesHcAdhesiveConsoleApi.GlueBoardStockImportResp>(
    `${ADHESIVE_BASE}/glue-board-stock/import`,
    {
      confirmClear,
      file,
    },
  );
}

export function getAdhesiveConsoleGlueBoardStockByBatch(glueBoardBatchNo: string, glueBoardModel?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.GlueBoardStock | null>(
    `${ADHESIVE_BASE}/glue-board-stock/get-by-batch`,
    { params: { glueBoardBatchNo, glueBoardModel } },
  );
}

export function createAdhesiveConsoleGlueBoardStock(data: MesHcAdhesiveConsoleApi.SaveGlueBoardStockReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/glue-board-stock/create`, data);
}

export function markAdhesiveConsoleGlueBoardStockPrinted(id: number) {
  return requestClient.post<MesHcAdhesiveConsoleApi.GlueBoardStock>(
    `${ADHESIVE_BASE}/glue-board-stock/mark-printed?id=${id}`,
  );
}

export function getAdhesiveConsoleCurrentGlueBoardUsage(planOperationId?: number, glueBoardModel?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.GlueBoardUsage | null>(`${ADHESIVE_BASE}/glue-board/current`, {
    params: { glueBoardModel, planOperationId },
  });
}

export function getAdhesiveConsoleLatestGlueBoardFai(params: {
  glueBoardBatchNo?: string;
  glueBoardModel?: string;
  glueBoardStockId?: number;
  planOperationId?: number;
  sourceProductionBatchNo?: string;
}) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary | null>(`${ADHESIVE_BASE}/glue-board-fai/latest`, {
    params,
  });
}

export function saveAdhesiveConsoleGlueBoardUsage(data: MesHcAdhesiveConsoleApi.SaveGlueBoardUsageReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/glue-board/save`, data);
}

export function reportAdhesiveConsoleGlueBoardLoss(data: MesHcAdhesiveConsoleApi.SaveGlueBoardLossReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.GlueBoardUsage>(`${ADHESIVE_BASE}/glue-board/loss`, data);
}

export function returnAdhesiveConsoleGlueBoardUsage(id: number) {
  return requestClient.post<MesHcAdhesiveConsoleApi.GlueBoardUsage>(`${ADHESIVE_BASE}/glue-board/return?id=${id}`);
}

export function submitAdhesiveConsoleAqcTask(data: MesHcAdhesiveConsoleApi.SaveAqcTaskReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.AqcTask>(`${ADHESIVE_BASE}/aqc/submit`, data);
}

export function getAdhesiveConsoleReportAqcTask(params: { batchNo: string; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.AqcTask | null>(`${ADHESIVE_BASE}/aqc/report/latest`, { params });
}

export function getAdhesiveFaiSummary(params: {
  planId: number;
  planOperationId: number;
  sourceGrindingSecondDetailId?: number;
}) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary>(`${ADHESIVE_BASE}/fai/summary`, { params });
}

export function applyAdhesiveFai(data: MesHcAdhesiveConsoleApi.FaiApplyReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.FaiSummary>(`${ADHESIVE_BASE}/fai/apply`, data);
}

export function feedbackAdhesiveConsoleAqcTask(data: MesHcAdhesiveConsoleApi.FeedbackAqcTaskReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.AqcTask>(`${ADHESIVE_BASE}/aqc/feedback`, data);
}

export function saveAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.SaveReportReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/report/save`, data);
}

export function updateAdhesiveConsoleReportTime(data: MesHcAdhesiveConsoleApi.ReportTimeUpdateReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/report/time/update`, data);
}

export function reviseAdhesiveConsoleStatisticsData(data: MesHcAdhesiveConsoleApi.StatisticsDataReviseReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/report/statistics-data/revise`, data);
}

export function completeAdhesiveConsoleWorkOrder(data: MesHcAdhesiveConsoleApi.CompleteWorkOrderReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/submit`, data);
}

export function completeAdhesiveConsoleSegment(data: MesHcAdhesiveConsoleApi.CompleteSegmentReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/segment/complete`, data);
}

export function confirmAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.ConfirmReportReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/report/confirm`, data);
}

export function markAdhesiveConsoleReportPrinted(data: MesHcAdhesiveConsoleApi.PrintReportReq) {
  return requestClient.post<boolean>(`${ADHESIVE_BASE}/report/mark-printed`, data);
}

export function getAdhesiveConsoleIntermediate(params: {
  adhesiveReportId?: number;
  batchNo?: string;
  planId: number;
  planOperationId: number;
}) {
  return requestClient.get<MesHcAdhesiveConsoleApi.IntermediateRecord>(`${ADHESIVE_BASE}/intermediate/get`, {
    params,
  });
}

export function getAdhesiveConsoleIntermediateById(recordId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.IntermediateRecord>(
    `${ADHESIVE_BASE}/intermediate/record`,
    { params: { recordId } },
  );
}

export function getAdhesiveConsoleIntermediateRecordPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcStationRecordApi.Record>>(
    `${ADHESIVE_BASE}/intermediate/page`,
    { params },
  );
}

export function saveAdhesiveConsoleIntermediate(data: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/intermediate/save`, data);
}

export function confirmAdhesiveConsoleIntermediate(data: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/intermediate/confirm`, data);
}

export function importAdhesiveConsoleIntermediate(recordId: number, file: File) {
  return requestClient.upload<number>(`${ADHESIVE_BASE}/intermediate/import`, {
    file,
    recordId,
  });
}

export function exportAdhesiveConsoleIntermediate(recordId: number) {
  return requestClient.download(`${ADHESIVE_BASE}/intermediate/export`, {
    params: { recordId },
  });
}
