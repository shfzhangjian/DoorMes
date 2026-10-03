import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcProductionFactAdjustApi {
  export type AdjustStatus =
    | 'APPROVED'
    | 'EXECUTED'
    | 'PENDING_APPROVAL'
    | 'REJECTED'
    | string;

  export interface OperationOption {
    id: number;
    opCode?: string;
    opName?: string;
  }

  export interface SegmentOption {
    reportCount?: number;
    segmentBatchNo: string;
  }

  export interface ProductOption {
    materialCode: string;
    materialId: number;
    materialName?: string;
    modelCode: string;
    productModelId: number;
    specification?: string;
  }

  export interface InstructionOption {
    completedQty?: number;
    executeStatus?: string;
    id: number;
    instructionNo?: string;
    targetMaterialCode?: string;
    targetModelCode?: string;
    targetQty?: number;
  }

  export interface PreviewReq {
    instructionId: number;
    planNo: string;
    planOperationId: number;
    segmentBatchNo: string;
    targetMaterialCode: string;
    targetModelCode: string;
  }

  export interface Preview extends PreviewReq {
    adhesive2ReportCount?: number;
    blockingReasons?: string[];
    cutInspectionDetailCount?: number;
    cutRoundReportCount?: number;
    eligible?: boolean;
    faiOrderCount?: number;
    finishedStockCount?: number;
    fqcOrderCount?: number;
    fqcSubmissionDetailCount?: number;
    instructionNo?: string;
    operationCode?: string;
    operationName?: string;
    outputStockCount?: number;
    packagingCount?: number;
    processFormCount?: number;
    sourceMaterialCode?: string;
    sourceMaterialName?: string;
    sourceModelCode?: string;
    targetProduct?: ProductOption;
    unsafeFaiOrderCount?: number;
    unsafeFqcOrderCount?: number;
  }

  export interface CreateReq extends PreviewReq {
    adjustReason: string;
    evidenceRemark?: string;
  }

  export interface ApproveReq {
    approved: boolean;
    approveRemark?: string;
    id: number;
  }

  export interface ExecuteReq {
    executionRemark?: string;
    id: number;
  }

  export interface Order {
    adjustNo?: string;
    adjustReason?: string;
    adjustType?: string;
    applicantName?: string;
    appliedTime?: string;
    approvedTime?: string;
    approverName?: string;
    evidenceRemark?: string;
    executedTime?: string;
    executorName?: string;
    executionRemark?: string;
    id: number;
    impactSummaryJson?: string;
    instructionNo?: string;
    operationName?: string;
    planNo?: string;
    segmentBatchNo?: string;
    sourceMaterialCode?: string;
    sourceModelCode?: string;
    status?: AdjustStatus;
    targetMaterialCode?: string;
    targetMaterialName?: string;
    targetModelCode?: string;
    targetSpecification?: string;
  }

  export interface Detail {
    adhesive2ReportId?: number;
    executionRemark?: string;
    executionStatus?: string;
    newMaterialCode?: string;
    newModelCode?: string;
    oldMaterialCode?: string;
    oldModelCode?: string;
    productionBatchNo?: string;
    seqNo?: number;
  }

  export interface PageReq extends PageParam {
    keyword?: string;
    status?: AdjustStatus;
  }
}

const BASE_URL = '/mes/hc/plan/production-fact-adjust';

export async function getProductionFactAdjustOperationOptions(planNo: string) {
  return requestClient.get<MesHcProductionFactAdjustApi.OperationOption[]>(
    `${BASE_URL}/operation-option-list`,
    { params: { planNo } },
  );
}

export async function getProductionFactAdjustSegmentOptions(
  planOperationId: number,
) {
  return requestClient.get<MesHcProductionFactAdjustApi.SegmentOption[]>(
    `${BASE_URL}/segment-option-list`,
    { params: { planOperationId } },
  );
}

export async function getProductionFactAdjustProductOptions(keyword?: string) {
  return requestClient.get<MesHcProductionFactAdjustApi.ProductOption[]>(
    `${BASE_URL}/product-option-list`,
    { params: { keyword } },
  );
}

export async function getProductionFactAdjustInstructionOptions(
  planOperationId: number,
  segmentBatchNo: string,
) {
  return requestClient.get<MesHcProductionFactAdjustApi.InstructionOption[]>(
    `${BASE_URL}/instruction-option-list`,
    {
      params: { planOperationId, segmentBatchNo },
    },
  );
}

export async function previewProductionFactAdjust(
  params: MesHcProductionFactAdjustApi.PreviewReq,
) {
  return requestClient.get<MesHcProductionFactAdjustApi.Preview>(
    `${BASE_URL}/preview`,
    { params },
  );
}

export async function createProductionFactAdjust(
  data: MesHcProductionFactAdjustApi.CreateReq,
) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export async function approveProductionFactAdjust(
  data: MesHcProductionFactAdjustApi.ApproveReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/approve`, data);
}

export async function executeProductionFactAdjust(
  data: MesHcProductionFactAdjustApi.ExecuteReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/execute`, data);
}

export async function getProductionFactAdjustPage(
  params: MesHcProductionFactAdjustApi.PageReq,
) {
  return requestClient.get<PageResult<MesHcProductionFactAdjustApi.Order>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export async function getProductionFactAdjustDetailList(orderId: number) {
  return requestClient.get<MesHcProductionFactAdjustApi.Detail[]>(
    `${BASE_URL}/detail-list`,
    { params: { orderId } },
  );
}
