import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcAdhesiveConsoleApi {
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
    glueBoardModel?: string;
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

  export interface PlanChangeoverLog {
    afterGlueBoardModel?: string;
    afterMaterialCode?: string;
    afterModelCode?: string;
    beforeGlueBoardModel?: string;
    beforeMaterialCode?: string;
    beforeModelCode?: string;
    changeoverFlag?: boolean;
    changeoverTime?: string;
    changeoverUserId?: number;
    changeoverUserName?: string;
    extraJson?: string;
    id?: number;
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    remark?: string;
  }

  export interface PlanChangeoverSaveReq {
    afterGlueBoardModel?: string;
    afterMaterialCode?: string;
    afterModelCode: string;
    beforeGlueBoardModel?: string;
    beforeMaterialCode?: string;
    beforeModelCode: string;
    extraJson?: string;
    planId: number;
    planOperationId: number;
    remark?: string;
  }

  export interface RuntimeProductSnapshot {
    changeoverInstructionId?: number;
    changeoverInstructionNo?: string;
    materialCode?: string;
    materialId?: number;
    materialName?: string;
    productModel?: string;
    sourceType?: 'CHANGEOVER_INSTRUCTION' | 'CHANGEOVER_PIECE' | 'PLAN';
    specification?: string;
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
    modelCode?: string;
    motherBatchNo?: string;
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
    actualSizeRule?: string;
    actualSizeSuffix?: string;
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
    glueBoardModel?: string;
    glueBoardStartPosition?: number;
    glueBoardUsageId?: number;
    glueBoardUseLength?: number;
    aqcStatus?: string;
    aqcTaskId?: number;
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

  export interface TailAssignReq {
    items: Array<{
      actualSizeRule: string;
      id: number;
    }>;
  }

  export interface TailSelectedAssignReq {
    planId: number;
    planOperationId: number;
    items: Array<{ sourceProductionBatchNo: string; actualSizeRule: string }>;
  }

  export interface TailBatchAssignReq {
    actualSizeRule: string;
    planId: number;
    planOperationId: number;
  }

  export interface ReportQuery {
    scanConfirmDate?: string;
  }

  export interface ChangeoverInspection {
    changeoverInstructionId?: number;
    checkItems?: CheckItem[];
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
    recorderName?: string;
    remark?: string;
    sourceSlittingSliceId?: number;
    submitTime?: string;
  }

  export interface FaiSummary {
    canWithdraw?: boolean;
    withdrawBlockedReason?: string;
    withdrawReason?: string;
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
    inspectionScene?: string;
    inspectionScopeBatchNo?: string;
    materialCode?: string;
    materialName?: string;
    productBatchNo?: string;
    productModel?: string;
    remark?: string;
    sampleLength?: number;
    sampleStartPosition?: number;
    specification?: string;
    sourceReportId?: number;
    sourceReportNo?: string;
  }

  export interface FaiApplyReq {
    coaSliceNos?: string[];
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardUsageId?: number;
    inspectionScene?: 'COA' | 'PROCESS_CHECK';
    inspectionScopeBatchNo?: string;
    materialCode?: string;
    materialName?: string;
    planId: number;
    planOperationId: number;
    productBatchNo?: string;
    productModel?: string;
    remark?: string;
    sampleLength?: number;
    sampleStartPosition?: number;
    sourceReportId?: number;
    specification?: string;
    standardMatchMode?: 'MATERIAL_PROCESS' | 'PRODUCT_MODEL_PROCESS';
    submitterName?: string;
    triggerReason?: 'NEW_ORDER' | 'REWORK_RECHECK';
  }

  export interface SourcePressSlotFaiQuery {
    motherBatchNo?: string;
    planId: number;
    planOperationId: number;
    sourcePlanId: number;
    sourcePlanOperationId: number;
  }

  export interface SaveReportReq extends Omit<ReportItem, 'planNo' | 'reportStatus'> {
    sourceGrindingSecondDetailId: number;
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
    planId?: number;
    planOperationId?: number;
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
    sourceGrindingSecondDetailId?: number;
    sourcePressSlotReportId?: number;
    sourceProductionBatchNo?: string;
  }

  export interface ConfirmReportReq {
    confirmerName?: string;
    confirmerTime?: string;
    /** 一键扫码确认时由服务端执行换型状态与型号快照校验。 */
    expectedRuntimeModelCode?: string;
    glueBoardBatchNo?: string;
    glueBoardMaterialCode?: string;
    glueBoardModel?: string;
    glueBoardUsageId?: number;
    id: number;
    oneClickBatchConfirm?: boolean;
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
    // 兼容旧粘胶2中间品字段。
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
    confirmTime?: string;
    createTime?: string;
    details?: IntermediateDetail[];
    endSliceNo?: string;
    extraJson?: string;
    fillTime?: string;
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
    glueBoardAdhesionGf?: string;
    glueBoardModel?: string;
    glueBoardWidthMm?: string;
    id?: number;
    importAttachment?: Record<string, any>;
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
    showGlueBoardModel?: boolean;
    sourceExcel?: string;
    sourceSheet?: string;
    stationFormCode?: string;
    stationFormDisplayName?: string;
    stationFormId?: number;
    stationFormName?: string;
    stationFormProcessCode?: string;
    stationFormSchemaJson?: string;
    templateCode?: string;
    templateName?: string;
    thicknessColumnCount?: number;
    thicknessHeaderText?: string;
    thicknessIntervalCm?: number;
    thicknessStandard?: string;
    updateTime?: string;
    widthEnd?: number;
    widthMiddle?: number;
    widthStart?: number;
  }

  export interface MiddleLedgerRecord {
    id?: number;
    remark?: string;
    reportDate?: string;
    reportType?: string;
    reportTypeName?: string;
    sliceBatchNo?: string;
    thickness1?: string;
    thickness2?: string;
    thickness3?: string;
    thickness4?: string;
    thickness5?: string;
    thickness6?: string;
    thickness7?: string;
    thickness8?: string;
    thickness9?: string;
    thickness10?: string;
    widthMm?: string;
  }

  export interface ProcessParamRecord {
    changeoverInstructionId?: number;
    confirmTime?: string;
    confirmUserName?: string;
    fillTime?: string;
    fillUserName?: string;
    formName?: string;
    formType?: string;
    formTypeName?: string;
    firstInspectionResult?: string;
    firstInspectionSliceNo?: string;
    id?: number;
    importAttachment?: Record<string, any>;
    inspectionScene?: string;
    inspectionTime?: string;
    items?: ProcessParamItem[];
    materialCode?: string;
    modelCode?: string;
    motherBatchNo?: string;
    parentProductionBatchNo?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    productionBatchNo?: string;
    recordStatus?: string;
    recorderName?: string;
    remark?: string;
    reportDate?: string;
    reportStatus?: string;
    sourceExcel?: string;
    statusName?: string;
  }

  export interface ProcessParamItem {
    abnormalRemark?: string;
    actualValue?: string;
    checkResult?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    itemCategory?: string;
    itemName?: string;
    modelCode?: string;
    motherBatchNo?: string;
    productionBatchNo?: string;
    recorderName?: string;
    remark?: string;
    reportDate?: string;
    requiredFlag?: boolean;
    seq?: number;
    sortNo?: number;
    sourceReportId?: number;
    standardValue?: string;
    valueMode?: string;
  }
}

const ADHESIVE_BASE = '/mes/hc/execution/formula-report/adhesive2';

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

export function switchAdhesive2ConsoleWorkOrderEquipment(data: MesHcAdhesiveConsoleApi.SwitchEquipmentReq) {
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

export function getAdhesiveConsoleSourceList(planId: number, planOperationId: number, sourceBatchNo?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.SourceItem[]>(`${ADHESIVE_BASE}/source/list`, {
    params: { planId, planOperationId, sourceBatchNo: sourceBatchNo || undefined },
  });
}

export function getAdhesiveConsoleCheckTemplate(modelCode?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.CheckItem[]>(`${ADHESIVE_BASE}/check-template`, {
    params: { modelCode },
  });
}

export function getAdhesiveConsoleReportList(
  planOperationId: number,
  query?: MesHcAdhesiveConsoleApi.ReportQuery,
) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ReportItem[]>(`${ADHESIVE_BASE}/report/list`, {
    params: { planOperationId, ...query },
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

export function feedbackAdhesiveConsoleAqcTask(data: MesHcAdhesiveConsoleApi.FeedbackAqcTaskReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.AqcTask>(`${ADHESIVE_BASE}/aqc/feedback`, data);
}

export function saveAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.SaveReportReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/report/save`, data);
}

export function assignAdhesive2ReportTails(data: MesHcAdhesiveConsoleApi.TailAssignReq) {
  return requestClient.post<number[]>(`${ADHESIVE_BASE}/report/tail-assign`, data);
}

export function assignAdhesive2TailForSelectedSources(data: MesHcAdhesiveConsoleApi.TailSelectedAssignReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/tail-selection/apply-selected`, data);
}

export function assignAdhesive2TailForAllSources(data: MesHcAdhesiveConsoleApi.TailBatchAssignReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/tail-selection/apply-all`, data);
}

export function saveAndConfirmAdhesiveConsoleReport(
  data: MesHcAdhesiveConsoleApi.SaveReportReq & Omit<MesHcAdhesiveConsoleApi.ConfirmReportReq, 'id'>,
) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/report/save-confirm`, data);
}

export function completeAdhesiveConsoleWorkOrder(data: MesHcAdhesiveConsoleApi.CompleteWorkOrderReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/submit`, data);
}

export function completeAdhesive2ConsoleSegment(data: MesHcAdhesiveConsoleApi.CompleteSegmentReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/segment/complete`, data);
}

