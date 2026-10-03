import type { PageResult } from '@vben/request';

import type { MesHcPlanOrderApi } from './index';

import { requestClient } from '#/api/request';

export namespace MesHcPlanProcessPivotDailyApi {
  export interface SyncStatus {
    latestStatDate?: number[] | string;
    latestSyncTime?: string;
    settledRowCount: number;
  }

  export interface SyncResult {
    alreadySettledRowCount: number;
    durationMs: number;
    endDate?: number[] | string;
    insertedRowCount: number;
    removedRowCount: number;
    snapshotRowCount: number;
    sourceRowCount: number;
    startDate?: number[] | string;
    syncTime: string;
    updatedRowCount: number;
  }

  export interface CompareResult {
    checkedTime: string;
    extraSnapshotCount: number;
    matchedRowCount: number;
    missingSnapshotCount: number;
    sampleMessages?: string[];
    snapshotRowCount: number;
    sourceRowCount: number;
    staleSnapshotCount: number;
  }
}

const BASE_URL = '/mes/hc/plan/process-pivot-daily';
const PROCESS_PIVOT_DAILY_REQUEST_TIMEOUT = 60_000;

export async function getPlanProcessPivotPage(
  params: MesHcPlanOrderApi.ProcessPivotPageReqVO,
) {
  return requestClient.get<PageResult<MesHcPlanOrderApi.ProcessPivotRow>>(
    `${BASE_URL}/page`,
    { params, timeout: PROCESS_PIVOT_DAILY_REQUEST_TIMEOUT },
  );
}

export async function exportPlanProcessPivot(
  params: MesHcPlanOrderApi.ProcessPivotPageReqVO,
) {
  return requestClient.download(`${BASE_URL}/export-excel`, {
    params,
    timeout: PROCESS_PIVOT_DAILY_REQUEST_TIMEOUT,
  });
}

export async function getPlanProcessPivotDailySyncStatus() {
  return requestClient.get<MesHcPlanProcessPivotDailyApi.SyncStatus>(
    `${BASE_URL}/sync-status`,
  );
}

export async function syncPlanProcessPivotDaily() {
  return requestClient.post<MesHcPlanProcessPivotDailyApi.SyncResult>(
    `${BASE_URL}/sync`,
    null,
    { timeout: 120_000 },
  );
}

export async function comparePlanProcessPivotDaily(
  params: MesHcPlanOrderApi.ProcessPivotPageReqVO,
) {
  return requestClient.get<MesHcPlanProcessPivotDailyApi.CompareResult>(
    `${BASE_URL}/compare-source`,
    { params, timeout: 120_000 },
  );
}
