import type { PageResult } from '@vben/request';

import type { MesQmsMotherRollGoodStatisticsApi } from './mother-roll-good-statistics';
import type { MesQmsYieldAnalysisApi } from './yield-analysis';

import { requestClient } from '#/api/request';

export namespace MesQmsYieldAnalysisV2Api {
  export interface QueryParams
    extends MesQmsMotherRollGoodStatisticsApi.ProcessPivotPageReqVO {
    actualReportDateEnd?: string;
    actualReportDateStart?: string;
    metricKey?: string;
    pieceNo?: string;
    processCode?: MesQmsYieldAnalysisApi.ProcessCode | string;
  }

  export type OverviewResult = MesQmsYieldAnalysisApi.OverviewResult;
  export type DetailRow = MesQmsYieldAnalysisApi.DetailRow;
  export type ProcessPivotRow =
    MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow;
  export type ProcessPivotStage =
    MesQmsMotherRollGoodStatisticsApi.ProcessPivotStage;

  export interface SyncResult {
    alreadySettledRecordCount: number;
    durationMs: number;
    endDate: string;
    insertedSourceCount: number;
    pivotRowCount: number;
    refreshedPivotRowCount: number;
    removedSourceCount: number;
    snapshotRecordCount: number;
    sourceRecordCount: number;
    startDate: string;
    syncTime: string;
    updatedSourceCount: number;
  }

  export interface SyncStatus {
    latestStatDate?: number[] | string;
    latestSyncTime?: string;
    settledSourceCount: number;
  }
}

const BASE_URL = '/mes/quality/statistics/yield-analysis-v2';

export function getYieldAnalysisV2Page(
  params: MesQmsYieldAnalysisV2Api.QueryParams,
) {
  return requestClient.get<
    PageResult<MesQmsYieldAnalysisV2Api.ProcessPivotRow>
  >(`${BASE_URL}/page`, { params });
}

export function getYieldAnalysisV2List(
  params: MesQmsYieldAnalysisV2Api.QueryParams,
) {
  return requestClient.get<MesQmsYieldAnalysisV2Api.ProcessPivotRow[]>(
    `${BASE_URL}/list`,
    { params },
  );
}

export function getYieldAnalysisV2Overview(
  params: MesQmsYieldAnalysisV2Api.QueryParams,
) {
  return requestClient.get<MesQmsYieldAnalysisV2Api.OverviewResult>(
    `${BASE_URL}/overview`,
    { params },
  );
}

export function getYieldAnalysisV2DetailPage(
  params: MesQmsYieldAnalysisV2Api.QueryParams,
) {
  return requestClient.get<PageResult<MesQmsYieldAnalysisV2Api.DetailRow>>(
    `${BASE_URL}/detail-page`,
    { params },
  );
}

export function getYieldAnalysisV2TargetModelOptions() {
  return requestClient.get<string[]>(`${BASE_URL}/target-model-options`);
}

export function getYieldAnalysisV2SyncStatus() {
  return requestClient.get<MesQmsYieldAnalysisV2Api.SyncStatus>(
    `${BASE_URL}/sync-status`,
  );
}

export function syncYieldAnalysisV2() {
  return requestClient.post<MesQmsYieldAnalysisV2Api.SyncResult>(
    `${BASE_URL}/sync`,
    null,
    { timeout: 120_000 },
  );
}
