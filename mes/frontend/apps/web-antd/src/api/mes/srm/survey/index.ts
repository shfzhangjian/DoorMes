import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSurveyApi {
  export interface SurveyReview {
    id?: number;
    surveyId?: number;
    reviewProject?: string;
    reviewDept?: string;
    reviewResult?: 'PASS' | 'REJECT';
    reviewOpinion?: string;
    reviewerId?: number;
    reviewerName?: string;
    reviewTime?: string;
    sort?: number;
  }

  export interface Survey {
    id?: number;
    surveyNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    unregisteredSupplier?: boolean;
    supplierSourceType?: 'REGISTERED';
    surveyDate?: string;
    surveyLeaderId?: number;
    surveyLeaderName?: string;
    conclusion?: string;
    confirmInitializePending?: boolean;
    nature?: 'AGENT' | 'MANUFACTURER';
    registerAddress?: string;
    establishDate?: string;
    legalPerson?: string;
    registeredCapital?: number;
    contactName?: string;
    contactPhone?: string;
    area?: number;
    employeeCount?: number;
    industryRank?: string;
    rdRatio?: number;
    qaRatio?: number;
    annualCapacity?: string;
    mainBrand?: string;
    coopYears?: number;
    capitalScale?: string;
    agentDelivery?: string;
    score?: number;
    status?: 'APPROVING' | 'DRAFT' | 'PASSED' | 'REJECTED';
    applicantId?: number; // 登记人ID
    applicantName?: string; // 登记人
    applyTime?: string; // 调查表登记时间，yyyy-MM-dd HH:mm:ss
    extraJson?: string;
    remark?: string;
    version?: number;
    reviews?: SurveyReview[];
    createTime?: string;
    updateTime?: string;
  }
}

export function getSurveyPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<SrmSurveyApi.Survey>>(
    '/mes/srm/survey/page',
    { params },
  );
}

export function getSurvey(id: number) {
  return requestClient.get<SrmSurveyApi.Survey>(`/mes/srm/survey/get?id=${id}`);
}

export function createSurvey(data: SrmSurveyApi.Survey) {
  return requestClient.post<number>('/mes/srm/survey/create', data);
}

export function updateSurvey(data: SrmSurveyApi.Survey) {
  return requestClient.put<boolean>('/mes/srm/survey/update', data);
}

export function deleteSurvey(id: number) {
  return requestClient.delete<boolean>(`/mes/srm/survey/delete?id=${id}`);
}
