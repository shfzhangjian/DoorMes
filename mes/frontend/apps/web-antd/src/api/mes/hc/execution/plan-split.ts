import { requestClient } from '#/api/request';

export namespace MesHcPlanSplitApi {
  export interface OperationOption {
    operationId: number;
    opSeq?: number;
    opCode?: string;
    opName?: string;
  }

  export interface SplitItem {
    sourceId?: number;
    stockId?: number;
    sourceTable?: string;
    code?: string;
    batchNo?: string;
    sourceBatchNo?: string;
    qty?: number;
    unit?: string;
  }

  export interface Node {
    nodeKey: string;
    stageCode?: string;
    stageName?: string;
    operationId?: number;
    opSeq?: number;
    opCode?: string;
    opName?: string;
    operationStatus?: string;
    batchNo?: string;
    sourceBatchNo?: string;
    totalQty?: number;
    processedQty?: number;
    remainingQty?: number;
    unit?: string;
    splitable?: boolean;
    splitMode?: 'METER' | 'PIECE' | string;
    sourceTable?: string;
    planned?: boolean;
    hasReport?: boolean;
    hasConfirmedReport?: boolean;
    reportState?: 'PLANNED_NOT_REPORTED' | 'REPORTED_CONFIRMED' | 'REPORTED_UNCONFIRMED' | string;
    transferIn?: boolean;
    sourcePlanNo?: string;
    sourceOperationName?: string;
    sourceSplitQty?: number;
    sourceSplitUnit?: string;
    blockedReason?: string;
    placeholder?: boolean;
    remark?: string;
    availableItems?: SplitItem[];
  }

  export interface Edge {
    from: string;
    to: string;
    label?: string;
  }

  export interface GraphRespVO {
    planId?: number;
    planNo?: string;
    planDate?: string;
    planStatus?: string;
    modelCode?: string;
    modelName?: string;
    materialCode?: string;
    materialName?: string;
    batchNo?: string;
    targetQty?: number;
    targetUom?: string;
    operations?: OperationOption[];
    nodes?: Node[];
    edges?: Edge[];
  }

  export interface SplitReqVO {
    planNo: string;
    nodeKey: string;
    splitQty: number;
    unit?: string;
    sourceIds?: number[];
    startOperationId?: number;
    endOperationId?: number;
    targetMaterialId?: number;
    targetMaterialCode?: string;
    targetMaterialName?: string;
    targetModelId?: number;
    targetModelCode?: string;
    targetModelName?: string;
    instructionText?: string;
    remark?: string;
  }

  export interface SplitRespVO {
    splitOrderId?: number;
    newPlanId?: number;
    newPlanNo?: string;
    status?: string;
    message?: string;
  }
}

export async function getPlanSplitGraph(planNo: string) {
  return requestClient.get<MesHcPlanSplitApi.GraphRespVO>('/mes/hc/execution/plan-split/graph', {
    params: { planNo },
  });
}

export async function createPlanSplit(data: MesHcPlanSplitApi.SplitReqVO) {
  return requestClient.post<MesHcPlanSplitApi.SplitRespVO>(
    '/mes/hc/execution/plan-split/split',
    data,
  );
}
