import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

import type { MesHcAdhesiveConsoleApi as BaseApi } from './adhesive-console';

export namespace MesHcAdhesiveConsoleApi {
  export type AqcTask = BaseApi.AqcTask;
  export type CheckItem = BaseApi.CheckItem;
  export type CompleteWorkOrderReq = BaseApi.CompleteWorkOrderReq;
  export type ConfirmReportReq = BaseApi.ConfirmReportReq;
  export type GlueBoardStock = BaseApi.GlueBoardStock;
  export type GlueBoardUsage = BaseApi.GlueBoardUsage;
  export interface SegmentCompleteReq extends CompleteWorkOrderReq {
    sourceBatchNo?: string;
    sourceProductionBatchNo?: string;
  }
  export interface ConsumableStatus {
    availableCount?: number;
    availableQuantity?: number;
    batchNo?: string;
    consumableType: 'BEARING' | 'PRESS_ROLLER' | string;
    consumableTypeName?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    lastEventTime?: string;
    lastCleanTime?: string;
    lastOperatorId?: number;
    lastOperatorName?: string;
    lastReplaceTime?: string;
    limitCount?: number;
    limitDays?: number;
    materialCode?: string;
    materialName?: string;
    message?: string;
    onlineQuantity?: number;
    stateId?: number;
    status?: string;
    stockId?: number;
    useDays?: number;
    useCount?: number;
    warningFlag?: number;
  }
  export interface ConsumableMaterialOption {
    baseUom?: string;
    id?: number;
    materialCategoryName?: string;
    materialCode?: string;
    materialName?: string;
    materialShortName?: string;
    specModel?: string;
    stockUom?: string;
  }
  export interface ReplaceConsumableReq {
    batchNo?: string;
    consumableType: 'BEARING' | 'PRESS_ROLLER' | string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    materialCode?: string;
    materialName?: string;
    operatorId?: number;
    operatorName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    initialUseCount?: number;
    replaceQuantity?: number;
    replaceReason?: string;
    replaceTime?: string;
    stockId?: number;
  }
  export interface CleanConsumableReq {
    batchNo?: string;
    cleanRemark?: string;
    cleanTime?: string;
    consumableType: 'PRESS_ROLLER' | string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    initialUseCount?: number;
    materialCode?: string;
    materialName?: string;
    operatorId?: number;
    operatorName?: string;
    planOperationId?: number;
    receiveQuantity?: number;
  }
  export interface IntermediateDetail {
    id?: number;
    pressSlotReportId?: number;
    sourceSlittingSliceId?: number;
    samplePosition?: string;
    samplePositionName?: string;
    sliceBatchNo?: string;
    widthMm?: number;
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
    remark?: string;
    sortNo?: number;
    seq?: number;
    // 兼容旧粘胶中间品字段，避免历史模板代码残留时报类型错误。
    lengthMark?: number;
    leftThickness?: number;
    rightThickness?: number;
  }
  export interface IntermediateRecord {
    id?: number;
    planId: number;
    planNo?: string;
    planOperationId: number;
    recordDate?: string;
    productionDate?: string;
    modelCode?: string;
    materialCode?: string;
    batchNo?: string;
    templateCode?: string;
    templateName?: string;
    sourceExcel?: string;
    sourceSheet?: string;
    stationFormCode?: string;
    stationFormDisplayName?: string;
    stationFormId?: number;
    stationFormName?: string;
    stationFormProcessCode?: string;
    stationFormSchemaJson?: string;
    slotDepthStandard?: string;
    thicknessStandard?: string;
    thicknessHeaderText?: string;
    thicknessColumnCount?: number;
    thicknessIntervalCm?: number;
    showGlueBoardModel?: boolean;
    firstSampleSliceNo?: string;
    frontSliceNo?: string;
    middleSliceNo?: string;
    endSliceNo?: string;
    confirmTime?: string;
    createTime?: string;
    fillTime?: string;
    inputQty?: number;
    outputQty?: number;
    updateTime?: string;
    firstSlotDepthMin?: number;
    firstSlotDepthMax?: number;
    firstSlotDepthAvg?: number;
    firstSlotDepthXMin?: number;
    firstSlotDepthXMax?: number;
    firstSlotDepthXAvg?: number;
    firstSlotDepthYMin?: number;
    firstSlotDepthYMax?: number;
    firstSlotDepthYAvg?: number;
    recorderName?: string;
    confirmerName?: string;
    recordStatus?: string;
    remark?: string;
    extraJson?: string;
    details?: IntermediateDetail[];
    // 兼容旧粘胶中间品字段。
    adhesiveReportId?: number;
    processLength?: number;
    widthStart?: number;
    widthMiddle?: number;
    widthEnd?: number;
  }
  export interface ChangeoverInspection {
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
    faiId?: number;
    faiNo?: string;
    faiStatus?:
      | 'CANCELED'
      | 'COMPLETED'
      | 'INSPECTING'
      | 'PENDING'
      | 'REJECTED'
      | 'REWORKING'
      | 'SUSPENDED'
      | 'WAITING_QA';
    faiJudgment?: 'NG' | 'OK' | 'PENDING' | 'WAIT_QA';
    /** PROCESS_CHECK_NG_RESTART：过程加检 NG 后重新首检；ADDITIONAL_FIRST_INSPECTION：追加普通首检 */
    triggerReason?: 'NEW_ORDER' | 'REWORK_RECHECK' | 'PROCESS_CHECK_NG_RESTART' | 'ADDITIONAL_FIRST_INSPECTION';
    faiStandardId?: number;
    faiStandardNo?: string;
    faiStandardVersion?: string;
    faiApplyTime?: string;
    faiReturnTime?: string;
    inspectionScene?: 'ABNORMAL_RELEASE' | 'PROCESS_CHECK';
    sourceReportId?: number;
    sourceReportNo?: string;
    productBatchNo?: string;
    faiRejectReason?: string;
    inspectionDate?: string;
    inspectionScopeBatchNo?: string;
    todayInspected?: boolean;
    /** 当前压槽首检管控范围内，是否存在已完成且判定 OK 的首检单 */
    overallQualified?: boolean;
    restartRequired?: boolean;
    displayText?: string;
    remark?: string;
    allowReportSubmit?: boolean;
    records?: Array<FaiSummary & { isCurrent?: boolean }>;
    reinspectionReason?: string;
    requiresReinspection?: boolean;
  }
  export interface FaiApplyReq {
    planId: number;
    planOperationId: number;
    standardMatchMode?: 'MATERIAL' | 'MATERIAL_PROCESS' | 'PROCESS' | 'PRODUCT_MODEL_PROCESS';
    inspectionScene?: 'ABNORMAL_RELEASE' | 'PROCESS_CHECK';
    inspectionScopeBatchNo?: string;
    sourceReportId?: number;
    sourceReportNo?: string;
    materialId?: number;
    /** 母料料号，仅用于展示追溯；压槽首检标准匹配使用产品型号 + 工序 */
    materialCode?: string;
    materialName?: string;
    productBatchNo?: string;
    specification?: string;
    triggerReason?: 'NEW_ORDER' | 'REWORK_RECHECK' | 'PROCESS_CHECK_NG_RESTART' | 'ADDITIONAL_FIRST_INSPECTION';
    submitterName?: string;
    remark?: string;
  }
  export interface FaiWithdrawReq {
    faiId: number;
    planId: number;
    planOperationId: number;
    withdrawReason: string;
  }
  export interface PressSlotScanGateResp {
    allowScan?: boolean;
    warningMessage?: string;
    confirmedAfterInspectionCount?: number;
    warningThresholdCount?: number;
    blockThresholdCount?: number;
  }
  export interface PressSlotAbnormalLockRecord {
    abnormalFaiId?: number;
    abnormalFaiNo?: string;
    abnormalFeedbackTime?: string;
    abnormalInspectorName?: string;
    abnormalResult?: string;
    abnormalSampleBatchNo?: string;
    abnormalSubmitTime?: string;
    confirmTime?: string;
    confirmerName?: string;
    id?: number;
    lockId?: number;
    locked?: boolean;
    lockStartTime?: string;
    lockStatus?: 'LOCKED' | 'RELEASED' | string;
    motherBatchNo?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    productionBatchNo?: string;
    releaseFaiId?: number;
    releaseFaiNo?: string;
    releaseFeedbackTime?: string;
    releaseInspectorName?: string;
    releaseOpinion?: string;
    releaseResult?: string;
    releaseSampleBatchNo?: string;
    releaseSubmitTime?: string;
    releaseTime?: string;
    reportId?: number;
  }
  export interface ProcessParamRecord {
    faiId?: number;
    templateId?: number;
    headerData?: Record<string, any>;
    runtimeSchema?: Record<string, any>;
    id?: number;
    confirmTime?: string;
    confirmUserName?: string;
    fillTime?: string;
    fillUserName?: string;
    formType?: string;
    formTypeName?: string;
    formName?: string;
    firstInspectionResult?: string;
    firstInspectionSliceNo?: string;
    importAttachment?: Record<string, any>;
    inspectionScene?: 'FIRST_INSPECTION' | 'PROCESS_CHECK' | string;
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
    statusName?: string;
    sourceExcel?: string;
    temperature1?: string;
    temperature2?: string;
    temperature3?: string;
    temperature4?: string;
    temperature5?: string;
  }
  export interface ProcessParamItem {
    templateItemId?: number;
    fieldKey?: string;
    actualValue2?: string;
    abnormalRemark?: string;
    actualValue?: string;
    checkResult?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    confirmerName?: string;
    endTime?: string;
    environmentHumidity?: string;
    environmentTemperature?: string;
    itemCategory?: string;
    itemName?: string;
    measuredTemperature1?: string;
    measuredTemperature2?: string;
    measuredTemperature3?: string;
    measuredTemperature4?: string;
    measuredTemperature5?: string;
    modelCode?: string;
    motherBatchNo?: string;
    pressSlotOrder?: string;
    pressSlotSize?: string;
    pressSlotSpeed?: string;
    productionBatchNo?: string;
    recorderName?: string;
    remark?: string;
    reportDate?: string;
    rollerGap?: string;
    seq?: number;
    setTemperature?: string;
    sourceReportId?: number;
    sortNo?: number;
    startTime?: string;
    standardValue?: string;
    thickness?: string;
    valueMode?: string;
    widthM?: number | string;
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
  export interface FormRecord {
    confirmTime?: string;
    confirmUserName?: string;
    createTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    fillTime?: string;
    fillUserName?: string;
    formName?: string;
    id: string;
    modelCode?: string;
    payload?: Record<string, any>;
    planNo?: string;
    pressSlotSliceNo?: string;
    recordDate?: string;
    sourceId?: number;
    sourceType?: string;
    status?: string;
    statusName?: string;
    type?: string;
    typeName?: string;
  }
  export interface FormRecordPageReq extends PageParam {
    formName?: string;
    modelCode?: string;
    planNo?: string;
    pressSlotSliceNo?: string;
    status?: string;
    type?: string;
  }
  export type PassWorkRecord = BaseApi.PassWorkRecord;
  export type PassWorkSaveReq = BaseApi.PassWorkSaveReq;
  export type PrintReportReq = BaseApi.PrintReportReq;
  export type ReportItem = BaseApi.ReportItem;
  export interface ReportQuery {
    scanConfirmDate?: string;
  }
  export interface CorrectAbnormalCategoryReq {
    category: string;
    id: number;
    reason: string;
  }
  export type SaveAqcTaskReq = BaseApi.SaveAqcTaskReq;
  export type SaveGlueBoardLossReq = BaseApi.SaveGlueBoardLossReq;
  export type SaveGlueBoardUsageReq = BaseApi.SaveGlueBoardUsageReq;
  export type SaveReportReq = BaseApi.SaveReportReq;
  export type SourceItem = BaseApi.SourceItem;
  export type StartWorkOrderReq = BaseApi.StartWorkOrderReq;
  export type SwitchEquipmentReq = BaseApi.SwitchEquipmentReq;
  export type TaskItem = BaseApi.TaskItem;
  export type TaskQuery = BaseApi.TaskQuery;
}

const PRESS_SLOT_BASE = '/mes/hc/execution/formula-report/press-slot';

export function getAdhesiveConsoleTaskList(params: MesHcAdhesiveConsoleApi.TaskQuery) {
  return requestClient.get<MesHcAdhesiveConsoleApi.TaskItem[]>(`${PRESS_SLOT_BASE}/task-list`, { params });
}

export function getPressSlotFormRecordPage(params: MesHcAdhesiveConsoleApi.FormRecordPageReq) {
  return requestClient.get<PageResult<MesHcAdhesiveConsoleApi.FormRecord>>(`${PRESS_SLOT_BASE}/form-record/page`, {
    params,
  });
}

export function getAdhesiveConsolePassWorkList(
  planId: number,
  planOperationId: number,
  params?: { equipmentId?: number; recordDate?: string },
) {
  return requestClient.get<MesHcAdhesiveConsoleApi.PassWorkRecord[]>(`${PRESS_SLOT_BASE}/pass-work/list`, {
    params: { planId, planOperationId, ...params },
  });
}

export function startAdhesiveConsoleWorkOrder(data: MesHcAdhesiveConsoleApi.StartWorkOrderReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/start`, data);
}

export function switchPressSlotConsoleWorkOrderEquipment(data: MesHcAdhesiveConsoleApi.SwitchEquipmentReq) {
  return requestClient.post<boolean>(`${PRESS_SLOT_BASE}/switch-equipment`, data);
}

export function saveAdhesiveConsolePassWork(data: MesHcAdhesiveConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/pass-work/save`, data);
}

