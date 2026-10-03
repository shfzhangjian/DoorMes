import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesExceptionApi {
  export interface TeamMember {
    id?: number;
    exceptionId?: number;
    deptId?: number;
    deptName?: string;
    userId?: number;
    userName?: string;
    delegateUserId?: number;
    delegateUserName?: string;
    delegateTime?: string;
    actualHandlerUserId?: number;
    actualHandlerUserName?: string;
    memberRole?: string;
    joinDate?: string;
    signStatus?: string;
  }

  export interface GroupTaskMember {
    id?: number;
    exceptionId?: number;
    taskId?: number;
    deptId?: number;
    deptName?: string;
    userId?: number;
    userName?: string;
    delegateUserId?: number;
    delegateUserName?: string;
    delegateTime?: string;
    actualHandlerUserId?: number;
    actualHandlerUserName?: string;
    memberRole?: string;
    sortNo?: number;
    actualFinishTime?: string;
    actionDescription?: string;
    rootCauseCategory?: string;
    rootCause?: string;
    preventiveAction?: string;
    attachmentUrls?: string[];
    confirmStatus?: string;
    confirmTime?: string;
    confirmRemark?: string;
    overdueFlag?: boolean;
  }

  export interface GroupTask {
    id?: number;
    exceptionId?: number;
    exceptionNo?: string;
    groupName?: string;
    taskType?: 'INVESTIGATION_GROUP' | 'ROOT_CAUSE_PREVENTIVE' | string;
    taskStatus?: string;
    replyCount?: number;
    dispatcherUserId?: number;
    dispatcherUserName?: string;
    executorUserId?: number;
    executorUserName?: string;
    dispatchTime?: string;
    containmentSuggestion?: string;
    containmentDeadline?: string;
    containmentDeptId?: number;
    containmentDeptName?: string;
    rootCauseOwnerId?: number;
    rootCauseOwnerName?: string;
    rootCauseAssignment?: string;
    preventiveAssignment?: string;
    actualFinishTime?: string;
    actionDescription?: string;
    rootCauseCategory?: string;
    rootCause?: string;
    preventiveAction?: string;
    attachmentUrls?: string[];
    submitterUserId?: number;
    submitterUserName?: string;
    submitTime?: string;
    reviewerUserId?: number;
    reviewerUserName?: string;
    reviewTime?: string;
    reviewOpinion?: string;
    remark?: string;
    members?: GroupTaskMember[];
    confirmLogs?: GroupTaskConfirmLog[];
    replies?: GroupTaskReply[];
    currentUserCanConfirm?: boolean;
    currentUserCanSubmit?: boolean;
    currentUserCanReview?: boolean;
  }

  export interface GroupTaskReply {
    id?: number;
    exceptionId?: number;
    taskId?: number;
    taskType?: string;
    replyNo?: number;
    replyStatus?: string;
    memberId?: number;
    memberUserId?: number;
    memberUserName?: string;
    actualFinishTime?: string;
    actionDescription?: string;
    rootCauseCategory?: string;
    rootCause?: string;
    preventiveAction?: string;
    attachmentUrls?: string[];
    submitterUserId?: number;
    submitterUserName?: string;
    submitTime?: string;
    reviewerUserId?: number;
    reviewerUserName?: string;
    reviewTime?: string;
    reviewOpinion?: string;
    remark?: string;
  }

  export interface GroupTaskConfirmLog {
    id?: number;
    exceptionId?: number;
    exceptionNo?: string;
    taskId?: number;
    taskType?: string;
    replyId?: number;
    replyNo?: number;
    memberId?: number;
    userId?: number;
    userName?: string;
    confirmAction?: string;
    confirmStatus?: string;
    confirmOpinion?: string;
    confirmTime?: string;
    taskStatusBefore?: string;
    taskStatusAfter?: string;
    remark?: string;
  }

  export interface Relation {
    id?: number;
    exceptionId?: number;
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

  export interface FlowLog {
    id?: number;
    exceptionId?: number;
    exceptionNo?: string;
    actionCode?: string;
    actionName?: string;
    fromStatus?: string;
    toStatus?: string;
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

  export interface ExceptionRecord {
    id?: number;
    exceptionNo?: string;
    sourceType?: string;
    sourceId?: number;
    sourceNo?: string;
    exceptionType?: string;
    exceptionLevel?: string;
    status?: string;
    processInstanceId?: string;
    currentNodeCode?: string;
    currentNodeName?: string;
    currentHandlerUserId?: number;
    currentHandlerUserName?: string;
    discoverDeptId?: number;
    discoverDeptCode?: string;
    discoverDeptName?: string;
    discovererId?: number;
    discovererCode?: string;
    discovererName?: string;
    discoverTime?: string;
    confirmDeptId?: number;
    confirmDeptCode?: string;
    confirmDeptName?: string;
    confirmerId?: number;
    confirmerCode?: string;
    confirmerName?: string;
    confirmTime?: string;
    isRelatedProduct?: boolean;
    relatedNcrNo?: string;
    related8dNo?: string;
    description?: string;
    initialImpact?: string;
    containmentOwnerId?: number;
    containmentOwnerName?: string;
    containmentAction?: string;
    containmentDeadline?: string;
    containmentFinishTime?: string;
    rootCauseCategory?: string;
    rootCause?: string;
    actionDeptId?: number;
    actionDeptName?: string;
    actionOwnerId?: number;
    actionOwnerName?: string;
    preventiveAction?: string;
    resultUploaderId?: number;
    resultUploaderName?: string;
    correctivePreventiveResult?: string;
    resultUploadTime?: string;
    copyUserIds?: string;
    copyUserNames?: string;
    effectConfirm?: string;
    qaConfirmValid?: boolean;
    qaConfirmerId?: number;
    qaConfirmerName?: string;
    finishTime?: string;
    closeUserId?: number;
    closeUserName?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
    teamMembers?: TeamMember[];
    groupTasks?: GroupTask[];
    relations?: Relation[];
    flowLogs?: FlowLog[];
    currentUserTaskTodo?: boolean;
    currentUserTaskTodoCount?: number;
    currentUserTaskTodoType?: string;
    currentUserTaskTodoLabel?: string;
    currentUserTaskDone?: boolean;
    currentUserTaskDoneCount?: number;
    currentUserTaskDoneType?: string;
    currentUserTaskDoneLabel?: string;
    canHandle?: boolean;
    canWithdraw?: boolean;
    listActionCode?: string;
    listActionName?: string;
    minePending?: boolean;
    mineDiscovered?: boolean;
    mineParticipated?: boolean;
    tabType?: string;
  }

  export interface ExceptionPageReq extends PageParam {
    tabType?: string;
    pendingMine?: boolean;
    discoveredMine?: boolean;
    participatedMine?: boolean;
    exceptionNo?: string;
    exceptionType?: string;
    exceptionLevel?: string;
    status?: string;
    discoverTime?: string[];
  }

  export interface HandleReq extends ExceptionRecord {
    id: number;
    actionCode?: string;
    opinion?: string;
    nextHandlerUserId?: number;
    nextHandlerUserName?: string;
  copyToUserIds?: number[];
  copyToUserNames?: string[];
  }

  export interface ReturnReq {
    id: number;
    targetNodeCode?: string;
    targetNodeName?: string;
    opinion: string;
  }

  export interface WithdrawReq {
    id: number;
    reason: string;
  }

  export interface CloseReq {
    id: number;
    effectConfirm: string;
    qaConfirmValid?: boolean;
    qaConfirmerId?: number;
    qaConfirmerName?: string;
    finishTime?: string;
    opinion?: string;
    relations?: Relation[];
  }

  export interface GroupTaskBatchSaveReq {
    exceptionId: number;
    groupTasks?: GroupTask[];
    opinion?: string;
  }

  export interface GroupTaskSubmitReq {
    id: number;
    actualFinishTime?: string;
    actionDescription?: string;
    rootCauseCategory?: string;
    rootCause?: string;
    preventiveAction?: string;
    attachmentUrls?: string[];
    opinion?: string;
  }

  export interface GroupTaskReviewReq {
    id: number;
    actionCode: 'ACCEPT' | 'RETURN';
    opinion?: string;
  }

  export interface GroupTaskMemberConfirmReq {
    taskId: number;
    memberId?: number;
    actionCode?: 'CONFIRM' | 'DISAGREE' | string;
    actualFinishTime?: string;
    actionDescription?: string;
    rootCauseCategory?: string;
    rootCause?: string;
    preventiveAction?: string;
    attachmentUrls?: string[];
    remark?: string;
  }

  export interface GroupTaskMemberConfirmResp {
    exceptionId?: number;
    exceptionNo?: string;
    taskId?: number;
    confirmStatus?: string;
    overdue?: boolean;
    message?: string;
  }

  export interface GroupTaskMemberDelegateReq {
    taskId: number;
    memberId: number;
    delegateUserId: number;
    delegateUserName?: string;
  }

  export interface LinkNcrReq {
    id: number;
    ncrId?: number;
    ncrNo?: string;
    ncrType?: 'NCR' | 'RAW_MATERIAL_NCR' | string;
    remark?: string;
  }

  export interface Generate8dReq {
    id: number;
    targetDate?: string;
    teamMembers?: Array<{
      memberRole?: string;
      deptId?: number;
      deptName?: string;
      userId?: number;
      userName?: string;
    }>;
    remark?: string;
  }

  export interface EightDReport {
    id?: number;
    reportNo?: string;
    sourceType?: string;
    sourceId?: number;
    sourceNo?: string;
    issueDate?: string;
    targetDate?: string;
    currentStep?: string;
    status?: string;
  }

  export interface ReviewConfig {
    id?: number;
    unitCode?: string;
    unitName?: string;
    deptId?: number;
    deptName?: string;
    handlerUserIds?: number[];
    handlerUserNames?: string[];
    status?: number;
    sort?: number;
    remark?: string;
    createTime?: string;
  }

  export interface ReviewConfigPageReq extends PageParam {
    unitName?: string;
    status?: number;
  }

  export interface NcrCandidate {
    ncrId?: number;
    ncrNo?: string;
    ncrType?: 'NCR' | 'RAW_MATERIAL_NCR' | string;
    status?: string;
    materialName?: string;
    batchNo?: string;
    defectCode?: string;
    remark?: string;
  }
}

const BASE_URL = '/mes/qms-exception-event';
const REVIEW_CONFIG_BASE_URL = '/mes/qms-exception-review-config';

export function getExceptionPage(params: MesExceptionApi.ExceptionPageReq) {
  return requestClient.get<PageResult<MesExceptionApi.ExceptionRecord>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getExceptionEvent(id: number) {
  return requestClient.get<MesExceptionApi.ExceptionRecord>(
    `${BASE_URL}/get?id=${id}`,
  );
}

export function createExceptionEvent(data: MesExceptionApi.ExceptionRecord) {
  return requestClient.post<MesExceptionApi.ExceptionRecord>(
    `${BASE_URL}/create`,
    data,
  );
}

export function handleExceptionEvent(data: MesExceptionApi.HandleReq) {
  return requestClient.post<boolean>(`${BASE_URL}/handle`, data);
}

export function returnExceptionEvent(data: MesExceptionApi.ReturnReq) {
  return requestClient.post<boolean>(`${BASE_URL}/return`, data);
}

export function withdrawExceptionEvent(data: MesExceptionApi.WithdrawReq) {
  return requestClient.post<boolean>(`${BASE_URL}/withdraw`, data);
}

export function closeExceptionEvent(data: MesExceptionApi.CloseReq) {
  return requestClient.post<boolean>(`${BASE_URL}/close`, data);
}

export function saveExceptionGroupTasks(
  data: MesExceptionApi.GroupTaskBatchSaveReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/group-task/save-dispatch`, data);
}

export function submitExceptionGroupTask(
  data: MesExceptionApi.GroupTaskSubmitReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/group-task/submit`, data);
}

export function reviewExceptionGroupTask(
  data: MesExceptionApi.GroupTaskReviewReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/group-task/review`, data);
}

export function confirmExceptionGroupTaskMember(
  data: MesExceptionApi.GroupTaskMemberConfirmReq,
) {
  return requestClient.post<MesExceptionApi.GroupTaskMemberConfirmResp>(
    `${BASE_URL}/group-task/member-confirm`,
    data,
  );
}

export function delegateExceptionGroupTaskMember(
  data: MesExceptionApi.GroupTaskMemberDelegateReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/group-task/member-delegate`, data);
}

export function linkExceptionNcr(data: MesExceptionApi.LinkNcrReq) {
  return requestClient.post<boolean>(`${BASE_URL}/link-ncr`, data);
}

export function generateException8d(data: MesExceptionApi.Generate8dReq) {
  return requestClient.post<MesExceptionApi.EightDReport>(
    `${BASE_URL}/generate-8d`,
    data,
  );
}

export function getExceptionReviewConfigSimpleList() {
  return requestClient.get<MesExceptionApi.ReviewConfig[]>(
    `${REVIEW_CONFIG_BASE_URL}/simple-list`,
  );
}

export function getExceptionReviewConfigPage(
  params: MesExceptionApi.ReviewConfigPageReq,
) {
  return requestClient.get<PageResult<MesExceptionApi.ReviewConfig>>(
    `${REVIEW_CONFIG_BASE_URL}/page`,
    { params },
  );
}

export function createExceptionReviewConfig(
  data: MesExceptionApi.ReviewConfig,
) {
  return requestClient.post<number>(`${REVIEW_CONFIG_BASE_URL}/create`, data);
}

export function updateExceptionReviewConfig(
  data: MesExceptionApi.ReviewConfig,
) {
  return requestClient.put<boolean>(`${REVIEW_CONFIG_BASE_URL}/update`, data);
}

export function deleteExceptionReviewConfig(id: number) {
  return requestClient.delete<boolean>(
    `${REVIEW_CONFIG_BASE_URL}/delete?id=${id}`,
  );
}

export function getExceptionFlowLogs(exceptionId: number) {
  return requestClient.get<MesExceptionApi.FlowLog[]>(
    `${BASE_URL}/flow-log/list`,
    { params: { exceptionId } },
  );
}

export function exportExceptionExcel(params: MesExceptionApi.ExceptionPageReq) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}

