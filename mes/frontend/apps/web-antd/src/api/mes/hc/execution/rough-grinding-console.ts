import type { GrindingConsumption } from '#/api/mes/hc/grinding-consumption';
import { requestClient } from '#/api/request';

export namespace MesHcRoughGrindingConsoleApi {
  export type FirstAllocationMode = 'FIRST_ALLOCATED' | 'ORIGINAL';

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

  export interface BoardQuery {
    equipmentId?: number;
    planId?: number;
    planOperationId?: number;
    recordDate?: string;
  }

  export interface EquipmentOption {
    code?: string;
    label: string;
    name?: string;
    value: number;
    workCenterCode?: string;
    workCenterId?: number;
    workCenterName?: string;
    workStatus?: string;
  }

  export interface EquipmentOptionQuery {
    planOperationId?: number;
    workCenterId?: number;
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

  export interface PassWorkRow {
    confirmRemark?: string;
    canConfirm?: boolean;
    canFill?: boolean;
    canView?: boolean;
    confirmer?: string;
    confirmerTime?: string;
    details?: PassWorkItem[];
    formCode: string;
    formId?: number;
    formRemark?: string;
    headerDataJson?: string;
    id?: string;
    inspectionResult?: string;
    name?: string;
    presetDetails?: PassWorkItem[];
    presetHeaderDataJson?: string;
    recordDate?: string;
    recorder?: string;
    recorderTime?: string;
    recordId?: number;
    result?: string;
    schemaJson?: string;
    status?: string;
    timing?: string;
  }

  export interface ConsumableState {
    batchNo?: string;
    consumableType?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    id?: number;
    lastEventTime?: string;
    lastOperatorName?: string;
    lastReplacePlanNo?: string;
    lastReplaceReason?: string;
    lastReplaceTime?: string;
    limitCount?: number;
    limitLength?: number;
    status?: string;
    useCount?: number;
    usedLength?: number;
    warningFlag?: number;
  }

  export interface SourceBalance {
    availableLength?: number;
    balanceKey?: string;
    id?: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    modelName?: string;
    remark?: string;
    reservedLength?: number;
    sourceBatchNo?: string;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    sourceProductionBatchNo?: string;
    sourceType?: string;
    status?: string;
    totalLength?: number;
    usedFirstLength?: number;
    usedSecondLength?: number;
  }

  export interface FirstReport {
    consumption?: GrindingConsumption;
    availableAfter?: number;
    availableBefore?: number;
    defectCode?: string;
    detailStatus?: string;
    endTime?: string;
    id?: number;
    lossLength?: number;
    motherBatchNo?: string;
    napSampleLength?: number;
    outputLength?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    pressure?: string;
    processLength?: number;
    remark?: string;
    rowUid?: string;
    sandpaperBatchNo?: string;
    selfCheck?: string;
    sourcePlanNo?: string;
    sourceProductionBatchNo?: string;
    startTime?: string;
    middleProductGeneratedLength?: number;
    middleProductRecordId?: number;
    middleProductRecordTime?: string;
    middleProductRecorder?: string;
    middleProductStatus?: string;
    segmentTimings?: SegmentTiming[];
  }

  /** 一磨前置分段处理：仅确认米数、耗材和工艺点检，不承载损耗、异常或留样。 */
  export interface FirstAllocation {
    consumption?: GrindingConsumption;
    checkRecordId?: number;
    confirmedLength?: number;
    detailStatus?: string;
    endTime?: string;
    firstDetailId?: number;
    guideClothBatchNo?: string;
    guideClothLife?: number;
    guideClothStateId?: number;
    id?: number;
    operatorId?: number;
    operatorName?: string;
    productionBatchNo?: string;
    remark?: string;
    sandpaperBatchNo?: string;
    sandpaperLife?: number;
    sandpaperStateId?: number;
    segmentMark: 'NONE' | 'P' | 'Q' | 'R' | 'S' | string;
    startPosition?: number;
    startTime?: string;
  }

  export interface FirstAllocationSaveReq {
    consumption?: GrindingConsumption;
    confirmedLength: number;
    currentGuideClothBatchNo?: string;
    currentSandpaperBatchNo?: string;
    equipmentId: number;
    guideClothBatchNo?: string;
    guideClothChanged?: boolean;
    guideClothReplaceReason?: string;
    motherBatchNo: string;
    operatorId?: number;
    operatorName?: string;
    planId: number;
    planNo?: string;
    planOperationId: number;
    processFormRecordId: number;
    processCheckDetails?: PassWorkItem[];
    processCheckHeaderDataJson?: string;
    sandpaperBatchNo?: string;
    sandpaperChanged?: boolean;
    sandpaperReplaceReason?: string;
    segmentMark: 'NONE' | 'P' | 'Q' | 'R' | 'S';
    startPosition: number;
  }