export function confirmAdhesiveConsolePassWork(data: MesHcAdhesiveConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/pass-work/confirm`, data);
}

export function scanAdhesiveConsoleSource(batchNo: string, includeAbnormal = false) {
  return requestClient.get<MesHcAdhesiveConsoleApi.SourceItem>(`${PRESS_SLOT_BASE}/source/scan`, {
    params: { batchNo, includeAbnormal },
  });
}

export function getAdhesiveConsoleSourceList(planId: number, planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.SourceItem[]>(`${PRESS_SLOT_BASE}/source/list`, {
    params: { planId, planOperationId },
  });
}

export function getAdhesiveConsoleCheckTemplate(modelCode?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.CheckItem[]>(`${PRESS_SLOT_BASE}/check-template`, {
    params: { modelCode },
  });
}

export function getAdhesiveConsoleReportList(
  planOperationId: number,
  query?: MesHcAdhesiveConsoleApi.ReportQuery,
) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ReportItem[]>(`${PRESS_SLOT_BASE}/report/list`, {
    params: { planOperationId, ...query },
  });
}

export function getPressSlotConsumableStatus(planOperationId: number, equipmentId?: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ConsumableStatus[]>(`${PRESS_SLOT_BASE}/consumable/status`, {
    params: { equipmentId, planOperationId },
  });
}

export function getPressSlotConsumableMaterialOptions(params: {
  consumableType: 'BEARING' | 'PRESS_ROLLER' | string;
  keyword?: string;
  pageSize?: number;
}) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ConsumableMaterialOption[]>(
    `${PRESS_SLOT_BASE}/consumable/material-options`,
    { params },
  );
}

export function replacePressSlotConsumable(data: MesHcAdhesiveConsoleApi.ReplaceConsumableReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/consumable/replace`, data);
}

