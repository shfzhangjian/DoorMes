import { requestClient } from '#/api/request';

export namespace MesHcRoughGrindingReportApi {
  export interface TaskQuery {
    taskStatus?: 'ALL' | 'CANCELLED' | 'COMPLETED' | 'IN_PROGRESS' | 'PAUSED' | 'PENDING' | 'UNFINISHED';
    taskKeyword?: string;
    productKeyword?: string;
    motherMaterialKeyword?: string;
    motherModelKeyword?: string;
    equipmentId?: number;
    equipmentCode?: string;
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
    motherLength?: number;
    remainingLength?: number;
    firstGrindingProcessLength?: number;
    secondGrindingProcessLength?: number;
    status?: 'CANCELLED' | 'COMPLETED' | 'IN_PROGRESS' | 'PAUSED' | 'PENDING' | 'RELEASED';
    startTime?: string;
    endTime?: string;
    requirements?: string;
    recorderName?: string;
    recorderTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    reportRemark?: string;
    extraJson?: string;
    previousOperationName?: string;
    previousOperationStatus?: string;
    previousProductionDate?: string;
    previousEndTime?: string;
    previousRecorderName?: string;
    previousGoodQty?: number;
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

  export interface SaveReq {
    planId: number;
    planOperationId: number;
    batchNo?: string;
    reportDate?: string;
    startTime?: string;
    endTime?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    inputLength?: number;
    reportQty?: number;
    firstProcessLength?: number;
    firstLossLength?: number;
    firstOutputLength?: number;
    firstNapSampleLength?: number;
    secondProcessLength?: number;
    secondLossLength?: number;
    secondOutputLength?: number;
    secondNapSampleLength?: number;
    lastFirstSandpaperBatchNo?: string;
    lastFirstSandpaperLife?: number;
    lastFirstSandpaperLifeDays?: number;
    lastSecondSandpaperBatchNo?: string;
    lastSecondSandpaperLife?: number;
    lastSecondSandpaperLifeDays?: number;
    remark?: string;
    recorderName?: string;
    recorderTime?: string;
    confirmerName?: string;
    confirmerTime?: string;
    extraJson?: string;
  }

  export interface SwitchEquipmentReq {
    planId: number;
    planOperationId: number;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
  }
}

export async function getRoughGrindingReportTaskList(params: MesHcRoughGrindingReportApi.TaskQuery) {
  return requestClient.get<MesHcRoughGrindingReportApi.TaskItem[]>(
    '/mes/hc/execution/formula-report/rough-grinding/task-list',
    { params },
  );
}

export async function startRoughGrindingReport(data: MesHcRoughGrindingReportApi.StartReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/rough-grinding/start', data);
}

export async function switchRoughGrindingEquipment(data: MesHcRoughGrindingReportApi.SwitchEquipmentReq) {
  return requestClient.post<boolean>('/mes/hc/execution/formula-report/rough-grinding/switch-equipment', data);
}

export async function saveRoughGrindingProgress(data: MesHcRoughGrindingReportApi.SaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/rough-grinding/save-progress', data);
}

export async function submitRoughGrindingReport(data: MesHcRoughGrindingReportApi.SaveReq) {
  return requestClient.post<number>('/mes/hc/execution/formula-report/rough-grinding/submit', data);
}
