import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSupplierReviewPlanApi {
  export type ReviewStatus =
    | 'ARCHIVED'
    | 'CANCELED'
    | 'CHANGED'
    | 'COMPLETED'
    | 'EXECUTING'
    | 'PLAN'
    | string;

  export interface UserSnapshot {
    deptName?: string;
    id?: number;
    name?: string;
  }

  export interface StatusLog {
    createTime?: string;
    fromStatus?: ReviewStatus;
    fromStatusName?: string;
    id?: number;
    monthPlanId?: number;
    operatorUserId?: number;
    operatorUserName?: string;
    reason?: string;
    toStatus?: ReviewStatus;
    toStatusName?: string;
    updateDescription?: string;
  }

  export interface Reply {
    id?: number;
    monthPlanId?: number;
    recorderUserId?: number;
    recorderUserName?: string;
    remark?: string;
    replyTime?: string;
    reviewDate?: string;
    reviewResult?: string;
  }

  export interface Participant {
    deptName?: string;
    id?: number;
    monthPlanId?: number;
    relationType?: 'LEAD' | 'RELATED' | string;
    userId?: number;
    userName?: string;
  }

  export interface MonthPlan {
    approvalOpinion?: string;
    approvalTime?: string;
    approverUserId?: number;
    approverUserName?: string;
    auditAttachment?: string;
    auditCategory?: string;
    auditCategoryName?: string;
    auditDate?: string;
    auditDesc?: string;
    executionStatus?: ReviewStatus;
    executionStatusName?: string;
    id?: number;
    leadUserId?: number;
    leadUserName?: string;
    lineId?: number;
    participants?: Participant[];
    planDesc?: string;
    planMonth?: number;
    planYear?: number;
    plannedFlag?: boolean;
    relatedUserIds?: string;
    relatedUserNames?: string;
    remark?: string;
    replies?: Reply[];
    statusLogs?: StatusLog[];
    statusRemark?: string;
    updateDescription?: string;
    version?: number;
    yearPlanId?: number;
  }

  export interface PlanLine {
    applicableProduct?: string;
    completionStatus?: string;
    contactPerson?: string;
    id?: number;
    latestAuditDate?: string;
    materialCode?: string;
    materialCodes?: string[];
    materialName?: string;
    model?: string;
    months?: MonthPlan[];
    planYear?: number;
    providedProduct?: string;
    remark?: string;
    rowNo?: number;
    supplierCode?: string;
    supplierId?: number;
    supplierName?: string;
    version?: number;
    yearPlanId?: number;
  }

  export interface YearPlan {
    approvedBy?: string;
    completionSummary?: string;
    confirmedBy?: string;
    createTime?: string;
    id?: number;
    lines?: PlanLine[];
    planNo?: string;
    planTitle?: string;
    planYear?: number;
    preparedBy?: string;
    preparedDept?: string;
    remark?: string;
    updateTime?: string;
    version?: number;
  }

  export interface AddSupplierLineReq {
    applicableProduct?: string;
    contactPerson?: string;
    materialCode?: string;
    materialName?: string;
    model?: string;
    planYear: number;
    providedProduct?: string;
    supplierCode?: string;
    supplierId?: number;
    supplierName?: string;
  }

  export interface UpdateLineContactReq {
    contactPerson?: string;
    id: number;
    remark?: string;
  }

  export interface DeleteAnnualPlansReq {
    lineId: number;
    planYear: number;
  }

  export interface DeleteAnnualPlansResp {
    deletedAttachmentCount?: number;
    deletedLineCount?: number;
    deletedMonthCount?: number;
    deletedParticipantCount?: number;
    deletedReplyCount?: number;
    deletedStatusLogCount?: number;
    lineId?: number;
    materialCode?: string;
    planYear?: number;
    supplierCode?: string;
    yearPlanId?: number;
  }

  export interface MonthSaveReq {
    approvalOpinion?: string;
    approverUserId?: number;
    approverUserName?: string;
    auditAttachment?: string;
    auditCategory?: string;
    auditDate?: string;
    auditDesc?: string;
    id: number;
    leadUserId?: number;
    leadUserName?: string;
    planDesc?: string;
    relatedUsers?: UserSnapshot[];
    remark?: string;
  }

  export interface StatusAdjustReq {
    executionStatus: ReviewStatus;
    id: number;
    statusRemark?: string;
    updateDescription?: string;
  }

  export interface SiteInspectionSaveReq {
    auditAttachment?: string;
    auditCategory?: string;
    auditDate?: string;
    auditDesc?: string;
    id: number;
  }

  export interface ReplyCreateReq {
    monthPlanId: number;
    recorderUserName?: string;
    remark?: string;
    reviewDate?: string;
    reviewResult?: string;
  }

  export interface ExecutionItem {
    applicableProduct?: string;
    auditAttachment?: string;
    auditCategory?: string;
    auditCategoryName?: string;
    auditDate?: string;
    auditDesc?: string;
    contactPerson?: string;
    executionStatus?: ReviewStatus;
    executionStatusName?: string;
    leadUserName?: string;
    lineId?: number;
    materialCode?: string;
    materialName?: string;
    model?: string;
    monthPlanId?: number;
    planDesc?: string;
    planMonth?: number;
    planYear?: number;
    providedProduct?: string;
    relatedUserNames?: string;
    supplierCode?: string;
    supplierName?: string;
    updateTime?: string;
    useDepartment?: string;
  }
}