  export interface FirstAllocationQuantityReviseReq {
    confirmedLength: number;
    firstAllocationId: number;
    reason: string;
  }

  export interface StatisticsDataReviseReq {
    confirmedLength?: number;
    firstAllocationId?: number;
    id?: number;
    lossLength?: number;
    outputLength?: number;
    processLength?: number;
    reason: string;
    recordRole?: 'FIRST_ALLOCATION' | 'FIRST_ORIGINAL' | 'SECOND' | string;
  }

  /** 一磨、二磨均按 P/Q/R/S/NONE 独立记录开工完工事实。 */
  export interface SegmentTiming {
    endOperatorName?: string;
    endTime?: string;
    firstDetailId?: number;
    id?: number;
    passType: 'FIRST' | 'SECOND' | string;
    secondDetailId?: number;
    segmentBatchNo?: string;
    segmentMark: 'NONE' | 'P' | 'Q' | 'R' | 'S' | string;
    startOperatorName?: string;
    startTime?: string;
    qtime?: QtimeInfo;
  }

  export interface SegmentTimingStampReq {
    action: 'END' | 'START';
    operatorId?: number;
    operatorName?: string;
    passType: 'FIRST' | 'SECOND';
    planId: number;
    planOperationId: number;
    segmentMark: 'NONE' | 'P' | 'Q' | 'R' | 'S';
  }

  export interface SecondReport extends FirstReport {
    researchConsumptionLength?: number;
    confirmStatus?: string;
    firstDetailId?: number;
    firstAllocationId?: number;
    inspectionApplyTime?: string;
    inspectionId?: number;
    inspectionNo?: string;
    inspectionRejectReason?: string;
    inspectionResult?: string;
    inspectionReturnTime?: string;
    inspectionStatus?: string;
    lastPrintTime?: string;
    parentProductionBatchNo?: string;
    printCount?: number;
    printStatus?: string;
    productionBatchNo?: string;
    segmentMark?: string;
    sourceRowUid?: string;
    startPosition?: number;
  }

  export interface BoardResp {
    allocationMode?: FirstAllocationMode;
    consumables?: ConsumableState[];
    dailyChecks?: PassWorkRow[];
    dailyCleaningDone?: boolean;
    dailyStartupDone?: boolean;
    firstLossLength?: number;
    firstNapSampleLength?: number;
    firstOutputLength?: number;
    firstProcessLength?: number;
    firstReports?: FirstReport[];
    firstAllocationMode?: FirstAllocationMode;
    firstAllocationModeLocked?: boolean;
    firstAllocations?: FirstAllocation[];
    firstSegmentTimings?: SegmentTiming[];
    pendingSecondLength?: number;
    secondLossLength?: number;
    secondNapSampleLength?: number;
    secondResearchConsumptionLength?: number;
    secondOutputLength?: number;
    secondProcessLength?: number;
    secondReports?: SecondReport[];
    secondSegmentTimings?: SegmentTiming[];
    sourceBalances?: SourceBalance[];
    task?: any;
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
  }

  export interface FaiApplyReq {
    planId: number;
    planOperationId: number;
    remark?: string;
    standardMatchMode?: 'MATERIAL_PROCESS' | 'PROCESS' | 'PRODUCT_MODEL_PROCESS';
    submitterName?: string;
    triggerReason?: 'NEW_ORDER' | 'REWORK_RECHECK';
  }

  export interface SecondSegmentInspectionSummary {
    displayText?: string;
    inspectionApplyTime?: string;
    inspectionId?: number;
    inspectionNo?: string;
    inspectionRejectReason?: string;
    inspectionResult?: string;
    inspectionReturnTime?: string;
    inspectionStatus?: string;
    motherBatchNo?: string;
    parentBatchNo?: string;
    processCategory?: string;
    productBatchNo?: string;
    productionBatchNo?: string;
    reapplyAllowed?: boolean;
    sampleType?: string;
    sampleTypeName?: string;
    sampleLength?: number;
    secondDetailId?: number;
    segmentMark?: string;
    sourceModule?: string;
    sourceReportId?: number;
    sourceReportNo?: string;
  }

  export interface SecondSegmentInspectionApplyReq {
    remark?: string;
    sampleLength?: number;
    secondDetailId: number;
    submitterName?: string;
  }

