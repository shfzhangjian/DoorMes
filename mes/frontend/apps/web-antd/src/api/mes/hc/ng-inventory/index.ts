import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcNgInventoryApi {
  export interface NgPiece {
    currentLocationKey?: string;
    currentLocationCode?: string;
    currentLocationName?: string;
    defectDetailJson?: string;
    defectSummary?: string;
    entryReason?:
      | 'FREEZE_INSTRUCTION'
      | 'HISTORY_BACKFILL'
      | 'HISTORY_FREEZE_BACKFILL'
      | 'NG_REPORT'
      | string;
    freezeInstructionId?: number;
    freezeInstructionNo?: string;
    freezeEffectiveTime?: string;
    id: number;
    materialCode?: string;
    materialName?: string;
    modelNo?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD' | string;
    originalLocationName?: string;
    pieceNo?: string;
    pieceQty?: number;
    printCount?: number;
    lastPrintTime?: string;
    qualityResult?: 'NG' | 'OK' | string;
    processName?: string;
    processType?: string;
    scrapReason?: string;
    scrappedTime?: string;
    shelvedTime?: string;
    sourceBatchNo?: string;
    sourceType?: 'MANUAL_HISTORY' | 'PRESS_SLOT' | 'SLITTING' | string;
    sourceParentBatchNo?: string;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    status?:
      | 'FROZEN'
      | 'RETURNED'
      | 'SCRAPPED'
      | 'STORED'
      | 'UNFROZEN_CLOSED'
      | 'WAIT_FREEZE_SHELF'
      | 'WAIT_SHELF'
      | string;
    currentWarehouseName?: string;
    unfreezeInstructionId?: number;
  }

  export interface NgHistoryLedger {
    creatorName?: string;
    currentStatus?: string;
    id: number;
    locationCode?: string;
    materialCode?: string;
    materialName?: string;
    modelNo?: string;
    operationName?: string;
    padType?: string;
    pieceNo?: string;
    processName?: string;
    processType?: string;
    remark?: string;
    scrapReason?: string;
    sourceBatchNo?: string;
    sourceParentBatchNo?: string;
    sourcePlanNo?: string;
    txnDirection?: 'IN' | 'OUT' | string;
    txnNo?: string;
    txnQty?: number;
    txnTime?: string;
    txnType?: string;
    uom?: string;
    warehouseCode?: string;
    warehouseName?: string;
  }

  export interface UnqualifiedHistoryLedger {
    afterStatus?: string;
    coaResult?: string;
    eventTime?: string;
    eventType?: string;
    inventoryDirection?: 'IN' | 'OUT' | 'NONE' | 'UNKNOWN';
    fqcResult?: string;
    id: string;
    inventoryArea?: 'FG_WAREHOUSE' | 'NG_WAREHOUSE' | 'WAIT_PACKAGING' | string;
    locationCode?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    operatorName?: string;
    planNo?: string;
    processName?: string;
    qualityStatus?: 'NG' | 'OK' | string;
    qty?: number;
    refDocNo?: string;
    remark?: string;
    segmentBatchNo?: string;
    sliceBatchNo?: string;
    txnNo?: string;
    warehouseCode?: string;
    warehouseName?: string;
  }

  export interface NgPieceLabel {
    lastPrintTime?: string;
    materialCode?: string;
    modelCode?: string;
    pieceId: number;
    pieceNo?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    printCount?: number;
    processName?: string;
    recordStatus?: string;
    recorderName?: string;
    segmentBatchNo?: string;
    sourceId?: number;
    sourceType?: string;
    workTime?: string;
  }

  export interface NgPieceLabelBatchQueryResult {
    failures?: string[];
    labels?: NgPieceLabel[];
  }

  export interface NgPieceLabelPrintedReq {
    labelContentJson?: string;
    pieceId: number;
    printerName?: string;
  }

  export interface NgPieceSegment {
    freezeInstructionId?: number;
    freezeInstructionNo?: string;
    materialCode?: string;
    materialName?: string;
    modelNo?: string;
    ngPieceCount?: number;
    okPieceCount?: number;
    processName?: string;
    processType?: string;
    samplePieceNos?: string[];
    segmentBatchNo?: string;
    sourcePlanId?: number;
    sourcePlanNo?: string;
    sourcePlanOperationId?: number;
    totalPieceCount?: number;
  }

  export interface NgLocationGrid {
    availableQty?: number;
    capacityQty?: number;
    id?: number;
    /** 后端定位及二维码使用的隐藏唯一键。 */
    locationKey: string;
    locationCode: string;
    locationName?: string;
    occupiedQty?: number;
    pieceNos?: string[];
    rackId?: number;
    rackNo?: string;
    locationNo?: number;
    storagePurpose?: 'FREEZE' | 'PRESS_SLOT_NG' | 'SLITTING_NG' | string;
    status?: '启用' | '停用' | string;
    warehouseCode?: string;
    warehouseId?: number;
    warehouseName?: string;
  }

  export interface NgWarehouseSaveReq {
    id?: number;
    padType: 'BLACK_PAD' | 'WHITE_PAD' | string;
    remark?: string;
    sortNo?: number;
    status?: '启用' | '停用' | string;
    warehouseCode: string;
    warehouseName: string;
  }

  export interface NgRackSaveReq {
    id?: number;
    /** 货架展示编码，例如 FQ1、HJ1。 */
    rackCode: string;
    capacityQty: number;
    rackName: string;
    rackNo: number;
    remark?: string;
    sortNo?: number;
    status?: '启用' | '停用' | string;
    warehouseId: number;
  }

  export interface NgLocationTreeRack {
    availableQty?: number;
    capacityQty?: number;
    id: number;
    locations?: NgLocationGrid[];
    occupiedQty?: number;
    rackCode: string;
    rackName: string;
    rackNo: number;
    remark?: string;
    sortNo?: number;
    status?: string;
    storageKey?: string;
    warehouseId: number;
  }

  export interface NgLocationTreeWarehouse {
    id: number;
    padType: string;
    racks?: NgLocationTreeRack[];
    remark?: string;
    sortNo?: number;
    status?: string;
    warehouseCode: string;
    warehouseName: string;
  }

  export interface NgShelfReq {
    locationKey: string;
    pieceIds: number[];
  }

  export interface NgScrapReq {
    pieceIds: number[];
    scrapReason: string;
  }

  export interface NgUnshelfReq {
    pieceIds: number[];
    unshelfReason: string;
  }

  export interface NgManualOutboundReq {
    pieceIds: number[];
    reason: string;
  }

  export interface NgTransferReq {
    pieceIds: number[];
    targetLocationKey: string;
  }

  export interface NgManualPieceCreateReq {
    backfillReason: string;
    defectSummary: string;
    materialCode?: string;
    modelNo: string;
    padType: 'BLACK_PAD' | 'WHITE_PAD';
    pieceNo: string;
    processType: 'PRESS_SLOT' | 'SLITTING';
    remark?: string;
    segmentBatchNo: string;
    sourceBatchNo: string;
    storageTarget: 'FREEZE' | 'NORMAL';
  }

  export interface NgManualPieceUpdateReq {
    defectSummary: string;
    materialCode?: string;
    modelNo: string;
    padType: 'BLACK_PAD' | 'WHITE_PAD';
    pieceId: number;
    pieceNo: string;
    processType: 'PRESS_SLOT' | 'SLITTING';
    segmentBatchNo: string;
    sourceBatchNo: string;
    storageTarget: 'FREEZE' | 'NORMAL';
  }

  export interface NgManualPieceDeleteReq {
    deleteReason: string;
    pieceId: number;
  }

  export interface NgManualPieceUnfreezeReq {
    pieceId: number;
    unfreezeReason: string;
  }

  export interface NgManualPieceImportResult {
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    successCount?: number;
    totalRows?: number;
  }
}

