import { requestClient } from '#/api/request';

export namespace MesHcAdhesiveReportApi {
  export interface TaskQuery {
    taskStatus?: 'ALL' | 'COMPLETED' | 'IN_PROGRESS' | 'PENDING';
    taskKeyword?: string;
    productKeyword?: string;
    motherMaterialKeyword?: string;
    motherModelKeyword?: string;
    productionDate?: string;
  }

  export interface TaskItem {
    id: string;
    planId: number;
    planOperationId: number;
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
    status?: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING' | 'RELEASED';
    startTime?: string;
    endTime?: string;
    requirements?: string;
    recorderName?: string;
    recorderTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    reportRemark?: string;
    extraJson?: string;
    sourceBatchNo?: string;
    sourceProductionBatchNo?: string;
    sourceMode?: 'SELF_GRINDING' | 'LOCKED_WIP' | string;
    sourceModeName?: string;
    sourcePlanNo?: string;
    sourceMotherBatchNo?: string;
    sourceSegmentMarks?: string;
    sourceDetailIds?: string;
    inventoryLockIds?: string;
    availableSourceLength?: number;
    confirmedSourceCount?: number;
  }

  export interface SourceItem {
    grindingSecondDetailId: number;
    grindingPlanId?: number;
    grindingPlanNo?: string;
    grindingPlanOperationId?: number;
    rowUid?: string;
    motherBatchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    segmentMark?: string;
    processLength?: number;
    lossLength?: number;
    outputLength?: number;
    napSampleLength?: number;
    selfCheck?: string;
    defectCode?: string;
    confirmStatus?: string;
    confirmedBatchNo?: string;
    confirmTime?: string;
    downstreamStatus?: string;
    stockId?: number;
    inventoryLockId?: number;
    sourcePlanNo?: string;
    sourceMode?: 'SELF_GRINDING' | 'LOCKED_WIP' | string;
    sourceMotherBatchNo?: string;
    lockQty?: number;
    consumedQty?: number;
    remainingQty?: number;
    lockStatus?: string;
  }

  export interface CheckItem {
    id?: number;
    itemCategory?: string;
    itemName?: string;
    standardValue?: string;
    actualValue?: string;
    checkResult?: 'NG' | 'OK' | string;
    abnormalRemark?: string;
    sortNo?: number;
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
    headerDataJson?: string;
    inspectionResult?: string;
    planId: number;
    planOperationId: number;
    recordId?: number;
    recorder?: string;
    recorderTime?: string;
    result?: string;
  }

  export interface ReportItem {
    id?: number;
    planId: number;
    planNo?: string;
    planOperationId: number;
    sourceGrindingSecondDetailId?: number;
    sourceType?: string;
    sourceMode?: string;
    sourcePlanNo?: string;
    sourceMotherBatchNo?: string;
    sourceBatchNo?: string;
    sourceProductionBatchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    reportDate?: string;
    startTime?: string;
    endTime?: string;
    inputLength?: number;
    lossLength?: number;
    outputLength?: number;
    napSampleLength?: number;
    glueBoardMaterialCode?: string;
    glueBoardBatchNo?: string;
    selfCheck?: 'NG' | 'OK' | string;
    defectCode?: string;
    reportStatus?: string;
    recorderName?: string;
    recorderTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    remark?: string;
    extraJson?: string;
    checkItems?: CheckItem[];
  }

  export interface StartReq {
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

  export interface SaveReportReq extends Omit<ReportItem, 'planNo' | 'reportStatus'> {
    sourceGrindingSecondDetailId: number;
  }

  export interface ConfirmReportReq {
    id: number;
    scannedBatchNo: string;
    confirmerName?: string;
    confirmerTime?: string;
  }

  export interface MarkPrintedReq {
    id: number;
    printCount?: number;
    printStatus?: string;
    printTime?: string;
  }

  export interface SubmitReq {
    planId: number;
    planOperationId: number;
    reportDate?: string;
    endTime?: string;
    recorderName?: string;
    recorderTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    remark?: string;
  }