export function cleanPressSlotConsumable(data: MesHcAdhesiveConsoleApi.CleanConsumableReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/consumable/clean`, data);
}

export function saveAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.SaveReportReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/report/save`, data);
}

export function saveAndConfirmPressSlotReport(
  data: MesHcAdhesiveConsoleApi.SaveReportReq & Omit<MesHcAdhesiveConsoleApi.ConfirmReportReq, 'id'>,
) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/report/save-confirm`, data);
}

export function setPressSlotReportMiddleType(id: number, reportType: string) {
  return requestClient.post<boolean>(
    `${PRESS_SLOT_BASE}/report/set-middle-type?id=${encodeURIComponent(String(id))}&reportType=${encodeURIComponent(reportType)}`,
    {},
  );
}

export function completeAdhesiveConsoleWorkOrder(data: MesHcAdhesiveConsoleApi.CompleteWorkOrderReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/submit`, data);
}

export function completePressSlotSegment(data: MesHcAdhesiveConsoleApi.SegmentCompleteReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/segment/complete`, data);
}

export function confirmAdhesiveConsoleReport(data: MesHcAdhesiveConsoleApi.ConfirmReportReq) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/report/confirm`, data);
}

export function markAdhesiveConsoleReportPrinted(data: MesHcAdhesiveConsoleApi.PrintReportReq) {
  return requestClient.post<boolean>(`${PRESS_SLOT_BASE}/report/mark-printed`, data);
}