export function confirmAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.ConfirmReportReq) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/report/confirm`, data);
}

export function setAdhesive2ReportMiddleType(id: number, reportType: string) {
  return requestClient.post<boolean>(
    `${ADHESIVE_BASE}/report/set-middle-type?id=${encodeURIComponent(String(id))}&reportType=${encodeURIComponent(reportType)}`,
    {},
  );
}

export function correctAdhesive2ReportAbnormalCategory(data: MesHcAdhesiveConsoleApi.CorrectAbnormalCategoryReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.ReportItem>(
    `${ADHESIVE_BASE}/report/abnormal-category/correct`,
    data,
  );
}

export function markAdhesiveConsoleReportPrinted(data: MesHcAdhesiveConsoleApi.PrintReportReq) {
  return requestClient.post<boolean>(`${ADHESIVE_BASE}/report/mark-printed`, data);
}

export function getAdhesiveConsoleIntermediate(params: {
  adhesiveReportId?: number;
  batchNo?: string;
  id?: number;
  planId: number;
  planOperationId: number;
  recordDate?: string;
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
  return requestClient.get<PageResult<any>>(`${ADHESIVE_BASE}/intermediate/page`, {
    params,
  });
}

export function saveAdhesiveConsoleIntermediate(data: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/intermediate/save`, data);
}

