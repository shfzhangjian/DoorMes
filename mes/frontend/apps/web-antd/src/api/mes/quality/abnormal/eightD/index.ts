import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesEightDApi {
  export interface TeamMember {
    id?: number;
    reportId?: number;
    memberRole?: string;
    deptId?: number;
    deptName?: string;
    userId?: number;
    userName?: string;
    responsibility?: string;
    sort?: number;
  }

  export interface Relation {
    id?: number;
    reportId?: number;
    reportNo?: string;
    relationType?: string;
    relatedObjectId?: number;
    relatedObjectNo?: string;
    relatedObjectName?: string;
    relationStatus?: string;
    primaryFlag?: boolean;
    relationTime?: string;
    relationUserId?: number;
    relationUserName?: string;
    remark?: string;
  }

  export interface ActionItem {
    id?: number;
    reportId?: number;
    reportNo?: string;
    actionType?: string;
    actionDesc?: string;
    rootCauseCategory?: string;
    ownerUserId?: number;
    ownerUserName?: string;
    ownerDeptId?: number;
    ownerDeptName?: string;
    planFinishDate?: string;
    actualFinishDate?: string;
    itemStatus?: string;
    finishDesc?: string;
    verificationResult?: string;
    sort?: number;
    remark?: string;
  }

  export interface FlowLog {
    id?: number;
    reportId?: number;
    reportNo?: string;
    actionCode?: string;
    actionName?: string;
    fromStatus?: string;
    toStatus?: string;
    fromStep?: string;
    toStep?: string;
    fromNodeCode?: string;
    fromNodeName?: string;
    toNodeCode?: string;
    toNodeName?: string;
    opinion?: string;
    handlerUserId?: number;
    handlerUserName?: string;
    handleTime?: string;
    businessSnapshot?: Record<string, any>;
  }

  export interface EightDRecord {
    id?: number;
    reportNo?: string;
    sourceType?: string;
    sourceId?: number;
    sourceNo?: string;
    issueDate?: string;
    targetDate?: string;
    currentStep?: string;
    status?: string;
    currentNodeCode?: string;
    currentNodeName?: string;
    currentHandlerUserId?: number;
    currentHandlerUserName?: string;
    nextHandlerUserId?: number;
    nextHandlerUserName?: string;
    initiatorUserId?: number;
    initiatorUserName?: string;
    initiatorDeptId?: number;
    initiatorDeptName?: string;
    problemDesc?: string;
    containmentAction?: string;
    containmentOwnerId?: number;
    containmentOwnerName?: string;
    containmentDate?: string;
    rootCauseCategory?: string;
    rootCauseAnalysis?: string;
    correctiveAction?: string;
    actionOwnerId?: number;
    actionOwnerName?: string;
    actionPlanDate?: string;
    validationResult?: string;
    validationDate?: string;
    updateSop?: boolean;
    updateFmea?: boolean;
    updateControlPlan?: boolean;
    standardizeDesc?: string;
    closeTime?: string;
    closeUserId?: number;
    closeUserName?: string;
    createTime?: string;
    teamMembers?: TeamMember[];
    relations?: Relation[];
    actionItems?: ActionItem[];
    flowLogs?: FlowLog[];
    tabType?: string;
    flowOpinion?: string;
  }

  export interface EightDPageReq extends PageParam {
    tabType?: string;
    reportNo?: string;
    sourceNo?: string;
    sourceType?: string;
    currentStep?: string;
    status?: string;
    issueDate?: string[];
    targetDate?: string[];
  }

  export interface HandleReq extends EightDRecord {
    id: number;
    opinion?: string;
    nextHandlerUserId?: number;
    nextHandlerUserName?: string;
  }

  export interface ReturnReq {
    id: number;
    targetStep?: string;
    opinion: string;
  }

  export interface CloseReq {
    id: number;
    validationResult?: string;
    standardizeDesc?: string;
    opinion?: string;
  }

  export interface LinkSourceReq {
    id: number;
    relationType: string;
    relatedObjectId?: number;
    relatedObjectNo: string;
    relatedObjectName?: string;
    relationStatus?: string;
    remark?: string;
  }

  export interface ActionItemDoneReq {
    itemId: number;
    finishDesc: string;
    verificationResult?: string;
    itemStatus?: string;
  }
}

const BASE_URL = '/mes/qms-8d-report';

export function getEightDPage(params: MesEightDApi.EightDPageReq) {
  return requestClient.get<PageResult<MesEightDApi.EightDRecord>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getEightDReport(id: number) {
  return requestClient.get<MesEightDApi.EightDRecord>(
    `${BASE_URL}/get?id=${id}`,
  );
}

export function createEightDReport(data: MesEightDApi.EightDRecord) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function updateEightDReport(data: MesEightDApi.EightDRecord) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function handleEightDReport(data: MesEightDApi.HandleReq) {
  return requestClient.post<boolean>(`${BASE_URL}/handle`, data);
}

export function returnEightDReport(data: MesEightDApi.ReturnReq) {
  return requestClient.post<boolean>(`${BASE_URL}/return`, data);
}

export function closeEightDReport(data: MesEightDApi.CloseReq) {
  return requestClient.post<boolean>(`${BASE_URL}/close`, data);
}

export function linkEightDSource(data: MesEightDApi.LinkSourceReq) {
  return requestClient.post<boolean>(`${BASE_URL}/link-source`, data);
}

export function doneEightDActionItem(data: MesEightDApi.ActionItemDoneReq) {
  return requestClient.post<boolean>(`${BASE_URL}/action-item-done`, data);
}

export function getEightDFlowLogs(reportId: number) {
  return requestClient.get<MesEightDApi.FlowLog[]>(`${BASE_URL}/flow-log/list`, {
    params: { reportId },
  });
}

export function exportEightDExcel(params: MesEightDApi.EightDPageReq) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}