export function correctPressSlotReportAbnormalCategory(data: MesHcAdhesiveConsoleApi.CorrectAbnormalCategoryReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.ReportItem>(
    `${PRESS_SLOT_BASE}/report/abnormal-category/correct`,
    data,
  );
}

export function getAdhesiveConsoleIntermediate(params: {
  adhesiveReportId?: number;
  batchNo?: string;
  id?: number;
  planId: number;
  planOperationId: number;
  recordDate?: string;
}) {
  return requestClient.get<MesHcAdhesiveConsoleApi.IntermediateRecord>(`${PRESS_SLOT_BASE}/intermediate/get`, {
    params,
  });
}

export function getPressSlotIntermediateList(planOperationId: number, batchNo?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.IntermediateRecord[]>(`${PRESS_SLOT_BASE}/intermediate/list`, {
    params: { batchNo, planOperationId },
  });
}

export function saveAdhesiveConsoleIntermediate(data: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/intermediate/save`, data);
}

export function confirmPressSlotIntermediate(data: MesHcAdhesiveConsoleApi.IntermediateRecord) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/intermediate/confirm`, data);
}

export function getPressSlotMiddleLedgerList(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.MiddleLedgerRecord[]>(`${PRESS_SLOT_BASE}/middle-ledger/list`, {
    params: { planOperationId },
  });
}