  export interface IntermediateDetail {
    id?: number;
    lengthMark?: number;
    leftThickness?: number;
    rightThickness?: number;
    remark?: string;
    sortNo?: number;
  }

  export interface IntermediateRecord {
    id?: number;
    planId: number;
    planNo?: string;
    planOperationId: number;
    productionDate?: string;
    modelCode?: string;
    materialCode?: string;
    batchNo?: string;
    processLength?: number;
    widthStart?: number;
    widthMiddle?: number;
    widthEnd?: number;
    recorderName?: string;
    confirmerName?: string;
    recordStatus?: string;
    remark?: string;
    extraJson?: string;
    details?: IntermediateDetail[];
  }
}

export async function getAdhesiveReportTaskList(params: MesHcAdhesiveReportApi.TaskQuery) {
  return requestClient.get<MesHcAdhesiveReportApi.TaskItem[]>(
    '/mes/hc/execution/formula-report/adhesive/task-list',
    { params },
  );
}

export async function scanAdhesiveSource(
  batchNo: string,
  planOperationId?: number,
  sourceMode?: string,
  sourcePlanNo?: string,
  sourceMotherBatchNo?: string,
) {
  return requestClient.get<MesHcAdhesiveReportApi.SourceItem>(
    '/mes/hc/execution/formula-report/adhesive/source/scan',
    { params: { batchNo, planOperationId, sourceMode, sourcePlanNo, sourceMotherBatchNo } },
  );
}

export async function getAdhesiveReportList(planOperationId: number) {
  return requestClient.get<MesHcAdhesiveReportApi.ReportItem[]>(
    '/mes/hc/execution/formula-report/adhesive/report/list',
    { params: { planOperationId } },
  );
}

export async function startAdhesiveReport(data: MesHcAdhesiveReportApi.StartReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/adhesive/start', data);
}

export async function switchAdhesiveEquipment(data: MesHcAdhesiveReportApi.SwitchEquipmentReq) {
  return requestClient.post<boolean>('/mes/hc/execution/formula-report/adhesive/switch-equipment', data);
}

export async function getAdhesivePassWorkList(planId: number, planOperationId: number) {
  return requestClient.get<MesHcAdhesiveReportApi.PassWorkRecord[]>(
    '/mes/hc/execution/formula-report/adhesive/pass-work/list',
    { params: { planId, planOperationId } },
  );
}

export async function saveAdhesivePassWork(data: MesHcAdhesiveReportApi.PassWorkSaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/adhesive/pass-work/save', data);
}

export async function confirmAdhesivePassWork(data: MesHcAdhesiveReportApi.PassWorkSaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/adhesive/pass-work/confirm', data);
}

export async function getAdhesiveCheckTemplate() {
  return requestClient.get<MesHcAdhesiveReportApi.CheckItem[]>(
    '/mes/hc/execution/formula-report/adhesive/check-template',
  );
}

export async function saveAdhesiveReport(data: MesHcAdhesiveReportApi.SaveReportReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/adhesive/report/save', data);
}

export async function confirmAdhesiveReport(data: MesHcAdhesiveReportApi.ConfirmReportReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/adhesive/report/confirm', data);
}

export async function markAdhesiveReportPrinted(data: MesHcAdhesiveReportApi.MarkPrintedReq) {
  return requestClient.post<boolean>('/mes/hc/execution/formula-report/adhesive/report/mark-printed', data);
}

export async function deleteAdhesiveReport(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/execution/formula-report/adhesive/report/delete?id=${id}`);
}

export async function submitAdhesiveReport(data: MesHcAdhesiveReportApi.SubmitReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/adhesive/submit', data);
}

export async function getAdhesiveIntermediate(planId: number, planOperationId: number) {
  return requestClient.get<MesHcAdhesiveReportApi.IntermediateRecord>(
    '/mes/hc/execution/formula-report/adhesive/intermediate/get',
    { params: { planId, planOperationId } },
  );
}

export async function saveAdhesiveIntermediate(data: MesHcAdhesiveReportApi.IntermediateRecord) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/adhesive/intermediate/save', data);
}
