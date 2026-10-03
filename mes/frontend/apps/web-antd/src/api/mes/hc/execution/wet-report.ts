import { requestClient } from '#/api/request';

export namespace MesHcWetReportApi {
  export interface TaskQuery {
    taskStatus?: 'ALL' | 'CANCELED' | 'CANCELLED' | 'COMPLETED' | 'IN_PROGRESS' | 'PAUSED' | 'PENDING';
    taskKeyword?: string;
    productKeyword?: string;
    motherMaterialKeyword?: string;
    motherModelKeyword?: string;
    productionDate?: string;
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

  export interface TaskItem {
    id: string;
    planId: number;
    planOperationId: number;
    operationReportId?: number;
    planNo?: string;
    erpOrderNo?: string;
    planType?: string;
    product?: string;
    materialCode?: string;
    productName?: string;
    motherMaterialCode?: string;
    motherMaterialName?: string;
    spec?: string;
    modelCode?: string;
    motherModelCode?: string;
    productionStartDate?: string;
    productionEndDate?: string;
    productionDate?: string;
    batchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    process?: string;
    workCenterId?: number;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    planQty?: number;
    uom?: string;
    goodQty?: number;
    scrapQty?: number;
    napSampleLength?: number;
    status?: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING' | 'RELEASED';
    startTime?: string;
    endTime?: string;
    requirements?: string;
    recorderName?: string;
    recorderTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    reportRemark?: string;
    previousOperationName?: string;
    previousOperationStatus?: string;
    previousProductionDate?: string;
    previousStartTime?: string;
    previousEndTime?: string;
    previousRecorderName?: string;
    previousGoodQty?: number;
    qtime?: QtimeInfo;
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
    faiStandardId?: number;
    faiStandardNo?: string;
    faiApplyTime?: string;
    faiReturnTime?: string;
    faiRejectReason?: string;
    extraJson?: string;
  }

  export interface FaiSummary {
    faiId?: number;
    faiNo?: string;
    faiStatus?: TaskItem['faiStatus'];
    faiJudgment?: TaskItem['faiJudgment'];
    faiStandardId?: number;
    faiStandardNo?: string;
    faiStandardVersion?: string;
    faiApplyTime?: string;
    faiReturnTime?: string;
    faiRejectReason?: string;
    productBatchNo?: string;
    napSampleLength?: number;
    displayText?: string;
    allowReportSubmit?: boolean;
    records?: FaiRecord[];
  }

  export interface FaiRecord {
    faiId?: number;
    faiNo?: string;
    faiStatus?: TaskItem['faiStatus'];
    faiJudgment?: TaskItem['faiJudgment'];
    faiStandardId?: number;
    faiStandardNo?: string;
    faiStandardVersion?: string;
    faiApplyTime?: string;
    faiReturnTime?: string;
    faiRejectReason?: string;
    productBatchNo?: string;
    napSampleLength?: number;
    displayText?: string;
    allowReportSubmit?: boolean;
    isCurrent?: boolean;
  }

  export interface FaiApplyReq {
    planId: number;
    planOperationId: number;
    standardMatchMode?:
      | 'MATERIAL'
      | 'MATERIAL_PROCESS'
      | 'PROCESS'
      | 'PRODUCT_MODEL_PROCESS';
    materialId?: number;
    /** 母料料号，仅用于展示追溯；湿法首检标准匹配使用产品型号 + 工序 */
    materialCode?: string;
    materialName?: string;
    specification?: string;
    triggerReason?: 'NEW_ORDER' | 'REWORK_RECHECK';
    submitterName?: string;
    napSampleLength?: number;
    remark?: string;
  }

  export interface PassWorkItem {
    itemSeq: number;
    category?: string;
    node?: string;
    item?: string;
    standard?: string;
    valueMode?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    actualValue?: string;
    actualValue2?: string;
    status?: string;
    remark?: string;
  }

  export interface PassWorkRow {
    recordId?: number;
    formId?: number;
    formCode: string;
    id: string;
    name: string;
    displayName?: string;
    timing?: string;
    status?: string;
    result?: string;
    inspectionResult?: string;
    recorder?: string;
    recorderTime?: string;
    confirmer?: string;
    confirmerTime?: string;
    formRemark?: string;
    confirmRemark?: string;
    presetHeaderDataJson?: string;
    headerDataJson?: string;
    schemaJson?: string;
    presetDetails?: PassWorkItem[];
    details?: PassWorkItem[];
  }

  export interface PassWorkSaveReq {
    recordId?: number;
    planId: number;
    planOperationId: number;
    formCode: string;
    result?: string;
    inspectionResult?: string;
    headerDataJson?: string;
    formRemark?: string;
    recorder?: string;
    recorderTime?: string;
    confirmer?: string;
    confirmerTime?: string;
    confirmRemark?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    details?: PassWorkItem[];
  }

  export interface StartReq {
    planId: number;
    planOperationId: number;
    reportDate?: string;
    startTime?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    recorderName?: string;
    recorderTime?: string;
  }

  export interface SubmitReq {
    planId: number;
    planOperationId: number;
    batchNo?: string;
    reportDate?: string;
    startTime?: string;
    endTime?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    receiveLength?: number;
    napSampleLength?: number;
    printLossLength?: number;
    abnormalPositionOption?: 'DETAIL' | 'NONE';
    petModel?: string;
    petBatchNo?: string;
    guideClothBatchNo?: string;
    guideClothUseCount?: number;
    guideClothChanged?: string;
    guideClothChangeReason?: string;
    inWashTime?: string;
    outWashTime?: string;
    inSolidifyTime?: string;
    outSolidifyTime?: string;
    inOvenTime?: string;
    outOvenTime?: string;
    remark?: string;
    recorderName?: string;
    recorderTime?: string;
    abnormalPositions?: AbnormalPositionSaveReq[];
  }