export function getAdhesive2IntermediateList(planOperationId: number, batchNo?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.IntermediateRecord[]>(`${ADHESIVE_BASE}/intermediate/list`, {
    params: { batchNo, planOperationId },
  });
}

export function getAdhesive2MiddleLedgerList(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.MiddleLedgerRecord[]>(`${ADHESIVE_BASE}/middle-ledger/list`, {
    params: { planOperationId },
  });
}

export function importAdhesive2MiddleLedger(
  planId: number,
  planOperationId: number,
  file: File,
  importAttachment?: Record<string, any>,
  options?: { batchNo?: string; recordDate?: string; recordId?: number },
) {
  return requestClient.upload<number>(`${ADHESIVE_BASE}/middle-ledger/import`, {
    file,
    ...(importAttachment ? { importAttachment: JSON.stringify(importAttachment) } : {}),
    planId,
    planOperationId,
    ...(options?.batchNo ? { batchNo: options.batchNo } : {}),
    ...(options?.recordDate ? { recordDate: options.recordDate } : {}),
    ...(options?.recordId ? { recordId: options.recordId } : {}),
  });
}

export function exportAdhesive2MiddleLedger(
  planOperationId: number,
  options?: { batchNo?: string; recordId?: number },
) {
  return requestClient.download(`${ADHESIVE_BASE}/middle-ledger/export`, {
    params: {
      planOperationId,
      ...(options?.batchNo ? { batchNo: options.batchNo } : {}),
      ...(options?.recordId ? { recordId: options.recordId } : {}),
    },
  });
}

export function getAdhesive2ProcessParamList(
  planOperationId: number,
  filters?: { formType?: string; motherBatchNo?: string; productionBatchNo?: string; reportDate?: string },
) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ProcessParamRecord[]>(`${ADHESIVE_BASE}/process-param/list`, {
    params: { planOperationId, ...filters },
  });
}

export function importAdhesive2ProcessParams(
  planId: number,
  planOperationId: number,
  file: File,
  importAttachment?: Record<string, any>,
  options?: {
    changeoverInstructionId?: number;
    formType?: string;
    inspectionScene?: string;
    motherBatchNo?: string;
    productionBatchNo?: string;
    recordDate?: string;
    recordId?: number;
  },
) {
  return requestClient.upload<number>(`${ADHESIVE_BASE}/process-param/import`, {
    file,
    ...(options?.changeoverInstructionId ? { changeoverInstructionId: options.changeoverInstructionId } : {}),
    ...(options?.formType ? { formType: options.formType } : {}),
    ...(options?.inspectionScene ? { inspectionScene: options.inspectionScene } : {}),
    ...(importAttachment ? { importAttachment: JSON.stringify(importAttachment) } : {}),
    ...(options?.motherBatchNo ? { motherBatchNo: options.motherBatchNo } : {}),
    ...(options?.productionBatchNo ? { productionBatchNo: options.productionBatchNo } : {}),
    ...(options?.recordDate ? { recordDate: options.recordDate } : {}),
    ...(options?.recordId ? { recordId: options.recordId } : {}),
    planId,
    planOperationId,
  });
}

