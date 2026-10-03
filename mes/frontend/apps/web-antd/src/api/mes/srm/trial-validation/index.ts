import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmTrialValidationApi {
  export type Status =
    | 'ARCHIVE_CONFIRM'
    | 'ARCHIVED'
    | 'NOTICE_SENT'
    | 'PRODUCTION_COMPLETE'
    | 'TRIAL_EXECUTION';

  export interface Log {
    action?: string;
    actionDescription?: string;
    actionName?: string;
    createTime?: string;
    fromStatus?: string;
    id?: number;
    operatorId?: number;
    operatorName?: string;
    toStatus?: string;
  }

  export interface TrialValidation {
    archiveOpinion?: string;
    archiveTime?: string;
    archiveUserId?: number;
    archiveUserName?: string;
    canArchiveConfirm?: boolean;
    canCompleteProduction?: boolean;
    canTrialExecute?: boolean;
    createTime?: string;
    currentNodeName?: string;
    id?: number;
    initiatorUserId?: number;
    initiatorUserName?: string;
    logs?: Log[];
    materialBatchNo?: string;
    materialCode?: string;
    materialId?: number;
    materialModel?: string;
    materialName?: string;
    noticeTime?: string;
    processInstanceId?: string;
    productionCompleteOpinion?: string;
    productionCompleteTime?: string;
    productionCompleteUserId?: number;
    productionCompleteUserName?: string;
    quantity?: number;
    remark?: string;
    sourceSampleEvaluationId?: number;
    sourceSampleEvaluationNo?: string;
    status?: Status | string;
    supplierCode?: string;
    supplierId?: number;
    supplierName?: string;
    trialExecutionOpinion?: string;
    trialExecutionTime?: string;
    trialExecutionUserId?: number;
    trialExecutionUserName?: string;
    trialNo?: string;
    updateTime?: string;
    version?: number;
  }

  export interface TrialExecuteAction {
    id: number;
    materialBatchNo: string;
    opinion?: string;
    quantity: number;
  }

  export type ProductionCompleteAction = TrialExecuteAction;

  export interface ArchiveConfirmAction {
    id: number;
    opinion?: string;
  }
}

const BASE_URL = '/mes/srm/trial-validation';

export function getTrialValidationPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmTrialValidationApi.TrialValidation>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getTrialValidation(id: number) {
  return requestClient.get<SrmTrialValidationApi.TrialValidation>(
    `${BASE_URL}/get`,
    { params: { id } },
  );
}

export function trialExecute(data: SrmTrialValidationApi.TrialExecuteAction) {
  return requestClient.put<boolean>(`${BASE_URL}/trial-execute`, data);
}

export function completeProduction(
  data: SrmTrialValidationApi.ProductionCompleteAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/complete-production`, data);
}

export function archiveConfirm(
  data: SrmTrialValidationApi.ArchiveConfirmAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/archive-confirm`, data);
}