  export interface AbnormalPositionSaveReq {
    positionText?: string;
    abnormalLength?: number;
    remark?: string;
    sortOrder?: number;
  }

  export interface AbnormalPositionItem extends AbnormalPositionSaveReq {
    id?: number;
    operationReportId?: number;
    planId?: number;
    planOperationId?: number;
    planNo?: string;
    createTime?: string;
  }

  export interface SwitchEquipmentReq {
    planId: number;
    planOperationId: number;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
  }

  export interface TimeUpdateReq {
    endTime?: string;
    id: number;
    reportDate?: string;
    startTime?: string;
  }

  export interface QuantityReviseReq {
    id: number;
    reason: string;
    receiveLength: number;
  }

  export interface StatisticsDataReviseReq {
    feedQty?: number;
    goodQty?: number;
    id: number;
    reason: string;
    scrapQty?: number;
  }

  export interface ConfirmReq {
    confirmerName?: string;
    confirmerTime?: string;
    id: number;
  }

  export interface TimeLogItem {
    id: number;
    operationReportId: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    beforeReportDate?: string;
    afterReportDate?: string;
    beforeStartTime?: string;
    afterStartTime?: string;
    beforeEndTime?: string;
    afterEndTime?: string;
    beforeReportMinutes?: number;
    afterReportMinutes?: number;
    operatorId?: number;
    operatorName?: string;
    changeTime?: string;
  }

  export type WaterChangeStatus = 'APPLY' | 'APPROVED';

  export interface WaterChangeApply {
    id?: number;
    changeStartDate: string;
    changeEndDate: string;
    areaDesc: string;
    status: WaterChangeStatus;
    applicantId?: number;
    applicantName?: string;
    applyTime?: string;
    confirmerId?: number;
    confirmerName?: string;
    confirmTime?: string;
    remark?: string;
    createTime?: string;
  }
}

export async function getWetReportTaskList(params: MesHcWetReportApi.TaskQuery) {
  return requestClient.get<MesHcWetReportApi.TaskItem[]>(
    '/mes/hc/execution/formula-report/wet/task-list',
    { params },
  );
}

export async function getWetPassWorkList(params: { planId: number; planOperationId: number }) {
  return requestClient.get<MesHcWetReportApi.PassWorkRow[]>(
    '/mes/hc/execution/formula-report/wet/pass-work/list',
    { params },
  );
}

export async function startWetReport(data: MesHcWetReportApi.StartReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/start', data);
}

export async function switchWetEquipment(data: MesHcWetReportApi.SwitchEquipmentReq) {
  return requestClient.post<boolean>('/mes/hc/execution/formula-report/wet/switch-equipment', data);
}

export async function updateWetReportTime(data: MesHcWetReportApi.TimeUpdateReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/report/time/update', data);
}

export async function reviseWetReportQuantity(data: MesHcWetReportApi.QuantityReviseReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/report/quantity/revise', data);
}

export async function reviseWetReportStatisticsData(data: MesHcWetReportApi.StatisticsDataReviseReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/report/statistics-data/revise', data);
}

export async function getWetReportTimeLogs(operationReportId: number) {
  return requestClient.get<MesHcWetReportApi.TimeLogItem[]>(
    '/mes/hc/execution/formula-report/wet/report/time-log/list',
    { params: { operationReportId } },
  );
}

export async function confirmWetReport(data: MesHcWetReportApi.ConfirmReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/report/confirm', data);
}

export async function getWetFaiSummary(params: { planId: number; planOperationId: number }) {
  return requestClient.get<MesHcWetReportApi.FaiSummary>(
    '/mes/hc/execution/formula-report/wet/fai/summary',
    { params },
  );
}

export async function applyWetFai(data: MesHcWetReportApi.FaiApplyReq) {
  return requestClient.post<MesHcWetReportApi.FaiSummary>(
    '/mes/hc/execution/formula-report/wet/fai/apply',
    data,
  );
}

export async function saveWetPassWork(data: MesHcWetReportApi.PassWorkSaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/pass-work/save', data);
}

export async function confirmWetPassWork(data: MesHcWetReportApi.PassWorkSaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/pass-work/confirm', data);
}

export async function submitWetReport(data: MesHcWetReportApi.SubmitReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/wet/submit', data);
}

export async function getWetAbnormalPositionList(params: {
  operationReportId?: number;
  planOperationId?: number;
}) {
  return requestClient.get<MesHcWetReportApi.AbnormalPositionItem[]>(
    '/mes/hc/execution/formula-report/wet/abnormal-position/list',
    { params },
  );
}

export async function getActiveWetWaterChangeApply() {
  return requestClient.get<MesHcWetReportApi.WaterChangeApply | null>(
    '/mes/hc/execution/formula-report/wet/water-change/active',
  );
}

export async function saveWetWaterChangeApply(data: MesHcWetReportApi.WaterChangeApply) {
  return requestClient.post<MesHcWetReportApi.WaterChangeApply>(
    '/mes/hc/execution/formula-report/wet/water-change/apply',
    data,
  );
}