export function importPressSlotMiddleLedger(
  planId: number,
  planOperationId: number,
  file: File,
  importAttachment?: Record<string, any>,
  options?: { batchNo?: string; recordDate?: string; recordId?: number },
) {
  return requestClient.upload<number>(`${PRESS_SLOT_BASE}/middle-ledger/import`, {
    file,
    ...(importAttachment ? { importAttachment: JSON.stringify(importAttachment) } : {}),
    planId,
    planOperationId,
    ...(options?.batchNo ? { batchNo: options.batchNo } : {}),
    ...(options?.recordDate ? { recordDate: options.recordDate } : {}),
    ...(options?.recordId ? { recordId: options.recordId } : {}),
  });
}

export function exportPressSlotMiddleLedger(
  planOperationId: number,
  options?: { batchNo?: string; recordId?: number },
) {
  return requestClient.download(`${PRESS_SLOT_BASE}/middle-ledger/export`, {
    params: {
      planOperationId,
      ...(options?.batchNo ? { batchNo: options.batchNo } : {}),
      ...(options?.recordId ? { recordId: options.recordId } : {}),
    },
  });
}

export function getPressSlotChangeoverLatest(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ChangeoverInspection | null>(`${PRESS_SLOT_BASE}/changeover/latest`, {
    params: { planOperationId },
  });
}

