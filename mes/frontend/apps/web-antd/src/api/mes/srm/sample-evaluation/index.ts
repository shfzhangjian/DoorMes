import type { PageParam, PageResult } from '@vben/request';

import type { SrmSampleRequestApi } from '../sample-req';

import { requestClient } from '#/api/request';

export namespace SrmSampleEvaluationApi {
  export type Status =
    | 'ARCHIVE_CONFIRM'
    | 'ARCHIVED'
    | 'DEPT_SIGN'
    | 'DRAFT'
    | 'FINAL_APPROVAL'
    | 'INITIATOR_CONFIRM'
    | 'INSPECTION_REPORT'
    | 'VALUE_CONFIRM';

  export interface Item {
    id?: number;
    itemJudgement?: string;
    itemName?: string;
    itemStatus?: string;
    rowNo?: number;
    technicalRequirement?: string;
    testData1?: string;
    testData2?: string;
    testData3?: string;
    testData4?: string;
    testData5?: string;
  }

  export interface Sign {
    deptCode?: string;
    deptName?: string;
    id?: number;
    requireAttachment?: boolean;
    signOpinion?: string;
    signResult?: string;
    signStatus?: string;
    signTime?: string;
    userId?: number;
    userName?: string;
  }

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

  export interface SampleEvaluation {
    applyDate?: string;
    applyDept?: string;
    approvedByName?: string;
    approvedByUserId?: number;
    archiveOpinion?: string;
    archiveTime?: string;
    assignedInspectorDeptId?: number;
    assignedInspectorDeptName?: string;
    assignedInspectorUserId?: number;
    assignedInspectorUserName?: string;
    assignedTime?: string;
    canArchiveConfirm?: boolean;
    canEdit?: boolean;
    canFinalApprove?: boolean;
    canInitiatorDecision?: boolean;
    canInspectionReport?: boolean;
    canIssueTrialValidation?: boolean;
    canSign?: boolean;
    canSubmit?: boolean;
    canValueConfirm?: boolean;
    canWithdrawConfirm?: boolean;
    confirmOpinion?: string;
    confirmResult?: string;
    confirmTime?: string;
    confirmUserId?: number;
    confirmUserName?: string;
    createTime?: string;
    currentNodeName?: string;
    currentSignId?: number;
    evaluationDate?: string;
    evaluationNo?: string;
    finalApproverHandleTime?: string;
    finalApproverOpinion?: string;
    finalApproverUserId?: number;
    finalApproverUserName?: string;
    id?: number;
    initiatorDecisionOpinion?: string;
    initiatorUserId?: number;
    initiatorUserName?: string;
    inspectionTypes?: string[];
    items?: Item[];
    logs?: Log[];
    materialModel?: string;
    materialName?: string;
    processInstanceId?: string;
    projectCode?: string;
    projectId?: number;
    projectName?: string;
    projectUsers?: ProjectUser[];
    remark?: string;
    reportTime?: string;
    reporterUserId?: number;
    reporterUserName?: string;
    sampleQty?: number;
    sampleRequestId?: number;
    sampleRequestNo?: string;
    sampleSendCount?: number;
    signs?: Sign[];
    status?: Status | string;
    supplierCode?: string;
    supplierId?: number;
    supplierName?: string;
    updateTime?: string;
    verificationTypes?: string[];
    version?: number;
  }

  export interface InspectionReportAction {
    confirmUserId?: number;
    confirmUserName?: string;
    id: number;
    items: Item[];
  }

  export interface SubmitAction {
    id: number;
    inspectorUserId: number;
    inspectorUserName?: string;
  }

  export interface SignUser {
    deptCode?: string;
    deptName?: string;
    userId?: number;
    userName?: string;
  }

  export interface ValueConfirmAction {
    id: number;
    opinion?: string;
    passed: boolean;
    signUsers?: SignUser[];
  }

  export interface SignAction {
    id: number;
    opinion?: string;
    result: string;
    signId?: number;
  }

  export interface InitiatorDecisionAction {
    directArchive: boolean;
    id: number;
    opinion?: string;
  }

  export interface OpinionAction {
    id: number;
    opinion?: string;
  }