const BASE = '/mes/hc/ng-inventory';

export function getNgWaitShelfPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcNgInventoryApi.NgPiece>>(
    `${BASE}/wait-shelf/page`,
    { params },
  );
}

export function getNgWaitShelfSegmentPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<MesHcNgInventoryApi.NgPieceSegment>>(
    `${BASE}/wait-shelf/segment/page`,
    { params },
  );
}

export function getNgWaitShelfSegmentPieceList(params: Record<string, any>) {
  return requestClient.get<MesHcNgInventoryApi.NgPiece[]>(
    `${BASE}/wait-shelf/segment/piece-list`,
    { params },
  );
}

export function getNgWaitFreezeShelfPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<MesHcNgInventoryApi.NgPiece>>(
    `${BASE}/wait-freeze-shelf/page`,
    { params },
  );
}

export function getNgWaitFreezeShelfSegmentPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<MesHcNgInventoryApi.NgPieceSegment>>(
    `${BASE}/wait-freeze-shelf/segment/page`,
    { params },
  );
}

export function getNgWaitFreezeShelfSegmentPieceList(
  params: Record<string, any>,
) {
  return requestClient.get<MesHcNgInventoryApi.NgPiece[]>(
    `${BASE}/wait-freeze-shelf/segment/piece-list`,
    { params },
  );
}

