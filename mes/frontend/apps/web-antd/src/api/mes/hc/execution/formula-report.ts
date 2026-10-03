import { requestClient } from '#/api/request';

export namespace MesHcFormulaReportApi {
  export interface TaskQuery {
    taskStatus?: 'ALL' | 'CANCELED' | 'CANCELLED' | 'COMPLETED' | 'IN_PROGRESS' | 'PAUSED' | 'PENDING';
    taskKeyword?: string;
    productKeyword?: string;
    motherMaterialKeyword?: string;
    motherModelKeyword?: string;
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
    productionDate?: string;
    batchNo?: string;
    batchNoPreview?: boolean;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    productionEndDate?: string;
    process?: string;
    equipmentName?: string;
    workCenterId?: number;
    planQty?: number;
    uom?: string;
    goodQty?: number;
    scrapQty?: number;
    status?: 'COMPLETED' | 'IN_PROGRESS' | 'PENDING';
    startTime?: string;
    requirements?: string;
    recipeCode?: string;
    recipeName?: string;
    batchingNo?: string;
    feedBatchNo?: string;
    feedQty?: number;
    stirStartTime?: string;
    stirEndTime?: string;
    endTime?: string;
    recorderName?: string;
    recorderTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    mixerEquipmentId?: number;
    mixerEquipmentCode?: string;
    mixerEquipmentName?: string;
    foamingEquipmentId?: number;
    foamingEquipmentCode?: string;
    foamingEquipmentName?: string;
    viscosity?: number;
    slurryTemperature?: number;
    filterBatchNo?: string;
    inputWeight?: number;
    batchingTankNo?: string;
    defoamingTankNo?: string;
    reportRemark?: string;
  }

  export interface DefectItem {
    defectCode: string;
    defectName?: string;
    defectQty: number;
    remark?: string;
  }

  export interface SubmitReq {
    planId: number;
    planOperationId: number;
    batchNo?: string;
    reportDate?: string;
    startTime?: string;
    endTime?: string;
    batchingNo?: string;
    feedBatchNo?: string;
    recipeId?: number;
    recipeCode?: string;
    recipeName?: string;
    goodQty: number;
    scrapQty: number;
    feedQty?: number;
    stirStartTime?: string;
    stirEndTime?: string;
    recorderName?: string;
    recorderTime?: string;
    mixerEquipmentId?: number;
    mixerEquipmentCode?: string;
    mixerEquipmentName?: string;
    foamingEquipmentId?: number;
    foamingEquipmentCode?: string;
    foamingEquipmentName?: string;
    viscosity?: number;
    slurryTemperature?: number;
    filterBatchNo?: string;
    inputWeight?: number;
    batchingTankNo?: string;
    defoamingTankNo?: string;
    laborHours?: number;
    reportType?: string;
    remark?: string;
    scrapReason?: string;
    defects?: DefectItem[];
  }

  export interface StartReq {
    planId: number;
    planOperationId: number;
    reportDate?: string;
    startTime?: string;
    recorderName?: string;
    recorderTime?: string;
    mixerEquipmentId?: number;
    mixerEquipmentCode?: string;
    mixerEquipmentName?: string;
    foamingEquipmentId?: number;
    foamingEquipmentCode?: string;
    foamingEquipmentName?: string;
  }

  export interface SwitchEquipmentReq {
    planId: number;
    planOperationId: number;
    mixerEquipmentId?: number;
    mixerEquipmentCode?: string;
    mixerEquipmentName?: string;
    foamingEquipmentId?: number;
    foamingEquipmentCode?: string;
    foamingEquipmentName?: string;
  }

  export interface TimeUpdateReq {
    endTime?: string;
    id: number;
    reportDate?: string;
    startTime?: string;
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

  export interface PassWorkItem {
    requiredFlag?: boolean;
    explicitValueMode?: boolean;
    itemSeq?: number;
    category?: string;
    node?: string;
    item?: string;
    standard?: string;
    valueMode?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    fieldDefinitionsJson?: string;
    fieldValuesJson?: string;
    actualValue?: string;
    actualValue2?: string;
    status?: string;
    remark?: string;
  }

  export interface ExcelAttachment {
    name?: string;
    path?: string;
    size?: number;
    type?: string;
    uploadTime?: string;
    url?: string;
  }

  export interface PassWorkRow {
    needConfirm?: boolean;
    complete?: boolean;
    completionMessage?: string;
    formulaCategory?: string;
    matchReason?: string;
    frozen?: boolean;
    recordId?: number;
    formId?: number;
    formCode?: string;
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
    importAttachment?: ExcelAttachment;
    details?: PassWorkItem[];
  }

  export interface PassWorkSaveReq {
    submit?: boolean;
    recordId?: number;
    planId: number;
    planOperationId: number;
    formCode: string;
    result?: string;
    inspectionResult?: string;
    formRemark?: string;
    recorder?: string;
    recorderTime?: string;
    confirmer?: string;
    confirmerTime?: string;
    confirmRemark?: string;
    importAttachment?: ExcelAttachment;
    mixerEquipmentId?: number;
    mixerEquipmentCode?: string;
    mixerEquipmentName?: string;
    foamingEquipmentId?: number;
    foamingEquipmentCode?: string;
    foamingEquipmentName?: string;
    details?: PassWorkItem[];
  }
}

export async function getFormulaReportTaskList(
  params: MesHcFormulaReportApi.TaskQuery,
) {
  return requestClient.get<MesHcFormulaReportApi.TaskItem[]>(
    '/mes/hc/execution/formula-report/task-list',
    { params },
  );
}

export async function submitFormulaReport(
  data: MesHcFormulaReportApi.SubmitReq,
) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/submit', data);
}

export async function startFormulaReport(
  data: MesHcFormulaReportApi.StartReq,
) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/start', data);
}

export async function switchFormulaEquipment(
  data: MesHcFormulaReportApi.SwitchEquipmentReq,
) {
  return requestClient.post<boolean>('/mes/hc/execution/formula-report/switch-equipment', data);
}

export async function updateFormulaReportTime(
  data: MesHcFormulaReportApi.TimeUpdateReq,
) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/report/time/update', data);
}

export async function reviseFormulaReportStatisticsData(
  data: MesHcFormulaReportApi.StatisticsDataReviseReq,
) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/report/statistics-data/revise', data);
}

export async function getFormulaReportTimeLogs(operationReportId: number) {
  return requestClient.get<MesHcFormulaReportApi.TimeLogItem[]>(
    '/mes/hc/execution/formula-report/report/time-log/list',
    { params: { operationReportId } },
  );
}

export async function confirmFormulaReport(
  data: MesHcFormulaReportApi.ConfirmReq,
) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/report/confirm', data);
}

export async function getFormulaPassWorkList(planId: number, planOperationId: number) {
  return requestClient.get<MesHcFormulaReportApi.PassWorkRow[]>(
    '/mes/hc/execution/formula-report/pass-work/list',
    { params: { planId, planOperationId } },
  );
}

export async function saveFormulaPassWork(data: MesHcFormulaReportApi.PassWorkSaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/pass-work/save', data);
}

export async function confirmFormulaPassWork(data: MesHcFormulaReportApi.PassWorkSaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/pass-work/confirm', data);
}

export async function previewFormulaStationMatch(modelCode: string) {
  return requestClient.get<{
    modelCode: string;
    candidates: { formCode: string; formName: string; category: string; selected: boolean; reason: string }[];
    errors: string[];
  }>('/mes/hc/execution/formula-report/pass-work/match-preview', { params: { modelCode } });
}