const BASE_URL = '/mes/srm/supplier-review-plan';

export function initSupplierReviewYearPlan(planYear: number) {
  return requestClient.post<number>(
    `${BASE_URL}/init-year?planYear=${planYear}`,
  );
}

export function getSupplierReviewYearPlan(planYear: number) {
  return requestClient.get<SrmSupplierReviewPlanApi.YearPlan>(
    `${BASE_URL}/year`,
    { params: { planYear } },
  );
}

export function addSupplierReviewLine(
  data: SrmSupplierReviewPlanApi.AddSupplierLineReq,
) {
  return requestClient.post<number>(`${BASE_URL}/line/add-supplier`, data);
}

export function clearSupplierReviewMonthPlan(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/month/clear`, {
    params: { id },
  });
}

export function updateSupplierReviewLineContact(
  data: SrmSupplierReviewPlanApi.UpdateLineContactReq,
) {
  return requestClient.put<boolean>(`${BASE_URL}/line/update-contact`, data);
}

export function deleteSupplierReviewAnnualPlans(
  data: SrmSupplierReviewPlanApi.DeleteAnnualPlansReq,
) {
  return requestClient.post<SrmSupplierReviewPlanApi.DeleteAnnualPlansResp>(
    `${BASE_URL}/line/delete-annual`,
    data,
  );
}

export function getSupplierReviewMonthPlan(id: number) {
  return requestClient.get<SrmSupplierReviewPlanApi.MonthPlan>(
    `${BASE_URL}/month/get`,
    { params: { id } },
  );
}

export function updateSupplierReviewMonthPlan(
  data: SrmSupplierReviewPlanApi.MonthSaveReq,
) {
  return requestClient.put<boolean>(`${BASE_URL}/month/update`, data);
}

export function saveSupplierReviewSiteInspection(
  data: SrmSupplierReviewPlanApi.SiteInspectionSaveReq,
) {
  return requestClient.put<boolean>(
    `${BASE_URL}/month/site-inspection/save`,
    data,
  );
}

export function adjustSupplierReviewMonthStatus(
  data: SrmSupplierReviewPlanApi.StatusAdjustReq,
) {
  return requestClient.put<boolean>(`${BASE_URL}/month/adjust-status`, data);
}

export function createSupplierReviewReply(
  data: SrmSupplierReviewPlanApi.ReplyCreateReq,
) {
  return requestClient.post<number>(`${BASE_URL}/reply/create`, data);
}

export function getSupplierReviewExecutionPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmSupplierReviewPlanApi.ExecutionItem>>(
    `${BASE_URL}/execution-page`,
    { params },
  );
}
