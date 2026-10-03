import { requestClient } from '#/api/request';

export namespace MesHcPackagingConsoleApi {
  export interface TaskQuery {
    motherMaterialKeyword?: string;
    motherModelKeyword?: string;
    productKeyword?: string;
    productionDate?: string;
    taskKeyword?: string;
    taskStatus?: 'ALL' | 'COMPLETED' | 'IN_PROGRESS' | 'PENDING';
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
    confirmer?: string;
    confirmerTime?: string;
    details?: PassWorkItem[];
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    formCode: string;
    inspectionResult?: string;
    planId: number;
    planOperationId: number;
    recordDate?: string;
    recordId?: number;
    recorder?: string;
    recorderTime?: string;
    result?: string;
  }

  export interface Summary {
    innerPieceCount?: number;
    innerUnitCount?: number;
    outerBoxCount?: number;
    outerPieceCount?: number;
    reviewedInnerUnitCount?: number;
    reviewedOuterBoxCount?: number;
    sourcePieceCount?: number;
    unpackedPieceCount?: number;
  }

  export interface SourceItem {
    confirmTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packageStatus?: string;
    parentProductionBatchNo?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    productSize?: string;
    productionBatchNo?: string;
    qualityStatus?: string;
    reportDate?: string;
    reportStatus?: string;
    sliceBatchNo?: string;
    sourceCutRoundReportId: number;
  }

  export interface InnerUnitItem {
    id?: number;
    innerUnitId?: number;
    innerUnitNo?: string;
    productionBatchNo?: string;
    qualityStatus?: string;
    scanTime?: string;
    scanUserName?: string;
    sliceBatchNo?: string;
    sourceCutRoundReportId?: number;
  }

  export interface InnerUnit {
    backfillFlag?: boolean;
    backfillReason?: string;
    batchNo?: string;
    currentQty?: number;
    id: number;
    innerUnitNo?: string;
    items?: InnerUnitItem[];
    labelNo?: string;
    lastPrintTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packageDate?: string;
    packageSpec?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    printCount?: number;
    productSize?: string;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    reviewTime?: string;
    reviewerName?: string;
    targetQty?: number;
    unitStatus?: string;
  }

  export interface OuterBoxItem {
    id?: number;
    innerPackageSpec?: number;
    innerUnitId?: number;
    innerUnitNo?: string;
    outerBoxId?: number;
    outerBoxNo?: string;
    pieceQty?: number;
    scanTime?: string;
    scanUserName?: string;
    sliceBatchListJson?: string;
  }

  export interface OuterBox {
    batchNo?: string;
    boxStatus?: string;
    currentQty?: number;
    id: number;
    items?: OuterBoxItem[];
    lastPrintTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    outerBoxNo?: string;
    outerLabelNo?: string;
    packMethod?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    printCount?: number;
    productSize?: string;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    reviewTime?: string;
    reviewerName?: string;
    standardQty?: number;
    tailBoxFlag?: boolean;
  }

  export interface CreateInnerUnitReq {
    actualWorkTime?: string;
    backfillFlag?: boolean;
    backfillReason?: string;
    packageSpec: number;
    planId: number;
    planOperationId: number;
    recorderName?: string;
    remark?: string;
  }

  export interface AddInnerItemReq {
    innerUnitId: number;
    scanUserName?: string;
    sliceBatchNo: string;
  }

  export interface CreateOuterBoxReq {
    packMethod?: string;
    planId: number;
    planOperationId: number;
    recorderName?: string;
    remark?: string;
    standardQty?: number;
    tailBoxFlag?: boolean;
  }

  export interface AddOuterUnitReq {
    innerUnitNo: string;
    outerBoxId: number;
    scanUserName?: string;
  }

  export interface ActionReq {
    id: number;
    operatorName?: string;
    reason?: string;
    scanNo?: string;
  }

  export interface StartReq {
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    planId: number;
    planOperationId: number;
    recorderName?: string;
    reportDate?: string;
    startTime?: string;
  }

