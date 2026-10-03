import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQmsCmpWarpageSliceStatApi {
  export interface PageReqVO extends PageParam {
    inspectionResult?: string;
    keyword?: string;
    manualOverride?: boolean;
    modelCode?: string;
    parentBatchNo?: string;
    recordDateEnd?: string;
    recordDateStart?: string;
    segmentSliceNo?: string;
  }

  export interface RecordVO {
    createTime?: string;
    customerCode?: string;
    customerName?: string;
    customerSliceNo?: string;
    id?: number;
    inspectionResult?: string;
    lastSyncTime?: string;
    manualOverride?: boolean;
    modelCode?: string;
    parentBatchNo?: string;
    productionSliceNo: string;
    recordDate?: string;
    remark?: string;
    segmentSliceNo?: string;
    shippingFqcDetailId?: number;
    shippingNoticeNo?: string;
    sourceActualValue?: string;
    sourceFqcId?: number;
    sourceFqcItemId?: number;
    sourceFqcNo?: string;
    sourceItemResult?: string;
    sourceSubmissionDetailId?: number;
    syncMessage?: string;
    updateTime?: string;
    warpageValueMm?: number;
  }

  export interface SaveReqVO {
    id?: number;
    manualOverride?: boolean;
    modelCode?: string;
    parentBatchNo?: string;
    productionSliceNo: string;
    recordDate?: string;
    remark?: string;
    segmentSliceNo?: string;
    warpageValueMm?: number;
  }

  export interface UpdateReqVO {
    customerSliceNo?: string;
    id: number;
    inspectionResult?: string;
    warpageValueMm?: number;
  }

  export interface SliceImportReqVO {
    keyword?: string;
    modelCode?: string;
    parentBatchNo?: string;
    recordDateEnd?: string;
    recordDateStart?: string;
    segmentSliceNo?: string;
  }

  export interface SliceImportResult {
    completedTime?: string;
    duplicateSourceCount: number;
    existingCount: number;
    importedCount: number;
    sourceCount: number;
  }

  export interface SyncResult {
    completedTime?: string;
    customerMissingCount: number;
    invalidValueCount: number;
    manualProtectedCount: number;
    requestedCount: number;
    sourceMissingCount: number;
    syncedCount: number;
  }
}

const BASE_URL = '/mes/quality/statistics/cmp-warpage-slice-stat';

export function getCmpWarpageSliceStatPage(
  params: MesQmsCmpWarpageSliceStatApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesQmsCmpWarpageSliceStatApi.RecordVO>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getCmpWarpageSliceStat(id: number) {
  return requestClient.get<MesQmsCmpWarpageSliceStatApi.RecordVO>(
    `${BASE_URL}/get`,
    { params: { id } },
  );
}

export function createCmpWarpageSliceStat(
  data: MesQmsCmpWarpageSliceStatApi.SaveReqVO,
) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function updateCmpWarpageSliceStat(
  data: MesQmsCmpWarpageSliceStatApi.UpdateReqVO,
) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function deleteCmpWarpageSliceStat(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete`, {
    params: { id },
  });
}

export function syncCmpWarpageSliceStats(ids: number[]) {
  return requestClient.post<MesQmsCmpWarpageSliceStatApi.SyncResult>(
    `${BASE_URL}/sync`,
    { ids },
  );
}

export function syncCmpWarpageSlices(
  data: MesQmsCmpWarpageSliceStatApi.SliceImportReqVO,
) {
  return requestClient.post<MesQmsCmpWarpageSliceStatApi.SliceImportResult>(
    `${BASE_URL}/sync-slices`,
    data,
  );
}
