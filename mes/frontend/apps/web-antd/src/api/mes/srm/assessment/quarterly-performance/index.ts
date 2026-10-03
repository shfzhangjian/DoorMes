import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmQuarterlyPerformanceApi {
  export type EvaluationStatus =
    | 'ARCHIVED'
    | 'DRAFT'
    | 'PENDING_CALCULATION'
    | 'PENDING_DATA'
    | 'PENDING_SIGN'
    | 'REJECTED'
    | 'SCORING'
    | 'SIGNING';
  export type IndicatorType = 'CALCULATED_SCORE' | 'MANUAL_SCORE' | 'MIXED';

  export interface Item {
    id: number;
    evaluationId: number;
    templateItemId?: number;
    groupCodeSnapshot?: string;
    groupNameSnapshot?: string;
    groupSort?: number;
    groupMaxScoreSnapshot?: number;
    vetoOperatorSnapshot?: string;
    vetoScoreSnapshot?: number;
    vetoResultSnapshot?: string;
    indicatorCodeSnapshot?: string;
    indicatorNameSnapshot?: string;
    indicatorSort?: number;
    scoringRuleSnapshot?: string;
    maxScoreSnapshot?: number;
    defaultDeptNamesSnapshot?: string;
    attachmentRequiredSnapshot?: boolean;
    indicatorTypeSnapshot?: IndicatorType;
    targetValueSnapshot?: number;
    targetUnitSnapshot?: string;
    redlineScoreSnapshot?: number;
    scorerCandidateUserIds?: string;
    scorerCandidateUserNames?: string;
    scorerUserId?: number;
    scorerUserName?: string;
    scorerUserNameDisplay?: string;
    reporterCandidateUserIds?: string;
    reporterCandidateUserNames?: string;
    reporterUserId?: number;
    reporterUserName?: string;
    calcRuleId?: number;
    calcActualValue?: number;
    calcScore?: number;
    manualScore?: number;
    finalScore?: number;
    finalScoreDisplay?: string;
    scoreStatus?: string;
    scoringDescription?: string;
    actualScoreTime?: string;
    dataStatus?: string;
    currentUserItem?: boolean;
  }

  export interface Trace {
    id?: number;
    evaluationId?: number;
    evaluationItemId?: number;
    calcRuleId?: number;
    nodeKey?: string;
    parentNodeKey?: string;
    nodeType?: string;
    displayName?: string;
    formulaExpr?: string;
    sourceReportId?: number;
    sourceValueId?: number;
    sourceMetricCode?: string;
    sourceMetricName?: string;
    periodYear?: number;
    periodQuarter?: number;
    periodMonth?: number;
    rawValue?: number;
    normalizedValue?: number;
    unit?: string;
    attachmentCount?: number;
    reporterUserId?: number;
    reporterUserName?: string;
    reportTime?: string;
    resultFlag?: string;
    resultMessage?: string;
  }

  export interface Sign {
    id?: number;
    evaluationId?: number;
    deptCode?: string;
    deptName?: string;
    userId?: number;
    userName?: string;
    signStatus?: string;
    signResult?: 'FAIL' | 'PASS';
    signOpinion?: string;
    signTime?: string;
    currentUserSign?: boolean;
  }

  export interface Log {
    id?: number;
    evaluationId?: number;
    action?: string;
    fromStatus?: string;
    toStatus?: string;
    operatorName?: string;
    actionDescription?: string;
    detailJson?: string;
    createTime?: string;
  }

  export interface Evaluation {
    id?: number;
    evaluationNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    supplierSourceType?: string;
    evalYear?: number;
    evalQuarter?: number;
    templateId?: number;
    templateVersionId?: number;
    templateCodeSnapshot?: string;
    templateNameSnapshot?: string;
    templateVersionSnapshot?: string;
    totalScoreBaseline?: number;
    qualificationScoreSnapshot?: number;
    status?: EvaluationStatus;
    initiatorId?: number;
    initiatorName?: string;
    generatedTime?: string;
    autoCalculatedTime?: string;
    sendTime?: string;
    allScoredTime?: string;
    calculatedTime?: string;
    signStartTime?: string;
    archivedTime?: string;
    autoScore?: number;
    manualScore?: number;
    totalScore?: number;
    totalScoreDisplay?: string;
    evalGrade?: string;
    redlineTriggered?: boolean;
    redlineDescription?: string;
    finalOpinion?: string;
    remark?: string;
    version?: number;
    viewerScope?: string;
    canMaintain?: boolean;
    canPullActuals?: boolean;
    canScore?: boolean;
    canCalculate?: boolean;
    canStartSign?: boolean;
    canSign?: boolean;
    items?: Item[];
    traces?: Trace[];
    signs?: Sign[];
    logs?: Log[];
    createTime?: string;
    updateTime?: string;
  }

  export interface AvailableSupplier {
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    level?: string;
    supplierSourceType?: string;
  }

  export interface BatchCreateReq {
    evalYear: number;
    evalQuarter: number;
    templateVersionId: number;
    supplierIds: number[];
  }

  export interface TrendTreeNode {
    key: string;
    title: string;
    indicatorCode?: string;
    indicatorName?: string;
    groupName?: string;
    chartType?: string;
    selectable?: boolean;
    children?: TrendTreeNode[];
  }

  export interface TrendSeries {
    name: string;
    type: 'bar' | 'line' | string;
    unit?: string;
    percent?: boolean;
    values: Array<null | number>;
  }

  export interface TrendRow {
    label: string;
    unit?: string;
    percent?: boolean;
    values: string[];
    summary?: string;
  }

  export interface TrendPanel {
    indicatorCode: string;
    indicatorName: string;
    groupName?: string;
    chartType?: string;
    title: string;
    summaryLabel?: string;
    months: string[];
    series: TrendSeries[];
    rows: TrendRow[];
  }

  export interface Trend {
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    evalYear?: number;
    tree: TrendTreeNode[];
    selectedIndicatorCodes: string[];
    panels: TrendPanel[];
  }
}

