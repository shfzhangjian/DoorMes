import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcProductionInstructionApi {
  export type InstructionStatus = 'CONFIRMED' | 'ISSUED' | 'REVOKED' | string;

  export interface Instruction {
    id?: number;
    instructionNo?: string;
    instructionBatchNo?: string;
    parentInstructionId?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    processId?: number;
    processCode?: string;
    processName?: string;
    operationCode?: string;
    operationName?: string;
    batchNo?: string;
    productionBatchNo?: string;
    segmentBatchNo?: string;
    instructionType?: 'CANCEL' | 'CHANGEOVER' | 'DAILY' | 'FREEZE_STOCK' | 'PAUSE' | 'RESUME' | 'UNFREEZE_STOCK' | string;
    scopeType?: 'OPERATION' | 'PLAN' | 'SEGMENT' | string;
    operationIds?: number[];
    recipientIds?: number[];
    recipientNames?: string;
    recipientNotifyStatus?: 'PENDING' | 'READ' | string;
    recipientNotifyTime?: string;
    unread?: boolean;
    instructionContent?: string;
    autoRestoreFlag?: boolean;
    beforeMaterialCode?: string;
    beforeModelCode?: string;
    completedQty?: number;
    executeEndTime?: string;
    executeStartTime?: string;
    executeStatus?: 'COMPLETED' | 'EXECUTING' | 'PENDING' | string;
    executeUserId?: number;
    executeUserName?: string;
    targetMaterialCode?: string;
    targetModelCode?: string;
    targetQty?: number;
    issuerId?: number;
    issuerName?: string;
    issuedTime?: string;
    confirmerId?: number;
    confirmerName?: string;
    confirmTime?: string;
    revokedBy?: number;
    revokedByName?: string;
    revokedTime?: string;
    revokeReason?: string;
    status?: InstructionStatus;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface PageReqVO extends PageParam {
    batchNo?: string;
    issuedTimeEnd?: string;
    issuedTimeStart?: string;
    keyword?: string;
    operationCode?: string;
    operationName?: string;
    planNo?: string;
    status?: string;
  }

  export interface OperationReq {
    batchNo?: string;
    includeConfirmed?: boolean;
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    processCode?: string;
    processName?: string;
    segmentBatchNo?: string;
    instructionType?: string;
    executeStatus?: string;
  }

  export interface MessagePageReqVO extends PageParam {
    batchNo?: string;
    instructionType?: 'CANCEL' | 'CHANGEOVER' | 'DAILY' | 'FREEZE_STOCK' | 'PAUSE' | 'RESUME' | 'UNFREEZE_STOCK' | string;
    keyword?: string;
    operationCode?: string;
    operationName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    processCode?: string;
    processName?: string;
    readStatus?: 'ALL' | 'READ' | 'UNREAD' | string;
    status?: InstructionStatus;
  }

  export interface RevokeReq {
    id: number;
    revokeReason?: string;
  }

  export interface ChangeoverStartReq {
    executeUserId?: number;
    executeUserName?: string;
    id: number;
    remark?: string;
  }

  export interface ChangeoverPieceReq {
    adhesive2ReportId?: number;
    id: number;
    pieceNo: string;
    remark?: string;
  }
}

const BASE_URL = '/mes/hc/plan/production-instruction';

export async function getProductionInstructionPage(
  params: MesHcProductionInstructionApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesHcProductionInstructionApi.Instruction>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export async function getProductionInstructionList(
  params: MesHcProductionInstructionApi.PageReqVO,
) {
  return requestClient.get<MesHcProductionInstructionApi.Instruction[]>(
    `${BASE_URL}/list`,
    { params },
  );
}

export async function getOperationInstructionList(
  params: MesHcProductionInstructionApi.OperationReq,
) {
  return requestClient.get<MesHcProductionInstructionApi.Instruction[]>(
    `${BASE_URL}/operation/list`,
    { params },
  );
}

export async function getProductionInstructionMessagePage(
  params: MesHcProductionInstructionApi.MessagePageReqVO,
) {
  return requestClient.get<PageResult<MesHcProductionInstructionApi.Instruction>>(
    `${BASE_URL}/message/page`,
    { params },
  );
}

export async function getProductionInstructionUnreadCount(
  params: MesHcProductionInstructionApi.MessagePageReqVO,
) {
  return requestClient.get<number>(`${BASE_URL}/message/unread-count`, { params });
}

export async function markProductionInstructionMessageRead(id: number) {
  return requestClient.put<boolean>(`${BASE_URL}/message/read?id=${id}`);
}

export async function getProductionInstruction(id: number) {
  return requestClient.get<MesHcProductionInstructionApi.Instruction>(
    `${BASE_URL}/get?id=${id}`,
  );
}

export async function issueProductionInstruction(
  data: MesHcProductionInstructionApi.Instruction,
) {
  return requestClient.post<number>(`${BASE_URL}/issue`, data);
}

export async function updateProductionInstruction(
  data: MesHcProductionInstructionApi.Instruction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export async function confirmProductionInstruction(id: number) {
  return requestClient.put<boolean>(`${BASE_URL}/confirm?id=${id}`);
}

export async function revokeProductionInstruction(
  data: MesHcProductionInstructionApi.RevokeReq,
) {
  return requestClient.put<boolean>(`${BASE_URL}/revoke`, data);
}

export async function startChangeoverInstruction(
  data: MesHcProductionInstructionApi.ChangeoverStartReq,
) {
  return requestClient.put<MesHcProductionInstructionApi.Instruction>(
    `${BASE_URL}/changeover/start`,
    data,
  );
}

export async function recordChangeoverInstructionPiece(
  data: MesHcProductionInstructionApi.ChangeoverPieceReq,
) {
  return requestClient.post<MesHcProductionInstructionApi.Instruction>(
    `${BASE_URL}/changeover/piece`,
    data,
  );
}

export async function deleteProductionInstruction(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete?id=${id}`);
}

export async function exportProductionInstruction(params: Record<string, any>) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}
