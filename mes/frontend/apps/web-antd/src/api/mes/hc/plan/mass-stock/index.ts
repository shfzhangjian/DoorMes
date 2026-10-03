import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcMassStockApi {
  export interface Stock {
    rowKey: string;
    modelCode?: string;
    motherBatchNo?: string;
    motherSegmentBatchNo?: string;
    wetMeter?: number;
    grindingFirstMeter?: number;
    grindingSecondMeter?: number;
    hcNapResult?: string;
    csNapResult?: string;
    shippingCoaResult?: string;
    semResult?: string;
    adhesive1UnprocessedMeter?: number;
    pressSlotQty?: number;
    adhesive2Qty?: number;
    cutRoundQty?: number;
    pendingInspectionQty?: number;
    qualifiedInspectionStockQty?: number;
    goodStockQty?: number;
    earliestProductionDate?: string;
    latestProductionDate?: string;
    demandStockQty?: number;
    shippingPickedQty?: number;
    shippingInspectionQty?: number;
    remark?: string;
    manualUpdateTime?: string;
  }

  export interface PageReqVO extends PageParam {
    keyword?: string;
    modelCode?: string;
    motherBatchNo?: string;
    motherSegmentBatchNo?: string;
    onlyAdhesive2?: boolean;
  }

  export interface ManualSaveReqVO {
    modelCode: string;
    motherBatchNo: string;
    motherSegmentBatchNo: string;
    semResult?: string;
    remark?: string;
  }

  export interface GoodStock {
    id: number;
    stockNo?: string;
    outerBoxNo?: string;
    innerUnitNo?: string;
    sliceBatchNo?: string;
    modelCode?: string;
    batchNo?: string;
    qty?: number;
    qualityStatus?: string;
    warehouseName?: string;
    locationCode?: string;
    locationName?: string;
    inboundTime?: string;
  }

  export type ShippingDetailMetric = 'demand' | 'inspection' | 'picked';

  export interface ShippingDetail {
    id: number;
    noticeId?: number;
    noticeNo?: string;
    sourceType?: string;
    customerName?: string;
    modelCode?: string;
    motherBatchNo?: string;
    motherSegmentBatchNo?: string;
    requiredBatchNo?: string;
    requiredSliceRange?: string;
    batchNo?: string;
    sliceBatchNo?: string;
    actualSliceBatchNo?: string;
    customerProductBatchNo?: string;
    internalItemCode?: string;
    qty?: number;
    lockStatus?: string;
    qualityStatus?: string;
    shippingInspectionResult?: string;
    shippingInspectorName?: string;
    shippingInspectionTime?: string;
    warehouseName?: string;
    locationCode?: string;
    locationName?: string;
    stockNo?: string;
    remark?: string;
  }
}

export async function getMassStockPage(params: MesHcMassStockApi.PageReqVO) {
  return requestClient.get<PageResult<MesHcMassStockApi.Stock>>('/mes/hc/plan/mass-stock/page', { params });
}

export async function updateMassStockManual(data: MesHcMassStockApi.ManualSaveReqVO) {
  return requestClient.put<boolean>('/mes/hc/plan/mass-stock/manual', data);
}

export async function exportMassStock(params: MesHcMassStockApi.PageReqVO) {
  return requestClient.download('/mes/hc/plan/mass-stock/export-excel', { params });
}

export async function getGoodStockList(params: {
  modelCode?: string;
  motherBatchNo: string;
  motherSegmentBatchNo: string;
}) {
  return requestClient.get<MesHcMassStockApi.GoodStock[]>('/mes/hc/plan/mass-stock/good-stock/list', { params });
}

export async function getShippingDetailList(params: {
  metric: MesHcMassStockApi.ShippingDetailMetric;
  modelCode?: string;
  motherBatchNo: string;
  motherSegmentBatchNo: string;
}) {
  return requestClient.get<MesHcMassStockApi.ShippingDetail[]>('/mes/hc/plan/mass-stock/shipping-detail/list', {
    params,
  });
}