export function getQuarterlyPerformancePage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmQuarterlyPerformanceApi.Evaluation>>(
    '/mes/srm/performance-quarter-evaluation/page',
    { params },
  );
}

export function getQuarterlyPerformance(id: number) {
  return requestClient.get<SrmQuarterlyPerformanceApi.Evaluation>(
    '/mes/srm/performance-quarter-evaluation/get',
    { params: { id } },
  );
}

export function createQuarterlyPerformance(
  data: SrmQuarterlyPerformanceApi.Evaluation,
) {
  return requestClient.post<number>(
    '/mes/srm/performance-quarter-evaluation/create',
    data,
  );
}

export function getQuarterlyPerformanceAvailableSuppliers(params: {
  evalQuarter: number;
  evalYear: number;
  level?: string;
  supplierInfo?: string;
  templateVersionId?: number;
}) {
  return requestClient.get<SrmQuarterlyPerformanceApi.AvailableSupplier[]>(
    '/mes/srm/performance-quarter-evaluation/available-suppliers',
    { params },
  );
}

export function batchCreateQuarterlyPerformance(
  data: SrmQuarterlyPerformanceApi.BatchCreateReq,
) {
  return requestClient.post<number[]>(
    '/mes/srm/performance-quarter-evaluation/batch-create',
    data,
  );
}

export function updateQuarterlyPerformance(
  data: SrmQuarterlyPerformanceApi.Evaluation,
) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-quarter-evaluation/update',
    data,
  );
}

export function deleteQuarterlyPerformance(id: number) {
  return requestClient.delete<boolean>(
    '/mes/srm/performance-quarter-evaluation/delete',
    { params: { id } },
  );
}

export function pullQuarterlyActuals(id: number) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-quarter-evaluation/pull-actuals',
    null,
    { params: { id } },
  );
}

export function sendQuarterlyScoring(id: number) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-quarter-evaluation/send',
    null,
    { params: { id } },
  );
}

export function submitQuarterlyScore(
  evaluationId: number,
  items: Array<{
    itemId: number;
    manualScore: number;
    scoringDescription?: string;
  }>,
) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-quarter-evaluation/score',
    { evaluationId, items },
  );
}

export function calculateQuarterlyTotal(id: number) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-quarter-evaluation/calculate',
    null,
    { params: { id } },
  );
}

export function startQuarterlySign(
  evaluationId: number,
  signers: Array<{
    deptCode?: string;
    deptName?: string;
    userId: number;
    userName?: string;
  }>,
) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-quarter-evaluation/start-sign',
    { evaluationId, signers },
  );
}

export function submitQuarterlySign(
  evaluationId: number,
  signResult: 'FAIL' | 'PASS',
  signOpinion?: string,
) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-quarter-evaluation/sign',
    { evaluationId, signOpinion, signResult },
  );
}

export function getQuarterlyPerformanceTrend(params: {
  evaluationId?: number;
  evalYear?: number;
  indicatorCodes?: string;
  supplierId?: number;
}) {
  return requestClient.get<SrmQuarterlyPerformanceApi.Trend>(
    '/mes/srm/performance-quarter-evaluation/trend',
    { params },
  );
}
