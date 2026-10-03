import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesProductAbnormalEventApi {
  export type SourceType =
    | 'CUT_ROUND_FQC'
    | 'FAI'
    | 'FG_SHIPPING_FQC'
    | 'GLUE_BOARD_FAI'
    | 'IQC'
    | 'OQC';

  export interface ProductAbnormalEvent {
    abnormalSummary?: string;
    eventKey?: string;
    inspectionId?: number;
    inspectionNo?: string;
    inspectionQty?: number | string;
    inspectionTime?: string;
    inspectionType?: string;
    judgment?: string;
    operationName?: string;
    processCategory?: string;
    productBatchNo?: string;
    productModel?: string;
    specification?: string;
    unqualifiedQty?: number | string;
    ncrGenerated?: boolean;
    ncrId?: number;
    ncrNo?: string;
    ncrStatus?: 'ALL' | 'GENERATED' | 'PENDING';
    sourceType?: SourceType;
    standardId?: number;
    status?: string;
    canGenerateNcr?: boolean;
    canRejectRecheck?: boolean;
    dispatchTaskId?: number;
    recheckCount?: number;
    recheckGroupId?: number;
    recheckLatestInspectionId?: number;
    recheckLatestInspectionNo?: string;
    recheckPrevInspectionId?: number;
    recheckPrevInspectionNo?: string;
    recheckResult?: string;
    recheckRootInspectionId?: number;
    recheckRootInspectionNo?: string;
    recheckRoundNo?: number;
    recheckStatus?: 'NONE' | 'RECHECK_NG' | 'RECHECK_OK' | 'RECHECKING';
    recheckStatusName?: string;
    rejectNextInspectionId?: number;
    rejectNextInspectionNo?: string;
    rejectReason?: string;
    rejectTime?: string;
    rejectUserName?: string;
  }

  export interface ProductAbnormalEventPageReq extends PageParam {
    inspectionNo?: string;
    inspectionTime?: string[];
    ncrStatus?: 'ALL' | 'GENERATED' | 'PENDING';
    operationName?: string;
    processCategory?: string;
    productBatchNo?: string;
    productModel?: string;
    recheckStatus?: 'ALL' | 'NONE' | 'RECHECK_NG' | 'RECHECK_OK' | 'RECHECKING';
    sourceType?: SourceType;
  }

  export interface RejectRecheckReq {
    inspectionId: number;
    rejectReason: string;
    selectedItemIds: number[];
    selectedScopes?: Array<{
      itemId: number;
      positions?: string[];
      scopeType: 'ITEM' | 'PIECE' | 'POSITION';
    }>;
    sourceType: SourceType;
  }

  export interface RejectRecheckResp {
    groupId?: number;
    newInspectionId?: number;
    newInspectionNo?: string;
    recheckRoundNo?: number;
    rootInspectionId?: number;
    rootInspectionNo?: string;
    sourceType?: SourceType;
  }

  export interface RecheckHistoryDetail {
    dispatchTaskRoundId?: number;
    inspectionId?: number;
    inspectionJudgment?: string;
    inspectionNo?: string;
    inspectionStatus?: string;
    prevInspectionId?: number;
    prevInspectionNo?: string;
    rejectReason?: string;
    rejectTime?: string;
    rejectUserId?: number;
    rejectUserName?: string;
    resultTime?: string;
    roundNo?: number;
  }

  export interface RecheckHistory {
    chainStatus?: string;
    details?: RecheckHistoryDetail[];
    groupId?: number;
    dispatchTaskId?: number;
    latestInspectionId?: number;
    latestInspectionNo?: string;
    rootInspectionId?: number;
    rootInspectionNo?: string;
    sourceType?: SourceType;
    totalRecheckCount?: number;
  }

  export interface ProductAbnormalEventDetailItem {
    abnormalDesc?: string;
    defectCode?: string;
    defectCodeId?: number;
    defectName?: string;
    inspectionItem?: string;
    inspectionTime?: string;
    inspectorName?: string;
    itemType?: string;
    measuredValue?: string;
    remark?: string;
    recheckDetailFlag?: boolean;
    recheckItemFlag?: boolean;
    result?: string;
    rowNo?: number;
    sampleSize?: number;
    sectionName?: string;
    standardDesc?: string;
    unit?: string;
  }

  export interface ProductAbnormalEventAbnormalItem {
    inspectionItem?: string;
    targetNo?: string;
  }

  export interface ProductAbnormalEventDetail extends ProductAbnormalEvent {
    abnormalItems?: ProductAbnormalEventAbnormalItem[];
    createTime?: string;
    customerName?: string;
    details?: ProductAbnormalEventDetailItem[];
    inspectorName?: string;
    materialCode?: string;
    materialName?: string;
    remark?: string;
    sampleQty?: number;
    sourceReportNo?: string;
    specification?: string;
    submissionTime?: string;
    submitterName?: string;
    workOrderNo?: string;
  }
}

const BASE_URL = '/mes/quality/product-abnormal-event';

export function getProductAbnormalEventPage(
  params: MesProductAbnormalEventApi.ProductAbnormalEventPageReq,
) {
  return requestClient.get<
    PageResult<MesProductAbnormalEventApi.ProductAbnormalEvent>
  >(`${BASE_URL}/page`, { params });
}

export function getProductAbnormalEventDetail(
  sourceType: MesProductAbnormalEventApi.SourceType,
  inspectionId: number,
) {
  return requestClient.get<MesProductAbnormalEventApi.ProductAbnormalEventDetail>(
    `${BASE_URL}/get`,
    { params: { inspectionId, sourceType } },
  );
}

export function rejectProductAbnormalEventRecheck(
  data: MesProductAbnormalEventApi.RejectRecheckReq,
) {
  return requestClient.post<MesProductAbnormalEventApi.RejectRecheckResp>(
    `${BASE_URL}/reject-recheck`,
    data,
  );
}

export function getProductAbnormalEventRecheckHistory(
  sourceType: MesProductAbnormalEventApi.SourceType,
  inspectionId: number,
) {
  return requestClient.get<MesProductAbnormalEventApi.RecheckHistory>(
    `${BASE_URL}/recheck-history`,
    { params: { inspectionId, sourceType } },
  );
}
