import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcResearchTaskApi {
  export interface ResearchTask {
    id?: number;
    taskNo?: string;
    rdModelCode?: string;
    displayModelCode?: string;
    taskStatus?: string;
    researchDate?: string;
    issueUserId?: number;
    issueUserName?: string;
    issueTime?: string;
    productClassCode?: string;
    productClassName?: string;
    batchTypeCode?: string;
    baseFormulaCode?: string;
    baseFormulaName?: string;
    wetProcessCode?: string;
    wetProcessName?: string;
    grindingProcessCode?: string;
    grindingProcessName?: string;
    postProcessCode?: string;
    postProcessName?: string;
    reuseSeq?: number;
    routeId?: number;
    routeCode?: string;
    routeName?: string;
    routeVersion?: string;
    batchRuleCode?: string;
    targetQty?: number;
    targetUom?: string;
    taskPurpose?: string;
    formulaUsageCount?: number;
    wetUsageCount?: number;
    grindingUsageCount?: number;
    postUsageCount?: number;
    combinationUsageCount?: number;
    archivedModelId?: number;
    archivedModelCode?: string;
    archivedTime?: string;
    planId?: number;
    planNo?: string;
    snapshotJson?: string;
    remark?: string;
    routeOperations?: RouteOperation[];
    creator?: string;
    createTime?: string;
    updater?: string;
    updateTime?: string;
  }

  export interface RouteOperation {
    opSeq?: number;
    opCode?: string;
    opName?: string;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    instructionText?: string;
  }

  export interface PageReqVO extends PageParam {
    taskNo?: string;
    rdModelCode?: string;
    keyword?: string;
    taskStatus?: string;
    taskStatuses?: string[];
    productClassCode?: string;
    baseFormulaCode?: string;
    wetProcessCode?: string;
    grindingProcessCode?: string;
    postProcessCode?: string;
    routeCode?: string;
    researchDateStart?: string;
    researchDateEnd?: string;
  }

  export interface CodePreviewReq {
    id?: number;
    productClassCode?: string;
    baseFormulaCode?: string;
    wetProcessCode?: string;
    grindingProcessCode?: string;
    postProcessCode?: string;
  }

  export interface CodePreviewResp {
    rdModelCode?: string;
    displayModelCode?: string;
    reuseSeq?: number;
    combinationUsageCount?: number;
    formulaUsageCount?: number;
    wetUsageCount?: number;
    grindingUsageCount?: number;
    postUsageCount?: number;
  }
}

export async function getResearchTaskPage(
  params: MesHcResearchTaskApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesHcResearchTaskApi.ResearchTask>>(
    '/mes/hc/plan/research-task/page',
    { params },
  );
}

export async function getResearchTask(id: number) {
  return requestClient.get<MesHcResearchTaskApi.ResearchTask>(
    `/mes/hc/plan/research-task/get?id=${id}`,
  );
}

export async function createResearchTask(
  data: MesHcResearchTaskApi.ResearchTask,
) {
  return requestClient.post<number>('/mes/hc/plan/research-task/create', data);
}

export async function updateResearchTask(
  data: MesHcResearchTaskApi.ResearchTask,
) {
  return requestClient.put<boolean>('/mes/hc/plan/research-task/update', data);
}

export async function confirmResearchTask(id: number) {
  return requestClient.put<boolean>(
    `/mes/hc/plan/research-task/confirm?id=${id}`,
  );
}

export async function archiveResearchTaskToModel(id: number) {
  return requestClient.put<number>(
    `/mes/hc/plan/research-task/archive-to-model?id=${id}`,
  );
}

export async function deleteResearchTask(id: number) {
  return requestClient.delete<boolean>(
    `/mes/hc/plan/research-task/delete?id=${id}`,
  );
}

export async function deleteResearchTaskList(ids: number[]) {
  return requestClient.delete<boolean>(
    `/mes/hc/plan/research-task/delete-list?ids=${ids.join(',')}`,
  );
}

export async function previewResearchTaskCode(
  params: MesHcResearchTaskApi.CodePreviewReq,
) {
  return requestClient.get<MesHcResearchTaskApi.CodePreviewResp>(
    '/mes/hc/plan/research-task/preview-code',
    { params },
  );
}

export async function exportResearchTask(params: Record<string, any>) {
  return requestClient.download('/mes/hc/plan/research-task/export-excel', {
    params,
  });
}