export function getNgPiecePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcNgInventoryApi.NgPiece>>(
    `${BASE}/piece/page`,
    { params },
  );
}

export function getNgHistoryLedgerPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<MesHcNgInventoryApi.NgHistoryLedger>>(
    `${BASE}/history-ledger/page`,
    { params },
  );
}

export function getUnqualifiedHistoryLedgerPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcNgInventoryApi.UnqualifiedHistoryLedger>
  >(`${BASE}/unqualified-history-ledger/page`, { params });
}

export function getNgInventorySegmentPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<MesHcNgInventoryApi.NgPieceSegment>>(
    `${BASE}/piece/segment/page`,
    { params },
  );
}

export function getNgInventorySegmentPieceList(params: Record<string, any>) {
  return requestClient.get<MesHcNgInventoryApi.NgPiece[]>(
    `${BASE}/piece/segment/piece-list`,
    { params },
  );
}

export function getNgPieceLabels(pieceIds: number[]) {
  return requestClient.post<MesHcNgInventoryApi.NgPieceLabelBatchQueryResult>(
    `${BASE}/piece-label/batch-query`,
    { pieceIds },
  );
}

export function markNgPieceLabelsPrinted(
  items: MesHcNgInventoryApi.NgPieceLabelPrintedReq[],
) {
  return requestClient.post<MesHcNgInventoryApi.NgPieceLabel[]>(
    `${BASE}/piece-label/batch-mark-printed`,
    { items },
  );
}

export function getNgLocationGrid() {
  return requestClient.get<MesHcNgInventoryApi.NgLocationGrid[]>(
    `${BASE}/location/grid`,
  );
}

export function getNgLocationTree() {
  return requestClient.get<MesHcNgInventoryApi.NgLocationTreeWarehouse[]>(
    `${BASE}/location/tree`,
  );
}

export function saveNgWarehouse(data: MesHcNgInventoryApi.NgWarehouseSaveReq) {
  return requestClient.post<number>(`${BASE}/location/warehouse/save`, data);
}

export function deleteNgWarehouse(id: number) {
  return requestClient.delete<boolean>(`${BASE}/location/warehouse/delete`, {
    params: { id },
  });
}

export function saveNgRack(data: MesHcNgInventoryApi.NgRackSaveReq) {
  return requestClient.post<number>(`${BASE}/location/rack/save`, data);
}

export function deleteNgRack(id: number) {
  return requestClient.delete<boolean>(`${BASE}/location/rack/delete`, {
    params: { id },
  });
}

export function shelfNgPieces(data: MesHcNgInventoryApi.NgShelfReq) {
  return requestClient.post<boolean>(`${BASE}/shelf`, data);
}

export function unshelfNgPieces(data: MesHcNgInventoryApi.NgUnshelfReq) {
  return requestClient.post<boolean>(`${BASE}/unshelf`, data);
}

export function manualOutboundNgPieces(
  data: MesHcNgInventoryApi.NgManualOutboundReq,
) {
  return requestClient.post<boolean>(`${BASE}/manual-outbound`, data);
}

export function transferNgPieces(data: MesHcNgInventoryApi.NgTransferReq) {
  return requestClient.post<boolean>(`${BASE}/transfer`, data);
}

export function scrapNgPieces(data: MesHcNgInventoryApi.NgScrapReq) {
  return requestClient.post<boolean>(`${BASE}/scrap`, data);
}

export function createNgManualPiece(
  data: MesHcNgInventoryApi.NgManualPieceCreateReq,
) {
  return requestClient.post<MesHcNgInventoryApi.NgPiece>(
    `${BASE}/manual-piece/create`,
    data,
  );
}

export function updateNgManualPiece(
  data: MesHcNgInventoryApi.NgManualPieceUpdateReq,
) {
  return requestClient.post<MesHcNgInventoryApi.NgPiece>(
    `${BASE}/manual-piece/update`,
    data,
  );
}

export function deleteNgManualPiece(
  data: MesHcNgInventoryApi.NgManualPieceDeleteReq,
) {
  return requestClient.post<boolean>(`${BASE}/manual-piece/delete`, data);
}

export function exportNgManualPieceImportTemplate() {
  return requestClient.download(`${BASE}/manual-piece/import-template`);
}

export function importNgManualPieces(file: File) {
  return requestClient.upload<MesHcNgInventoryApi.NgManualPieceImportResult>(
    `${BASE}/manual-piece/import-excel`,
    { file },
  );
}

export function unfreezeNgManualPiece(
  data: MesHcNgInventoryApi.NgManualPieceUnfreezeReq,
) {
  return requestClient.post<boolean>(`${BASE}/manual-piece/unfreeze`, data);
}