  export interface DailyCheckSaveReq {
    confirmer?: string;
    confirmerTime?: string;
    details?: PassWorkItem[];
    equipmentCode?: string;
    equipmentId: number;
    equipmentName?: string;
    formCode: string;
    formRemark?: string;
    headerDataJson?: string;
    inspectionResult?: string;
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    recordDate: string;
    recorder?: string;
    recorderTime?: string;
    recordId?: number;
    result?: string;
    workCenterCode?: string;
    workCenterId?: number;
    workCenterName?: string;
  }

  export interface ConsumableReplaceReq {
    consumption?: GrindingConsumption;
    batchNo: string;
    consumableType: string;
    equipmentCode?: string;
    equipmentId: number;
    equipmentName?: string;
    initialUseCount?: number;
    initialUsedLength?: number;
    limitCount?: number;
    limitLength?: number;
    operationCode?: string;
    operationName?: string;
    operatorId?: number;
    operatorName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    replaceReason?: string;
    replaceTime?: string;
    stateId?: number;
    workCenterCode?: string;
    workCenterId?: number;
    workCenterName?: string;
  }

  export interface ReportSaveReq {
    consumption?: GrindingConsumption;
    afterGrindingThickness?: string;
    abnormalPositions?: AbnormalPositionSaveReq[];
    availableAfter?: number;
    availableBefore?: number;
    defectCode?: string;
    endTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    firstDetailId?: number;
    firstAllocationId?: number;
    grindingThickness?: string;
    currentGuideClothBatchNo?: string;
    currentSandpaperBatchNo?: string;
    id?: number;
    lineSpeed?: string;
    lossLength?: number;
    meterCounter?: string;
    middleProductDetails?: MiddleProductItem[];
    middleProductHeaderDataJson?: string;
    motherBatchNo: string;
    napSampleLength?: number;
    researchConsumptionLength?: number;
    operatorId?: number;
    operatorName?: string;
    outputLength?: number;
    parentProductionBatchNo?: string;
    planId: number;
    planNo?: string;
    planOperationId: number;
    processFormRecordId?: number;
    pressure?: string;
    processCheckDetails?: PassWorkItem[];
    processCheckHeaderDataJson?: string;
    processLength?: number;
    productionBatchNo?: string;
    qualityThickness?: string;
    qualityWidth?: string;
    remainLength?: number;
    remainStartMeter?: number;
    remark?: string;
    reportDate?: string;
    rotationSpeed?: string;
    guideClothBatchNo?: string;
    guideClothChanged?: boolean;
    guideClothReplaceReason?: string;
    sandpaperBatchNo?: string;
    sandpaperChanged?: boolean;
    sandpaperReplaceReason?: string;
    segmentMark?: string;
    selfCheck?: string;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    sourceProductionBatchNo?: string;
    sourceType?: string;
    startPosition?: number;
    startTime?: string;
    workCenterCode?: string;
    workCenterId?: number;
    workCenterName?: string;
  }

  export interface ReportTimeUpdateReq {
    endTime?: string;
    id: number;
    passType: 'FIRST' | 'SECOND' | string;
    reportDate?: string;
    startTime?: string;
  }

  export interface AbnormalPositionSaveReq {
    abnormalLength?: number;
    positionText: string;
    remark?: string;
    sortOrder?: number;
  }

  export interface AbnormalPositionItem extends AbnormalPositionSaveReq {
    batchNo?: string;
    createTime?: string;
    id?: number;
    operationCode?: string;
    operationName?: string;
    operationReportId?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    processStage?: string;
    productionBatchNo?: string;
    sourceDetailId?: number;
    sourceMenuCode?: string;
    sourcePlanNo?: string;
    sourceRowUid?: string;
  }

  export interface MiddleProductItem {
    batchNo?: string;
    grindingPass?: string;
    guideClothBatchNo?: string;
    guideClothLife?: number | string;
    innerThickness?: string;
    inputLength?: number | string;
    length?: number | string;
    lengthMeter?: number | string;
    materialCode?: string;
    modelCode?: string;
    outerThickness?: string;
    outputLength?: number | string;
    recorderName?: string;
    recordDate?: string;
    remark?: string;
    replaceReason?: string;
    result?: string;
    sandpaperBatchNo?: string;
    sandpaperLife?: number | string;
    seq?: number;
    thickness?: string;
    width?: string;
  }

