import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmOnboardingApplyApi {
  export interface OnboardingApply {
    id?: number;
    applyNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    materialName?: string;
    materialCode?: string;
    materialModel?: string;
    applicableProduct?: string;
    importType?: 'CUSTOMER_SPECIFIED' | 'NEW' | 'REPLACE' | string;
    replacedMaterialCode?: string;
    replacedMaterialName?: string;
    customerMaterialCode?: string;
    customerMaterialName?: string;
    applyReason?: string;
    supplierAdvantageDesc?: string;
    supplementDesc?: string;
    materialCodeCreated?: boolean;
    materialCodeCompleteDate?: string;
    supplierRosterCreated?: boolean;
    supplierRosterCompleteDate?: string;
    specReq?: string;
    natureRequirement?: string;
    certRequirement?: string;
    status?:
      | 'ARCHIVED'
      | 'DRAFT'
      | 'GENERAL_MANAGER_REVIEW'
      | 'PURCHASE_REVIEW'
      | 'QUALITY_REVIEW'
      | 'REJECTED'
      | 'TECH_REVIEW'
      | 'USE_DEPT_REVIEW'
      | string;
    currentNodeName?: string;
    processInstanceId?: string;
    purchaseHandlerUserId?: number;
    purchaseHandlerUserName?: string;
    purchaseIntakeOpinion?: string;
    purchaseIntakeTime?: string;
    useDeptReviewerUserId?: number;
    useDeptReviewerUserName?: string;
    useDeptResult?: 'PASS' | 'REJECT' | string;
    useDeptOpinion?: string;
    useDeptHandleTime?: string;
    qualityReviewerUserId?: number;
    qualityReviewerUserName?: string;
    qualityResult?: 'PASS' | 'REJECT' | string;
    qualityOpinion?: string;
    qualityHandleTime?: string;
    techReviewerUserId?: number;
    techReviewerUserName?: string;
    techResult?: 'PASS' | 'REJECT' | string;
    techOpinion?: string;
    techHandleTime?: string;
    purchaseReviewerUserId?: number;
    purchaseReviewerUserName?: string;
    purchaseResult?: 'PASS' | 'REJECT' | string;
    purchaseOpinion?: string;
    purchaseHandleTime?: string;
    generalManagerUserId?: number;
    generalManagerUserName?: string;
    generalManagerResult?: 'PASS' | 'REJECT' | string;
    generalManagerOpinion?: string;
    generalManagerHandleTime?: string;
    materialEntryUserId?: number;
    materialEntryUserName?: string;
    materialEntryRequiredDate?: string;
    materialEntryCode?: string;
    materialEntryOpinion?: string;
    materialEntryHandleTime?: string;
    supplierRosterEntryUserId?: number;
    supplierRosterEntryUserName?: string;
    supplierRosterEntryRequiredDate?: string;
    supplierRosterEntryCode?: string;
    supplierRosterEntryOpinion?: string;
    supplierRosterEntryHandleTime?: string;
    applicantId?: number;
    applicantName?: string;
    applyDept?: string;
    applyTime?: string;
    remark?: string;
    version?: number;
    canEdit?: boolean;
    canSubmit?: boolean;
    canPurchaseIntake?: boolean;
    canUseDeptReview?: boolean;
    canQualityReview?: boolean;
    canTechReview?: boolean;
    canPurchaseReview?: boolean;
    canGeneralManagerReview?: boolean;
    canSign?: boolean;
    currentSignId?: number;
    canPurchaseTransfer?: boolean;
    canMaterialEntry?: boolean;
    canSupplierRosterEntry?: boolean;
    canArchive?: boolean;
    signs?: OnboardingApplySign[];
    logs?: OnboardingApplyLog[];
    createTime?: string;
    updateTime?: string;
  }

  export interface OnboardingApplyLog {
    id?: number;
    action?: string;
    actionName?: string;
    fromStatus?: string;
    toStatus?: string;
    operatorId?: number;
    operatorName?: string;
    actionDescription?: string;
    detailJson?: string;
    createTime?: string;
  }

  export interface OnboardingApplySign {
    id?: number;
    applyId?: number;
    deptCode?: string;
    deptName?: string;
    userId?: number;
    userName?: string;
    signStatus?: 'COMPLETED' | 'PENDING' | string;
    signResult?: 'PASS' | 'REJECT' | string;
    signOpinion?: string;
    signTime?: string;
  }

  export interface ReviewAction {
    id: number;
    opinion?: string;
    result: 'PASS' | 'REJECT';
  }

  export interface SignUser {
    deptCode?: string;
    deptName: string;
    userId: number;
    userName?: string;
  }

  export interface PurchaseIntakeAction {
    id: number;
    opinion?: string;
    signUsers: SignUser[];
    useDeptReviewerUserId: number;
    useDeptReviewerUserName?: string;
  }

  export interface PurchaseTransferAction {
    generalManagerUserId?: number;
    generalManagerUserName?: string;
    id: number;
    materialEntryUserId?: number;
    materialEntryUserName?: string;
    opinion?: string;
    supplierRosterEntryUserId?: number;
    supplierRosterEntryUserName?: string;
    transferAction: 'ARCHIVE' | 'ASSIGN_ENTRY' | 'GENERAL_MANAGER';
  }

  export interface EntryAction {
    id: number;
    materialEntryCode?: string;
    materialEntryOpinion?: string;
    supplierRosterEntryCode?: string;
    supplierRosterEntryOpinion?: string;
  }
}

