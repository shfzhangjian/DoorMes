import { requestClient } from '#/api/request';

export namespace MesHcSlittingConsoleApi {
  export interface QtimeInfo {
    elapsedMinutes?: number;
    message?: string;
    standardMinutes?: number;
    status?: string;
    targetStarted?: boolean;
    timeout?: boolean;
  }

  export interface TaskQuery {
    taskKeyword?: string;
    taskStatus?: 'ALL' | 'CANCELLED' | 'COMPLETED' | 'IN_PROGRESS' | 'PAUSED' | 'PENDING' | 'UNFINISHED';
  }

  export interface TaskItem {
    availableSourceLength?: number;
    batchNo?: string;
    confirmedSourceCount?: number;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    id?: string;
    materialCode?: string;
    modelCode?: string;
    motherMaterialCode?: string;
    motherMaterialName?: string;
    parentProductionBatchNo?: string;
    planId: number;
    planNo?: string;
    planOperationId: number;
    process?: string;
    productName?: string;
    productionBatchNo?: string;
    requirements?: string;
    sizeName?: string;
    sizeSpec?: string;
    sourceBatchNo?: string;
    sourceProductionBatchNo?: string;
    spec?: string;
    status?: string;
    workCenterId?: number;
    workCenterName?: string;
  }

  export interface SourceItem {
    adhesivePlanNo?: string;
    adhesivePlanOperationId?: number;
    adhesiveReportId: number;
    adhesiveProductionBatchNo?: string;
    availableSourceLength?: number;
    confirmedSliceCount?: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    motherWetOutputLength?: number;
    outputLength?: number;
    parentProductionBatchNo?: string;
    productionBatchNo?: string;
    segmentBatchNo?: string;
    segmentMark?: string;
    sliceCount?: number;
    sourceBatchNo?: string;
    sourceEndPosition?: number;
    sourceProductionBatchNo?: string;
    sourceStartPosition?: number;
    status?: string;
    qtime?: QtimeInfo;
  }

  export interface SourceQuery {
    productionBatchNo?: string;
  }

  export interface SliceQuery {
    scanConfirmDate?: string;
  }

  export interface SourceCompleteReq {
    confirmerUserId: number;
    disposeRemainingTail?: boolean;
    planId: number;
    planOperationId: number;
    sourceAdhesiveReportId: number;
    tailDisposalLength?: number;
    tailDisposalReason?: string;
  }

  export interface SliceItem {
    upstreamSampleLockReason?: string;
    createTime?: string;
    downstreamFeedbackAbnormal?: boolean;
    downstreamFeedbackProcessCode?: string;
    downstreamFeedbackProcessName?: string;
    downstreamFeedbackReason?: string;
    downstreamFeedbackReportId?: number;
    editBlockedReason?: string;
    id: number;
    lastPrintTime?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    printCount?: number;
    printStatus?: 'PRINTED' | 'UNPRINTED' | string;
    remark?: string;
    scanStatus?: 'CONFIRMED' | 'UNCONFIRMED' | string;
    scanTime?: string;
    scannerName?: string;
    selfCheck?: 'NG' | 'OK' | string;
    sizeCode?: string;
    sizeName?: string;
    cutMode?: 'AUTO' | 'MANUAL' | string;
    endPosition?: number;
    sliceIndex?: number;
    sliceLength?: number;
    sliceSerialNo?: string;
    sourceAdhesiveReportId?: number;
    sourceBatchNo?: string;
    sourceLength?: number;
    sourceProductionBatchNo?: string;
    startPosition?: number;
    visualResultJson?: string;
  }

  export interface SliceGenerateReq {
    cutMode?: 'AUTO' | 'MANUAL';
    planId: number;
    planOperationId: number;
    sizeCode?: string;
    sizeName?: string;
    sliceCount: number;
    sliceLength?: number;
    sourceAdhesiveReportId: number;
    startSerialNo?: number;
  }

  export interface VisualItem {
    itemName: string;
    remark?: string;
    result: 'NG' | 'OK';
  }

  export interface SliceConfirmReq {
    id: number;
    remark?: string;
    scannedSliceNo?: string;
    selfCheck?: 'NG' | 'OK';
    sizeCode?: string;
    sizeName?: string;
    visualItems?: VisualItem[];
  }

  export interface CorrectAbnormalCategoryReq {
    category: string;
    id: number;
    reason: string;
  }
}

const SLITTING_BASE = '/mes/hc/execution/formula-report/slitting';

export function getSlittingConsoleTaskList(params: MesHcSlittingConsoleApi.TaskQuery) {
  return requestClient.get<MesHcSlittingConsoleApi.TaskItem[]>(`${SLITTING_BASE}/task-list`, { params });
}

export function getSlittingConsoleSourceList(
  planId: number,
  planOperationId: number,
  query?: MesHcSlittingConsoleApi.SourceQuery,
) {
  return requestClient.get<MesHcSlittingConsoleApi.SourceItem[]>(`${SLITTING_BASE}/source/list`, {
    params: { planId, planOperationId, ...query },
  });
}

export function completeSlittingConsoleSource(data: MesHcSlittingConsoleApi.SourceCompleteReq) {
  return requestClient.post<MesHcSlittingConsoleApi.SourceItem>(`${SLITTING_BASE}/source/complete`, data);
}

export function getSlittingConsoleSliceList(
  planOperationId: number,
  query?: MesHcSlittingConsoleApi.SliceQuery,
) {
  return requestClient.get<MesHcSlittingConsoleApi.SliceItem[]>(`${SLITTING_BASE}/slice/list`, {
    params: { planOperationId, ...query },
  });
}

export function generateSlittingConsoleSlices(data: MesHcSlittingConsoleApi.SliceGenerateReq) {
  return requestClient.post<MesHcSlittingConsoleApi.SliceItem[]>(`${SLITTING_BASE}/slice/generate`, data);
}

export function deleteSlittingConsoleSlice(id: number) {
  return requestClient.delete<boolean>(`${SLITTING_BASE}/slice/delete`, { params: { id } });
}

export function markSlittingConsoleSlicesPrinted(ids: number[]) {
  return requestClient.post<boolean>(`${SLITTING_BASE}/slice/mark-printed`, { ids });
}

export function confirmSlittingConsoleSlice(data: MesHcSlittingConsoleApi.SliceConfirmReq) {
  return requestClient.post<MesHcSlittingConsoleApi.SliceItem>(`${SLITTING_BASE}/slice/confirm`, data);
}

export function updateSlittingConsoleSliceContent(data: MesHcSlittingConsoleApi.SliceConfirmReq) {
  return requestClient.post<MesHcSlittingConsoleApi.SliceItem>(`${SLITTING_BASE}/slice/update-content`, data);
}

export function correctSlittingConsoleSliceAbnormalCategory(
  data: MesHcSlittingConsoleApi.CorrectAbnormalCategoryReq,
) {
  return requestClient.post<MesHcSlittingConsoleApi.SliceItem>(
    `${SLITTING_BASE}/slice/abnormal-category/correct`,
    data,
  );
}