  export interface MiddleProductRecord {
    details?: MiddleProductItem[];
    docStatus?: string;
    formCode?: string;
    formName?: string;
    generatedLength?: number;
    headerDataJson?: string;
    materialCode?: string;
    materialName?: string;
    motherBatchNo?: string;
    motherModelCode?: string;
    motherModelName?: string;
    passName?: string;
    passType?: string;
    processLength?: number | string;
    productionBatchNo?: string;
    recordDate?: string;
    recordId?: number;
    recorder?: string;
    recorderTime?: string;
    confirmer?: string;
    confirmerTime?: string;
    resultStatus?: string;
    segmentMark?: string;
    segmentName?: string;
    segmentTotalLength?: number;
    widthMm?: number | string;
  }

  export interface MiddleProductRecordSaveReq {
    details?: MiddleProductItem[];
    headerDataJson?: string;
    recordId: number;
  }

  export interface MiddleProductSegmentReq {
    passType?: string;
    planOperationId: number;
    segmentMark?: string;
  }

  export interface WorkOrderStartReq {
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

  export interface WorkOrderSwitchEquipmentReq {
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    planId: number;
    planOperationId: number;
  }

  export interface WorkOrderCompleteReq {
    batchNo?: string;
    confirmerName?: string;
    confirmerTime?: string;
    endTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    extraJson?: string;
    firstLossLength?: number;
    firstNapSampleLength?: number;
    firstOutputLength?: number;
    firstProcessLength?: number;
    inputLength?: number;
    planId: number;
    planOperationId: number;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    reportDate?: string;
    reportQty?: number;
    secondLossLength?: number;
    secondNapSampleLength?: number;
    secondOutputLength?: number;
    secondProcessLength?: number;
    startTime?: string;
  }

  export interface SecondReportConfirmReq {
    confirmerName?: string;
    confirmerTime?: string;
    forcePostWip?: boolean;
    id: number;
    scannedBatchNo: string;
  }

  export interface SecondReportPrintReq {
    id: number;
    lastPrintTime?: string;
    printCount?: number;
    printStatus?: string;
  }
}

const CONSOLE_BASE = '/mes/hc/execution/rough-grinding-console';

export function getRoughGrindingConsoleBoard(
  params: MesHcRoughGrindingConsoleApi.BoardQuery,
) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.BoardResp>(
    `${CONSOLE_BASE}/board`,
    { params },
  );
}

export function getRoughGrindingConsoleFaiSummary(params: {
  planId: number;
  planOperationId: number;
}) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.FaiSummary>(
    `${CONSOLE_BASE}/fai/summary`,
    { params },
  );
}

export function applyRoughGrindingConsoleFai(
  data: MesHcRoughGrindingConsoleApi.FaiApplyReq,
) {
  return requestClient.post<MesHcRoughGrindingConsoleApi.FaiSummary>(
    `${CONSOLE_BASE}/fai/apply`,
    data,
  );
}

export function getRoughGrindingConsoleSecondSegmentInspectionSummary(params: {
  secondDetailId: number;
}) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.SecondSegmentInspectionSummary>(
    `${CONSOLE_BASE}/second-segment-inspection/summary`,
    { params },
  );
}

export function applyRoughGrindingConsoleSecondSegmentInspection(
  data: MesHcRoughGrindingConsoleApi.SecondSegmentInspectionApplyReq,
) {
  return requestClient.post<MesHcRoughGrindingConsoleApi.SecondSegmentInspectionSummary>(
    `${CONSOLE_BASE}/second-segment-inspection/apply`,
    data,
  );
}

export function getRoughGrindingConsoleAbnormalPositionList(params: {
  motherBatchNo: string;
}) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.AbnormalPositionItem[]>(
    `${CONSOLE_BASE}/abnormal-position/list`,
    { params },
  );
}

export function getRoughGrindingConsoleEquipmentOptions(
  params: MesHcRoughGrindingConsoleApi.EquipmentOptionQuery,
) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.EquipmentOption[]>(
    `${CONSOLE_BASE}/equipment-options`,
    { params },
  );
}

export function saveRoughGrindingConsoleDailyCheck(
  data: MesHcRoughGrindingConsoleApi.DailyCheckSaveReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/daily-check/save`, data);
}

export function startRoughGrindingConsoleWorkOrder(
  data: MesHcRoughGrindingConsoleApi.WorkOrderStartReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/start`, data);
}

export function switchRoughGrindingConsoleWorkOrderEquipment(
  data: MesHcRoughGrindingConsoleApi.WorkOrderSwitchEquipmentReq,
) {
  return requestClient.post<boolean>(`${CONSOLE_BASE}/switch-equipment`, data);
}