export function getOnboardingApplyPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<SrmOnboardingApplyApi.OnboardingApply>>(
    '/mes/srm/onboarding-apply/page',
    { params },
  );
}

export function getOnboardingApply(id: number) {
  return requestClient.get<SrmOnboardingApplyApi.OnboardingApply>(
    `/mes/srm/onboarding-apply/get?id=${id}`,
  );
}

export function createOnboardingApply(
  data: SrmOnboardingApplyApi.OnboardingApply,
) {
  return requestClient.post<number>('/mes/srm/onboarding-apply/create', data);
}

export function updateOnboardingApply(
  data: SrmOnboardingApplyApi.OnboardingApply,
) {
  return requestClient.put<boolean>('/mes/srm/onboarding-apply/update', data);
}

export function deleteOnboardingApply(id: number) {
  return requestClient.delete<boolean>(
    `/mes/srm/onboarding-apply/delete?id=${id}`,
  );
}

export function submitOnboardingApply(id: number) {
  return requestClient.put<boolean>('/mes/srm/onboarding-apply/submit', { id });
}

export function purchaseIntakeOnboardingApply(
  data: SrmOnboardingApplyApi.PurchaseIntakeAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/onboarding-apply/purchase-intake',
    data,
  );
}

export function signOnboardingApply(
  data: SrmOnboardingApplyApi.ReviewAction & { signId?: number },
) {
  return requestClient.put<boolean>('/mes/srm/onboarding-apply/sign', data);
}

export function purchaseTransferOnboardingApply(
  data: SrmOnboardingApplyApi.PurchaseTransferAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/onboarding-apply/purchase-transfer',
    data,
  );
}

export function entryOnboardingApply(
  data: SrmOnboardingApplyApi.EntryAction,
) {
  return requestClient.put<boolean>('/mes/srm/onboarding-apply/entry', data);
}

export function useDeptReviewOnboardingApply(
  data: SrmOnboardingApplyApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/onboarding-apply/use-dept-review',
    data,
  );
}

export function qualityReviewOnboardingApply(
  data: SrmOnboardingApplyApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/onboarding-apply/quality-review',
    data,
  );
}

export function techReviewOnboardingApply(
  data: SrmOnboardingApplyApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/onboarding-apply/tech-review',
    data,
  );
}

export function purchaseReviewOnboardingApply(
  data: SrmOnboardingApplyApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/onboarding-apply/purchase-review',
    data,
  );
}

export function generalManagerReviewOnboardingApply(
  data: SrmOnboardingApplyApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/onboarding-apply/general-manager-review',
    data,
  );
}