export function getPressSlotChangeoverList(planOperationId: number, motherSegmentBatchNo?: string) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ChangeoverInspection[]>(`${PRESS_SLOT_BASE}/changeover/list`, {
    params: { motherSegmentBatchNo, planOperationId },
  });
}

export function savePressSlotChangeover(data: MesHcAdhesiveConsoleApi.ChangeoverInspection) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/changeover/save`, data);
}

export function getPressSlotFaiSummary(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary>(`${PRESS_SLOT_BASE}/fai/summary`, {
    params,
  });
}

export function getPressSlotFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary[]>(`${PRESS_SLOT_BASE}/fai/list`, {
    params,
  });
}

export function getPressSlotProcessCheckFaiList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.FaiSummary[]>(`${PRESS_SLOT_BASE}/fai/process-check-list`, {
    params,
  });
}

export function getPressSlotActiveAbnormalLock(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord | null>(`${PRESS_SLOT_BASE}/abnormal-lock/active`, {
    params,
  });
}

export function getPressSlotAbnormalLockList(params: { motherBatchNo?: string; planId: number; planOperationId: number }) {
  return requestClient.get<MesHcAdhesiveConsoleApi.PressSlotAbnormalLockRecord[]>(`${PRESS_SLOT_BASE}/abnormal-lock/list`, {
    params,
  });
}

export function validatePressSlotFaiScan(params: {
  motherBatchNo?: string;
  planId: number;
  planOperationId: number;
  productionBatchNo?: string;
  reportType?: string;
}) {
  return requestClient.get<MesHcAdhesiveConsoleApi.PressSlotScanGateResp>(`${PRESS_SLOT_BASE}/fai/validate-scan`, {
    params,
  });
}

export function applyPressSlotFai(data: MesHcAdhesiveConsoleApi.FaiApplyReq) {
  return requestClient.post<MesHcAdhesiveConsoleApi.FaiSummary>(`${PRESS_SLOT_BASE}/fai/apply`, data);
}

export function withdrawPressSlotFai(data: MesHcAdhesiveConsoleApi.FaiWithdrawReq) {
  return requestClient.post<boolean>(`${PRESS_SLOT_BASE}/fai/withdraw`, data);
}

export function getPressSlotProcessParamList(
  planOperationId: number,
  filters?: { formType?: string; motherBatchNo?: string; productionBatchNo?: string; reportDate?: string },
) {
  return requestClient.get<MesHcAdhesiveConsoleApi.ProcessParamRecord[]>(`${PRESS_SLOT_BASE}/process-param/list`, {
    params: { planOperationId, ...filters },
  });
}

export function importPressSlotProcessParams(
  planId: number,
  planOperationId: number,
  file: File,
  importAttachment?: Record<string, any>,
  options?: {
    formType?: string;
    inspectionScene?: string;
    motherBatchNo?: string;
    productionBatchNo?: string;
    recordDate?: string;
    recordId?: number;
  },
) {
  return requestClient.upload<number>(`${PRESS_SLOT_BASE}/process-param/import`, {
    file,
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

export function savePressSlotProcessParam(data: MesHcAdhesiveConsoleApi.ProcessParamRecord) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/process-param/save`, data);
}

export function confirmPressSlotProcessParam(data: MesHcAdhesiveConsoleApi.ProcessParamRecord) {
  return requestClient.post<number>(`${PRESS_SLOT_BASE}/process-param/confirm`, data);
}

export function exportPressSlotProcessParams(
  planOperationId: number,
  options?: { formType?: string; motherBatchNo?: string; recordId?: number },
) {
  return requestClient.download(`${PRESS_SLOT_BASE}/process-param/export`, {
    params: {
      formType: options?.formType,
      motherBatchNo: options?.motherBatchNo,
      planOperationId,
      recordId: options?.recordId,
    },
  });
}

export function getPressSlotProductionCheckTemplate(modelCode: string) {
  return requestClient.get<MesHcStationFormApi.StationForm>(`${PRESS_SLOT_BASE}/production-check-template`, { params: { modelCode } });
}
