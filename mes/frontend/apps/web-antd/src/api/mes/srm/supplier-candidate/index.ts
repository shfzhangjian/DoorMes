import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSupplierCandidateApi {
  export interface Candidate {
    candidateKey?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    usingDepartment?: string;
    shortName?: string;
    contactPerson?: string;
    contactPhone?: string;
    email?: string;
    address?: string;
    companyNature?: string;
    legalPerson?: string;
    registeredCapital?: number;
    establishDate?: string;
    mainProducts?: string;
    providedProduct?: string;
    materialCode?: string;
    model?: string;
    applicableProduct?: string;
    importDate?: string;
    materialCategory?: string;
    materialGrade?: string;
    paymentTerms?: string;
    deliveryMethod?: string;
    originPlace?: string;
    originalFactoryInfo?: string;
    level?: string;
    remark?: string;
    sourceType?: 'REGISTERED';
    unregisteredSupplier?: boolean;
    status?: string;
    scopeCode?: string;
    scopeId?: number;
    scopeName?: string;
    canEdit?: boolean;
    maskedFields?: string[];
    viewPermission?: 'EDIT' | 'FULL' | 'MASKED';
    sourceSurveyId?: number;
    sourceSurveyNo?: string;
  }

  export interface NameCheckResult {
    normalizedName?: string;
    pendingMatches?: Candidate[];
    registeredMatches?: Candidate[];
  }

  export interface ResourceStatusAdjustReq {
    sourceType: 'REGISTERED';
    supplierId?: number;
    status: string;
    reason: string;
  }

  export interface ResourceStatusLog {
    id?: number;
    sourceType?: 'REGISTERED';
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    fromStatus?: string;
    toStatus?: string;
    reason?: string;
    operatorUserId?: number;
    operatorUserName?: string;
    createTime?: string;
  }
}

export function getSupplierCandidatePage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<SrmSupplierCandidateApi.Candidate>>(
    '/mes/srm/supplier-candidate/page',
    { params },
  );
}

export function getSupplierCandidateSelectPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<SrmSupplierCandidateApi.Candidate>>(
    '/mes/srm/supplier-candidate/select-page',
    { params },
  );
}

export function checkSupplierName(supplierName: string) {
  return requestClient.get<SrmSupplierCandidateApi.NameCheckResult>(
    '/mes/srm/supplier-candidate/name-check',
    { params: { supplierName } },
  );
}

export function adjustSupplierResourceStatus(
  data: SrmSupplierCandidateApi.ResourceStatusAdjustReq,
) {
  return requestClient.put<boolean>(
    '/mes/srm/supplier-candidate/adjust-resource-status',
    data,
  );
}

export function getSupplierResourceStatusLogs(
  params: Pick<SrmSupplierCandidateApi.ResourceStatusLog, 'supplierId'>,
) {
  return requestClient.get<SrmSupplierCandidateApi.ResourceStatusLog[]>(
    '/mes/srm/supplier-candidate/resource-status-log-list',
    { params },
  );
}
