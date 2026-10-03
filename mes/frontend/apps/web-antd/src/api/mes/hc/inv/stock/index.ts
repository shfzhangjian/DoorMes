import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcInvStockApi {
  export interface Stock {
    id: number;
    stockType?: string;
    warehouseCode?: string;
    warehouseName?: string;
    locationCode?: string;
    locationName?: string;
    ownerId?: number;
    ownerCode?: string;
    ownerName?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    recipeCode?: string;
    recipeName?: string;
    modelNo?: string;
    specSize?: string;
    opSeq?: number;
    opCode?: string;
    opName?: string;
    segmentCode?: string;
    segmentName?: string;
    thickness?: number;
    batchNo?: string;
    productionDate?: string;
    expiryDate?: string;
    onHandQty?: number;
    availableQty?: number;
    shareableQty?: number;
    frozenQty?: number;
    planLockedQty?: number;
    qualityStatus?: string;
    bizStatus?: string;
    businessRemark?: string;
    uom?: string;
    sourceType?: string;
    sourceTable?: string;
    sourceId?: number;
    sourceReportId?: number;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    sourceBatchNo?: string;
    sourceParentBatchNo?: string;
    lastTxnNo?: string;
    lastTxnTime?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface PageReqVO extends PageParam {
    stockType?: string;
    keyword?: string;
    warehouseCode?: string;
    materialCode?: string;
    recipeCode?: string;
    specSize?: string;
    opSeq?: number;
    segmentCode?: string;
    modelNo?: string;
    batchNo?: string;
    sourceType?: string;
    sourcePlanNo?: string;
    sourceBatchNo?: string;
    sourceParentBatchNo?: string;
    qualityStatus?: string;
    bizStatus?: string;
    productionDateStart?: string;
    productionDateEnd?: string;
    includeUnavailable?: boolean;
    onlyShareable?: boolean;
  }
}

export async function getInvStockPage(params: MesHcInvStockApi.PageReqVO) {
  return requestClient.get<PageResult<MesHcInvStockApi.Stock>>('/mes/hc/inv/stock/page', { params });
}

export async function exportInvStock(params: MesHcInvStockApi.PageReqVO) {
  return requestClient.download('/mes/hc/inv/stock/export-excel', { params });
}
