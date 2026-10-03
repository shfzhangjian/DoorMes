import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcHistoryPieceMassStockApi {
  export interface Stock {
    rowKey: string;
    modelCode?: string;
    segmentBatchNo?: string;
    goodStockQty?: number;
    earliestProductionDate?: string;
    latestProductionDate?: string;
    remark?: string;
  }

  export interface PageReqVO extends PageParam {
    keyword?: string;
    modelCode?: string;
    segmentBatchNo?: string;
  }

  export interface GoodStock {
    id: number;
    stockNo?: string;
    outerBoxNo?: string;
    innerUnitNo?: string;
    sliceBatchNo?: string;
    modelCode?: string;
    segmentBatchNo?: string;
    qty?: number;
    qualityStatus?: string;
    warehouseName?: string;
    locationCode?: string;
    locationName?: string;
    inboundTime?: string;
  }
}

export async function getHistoryPieceMassStockPage(
  params: MesHcHistoryPieceMassStockApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesHcHistoryPieceMassStockApi.Stock>>(
    '/mes/hc/plan/history-piece-mass-stock/page',
    { params },
  );
}

export async function getHistoryPieceGoodStockList(params: {
  modelCode?: string;
  segmentBatchNo: string;
}) {
  return requestClient.get<MesHcHistoryPieceMassStockApi.GoodStock[]>(
    '/mes/hc/plan/history-piece-mass-stock/good-stock/list',
    { params },
  );
}