export function saveAdhesive2ProcessParam(data: MesHcAdhesiveConsoleApi.ProcessParamRecord) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/process-param/save`, data);
}

export function confirmAdhesive2ProcessParam(data: MesHcAdhesiveConsoleApi.ProcessParamRecord) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/process-param/confirm`, data);
}

export function deleteAdhesive2ProcessParam(recordId: number, planOperationId: number) {
  return requestClient.delete<boolean>(
    `${ADHESIVE_BASE}/process-param/delete?recordId=${recordId}&planOperationId=${planOperationId}`,
  );
}

export function exportAdhesive2ProcessParams(
  planOperationId: number,
  options?: { formType?: string; motherBatchNo?: string; recordId?: number },
) {
  return requestClient.download(`${ADHESIVE_BASE}/process-param/export`, {
    params: {
      formType: options?.formType,
      motherBatchNo: options?.motherBatchNo,
      planOperationId,
      recordId: options?.recordId,
    },
  });
}

export function getAdhesive2ChangeoverLatest(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ChangeoverInspection | null>(`${ADHESIVE_BASE}/changeover/latest`, {
    params: { planOperationId },
  });
}

export function getAdhesive2ChangeoverList(planOperationId: number, motherSegmentBatchNo?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ChangeoverInspection[]>(`${ADHESIVE_BASE}/changeover/list`, {
    params: { motherSegmentBatchNo, planOperationId },
  });
}

export function saveAdhesive2Changeover(data: MesHcAdhesiveConsoleApi.ChangeoverInspection) {
  return requestClient.post<number>(`${ADHESIVE_BASE}/changeover/save`, data);
}

export function getAdhesive2PlanChangeoverLatest(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.PlanChangeoverLog | null>(
    `${ADHESIVE_BASE}/plan-changeover/latest`,
    { params: { planOperationId } },
  );
}

export function getAdhesive2PlanChangeoverList(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.PlanChangeoverLog[]>(
    `${ADHESIVE_BASE}/plan-changeover/list`,
    { params: { planOperationId } },
  );
}

export function saveAdhesive2PlanChangeover(data: MesHcAdhesiveConsoleApi.PlanChangeoverSaveReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.PlanChangeoverLog>(
    `${ADHESIVE_BASE}/plan-changeover/save`,
    data,
  );
}

export function getAdhesive2RuntimeProductSnapshot(params: {
  planId: number;
  planOperationId: number;
  segmentBatchNo?: string;
}) {
  return requestClient.get<MesHcAdhesiveConsoleApi.RuntimeProductSnapshot>(
    `${ADHESIVE_BASE}/runtime-product-snapshot`,
    { params },
  );
}

export function getAdhesive2FaiSummary(params: { planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary>(`${ADHESIVE_BASE}/fai/summary`, { params });
}

export function getAdhesive2CoaFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary[]>(`${ADHESIVE_BASE}/fai/coa-list`, { params });
}

export function getAdhesive2ProcessCheckFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary[]>(`${ADHESIVE_BASE}/fai/process-check-list`, { params });
}

export function getAdhesive2SourcePressSlotFaiList(params: MesHcAdhesiveConsoleApi.SourcePressSlotFaiQuery) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary[]>(`${ADHESIVE_BASE}/source-press-slot/fai/list`, { params });
}

export function getAdhesive2SourcePressSlotProcessCheckFaiList(params: MesHcAdhesiveConsoleApi.SourcePressSlotFaiQuery) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary[]>(`${ADHESIVE_BASE}/source-press-slot/fai/process-check-list`, { params });
}

export function applyAdhesive2Fai(data: MesHcAdhesiveConsoleApi.FaiApplyReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.FaiSummary>(`${ADHESIVE_BASE}/fai/apply`, data);
}

/**
 * 以粘胶2成品报工为来源提交 COA；草稿记录会在后端事务内先完成扫码确认。
 */
export function applyAdhesive2PostConfirmCoa(data: MesHcAdhesiveConsoleApi.ConfirmReportReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.FaiSummary>(`${ADHESIVE_BASE}/fai/post-confirm-coa`, data);
}

export function withdrawAdhesive2Coa(data: { planId: number; planOperationId: number; faiId: number; withdrawReason: string }) {
  return requestClient.post<boolean>(`${ADHESIVE_BASE}/fai/coa-withdraw`, data);
}
