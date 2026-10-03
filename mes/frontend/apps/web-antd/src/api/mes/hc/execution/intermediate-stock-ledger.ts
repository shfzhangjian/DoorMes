import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcIntermediateStockLedgerApi {
  export interface TxnDetail {
    eventType?: string;
    eventTypeName?: string;
    txnId?: number;
    txnNo?: string;
    txnType?: string;
    txnTime?: string;
    stockId?: number;
    lockId?: number;
    targetPlanNo?: string;
    targetPlanOperationId?: number;
    targetOpName?: string;
    qty?: number;
    uom?: string;
    refDocType?: string;
    refDocId?: number;
    refDocNo?: string;
    displayBatchNo?: string;
    summary?: string;
    remark?: string;
  }

  export interface Ledger {
    id: number;
    rowType?: string;
    motherBatchNo?: string;
    stockCount?: number;
    stockType?: string;
    sourceType?: string;
    sourceTable?: string;
    sourceId?: number;
    sourceReportId?: number;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    batchNo?: string;
    sourceBatchNo?: string;
    sourceParentBatchNo?: string;
    materialCode?: string;
    materialName?: string;
    modelNo?: string;
    opCode?: string;
    opName?: string;
    opSeq?: number;
    segmentCode?: string;
    segmentName?: string;
    onHandQty?: number;
    availableQty?: number;
    shareableQty?: number;
    frozenQty?: number;
    planLockedQty?: number;
    lockedQty?: number;
    consumedQty?: number;
    releasedQty?: number;
    lockRemainingQty?: number;
    uom?: string;
    stockStatus?: string;
    activeLockId?: number;
    activeLockStatus?: string;
    activeLockTargetPlanNo?: string;
    activeLockTargetOpName?: string;
    activeLockRemainingQty?: number;
    qualityStatus?: string;
    bizStatus?: string;
    warehouseName?: string;
    locationName?: string;
    productionDate?: string;
    lastTxnNo?: string;
    lastTxnTime?: string;
    txnSummary?: string;
    txnDetails?: TxnDetail[];
    businessRemark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface PageReqVO extends PageParam {
    keyword?: string;
    sourcePlanNo?: string;
    sourceType?: string;
    stockStatus?: string;
    batchNo?: string;
    sourceBatchNo?: string;
    sourceParentBatchNo?: string;
    materialCode?: string;
    modelNo?: string;
    qualityStatus?: string;
    onlyAvailable?: boolean;
    txnDateStart?: string;
    txnDateEnd?: string;
  }

  export interface InventoryLockReleaseReq {
    lockId: number;
    releaseQty: number;
    releaseReason: string;
  }

  export interface HistoryImportResp {
    totalRows?: number;
    skippedRows?: number;
    successCount?: number;
    createdCount?: number;
    updatedCount?: number;
    failureCount?: number;
    messages?: string[];
    failures?: string[];
  }
}

export async function getIntermediateStockLedgerPage(
  params: MesHcIntermediateStockLedgerApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesHcIntermediateStockLedgerApi.Ledger>>(
    '/mes/hc/execution/intermediate-stock-ledger/page',
    { params },
  );
}

export async function getIntermediateStockLedgerAggregatePage(
  params: MesHcIntermediateStockLedgerApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesHcIntermediateStockLedgerApi.Ledger>>(
    '/mes/hc/execution/intermediate-stock-ledger/aggregate-page',
    { params },
  );
}

export async function releaseIntermediateStockLedgerLock(
  data: MesHcIntermediateStockLedgerApi.InventoryLockReleaseReq,
) {
  return requestClient.put<boolean>(
    '/mes/hc/execution/intermediate-stock-ledger/inventory-lock/release',
    data,
  );
}

export async function exportIntermediateStockHistoryTemplate(
  params: MesHcIntermediateStockLedgerApi.PageReqVO,
) {
  return requestClient.download(
    '/mes/hc/execution/intermediate-stock-ledger/history-import-template',
    { params },
  );
}

export async function importIntermediateStockHistory(file: File) {
  return requestClient.upload<MesHcIntermediateStockLedgerApi.HistoryImportResp>(
    '/mes/hc/execution/intermediate-stock-ledger/history-import',
    { file },
  );
}
