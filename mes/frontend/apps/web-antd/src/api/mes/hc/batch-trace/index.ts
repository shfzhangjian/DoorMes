import { requestClient } from '#/api/request';

export namespace MesHcBatchTraceApi {
  export interface Overview {
    inputBatchNo?: string;
    normalizedBatchNo?: string;
    batchType?: string;
    batchTypeName?: string;
    rootBatchNo?: string;
    segmentBatchNo?: string;
    sliceBatchNo?: string;
    finalBatchNo?: string;
    currentProcessCode?: string;
    currentProcessName?: string;
    currentStatus?: string;
    currentStatusText?: string;
    branchCount?: number;
    factCount?: number;
  }

  export interface TreeNode {
    key: string;
    title: string;
    batchNo: string;
    nodeType: string;
    status: string;
    statusText: string;
    children?: TreeNode[];
  }

  export interface QualityItem {
    timelineKey?: string;
    inspectionNo?: string;
    inspectionType?: string;
    status?: string;
    result?: string;
    eventTime?: string;
    remark?: string;
  }

  export interface AuxiliaryItem {
    timelineKey?: string;
    materialType?: string;
    materialTypeName?: string;
    materialCode?: string;
    materialName?: string;
    batchNo?: string;
    usageInfo?: string;
    sourceTable?: string;
  }

  export interface ProcessParamItem {
    timelineKey?: string;
    paramCode?: string;
    paramName?: string;
    paramValue?: string;
    paramValueNum?: number;
    uom?: string;
    recordTime?: string;
    recorderName?: string;
    sourceFormName?: string;
    remark?: string;
  }

  export interface TimelineNode {
    key: string;
    sourceType?: string;
    sourceId?: number;
    processCode?: string;
    processName?: string;
    processOrder?: number;
    batchNo?: string;
    sourceBatchNo?: string;
    parentBatchNo?: string;
    status?: string;
    statusText?: string;
    eventTime?: string;
    summary?: string;
    reportNo?: string;
    reportStatus?: string;
    planNo?: string;
    workCenterName?: string;
    equipmentName?: string;
    recorderName?: string;
    confirmerName?: string;
    details?: Record<string, string>;
    qualityItems?: QualityItem[];
    auxiliaryItems?: AuxiliaryItem[];
    processParams?: ProcessParamItem[];
  }

  export interface TraceRespVO {
    overview?: Overview;
    treeNodes?: TreeNode[];
    timelineNodes?: TimelineNode[];
    qualityEvents?: QualityItem[];
    auxiliaryEvents?: AuxiliaryItem[];
    processParamEvents?: ProcessParamItem[];
  }
}

export function getBatchTrace(batchNo: string) {
  return requestClient.get<MesHcBatchTraceApi.TraceRespVO>('/mes/hc/plan/batch-trace/trace', {
    params: { batchNo },
  });
}