export async function getNcrCandidatePage(params: PageParam & Record<string, any>) {
  const page = await requestClient.get<PageResult<Record<string, any>>>(
    '/mes/qms-nc-record/page',
    {
      params: {
        pageNo: params.pageNo,
        pageSize: params.pageSize,
        ncNo: params.ncrNo || params.ncNo,
        lotNo: params.batchNo || params.lotNo,
        defectCode: params.defectCode,
        linkableExceptionOnly: true,
        mrbDecision: params.status || params.mrbDecision,
      },
    },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => ({
      ...item,
      ncrId: item.id,
      ncrNo: item.ncNo,
      ncrType: 'NCR',
      status: item.mrbDecision,
      materialName: item.remark || item.defectCode || '-',
      batchNo: item.lotNo,
      defectCode: item.defectCode,
    })),
  } as PageResult<MesExceptionApi.NcrCandidate>;
}

export async function getRawMaterialNcrCandidatePage(
  params: PageParam & Record<string, any>,
) {
  const page = await requestClient.get<PageResult<Record<string, any>>>(
    '/mes/qms-raw-material-nc-record/page',
    {
      params: {
        pageNo: params.pageNo,
        pageSize: params.pageSize,
        ncNo: params.ncrNo || params.ncNo,
        lotNo: params.batchNo || params.lotNo,
        defectCode: params.defectCode,
        finalDisposition: params.status || params.finalDisposition,
        linkableExceptionOnly: true,
        sourceType: 'RAW_MATERIAL',
      },
    },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => ({
      ...item,
      ncrId: item.id,
      ncrNo: item.ncNo,
      ncrType: 'RAW_MATERIAL_NCR',
      status: item.finalDisposition || item.mrbDecision || item.status,
      materialName: item.materialName || item.remark || '-',
      batchNo: item.lotNo,
      defectCode: item.defectCode,
    })),
  } as PageResult<MesExceptionApi.NcrCandidate>;
}
