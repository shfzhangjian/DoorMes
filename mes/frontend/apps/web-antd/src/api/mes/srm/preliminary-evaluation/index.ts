import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmPreliminaryEvaluationApi {
  export type Decision = 'QUALIFIED' | 'UNQUALIFIED';
  export type Status =
    | 'DRAFT'
    | 'GM_REVIEW'
    | 'PENDING_CALCULATION'
    | 'PENDING_DECISION'
    | 'PENDING_PUBLISH'
    | 'PUBLISHED'
    | 'SCORING';

  export interface Item {
    id: number;
    evaluationId: number;
    templateItemId?: number;
    groupCodeSnapshot: string;
    groupNameSnapshot: string;
    groupSort: number;
    groupMaxScoreSnapshot: number;
    vetoOperatorSnapshot?: string;
    vetoScoreSnapshot?: number;
    indicatorCodeSnapshot: string;
    indicatorNameSnapshot: string;
    indicatorSort: number;
    scoringRuleSnapshot?: string;
    maxScoreSnapshot: number;
    defaultDeptNamesSnapshot?: string;
    attachmentRequiredSnapshot?: boolean;
    scorerCandidateUserIds?: string;
    scorerCandidateUserNames?: string;
    scorerUserId?: number;
    scorerUserName?: string;
    scorerUserNameDisplay?: string;
    scoreStatus: 'COMPLETED' | 'PENDING';
    actualScore?: number;
    actualScoreDisplay?: string;
    scoringDescription?: string;
    scoringDescriptionDisplay?: string;
    actualScoreTime?: string;
    currentUserItem?: boolean;
  }

  export interface Log {
    id?: number;
    action?: string;
    fromStatus?: string;
    toStatus?: string;
    actionDescription?: string;
    operatorName?: string;
    createTime?: string;
  }

  export interface Evaluation {
    id?: number;
    evaluationNo: string;
    supplierId?: number;
    supplierCode: string;
    supplierName: string;
    supplierSourceType?: 'REGISTERED';
    projectId?: number;
    projectCode?: string;
    projectName?: string;
    templateId?: number;
    templateVersionId: number;
    templateCodeSnapshot?: string;
    templateNameSnapshot?: string;
    templateVersionSnapshot?: string;
    totalScoreBaseline?: number;
    qualificationScoreSnapshot?: number;
    status?: Status;
    processInstanceId?: string;
    initiatorId?: number;
    initiatorName?: string;
    sendTime?: string;
    allScoredTime?: string;
    calculatedTime?: string;
    totalScore?: number;
    totalScoreDisplay?: string;
    autoDecision?: Decision;
    vetoTriggered?: boolean;
    vetoDescription?: string;
    finalDecision?: Decision;
    finalDescription?: string;
    generalManagerUserId?: number;
    generalManagerUserName?: string;
    generalManagerOpinion?: string;
    generalManagerOpinionDisplay?: string;
    generalManagerHandleTime?: string;
    publisherName?: string;
    publishTime?: string;
    remark?: string;
    version?: number;
    viewerScope?: string;
    canMaintain?: boolean;
    canScore?: boolean;
    canCalculate?: boolean;
    canHandleGeneralManager?: boolean;
    canPublish?: boolean;
    items?: Item[];
    ccUsers?: Array<{ userId: number; userName: string }>;
    logs?: Log[];
    createTime?: string;
    updateTime?: string;
  }
}

export function getEvaluationPage(params: PageParam & Record<string, unknown>) {
  return requestClient.get<PageResult<SrmPreliminaryEvaluationApi.Evaluation>>(
    '/mes/srm/preliminary-evaluation/page',
    { params },
  );
}

export function getEvaluation(id: number) {
  return requestClient.get<SrmPreliminaryEvaluationApi.Evaluation>(
    '/mes/srm/preliminary-evaluation/get',
    { params: { id } },
  );
}

export function createEvaluation(data: SrmPreliminaryEvaluationApi.Evaluation) {
  return requestClient.post<number>(
    '/mes/srm/preliminary-evaluation/create',
    data,
  );
}

export function updateEvaluation(data: SrmPreliminaryEvaluationApi.Evaluation) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-evaluation/update',
    data,
  );
}

export function deleteEvaluation(id: number) {
  return requestClient.delete<boolean>(
    '/mes/srm/preliminary-evaluation/delete',
    {
      params: { id },
    },
  );
}

export function assignScorers(
  evaluationId: number,
  assignments: Array<{
    itemId: number;
    scorerCandidateUserIds?: number[];
    scorerUserId: number;
  }>,
) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-evaluation/assign-scorers',
    {
      evaluationId,
      assignments,
    },
  );
}

export function sendEvaluation(id: number) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-evaluation/send',
    null,
    {
      params: { id },
    },
  );
}

export function submitScore(
  evaluationId: number,
  items: Array<{
    actualScore: number;
    itemId: number;
    scoringDescription: string;
  }>,
) {
  return requestClient.put<boolean>('/mes/srm/preliminary-evaluation/score', {
    evaluationId,
    items,
  });
}

export function calculateEvaluation(id: number) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-evaluation/calculate',
    null,
    {
      params: { id },
    },
  );
}

export function submitDecision(data: {
  evaluationId: number;
  finalDecision: SrmPreliminaryEvaluationApi.Decision;
  finalDescription: string;
  generalManagerUserId?: number;
  totalScore: number;
}) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-evaluation/decision',
    data,
  );
}

export function submitGeneralManagerOpinion(
  evaluationId: number,
  opinion: string,
) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-evaluation/general-manager-opinion',
    { evaluationId, opinion },
  );
}

export function publishEvaluation(evaluationId: number, ccUserIds: number[]) {
  return requestClient.put<boolean>('/mes/srm/preliminary-evaluation/publish', {
    evaluationId,
    ccUserIds,
  });
}
