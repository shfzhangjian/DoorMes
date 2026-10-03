import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcProcessOutputBalanceApi {
  export interface Balance {
    ledgerId: string;
    stageCode?: string;
    stageName?: string;
    stageSort?: number;
    sourceTable?: string;
    sourceId?: number;
    sourceReportId?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    operationCode?: string;
    operationName?: string;
    outputBatchNo?: string;
    parentBatchNo?: string;
    sourceBatchNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    outputQty?: number;
    consumedQty?: number;
    remainingQty?: number;
    uom?: string;
    balanceStatus?: string;
    reportStatus?: string;
    reportTime?: string;
    consumeSummary?: string;
    sourcePlanLockId?: number;
    sourceLockStatus?: string;
    sourceLockedQty?: number;
    sourceLockRemainingQty?: number;
    sourceLockTargetPlanNo?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface PageReqVO extends PageParam {
    keyword?: string;
    planNo?: string;
    stageCode?: string;
    balanceStatus?: string;
    outputBatchNo?: string;
    sourceBatchNo?: string;
    parentBatchNo?: string;
    materialCode?: string;
    modelCode?: string;
    onlyRemaining?: boolean;
    reportDateStart?: string;
    reportDateEnd?: string;
  }
}

export async function getProcessOutputBalancePage(params: MesHcProcessOutputBalanceApi.PageReqVO) {
  return requestClient.get<PageResult<MesHcProcessOutputBalanceApi.Balance>>(
    '/mes/hc/execution/process-output-balance/page',
    { params },
  );
}
