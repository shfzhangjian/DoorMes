import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSampleRequestApi {
  export type ApplyType = 'NORMAL' | 'URGENT';
  export type SpecifiedSupplierType = 'HAS' | 'NONE' | 'OTHER';
  export type Status =
    | 'ARCHIVE_CONFIRM'
    | 'ARCHIVED'
    | 'DRAFT'
    | 'FINAL_APPROVAL'
    | 'INITIATOR_CONFIRM'
    | 'PROJECT_REVIEW'
    | 'PURCHASE_REVIEW';

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
    archiveTime?: string;
    currentNodeName?: string;
    id?: number;
    initiatorUserId?: number;
    initiatorUserName?: string;
    materialBatchNo?: string;
    materialCode?: string;
    materialId?: number;
    materialModel?: string;
    materialName?: string;
    noticeTime?: string;
    productionCompleteTime?: string;
    quantity?: number;
    sourceSampleEvaluationId?: number;
    sourceSampleEvaluationNo?: string;
    status?: string;
    supplierCode?: string;
    supplierId?: number;
    supplierName?: string;
    trialExecutionTime?: string;
    trialNo?: string;
  }

  export interface SampleRequest {
    applicantId?: number;
    applicantName?: string;
    applyDate?: string;
    applyDept?: string;
    applyType?: ApplyType | string;
    archiveOpinion?: string;
    archiveTime?: string;
    canArchiveConfirm?: boolean;
    canEdit?: boolean;
    canFinalApprove?: boolean;
    canInitiatorDecision?: boolean;
    canProjectReview?: boolean;
    canPurchaseReview?: boolean;
    canSubmit?: boolean;
    createTime?: string;
    currentNodeName?: string;
    directArchive?: boolean;
    finalApproverHandleTime?: string;
    finalApproverOpinion?: string;
    finalApproverUserId?: number;
    finalApproverUserName?: string;
    id?: number;
    logs?: Log[];
    materialModel?: string;
    materialName?: string;
    oaApprovalUrl?: string;
    processInstanceId?: string;
    projectLeaderHandleTime?: string;
    projectLeaderOpinion?: string;
    projectLeaderUserId?: number;
    projectLeaderUserName?: string;
    purchaseDifficulty?: string;
    purchaseOwnerHandleTime?: string;
    purchaseOwnerOpinion?: string;
    purchaseOwnerUserId?: number;
    purchaseOwnerUserName?: string;
    rdSampleNecessity?: string;
    remark?: string;
    requestNo?: string;
    requireDate?: string;
    requireQty?: number;
    sampleEvaluationCount?: number;
    specifiedSupplierType?: SpecifiedSupplierType | string;
    status?: Status | string;
    supplierCode?: string;
    supplierId?: number;
    supplierName?: string;
    technicalRequirement?: string;
    trialValidations?: TrialValidation[];
    updateTime?: string;
    usedProduct?: string;
    version?: number;
  }

  export interface ReviewAction {
    id: number;
    nextPurchaseOwnerUserId?: number;
    nextPurchaseOwnerUserName?: string;
    opinion?: string;
    purchaseDifficulty?: string;
  }

  export interface InitiatorDecisionAction {
    directArchive?: boolean;
    finalApproverUserId?: number;
    finalApproverUserName?: string;
    id: number;
    opinion?: string;
  }

  export interface SupplierOption {
    supplierCode?: string;
    supplierId?: number;
    supplierKey?: string;
    supplierName?: string;
  }
}

const BASE_URL = '/mes/srm/sample-request';

export function getSampleRequestPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmSampleRequestApi.SampleRequest>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getSampleRequestSupplierPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmSampleRequestApi.SupplierOption>>(
    `${BASE_URL}/supplier-page`,
    { params },
  );
}

export function getSampleRequest(id: number) {
  return requestClient.get<SrmSampleRequestApi.SampleRequest>(
    `${BASE_URL}/get`,
    {
      params: { id },
    },
  );
}

export function createSampleRequest(data: SrmSampleRequestApi.SampleRequest) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function updateSampleRequest(data: SrmSampleRequestApi.SampleRequest) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function deleteSampleRequest(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete`, {
    params: { id },
  });
}

export function submitSampleRequest(id: number) {
  return requestClient.put<boolean>(`${BASE_URL}/submit`, { id });
}

export function projectReviewSampleRequest(
  data: SrmSampleRequestApi.ReviewAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/project-review`, data);
}

export function purchaseReviewSampleRequest(
  data: SrmSampleRequestApi.ReviewAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/purchase-review`, data);
}

export function initiatorDecisionSampleRequest(
  data: SrmSampleRequestApi.InitiatorDecisionAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/initiator-decision`, data);
}

export function finalApproveSampleRequest(
  data: SrmSampleRequestApi.ReviewAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/final-approve`, data);
}

export function archiveConfirmSampleRequest(
  data: SrmSampleRequestApi.ReviewAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/archive-confirm`, data);
}
