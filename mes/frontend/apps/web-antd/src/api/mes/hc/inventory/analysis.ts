import { requestClient } from '#/api/request';

export namespace MesHcInventoryAnalysisApi {
  export type Dimension =
    | 'LAYER'
    | 'LOCATION'
    | 'MATERIAL'
    | 'MODEL'
    | 'QUALITY_STATUS'
    | 'RACK'
    | 'SEGMENT_BATCH'
    | 'SLICE_BATCH'
    | 'STOCK_STATUS'
    | 'WAREHOUSE';

  export interface OverviewReqVO {
    warehouseCode?: string;
    locationCode?: string;
    modelCode?: string;
    batchNo?: string;
    sliceBatchNo?: string;
    qualityStatus?: 'NG' | 'OK';
    stockStatus?:
      | 'ALLOCATED'
      | 'AVAILABLE'
      | 'INBOUND_LOCKED'
      | 'INBOUNDED'
      | 'OUTBOUND_LOCKED';
    dimension: Dimension;
    topN: number;
    granularity: 'DAY' | 'MONTH' | 'WEEK';
    startDate?: string;
    endDate?: string;
  }

  export interface Summary {
    recordCount: number;
    occupiedLocationCount: number;
    onHandQty: number;
    qualifiedQty: number;
    unqualifiedQty: number;
    shippableQty: number;
    lockedQty: number;
  }

  export interface DistributionItem {
    dimension: Dimension;
    dimensionKey: string;
    dimensionName: string;
    other: boolean;
    recordCount: number;
    onHandQty: number;
    shippableQty: number;
    lockedQty: number;
  }

  export interface MovementTrendItem {
    periodLabel: string;
    inboundQty: number;
    outboundQty: number;
    repackReturnQty: number;
    netChangeQty: number;
  }

  export interface Overview {
    dimension: Dimension;
    summary: Summary;
    distributionBars: DistributionItem[];
    movementTrend: MovementTrendItem[];
  }
}

export async function getInventoryAnalysisOverview(
  params: MesHcInventoryAnalysisApi.OverviewReqVO,
) {
  return requestClient.get<MesHcInventoryAnalysisApi.Overview>(
    '/mes/hc/inv/analysis/overview',
    { params },
  );
}