  export interface Project {
    createTime?: string;
    id?: number;
    projectCode?: string;
    projectName?: string;
    remark?: string;
    status?: string;
    updateTime?: string;
    users?: ProjectUser[];
    version?: number;
  }

  export interface ProjectUser {
    canAssign?: boolean;
    canInitiate?: boolean;
    deptCode?: string;
    deptName?: string;
    id?: number;
    projectId?: number;
    remark?: string;
    sortNo?: number;
    userId?: number;
    userName?: string;
  }

  export interface AssignableInspector {
    deptId?: number;
    deptName?: string;
    recommended?: boolean;
    userId?: number;
    userName?: string;
  }

  export interface AssignableInspectors {
    projectCode?: string;
    projectId?: number;
    projectName?: string;
    recommendedUserId?: number;
    recommendedUserName?: string;
    users?: AssignableInspector[];
  }
}

const BASE_URL = '/mes/srm/sample-evaluation';

export function getSampleEvaluationPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmSampleEvaluationApi.SampleEvaluation>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getSampleEvaluation(id: number) {
  return requestClient.get<SrmSampleEvaluationApi.SampleEvaluation>(
    `${BASE_URL}/get`,
    { params: { id } },
  );
}

export function createSampleEvaluation(
  data: SrmSampleEvaluationApi.SampleEvaluation,
) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function updateSampleEvaluation(
  data: SrmSampleEvaluationApi.SampleEvaluation,
) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function deleteSampleEvaluation(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete`, {
    params: { id },
  });
}

export function getSampleEvaluationSampleRequestPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmSampleRequestApi.SampleRequest>>(
    `${BASE_URL}/sample-request-page`,
    { params },
  );
}

export function getLatestSampleEvaluationItems(sampleRequestId: number) {
  return requestClient.get<SrmSampleEvaluationApi.Item[]>(
    `${BASE_URL}/latest-items`,
    { params: { sampleRequestId } },
  );
}

export function getSampleEvaluationHistoryPage(
  sampleRequestId: number,
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmSampleEvaluationApi.SampleEvaluation>>(
    `${BASE_URL}/history-page`,
    { params: { ...params, sampleRequestId } },
  );
}

export function getSampleEvaluationProjectEnabledList() {
  return requestClient.get<SrmSampleEvaluationApi.Project[]>(
    `${BASE_URL}/project/enabled-list`,
  );
}

export function getSampleEvaluationProjectConfig(projectId: number) {
  return requestClient.get<SrmSampleEvaluationApi.Project>(
    `${BASE_URL}/project/get`,
    { params: { projectId } },
  );
}

export function getSampleEvaluationAssignableInspectors(projectId: number) {
  return requestClient.get<SrmSampleEvaluationApi.AssignableInspectors>(
    `${BASE_URL}/assignable-inspectors`,
    { params: { projectId } },
  );
}

export function submitSampleEvaluation(
  data: SrmSampleEvaluationApi.SubmitAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/submit`, data);
}

export function inspectionReportSampleEvaluation(
  data: SrmSampleEvaluationApi.InspectionReportAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/inspection-report`, data);
}

export function valueConfirmSampleEvaluation(
  data: SrmSampleEvaluationApi.ValueConfirmAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/value-confirm`, data);
}

export function signSampleEvaluation(data: SrmSampleEvaluationApi.SignAction) {
  return requestClient.put<boolean>(`${BASE_URL}/sign`, data);
}

export function initiatorDecisionSampleEvaluation(
  data: SrmSampleEvaluationApi.InitiatorDecisionAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/initiator-decision`, data);
}

export function finalApproveSampleEvaluation(
  data: SrmSampleEvaluationApi.OpinionAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/final-approve`, data);
}

export function archiveConfirmSampleEvaluation(
  data: SrmSampleEvaluationApi.OpinionAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/archive-confirm`, data);
}

export function withdrawConfirmSampleEvaluation(
  data: SrmSampleEvaluationApi.OpinionAction,
) {
  return requestClient.put<boolean>(`${BASE_URL}/withdraw-confirm`, data);
}

export function issueTrialValidation(id: number) {
  return requestClient.put<number>(`${BASE_URL}/issue-trial-validation`, null, {
    params: { id },
  });
}
