import { requestClient } from '#/api/request';

export namespace MesHcSliceAdjustApi {
  export interface CandidateQuery {
    segmentBatchNo?: string;
    keyword?: string;
  }

  export interface Candidate {
    sliceNo?: string;
    segmentBatchNo?: string;
    lastProcessCode?: string;
    lastProcessName?: string;
    planNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    statusText?: string;
    resultText?: string;
    relatedCount?: number;
    lastReportTime?: string;
  }

  export interface TraceQuery {
    segmentBatchNo?: string;
    leftSliceNo?: string;
    rightSliceNo?: string;
  }

  export interface TraceRow {
    side?: 'left' | 'right' | string;
    sliceNo?: string;
    segmentBatchNo?: string;
    processCode?: string;
    processName?: string;
    stageSort?: number;
    sourceTable?: string;
    sourceId?: number;
    statusText?: string;
    resultText?: string;
    reportTime?: string;
  }

  export interface AuditQuery {
    keyword?: string;
  }

  export interface AuditRecord {
    id?: number;
    adjustNo?: string;
    adjustType?: string;
    segmentBatchNo?: string;
    leftSliceNo?: string;
    rightSliceNo?: string;
    leftLastProcessName?: string;
    rightLastProcessName?: string;
    operatorName?: string;
    adjustTime?: string;
    adjustReason?: string;
    affectedRows?: number;
  }

  export interface SwapReq {
    segmentBatchNo?: string;
    leftSliceNo: string;
    rightSliceNo: string;
    operatorName?: string;
    reason: string;
  }

  export interface RenameReq {
    segmentBatchNo?: string;
    sourceSliceNo: string;
    targetSliceNo: string;
    operatorName?: string;
    reason: string;
  }

  export interface AffectedColumn {
    tableName?: string;
    columnName?: string;
    columnComment?: string;
    affectedRows?: number;
  }

  export interface SwapResp {
    adjustNo?: string;
    segmentBatchNo?: string;
    leftSliceNo?: string;
    rightSliceNo?: string;
    affectedRows?: number;
    message?: string;
    affectedColumns?: AffectedColumn[];
  }
}

export async function getSliceAdjustCandidateList(params: MesHcSliceAdjustApi.CandidateQuery) {
  return requestClient.get<MesHcSliceAdjustApi.Candidate[]>('/mes/hc/execution/slice-adjust/candidate-list', {
    params,
  });
}

export async function getSliceAdjustTraceList(params: MesHcSliceAdjustApi.TraceQuery) {
  return requestClient.get<MesHcSliceAdjustApi.TraceRow[]>('/mes/hc/execution/slice-adjust/trace-list', {
    params,
  });
}

export async function getSliceAdjustRecordList(params?: MesHcSliceAdjustApi.AuditQuery) {
  return requestClient.get<MesHcSliceAdjustApi.AuditRecord[]>('/mes/hc/execution/slice-adjust/record-list', {
    params,
  });
}

export async function swapSliceAdjustNo(data: MesHcSliceAdjustApi.SwapReq) {
  return requestClient.post<MesHcSliceAdjustApi.SwapResp>('/mes/hc/execution/slice-adjust/swap', data);
}

export async function renameSliceAdjustNo(data: MesHcSliceAdjustApi.RenameReq) {
  return requestClient.post<MesHcSliceAdjustApi.SwapResp>('/mes/hc/execution/slice-adjust/rename', data);
}
