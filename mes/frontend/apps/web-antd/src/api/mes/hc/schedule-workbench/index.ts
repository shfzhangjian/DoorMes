import { requestClient } from '#/api/request';

export namespace MesHcScheduleWorkbenchApi {
  export interface ReqVO {
    startDate?: string;
    endDate?: string;
    keyword?: string;
    planStatuses?: string[];
    modelCode?: string;
    sizeSpec?: string;
    operationName?: string;
  }

  export interface Metric {
    code?: string;
    label?: string;
    value?: number;
    unit?: string;
    description?: string;
  }

  export interface Fact {
    operationName?: string;
    factSource?: string;
    segmentBatchNo?: string;
    inputQty?: number;
    inputUnit?: string;
    reportQty?: number;
    reportUnit?: string;
    pendingQty?: number;
    pendingUnit?: string;
    lossNgQty?: number;
    lossNgUnit?: string;
    recordCount?: number;
    ngQty?: number;
    coaFlag?: boolean;
    coaNgFlag?: boolean;
    completedFlag?: boolean;
    equipmentNames?: string;
    lastReportTime?: string;
    glueBoardModel?: string;
    firstInspectionResult?: string;
  }

  export interface Cell {
    date?: string;
    plannedOperations?: string[];
    plannedOperationDetails?: PlannedOperation[];
    facts?: Fact[];
    ngQty?: number;
    statusLabel?: string;
  }

  export interface PlannedOperation {
    planOperationId?: number;
    operationName?: string;
    statusCode?: string;
    statusLabel?: string;
  }

  export interface ChangeoverSummary {
    actualSuffix?: string;
    actualValue?: string;
    planValue?: string;
    qty?: number;
    sourceOperation?: string;
    type?: 'MODEL' | 'SIZE' | string;
    unit?: string;
  }

  export interface ChangeoverDetail {
    actualSuffix?: string;
    actualValue?: string;
    glueBoardModel?: string;
    planId?: number;
    planValue?: string;
    productionBatchNo?: string;
    reporterName?: string;
    reportStatus?: string;
    reportTime?: string;
    sourceOperation?: string;
    sourceReportId?: number;
  }

  export interface Row {
    planId?: number;
    planNo?: string;
    planDate?: string;
    planMode?: string;
    sourceType?: string;
    prodType?: string;
    planStatus?: string;
    modelCode?: string;
    modelChangeoverSummaries?: ChangeoverSummary[];
    motherRollNo?: string;
    motherBatchPreviewNo?: string;
    inventorySourceBatchNos?: string;
    requirement?: string;
    frontProcessEnabled?: boolean;
    postProcessEnabled?: boolean;
    splitPlanNos?: string;
    sizeSpec?: string;
    sizeChangeoverSummaries?: ChangeoverSummary[];
    quantity?: number;
    quantityUnit?: string;
    quantityOperationName?: string;
    wetRollQty?: number;
    secondGrindingQty?: number;
    slittingConfirmedQty?: number;
    adhesive2Qty?: number;
    adhesive2ChangeoverFlag?: boolean;
    pickedQty?: number;
    dueDate?: string;
    completionStatus?: string;
    batchNo?: string;
    productionBatchNo?: string;
    cells?: Record<string, Cell>;
  }

  export interface NgSummary {
    operationName?: string;
    ngQty?: number;
  }

  export type WaterChangeStatus = 'APPLY' | 'APPROVED';

  export interface WetWaterChangeApply {
    id?: number;
    changeStartDate?: string;
    changeEndDate?: string;
    areaDesc?: string;
    status?: WaterChangeStatus;
    applicantId?: number;
    applicantName?: string;
    applyTime?: string;
    confirmerId?: number;
    confirmerName?: string;
    confirmTime?: string;
    remark?: string;
    createTime?: string;
  }

  export interface RespVO {
    startDate?: string;
    endDate?: string;
    dateColumns?: string[];
    metrics?: Metric[];
    rows?: Row[];
    ngSummaries?: NgSummary[];
    wetWaterChangeApplies?: WetWaterChangeApply[];
  }
}

export async function getScheduleWorkbench(params: MesHcScheduleWorkbenchApi.ReqVO) {
  return requestClient.get<MesHcScheduleWorkbenchApi.RespVO>(
    '/mes/hc/plan/schedule-workbench/get',
    { params },
  );
}

export async function getScheduleWorkbenchChangeoverDetail(params: {
  planId?: number;
  type: 'MODEL' | 'SIZE';
}) {
  return requestClient.get<MesHcScheduleWorkbenchApi.ChangeoverDetail[]>(
    '/mes/hc/plan/schedule-workbench/changeover-detail',
    { params },
  );
}

export async function exportScheduleWorkbench(params: MesHcScheduleWorkbenchApi.ReqVO) {
  return requestClient.download('/mes/hc/plan/schedule-workbench/export-excel', {
    params,
  });
}

export async function updateWetWaterChangeApplyStatus(
  data: MesHcScheduleWorkbenchApi.WetWaterChangeApply,
) {
  return requestClient.post<MesHcScheduleWorkbenchApi.WetWaterChangeApply>(
    '/mes/hc/execution/formula-report/wet/water-change/apply',
    data,
  );
}