export function completeRoughGrindingConsoleWorkOrder(
  data: MesHcRoughGrindingConsoleApi.WorkOrderCompleteReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/complete`, data);
}

export function confirmRoughGrindingConsoleDailyCheck(
  data: MesHcRoughGrindingConsoleApi.DailyCheckSaveReq,
) {
  return requestClient.post<number>(
    `${CONSOLE_BASE}/daily-check/confirm`,
    data,
  );
}

export function replaceRoughGrindingConsoleConsumable(
  data: MesHcRoughGrindingConsoleApi.ConsumableReplaceReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/consumable/replace`, data);
}

export function scanRoughGrindingConsoleSource(batchNo: string) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.SourceBalance>(
    `${CONSOLE_BASE}/source/scan`,
    {
      params: { batchNo },
    },
  );
}

export function saveRoughGrindingConsoleFirstAllocation(
  data: MesHcRoughGrindingConsoleApi.FirstAllocationSaveReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/first-allocation/save`, data);
}

export function reviseRoughGrindingConsoleFirstAllocationQuantity(
  data: MesHcRoughGrindingConsoleApi.FirstAllocationQuantityReviseReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/first-allocation/quantity/revise`, data);
}

export function reviseRoughGrindingConsoleStatisticsData(
  data: MesHcRoughGrindingConsoleApi.StatisticsDataReviseReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/statistics-data/revise`, data);
}

export function deleteRoughGrindingConsoleFirstAllocation(id: number) {
  return requestClient.post<boolean>(`${CONSOLE_BASE}/first-allocation/delete`, null, {
    params: { id },
  });
}

export function saveRoughGrindingConsoleSecondReport(
  data: MesHcRoughGrindingConsoleApi.ReportSaveReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/second-report/save`, data);
}

export function stampRoughGrindingConsoleSegmentTiming(
  data: MesHcRoughGrindingConsoleApi.SegmentTimingStampReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/segment-timing/stamp`, data);
}

export function updateRoughGrindingConsoleReportTime(
  data: MesHcRoughGrindingConsoleApi.ReportTimeUpdateReq,
) {
  return requestClient.post<number>(`${CONSOLE_BASE}/report/time/update`, data);
}

export function deleteRoughGrindingConsoleFirstReport(id: number) {
  return requestClient.post<boolean>(`${CONSOLE_BASE}/first-report/delete`, null, {
    params: { id },
  });
}

export function deleteRoughGrindingConsoleSecondReport(id: number) {
  return requestClient.post<boolean>(`${CONSOLE_BASE}/second-report/delete`, null, {
    params: { id },
  });
}

export function confirmRoughGrindingConsoleSecondReport(
  data: MesHcRoughGrindingConsoleApi.SecondReportConfirmReq,
) {
  return requestClient.post<number>(
    `${CONSOLE_BASE}/second-report/confirm`,
    data,
  );
}

export function markRoughGrindingConsoleSecondReportPrinted(
  data: MesHcRoughGrindingConsoleApi.SecondReportPrintReq,
) {
  return requestClient.post<number>(
    `${CONSOLE_BASE}/second-report/mark-printed`,
    data,
  );
}

export function getRoughGrindingConsoleMiddleProductRecord(recordId: number) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.MiddleProductRecord>(
    `${CONSOLE_BASE}/middle-product-record/get`,
    {
      params: { recordId },
    },
  );
}

export function getRoughGrindingConsoleMiddleProductRecordPage(params: Record<string, any>) {
  return requestClient.get<{
    list: any[];
    total: number;
  }>(`${CONSOLE_BASE}/middle-product-record/page`, {
    params,
  });
}

export function getRoughGrindingConsoleMiddleProductRecordByStation(
  stationRecordId: number,
) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.MiddleProductRecord>(
    `${CONSOLE_BASE}/middle-product-record/station`,
    {
      params: { stationRecordId },
    },
  );
}

export function getOrInitRoughGrindingConsoleSegmentMiddleProductRecord(
  params: MesHcRoughGrindingConsoleApi.MiddleProductSegmentReq,
) {
  return requestClient.get<MesHcRoughGrindingConsoleApi.MiddleProductRecord>(
    `${CONSOLE_BASE}/middle-product-record/segment`,
    {
      params,
    },
  );
}

export function saveRoughGrindingConsoleMiddleProductRecord(
  data: MesHcRoughGrindingConsoleApi.MiddleProductRecordSaveReq,
) {
  return requestClient.post<number>(
    `${CONSOLE_BASE}/middle-product-record/save`,
    data,
  );
}

export function confirmRoughGrindingConsoleMiddleProductRecord(
  data: MesHcRoughGrindingConsoleApi.MiddleProductRecordSaveReq,
) {
  return requestClient.post<number>(
    `${CONSOLE_BASE}/middle-product-record/confirm`,
    data,
  );
}
