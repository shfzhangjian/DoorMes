import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSupplierExitApprovalApi {
  export interface SupplierExitApproval {
    id?: number;
    exitNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    materialName?: string;
    materialCode?: string;
    materialModel?: string;
    reasonQualityDeliveryService?: boolean;
    reasonSupplierInitiated?: boolean;
    reasonBusinessAdjustment?: boolean;
    reasonOther?: boolean;
    reasonOtherText?: string;
    exitReasonDesc?: string;
    replacementSupplierStatus?: 'CONFIRMED' | 'UNCONFIRMED' | string;
    replacementSupplierName?: string;
    stockStatus?: 'NONE' | 'REMAINING' | string;
    remainingStockDesc?: string;
    stockDisposalReturn?: boolean;
    stockDisposalScrap?: boolean;
    stockDisposalConsume?: boolean;
    stockDisposalOther?: boolean;
    stockDisposalOtherText?: string;
    contractPaymentCleared?: boolean;
    unpaidAmount?: number | string;
    uninvoicedAmount?: number | string;
    businessRiskImpact?: string;
    impactDesc?: string;
    materialCodeCreated?: boolean;
    materialCodeCompleteDate?: string;
    supplierRosterCreated?: boolean;
    supplierRosterCompleteDate?: string;
    specReq?: string;
    natureRequirement?: string;
    certRequirement?: string;
    status?:
      | 'ARCHIVED'
      | 'DEPT_SIGN'
      | 'DRAFT'
      | 'ENTRY_PROCESSING'
      | 'GENERAL_MANAGER_REVIEW'
      | 'PURCHASE_INTAKE'
      | 'PURCHASE_REVIEW'
      | 'PURCHASE_TRANSFER'
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
    signs?: SupplierExitApprovalSign[];
    logs?: SupplierExitApprovalLog[];
    createTime?: string;
    updateTime?: string;
  }

  export interface SupplierExitApprovalLog {
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

  export interface SupplierExitApprovalSign {
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
    materialCodeCompleteDate?: string;
    materialEntryOpinion?: string;
    supplierRosterCompleteDate?: string;
    supplierRosterEntryOpinion?: string;
  }
}

export function getSupplierExitApprovalPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<SrmSupplierExitApprovalApi.SupplierExitApproval>>(
    '/mes/srm/supplier-exit-approval/page',
    { params },
  );
}

export function getSupplierExitApproval(id: number) {
  return requestClient.get<SrmSupplierExitApprovalApi.SupplierExitApproval>(
    `/mes/srm/supplier-exit-approval/get?id=${id}`,
  );
}

export function createSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.SupplierExitApproval,
) {
  return requestClient.post<number>('/mes/srm/supplier-exit-approval/create', data);
}

export function updateSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.SupplierExitApproval,
) {
  return requestClient.put<boolean>('/mes/srm/supplier-exit-approval/update', data);
}

export function deleteSupplierExitApproval(id: number) {
  return requestClient.delete<boolean>(
    `/mes/srm/supplier-exit-approval/delete?id=${id}`,
  );
}

export function submitSupplierExitApproval(id: number) {
  return requestClient.put<boolean>('/mes/srm/supplier-exit-approval/submit', { id });
}

export function purchaseIntakeSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.PurchaseIntakeAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-exit-approval/purchase-intake',
    data,
  );
}

export function signSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.ReviewAction & { signId?: number },
) {
  return requestClient.put<boolean>('/mes/srm/supplier-exit-approval/sign', data);
}

export function purchaseTransferSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.PurchaseTransferAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-exit-approval/purchase-transfer',
    data,
  );
}

export function entrySupplierExitApproval(
  data: SrmSupplierExitApprovalApi.EntryAction,
) {
  return requestClient.put<boolean>('/mes/srm/supplier-exit-approval/entry', data);
}

export function useDeptReviewSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-exit-approval/use-dept-review',
    data,
  );
}

export function qualityReviewSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-exit-approval/quality-review',
    data,
  );
}

export function techReviewSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-exit-approval/tech-review',
    data,
  );
}

export function purchaseReviewSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-exit-approval/purchase-review',
    data,
  );
}

export function generalManagerReviewSupplierExitApproval(
  data: SrmSupplierExitApprovalApi.ReviewAction,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-exit-approval/general-manager-review',
    data,
  );
}
