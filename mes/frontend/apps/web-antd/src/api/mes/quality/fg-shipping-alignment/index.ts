import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesFgShippingAlignmentApi {
  export type AlignmentStatus = 'ALIGNED' | 'MISMATCH' | 'PENDING';

  export interface Notice {
    alignedCount: number;
    candidateCount: number;
    customerName?: string;
    erpOrderNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    noticeStatus?: string;
    plannedCount: number;
    shippingNoticeId: number;
    shippingNoticeNo: string;
  }

  export interface NoticePageReq extends PageParam {
    keyword?: string;
  }

  export interface AlignmentRow {
    actualSliceBatchNo?: string;
    alignmentStatus?: AlignmentStatus;
    customerProductBatchNo?: string;
    fqcId?: number;
    fqcNo?: string;
    id: number;
    internalItemCode?: string;
    materialCode?: string;
    materialName?: string;
    mismatchReason?: string;
    modelCode?: string;
    packageSliceNo?: string;
    productSize?: string;
    rowJudgment?: 'NG' | 'OK' | 'PENDING';
    shippingDetailId?: number;
    shippingNoticeItemId?: number;
    shippingPickItemId?: number;
    shippingQty?: number;
    sliceBatchNo?: string;
    stockNo?: string;
  }

  export interface AlignmentCandidate {
    actualSliceBatchNo?: string;
    alignmentStatus?: AlignmentStatus;
    customerProductBatchNo?: string;
    fqcId?: number;
    fqcNo?: string;
    internalItemCode?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    productSize?: string;
    shippingDetailId?: number;
    shippingPickItemId: number;
    sliceBatchNo?: string;
    stockNo?: string;
  }

  export interface Detail {
    alignedCount: number;
    alignmentCompleted: boolean;
    candidates: AlignmentCandidate[];
    candidateCount: number;
    customerName?: string;
    erpOrderNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    noticeStatus?: string;
    plannedCount: number;
    rows: AlignmentRow[];
    shippingNoticeId: number;
    shippingNoticeNo: string;
  }

  export interface SaveReq {
    shippingNoticeId: number;
    details: Array<{
      shippingNoticeItemId: number;
      shippingPickItemId: null | number;
    }>;
  }
}

export function getFgShippingAlignmentNoticeList(keyword?: string) {
  return requestClient.get<MesFgShippingAlignmentApi.Notice[]>(
    '/mes/quality/fg-shipping-alignment/notice-list',
    { params: { keyword } },
  );
}

export function getFgShippingAlignmentNoticePage(
  params: MesFgShippingAlignmentApi.NoticePageReq,
) {
  return requestClient.get<PageResult<MesFgShippingAlignmentApi.Notice>>(
    '/mes/quality/fg-shipping-alignment/notice-page',
    { params },
  );
}

export function getFgShippingAlignment(shippingNoticeId: number) {
  return requestClient.get<MesFgShippingAlignmentApi.Detail>(
    '/mes/quality/fg-shipping-alignment/get',
    { params: { shippingNoticeId } },
  );
}

export function saveFgShippingAlignment(
  data: MesFgShippingAlignmentApi.SaveReq,
) {
  return requestClient.put<MesFgShippingAlignmentApi.Detail>(
    '/mes/quality/fg-shipping-alignment/save',
    data,
  );
}