  export interface SubmitReq {
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
}

const BASE = '/mes/hc/execution/formula-report/packaging';

export function getPackagingTaskList(params: MesHcPackagingConsoleApi.TaskQuery) {
  return requestClient.get<MesHcPackagingConsoleApi.TaskItem[]>(`${BASE}/task-list`, { params });
}

export function startPackagingWorkOrder(data: MesHcPackagingConsoleApi.StartReq) {
  return requestClient.post<number>(`${BASE}/start`, data);
}

export function completePackagingWorkOrder(data: MesHcPackagingConsoleApi.SubmitReq) {
  return requestClient.post<number>(`${BASE}/submit`, data);
}

export function getPackagingSummary(planId: number, planOperationId: number) {
  return requestClient.get<MesHcPackagingConsoleApi.Summary>(`${BASE}/summary`, {
    params: { planId, planOperationId },
  });
}

export function getPackagingPassWorkList(planId: number, planOperationId: number) {
  return requestClient.get<MesHcPackagingConsoleApi.PassWorkRecord[]>(`${BASE}/pass-work/list`, {
    params: { planId, planOperationId },
  });
}

export function savePackagingPassWork(data: MesHcPackagingConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${BASE}/pass-work/save`, data);
}

export function confirmPackagingPassWork(data: MesHcPackagingConsoleApi.PassWorkSaveReq) {
  return requestClient.post<number>(`${BASE}/pass-work/confirm`, data);
}

export function getPackagingSourceList(planId: number, planOperationId: number) {
  return requestClient.get<MesHcPackagingConsoleApi.SourceItem[]>(`${BASE}/source/list`, {
    params: { planId, planOperationId },
  });
}

export function scanPackagingSource(sliceBatchNo: string) {
  return requestClient.get<MesHcPackagingConsoleApi.SourceItem>(`${BASE}/source/scan`, {
    params: { sliceBatchNo },
  });
}

export function getPackagingInnerUnitList(planOperationId: number) {
  return requestClient.get<MesHcPackagingConsoleApi.InnerUnit[]>(`${BASE}/inner-unit/list`, {
    params: { planOperationId },
  });
}

export function createPackagingInnerUnit(data: MesHcPackagingConsoleApi.CreateInnerUnitReq) {
  return requestClient.post<number>(`${BASE}/inner-unit/create`, data);
}

export function addPackagingInnerItem(data: MesHcPackagingConsoleApi.AddInnerItemReq) {
  return requestClient.post<number>(`${BASE}/inner-unit/add-item`, data);
}

export function confirmPackagingInnerUnit(data: MesHcPackagingConsoleApi.ActionReq) {
  return requestClient.post<number>(`${BASE}/inner-unit/confirm`, data);
}

export function printPackagingInnerUnit(data: MesHcPackagingConsoleApi.ActionReq) {
  return requestClient.post<number>(`${BASE}/inner-unit/print`, data);
}

export function reviewPackagingInnerUnit(data: MesHcPackagingConsoleApi.ActionReq) {
  return requestClient.post<number>(`${BASE}/inner-unit/review`, data);
}

export function getPackagingOuterBoxList(planOperationId: number) {
  return requestClient.get<MesHcPackagingConsoleApi.OuterBox[]>(`${BASE}/outer-box/list`, {
    params: { planOperationId },
  });
}

export function createPackagingOuterBox(data: MesHcPackagingConsoleApi.CreateOuterBoxReq) {
  return requestClient.post<number>(`${BASE}/outer-box/create`, data);
}

export function addPackagingOuterUnit(data: MesHcPackagingConsoleApi.AddOuterUnitReq) {
  return requestClient.post<number>(`${BASE}/outer-box/add-unit`, data);
}

export function confirmPackagingOuterBox(data: MesHcPackagingConsoleApi.ActionReq) {
  return requestClient.post<number>(`${BASE}/outer-box/confirm`, data);
}

export function printPackagingOuterBox(data: MesHcPackagingConsoleApi.ActionReq) {
  return requestClient.post<number>(`${BASE}/outer-box/print`, data);
}

export function reviewPackagingOuterBox(data: MesHcPackagingConsoleApi.ActionReq) {
  return requestClient.post<number>(`${BASE}/outer-box/review`, data);
}
