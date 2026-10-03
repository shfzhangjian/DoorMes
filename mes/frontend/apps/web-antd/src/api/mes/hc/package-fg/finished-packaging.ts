import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcFinishedPackagingApi {
  export type ShippingPickCandidateType =
    | 'PACKAGING_DIRECT'
    | 'WAREHOUSE_STOCK';

  export interface PackageBox {
    bizNo?: string;
    boxNo?: string;
    boxType?: 'INBOUND' | 'OUTBOUND' | string;
    boxTypeName?: string;
    currentQty?: number;
    id: number;
    labelNo?: string;
    lastPrintTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    motherSegmentBatchNo?: string;
    printCount?: number;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    sourceNo?: string;
    status?: string;
    targetQty?: number;
  }

  export interface PackageBoxPrintReq {
    boxType: string;
    id: number;
    operatorName?: string;
  }

  export interface InboundPackageRemarkUpdateReq {
    id: number;
    remark?: string;
  }

  export interface FgLocationPiece {
    batchNo?: string;
    inboundTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    productSize?: string;
    qualityStatus?: string;
    qty?: number;
    sliceBatchNo?: string;
    stockId?: number;
    stockNo?: string;
    stockStatus?: string;
  }

  export interface FgLocationGrid {
    availableQty?: number;
    coaFrozen?: boolean;
    coaFreezeReason?: string;
    frozenQty?: number;
    capacityQty?: number;
    deletable?: boolean;
    gridNo: number;
    id?: number;
    locationCode?: string;
    locationName?: string;
    locationType?: string;
    qualityScope?: 'QUALIFIED' | 'QUARANTINE' | 'UNASSIGNED' | string;
    areaNo?: number;
    layerId?: number;
    layerName?: string;
    layerNo?: number;
    mixBatchFlag?: boolean;
    mixModelFlag?: boolean;
    occupied?: boolean;
    occupiedInnerUnitNo?: string;
    occupiedPieceQty?: number;
    occupiedQty?: number;
    occupiedSliceBatchNo?: string;
    occupiedSliceBatchNos?: string[];
    occupiedStockNo?: string;
    pieces?: FgLocationPiece[];
    positionDesc?: string;
    qrCode?: string;
    rackId?: number;
    rackName?: string;
    rackNo?: number;
    status?: string;
    warehouseId?: number;
    warehouseCode?: string;
    warehouseName?: string;
  }

  export interface FgLocationSaveReq {
    areaNo: number;
    capacityQty: number;
    id?: number;
    layerId: number;
    locationName?: string;
    mixBatchFlag?: boolean;
    mixModelFlag?: boolean;
    positionDesc?: string;
    qualityScope?: 'QUALIFIED' | 'QUARANTINE' | string;
    rackId: number;
    status?: string;
  }

  export interface FgRackSaveReq {
    id?: number;
    rackName: string;
    rackNo: number;
    remark?: string;
    sortNo?: number;
    status?: string;
    warehouseId: number;
  }

  export interface FgWarehouseSaveReq {
    id?: number;
    remark?: string;
    sortNo?: number;
    status?: string;
    warehouseCode: string;
    warehouseName: string;
  }

  export interface FgWarehouseQualityScopeUpdateReq {
    qualityScope: 'QUALIFIED' | 'QUARANTINE';
    warehouseId: number;
  }

  export interface FgWarehouseQualityScopeUpdateResult {
    activePieceCount: number;
    locationCount: number;
    occupiedLocationCount: number;
    qualityScope: 'QUALIFIED' | 'QUARANTINE';
    warehouseCode: string;
    warehouseId: number;
    warehouseName: string;
  }

  export interface FgLayerSaveReq {
    id?: number;
    layerName: string;
    layerNo: number;
    rackId: number;
    remark?: string;
    sortNo?: number;
    status?: string;
  }

  export interface FgLocationTreeLayer {
    areas: FgLocationGrid[];
    id: number;
    layerName?: string;
    layerNo: number;
    rackId: number;
    remark?: string;
    sortNo?: number;
    status?: string;
  }

  export interface FgLocationTreeRack {
    id: number;
    layers: FgLocationTreeLayer[];
    rackName?: string;
    rackNo: number;
    remark?: string;
    sortNo?: number;
    status?: string;
    warehouseCode?: string;
    warehouseId: number;
    warehouseName?: string;
  }

  export interface FgLocationTreeWarehouse {
    id: number;
    racks: FgLocationTreeRack[];
    remark?: string;
    sortNo?: number;
    status?: string;
    warehouseCode: string;
    warehouseName: string;
  }

  export interface FgLocationPrintReq {
    id: number;
  }

  export interface FgStockLocationOverview {
    availableQty?: number;
    coaFrozen?: boolean;
    coaFreezeReason?: string;
    frozenQty?: number;
    capacityQty?: number;
    gridNo: number;
    id?: number;
    locationCode?: string;
    locationName?: string;
    occupiedQty?: number;
    packageCount?: number;
    percent?: number;
    pieceCount?: number;
    positionDesc?: string;
    status?: string;
    warehouseCode?: string;
    warehouseName?: string;
  }

  export interface FgStockLedger {
    batchNo?: string;
    coaInspectionResult?: string;
    id: number;
    inboundNo?: string;
    inboundTime?: string;
    inboundUserName?: string;
    innerUnitNo?: string;
    inspectionResult?: string;
    inspectionStatus?: string;
    inspectionTaskNo?: string;
    locationCode?: string;
    locationName?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packageNo?: string;
    productSize?: string;
    productionDate?: string;
    expiryDate?: string;
    qualityStatus?: string;
    remark?: string;
    sourceManualPieceId?: number;
    sourceType?: string;
    importedDataEditable?: boolean;
    labelReprintRequired?: boolean;
    qty?: number;
    lockedQty?: number;
    availableQty?: number;
    coaFrozen?: boolean;
    coaFreezeReason?: string;
    frozenQty?: number;
    sliceBatchNo?: string;
    stockNo?: string;
    stockStatus?: string;
    outboundLocation?: string;
    outboundQualityNo?: string;
    outboundRecorderName?: string;
    outboundTime?: string;
    warehouseCode?: string;
    warehouseName?: string;
  }

  export interface ImportedStockDataUpdateReq {
    coaInspectionResult: string;
    correctionReason?: string;
    inspectionResult: string;
    materialCode: string;
    modelCode: string;
    productionDate: string;
    remark?: string;
    sliceBatchNo: string;
    segmentBatchNo: string;
    stockId: number;
  }

  export interface FgStockHistoryLedger {
    afterStockStatus?: string;
    batchNo?: string;
    beforeStockStatus?: string;
    finishedStockId?: number;
    id: number;
    innerUnitNo?: string;
    locationCode?: string;
    locationName?: string;
    materialCode?: string;
    materialName?: string;
    /** 当前手工成品出库流水是否仍可拆包退回待重新包装 */
    manualOutboundRepackReturnable?: boolean;
    modelCode?: string;
    operatorName?: string;
    outerBoxNo?: string;
    qualityStatus?: string;
    qty?: number;
    refDocNo?: string;
    refDocType?: string;
    remark?: string;
    sliceBatchNo?: string;
    stockNo?: string;
    txnNo?: string;
    txnTime?: string;
    txnType?: string;
    warehouseCode?: string;
    warehouseName?: string;
  }

  export interface ShippingNoticePickCandidate extends Omit<
    FgStockLedger,
    'id'
  > {
    candidateKey?: string;
    candidateType?: ShippingPickCandidateType;
    id?: number;
    sourceCutRoundReportId?: number;
    sourceInnerPackItemId?: number;
    stockId?: number;
  }

  export interface ShippingNoticePickCandidateSegment {
    modelCode?: string;
    packagingDirectPieceCount?: number;
    sampleSliceBatchNos?: string[];
    segmentBatchNo?: string;
    totalPieceCount?: number;
    warehousePieceCount?: number;
  }

  export interface FgStockLocationDetail {
    location?: FgStockLocationOverview;
    packages?: InboundBox[];
    pieces?: FgStockLedger[];
  }

  export interface ShippingNotice {
    cancelName?: string;
    cancelReason?: string;
    cancelTime?: string;
    customerCode?: string;
    customerId?: number;
    customerName?: string;
    erpOrderNo?: string;
    externalProductCode?: string;
    externalProductInfo?: string;
    externalProductModel?: string;
    id: number;
    inspectionCompletedQty?: number;
    attachments?: ShippingNoticeAttachment[];
    items?: ShippingNoticeItem[];
    lockedQty?: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    noticeNo?: string;
    noticeQty?: number;
    changeVersion?: number;
    noticeStatus?: string;
    orderNo?: string;
    outboundConfirmName?: string;
    outboundConfirmRemark?: string;
    outboundConfirmTime?: string;
    packingRequirement?: string;
    productSize?: string;
    productType?: string;
    recorderName?: string;
    recorderTime?: string;
    requiredBatchNo?: string;
    requiredExpiryDate?: string;
    requiredProductionDate?: string;
    requiredShipQty?: number;
    requiredSliceRange?: string;
    remark?: string;
    shippingPackageName?: string;
    shippingPackageTime?: string;
    shippingConfirmName?: string;
    shippingTime?: string;
    pickItems?: ShippingNoticePickItem[];
  }

  export interface ShippingNoticeAttachment {
    attachmentName?: string;
    attachmentType?: string;
    attachmentUrl?: string;
    createTime?: string;
    fileSize?: number;
    id?: number;
    nextNoticeId?: number;
    nextSheetName?: string;
    noticeId?: number;
    noticeNo?: string;
    previousNoticeId?: number;
    previousSheetName?: string;
    remark?: string;
    sourceFileName?: string;
    sourceSheetIndex?: number;
    sourceSheetName?: string;
    sourceSheetTotal?: number;
  }

  export interface ShippingNoticeExcelImportResult {
    fileName?: string;
    fileUrl?: string;
    importCount?: number;
    notices?: ShippingNotice[];
  }

  export interface ShippingNoticeItem {
    actualFinishedStockId?: number;
    actualLocationCode?: string;
    actualLocationName?: string;
    actualRemark?: string;
    actualShipQty?: number;
    actualSliceBatchNo?: string;
    actualStockNo?: string;
    batchNo?: string;
    cancelName?: string;
    cancelTime?: string;
    customerProductBatchNo?: string;
    customerModelCode?: string;
    customerSliceBatchNo?: string;
    finishedStockId?: number;
    id: number;
    inboundNo?: string;
    inboundTime?: string;
    innerUnitNo?: string;
    internalItemCode?: string;
    internalModelCode?: string;
    stockQty?: number;
    availableQty?: number;
    coaFrozen?: boolean;
    coaFreezeReason?: string;
    frozenQty?: number;
    lockedQty?: number;
    locationCode?: string;
    locationName?: string;
    lockName?: string;
    lockStatus?: string;
    lockTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    noticeId?: number;
    noticeNo?: string;
    outerBoxNo?: string;
    packageNo?: string;
    packageSliceNo?: string;
    productSize?: string;
    qualityStatus?: string;
    remark?: string;
    shippedName?: string;
    shippedTime?: string;
    shippingInspectorName?: string;
    shippingInspectionRemark?: string;
    shippingInspectionResult?: string;
    shippingInspectionTime?: string;
    shippingPackageName?: string;
    shippingPackageRemark?: string;
    shippingPackageTime?: string;
    shippingQualityNo?: string;
    oqcOrderId?: number;
    oqcStatus?: string;
    sliceBatchNo?: string;
    stockNo?: string;
    warehouseCode?: string;
    warehouseName?: string;
    mismatchReason?: string;
  }

  export interface ShippingNoticePickItem {
    actualShipQty?: number;
    actualSliceBatchNo?: string;
    batchNo?: string;
    cancelName?: string;
    cancelTime?: string;
    customerProductBatchNo?: string;
    finishedStockId?: number;
    id: number;
    inboundNo?: string;
    inboundTime?: string;
    innerUnitNo?: string;
    internalItemCode?: string;
    internalModelCode?: string;
    stockQty?: number;
    availableQty?: number;
    coaFrozen?: boolean;
    coaFreezeReason?: string;
    frozenQty?: number;
    lockedQty?: number;
    locationCode?: string;
    locationName?: string;
    lockName?: string;
    lockStatus?: string;
    lockTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    noticeId?: number;
    noticeNo?: string;
    outerBoxNo?: string;
    packageNo?: string;
    pickSourceType?: ShippingPickCandidateType;
    productSize?: string;
    qualityStatus?: string;
    remark?: string;
    shippedName?: string;
    shippedTime?: string;
    shippingInspectorName?: string;
    shippingInspectionRemark?: string;
    shippingInspectionResult?: string;
    shippingInspectionTime?: string;
    shippingPackageName?: string;
    shippingPackageRemark?: string;
    shippingPackageTime?: string;
    shippingQualityNo?: string;
    sliceBatchNo?: string;
    sourceNoticeItemId?: number;
    sourceCutRoundReportId?: number;
    sourceInnerPackItemId?: number;
    stockNo?: string;
    warehouseCode?: string;
    warehouseName?: string;
  }

  export interface ShippingBatchCandidate {
    availableQty?: number;
    coaFrozen?: boolean;
    coaFreezeReason?: string;
    frozenQty?: number;
    batchNo?: string;
    lockedQty?: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packagingReadyQty?: number;
    stockAvailableQty?: number;
    totalQty?: number;
  }

  export interface SubmitShippingNoticeDeliveryItemReq {
    actualSliceBatchNo: string;
    customerModelCode?: string;
    customerSliceBatchNo?: string;
    id: number;
    mismatchReason?: string;
    remark?: string;
    shippingInspectorName?: string;
    shippingQualityNo?: string;
  }

  export interface SubmitShippingNoticeDeliveryReq {
    id: number;
    items: SubmitShippingNoticeDeliveryItemReq[];
    operatorName?: string;
  }

  export interface SaveShippingNoticeItemReq {
    id?: number;
    customerProductBatchNo?: string;
    internalItemCode?: string;
    internalModelCode?: string;
    packageSliceNo?: string;
    remark?: string;
    shipQty: number;
    stockId?: number;
  }

  export interface ChangeShippingNoticeReq extends SaveShippingNoticeReq {
    changeReason: string;
    expectedChangeVersion: number;
  }

  export interface SaveShippingNoticeReq {
    customerCode?: string;
    customerId?: number;
    customerName: string;
    erpOrderNo?: string;
    externalProductCode?: string;
    externalProductInfo?: string;
    externalProductModel?: string;
    id?: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    noticeNo?: string;
    noticeQty: number;
    orderNo?: string;
    packingRequirement?: string;
    productSize?: string;
    productType: string;
    recorderName?: string;
    requiredBatchNo?: string;
    requiredExpiryDate?: string;
    requiredProductionDate?: string;
    requiredShipQty?: number;
    requiredSliceRange?: string;
    remark?: string;
    shippingConfirmName?: string;
    shippingTime?: string;
    items: SaveShippingNoticeItemReq[];
    stockIds?: number[];
  }

  export interface IssueShippingNoticeReq {
    id: number;
    operatorName?: string;
  }

  export interface CancelShippingNoticeReq {
    id: number;
    operatorName?: string;
    physicalReturned: boolean;
    reason: string;
  }

  interface PickShippingNoticeItemBaseReq {
    candidateKey?: string;
    noticeItemId?: number;
    remark?: string;
  }

  export type PickShippingNoticeItemReq =
    | (PickShippingNoticeItemBaseReq & {
        candidateType: 'PACKAGING_DIRECT';
        sourceInnerPackItemId: number;
        sourceCutRoundReportId?: never;
        stockId?: never;
      })
    | (PickShippingNoticeItemBaseReq & {
        candidateType: 'PACKAGING_DIRECT';
        sourceCutRoundReportId: number;
        sourceInnerPackItemId?: never;
        stockId?: never;
      })
    | (PickShippingNoticeItemBaseReq & {
        candidateType?: 'WAREHOUSE_STOCK';
        sourceCutRoundReportId?: never;
        sourceInnerPackItemId?: never;
        stockId: number;
      });

  export interface PickShippingNoticeReq {
    confirmPicked?: boolean;
    items: PickShippingNoticeItemReq[];
    noticeId: number;
    operatorName?: string;
  }

  export interface ReturnShippingNoticePickReq {
    noticeId: number;
    operatorName?: string;
    pickItemIds: number[];
    reason: string;
  }

  export interface ConfirmShippingNoticeForFqcReq {
    noticeId: number;
    operatorName?: string;
    remark?: string;
  }

  export interface InspectShippingNoticeReq {
    actualSliceBatchNo: string;
    inspectionResult: 'FROZEN' | 'NG' | 'OK' | string;
    inspectorName?: string;
    noticeItemId: number;
    noticeId: number;
    remark?: string;
  }

  export interface PushShippingNoticeOqcReq {
    noticeId: number;
    operatorName?: string;
    remark?: string;
  }

  export interface CompleteShippingNoticeReq {
    noticeId: number;
    operatorName?: string;
    remark?: string;
  }

  export interface PackShippingNoticeReq {
    auxConsumeItems: PackageAuxConsumeItem[];
    noticeId: number;
    noticeItemIds: number[];
    operatorName?: string;
    packageMethod: string;
    remark?: string;
  }

  export interface PackageAuxStock {
    auxCategory?: string;
    auxCategoryName?: string;
    auxSpec?: string;
    availableQty?: number;
    coaFrozen?: boolean;
    coaFreezeReason?: string;
    frozenQty?: number;
    batchNo?: string;
    edgeWarehouseCode?: string;
    edgeWarehouseName?: string;
    erpTransferNo?: string;
    erpTransferStatus?: string;
    id: number;
    materialCode?: string;
    materialName?: string;
    printCount?: number;
    printTime?: string;
    receiveDate?: string;
    receiveQty?: number;
    receiveTime?: string;
    receiverName?: string;
    remark?: string;
    sourceWarehouseCode?: string;
    sourceWarehouseName?: string;
    stockMeasureMode?: string;
    stockStatus?: string;
    transferQty?: number;
    transferUnit?: string;
    unpackQty?: number;
    unpackUnit?: string;
    usedQty?: number;
  }

  export interface SavePackageAuxStockReq {
    auxCategory?: string;
    auxCategoryName?: string;
    auxSpec?: string;
    batchNo: string;
    edgeWarehouseCode?: string;
    edgeWarehouseName?: string;
    erpTransferNo?: string;
    id?: number;
    materialCode: string;
    materialName: string;
    receiveDate?: string;
    receiveQty: number;
    receiveTime?: string;
    receiverName?: string;
    remark?: string;
    sourceWarehouseCode?: string;
    sourceWarehouseName?: string;
    transferQty?: number;
    transferUnit?: string;
    unpackQty?: number;
    unpackUnit?: string;
  }

  export interface PackageAuxConsumeReq {
    auxSpec?: string;
    batchNo?: string;
    bizType?: string;
    bizNo?: string;
    consumeQty: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    recordDate?: string;
    recorderName?: string;
    remark?: string;
    stockId?: number;
  }

  export interface PackageAuxConsumeItem {
    /** 操作员确认的本次领用量 */
    consumeQty: number;
    /** 包装工序耗材领用台账 ID */
    ledgerId: number;
  }

  export interface PackageAuxConsumeRecord {
    afterAvailableQty?: number;
    auxCategory?: string;
    auxCategoryName?: string;
    auxSpec?: string;
    batchNo?: string;
    beforeAvailableQty?: number;
    bizType?: string;
    bizNo?: string;
    consumeQty?: number;
    consumeStatus?: string;
    id: number;
    materialCode?: string;
    materialName?: string;
    recordDate?: string;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    stockId?: number;
  }

  export interface PackageAuxConsumeSummary {
    auxCategory?: string;
    auxCategoryName?: string;
    auxSpec?: string;
    batchNo?: string;
    materialCode?: string;
    materialName?: string;
    recordCount?: number;
    totalConsumeQty?: number;
  }

  export interface InspectionSlice {
    coaInspectionNo?: string;
    coaInspectionResult?: string;
    coaInspectionStatus?: string;
    coaNgReason?: string;
    coaSampleBatchNo?: string;
    coaScopeBatchNo?: string;
    currentLocationCode?: string;
    currentLocationName?: string;
    expiryDate?: string;
    inboundTime?: string;
    inboundUserName?: string;
    inspectionRemark?: string;
    inspectionResult?: string;
    inspectionStatus?: string;
    inspectionTaskNo?: string;
    inspectionTime?: string;
    inspectorName?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    outboundLocation?: string;
    outboundQualityNo?: string;
    outboundRecorderName?: string;
    outboundTime?: string;
    parentProductionBatchNo?: string;
    packagingQualityStatus?: string;
    qualityRiskFlag?:
      | 'ADHESIVE2_NG'
      | 'BOTH_NG'
      | 'CUT_ROUND_NG'
      | 'NONE'
      | string;
    qualityRiskSnapshotJson?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    productionBatchNo?: string;
    productionDate?: string;
    printCount?: number;
    printStatus?: string;
    lastPrintTime?: string;
    recorderName?: string;
    recorderTime?: string;
    segmentBatchNo?: string;
    sliceBatchNo?: string;
    sourceCutRoundReportId?: number;
    sourceManualPieceId?: number;
    sourceType?: 'CUT_ROUND_REPORT' | 'MANUAL_HISTORY' | string;
    stockNo?: string;
    stockStatus?: string;
  }

  export interface InspectionSliceSegment {
    expiryDate?: string;
    inspectionStatus?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    ngPieceCount?: number;
    okPieceCount?: number;
    packagingQualityStatus?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    productionDateEnd?: string;
    productionDateStart?: string;
    sampleSliceBatchNo?: string;
    sampleSliceBatchNos?: string[];
    segmentBatchNo?: string;
    stockStatus?: string;
    totalPieceCount?: number;
  }

  export interface CompleteMockInspectionReq {
    cutRoundReportIds: number[];
    inspectionRemark?: string;
    inspectionResult: 'FROZEN' | 'NG' | 'OK' | string;
    inspectorName?: string;
  }

  export interface InboundTask {
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    motherSegmentBatchNo?: string;
    packedPieceCount?: number;
    planId: number;
    planNo?: string;
    planOperationId: number;
    totalPieceCount?: number;
    waitPackPieceCount?: number;
  }

  export interface SourcePiece {
    confirmTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packageStatus?: string;
    parentProductionBatchNo?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    productionBatchNo?: string;
    qualityStatus?: string;
    reportDate?: string;
    reportStatus?: string;
    sliceBatchNo?: string;
    sourceCutRoundReportId?: number;
  }

  export interface InboundBoxItem {
    boxId?: number;
    boxNo?: string;
    expiryDate?: string;
    id?: number;
    inspectionRemark?: string;
    inspectionResult?: string;
    productionBatchNo?: string;
    coaInspectionResult?: string;
    coaInspectionStatus?: string;
    coaNgReason?: string;
    ngReason?: string;
    qualityRiskFlag?: string;
    qualityRiskSnapshotJson?: string;
    qualityStatus?: string;
    scanTime?: string;
    scanUserName?: string;
    sliceBatchNo?: string;
    sourceCutRoundReportId?: number;
    sourceManualPieceId?: number;
    sourceType?: string;
  }

  export interface InboundBox {
    boxNo?: string;
    currentQty?: number;
    extraJson?: string;
    expiryDate?: string;
    id: number;
    items?: InboundBoxItem[];
    printItems?: InboundBoxItem[];
    labelNo?: string;
    lastPrintTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    qualityStatus?: string;
    motherSegmentBatchNo?: string;
    warehouseCode?: string;
    warehouseName?: string;
    locationCode?: string;
    locationName?: string;
    packageSpec?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    printCount?: number;
    lockUserName?: string;
    lockTime?: string;
    inboundUserName?: string;
    inboundTime?: string;
    inboundLockedQty?: number;
    outboundLockedQty?: number;
    recorderName?: string;
    recorderTime?: string;
    remark?: string;
    status?: string;
    targetQty?: number;
  }

  export type InboundPackageShelfStatus = 'PENDING' | 'SHELVED';

  export interface InboundPackagePageParams extends PageParam {
    keyword?: string;
    qualityStatus?: 'FROZEN' | 'NG' | 'OK';
    shelfStatus: InboundPackageShelfStatus;
  }

  export interface InboundPackageSegmentPageParams extends PageParam {
    keyword?: string;
    qualityStatus?: 'FROZEN' | 'NG' | 'OK';
    shelfStatus: InboundPackageShelfStatus;
  }

  export interface InboundPackageSegment {
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packageCount?: number;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    sampleSliceBatchNos?: string[];
    segmentBatchNo?: string;
    totalPieceCount?: number;
    qualityStatus?: 'FROZEN' | 'NG' | 'OK';
  }

  export interface LockInboundPackageReq {
    auxConsumeItems: PackageAuxConsumeItem[];
    cutRoundReportIds?: number[];
    manualPieceIds?: number[];
    locationCode?: string;
    operatorName?: string;
    packageNo?: string;
    remark?: string;
  }

  export interface InboundPackageBatchResult {
    packageCount: number;
    packageNos: string[];
    pieceCount: number;
  }

  export interface PackagingManualPieceCreateReq {
    backfillReason: string;
    coaInspectionResult: 'FROZEN' | 'NG' | 'OK';
    expiryDate: string;
    inspectionResult: 'FROZEN' | 'NG' | 'OK';
    materialCode: string;
    modelCode: string;
    productionDate: string;
    recorderName?: string;
    remark?: string;
    segmentBatchNo: string;
    sliceBatchNo: string;
  }

  export interface PackagingManualPieceDeleteReq {
    deleteReason: string;
    id: number;
  }

  export interface PackagingManualPieceImportResult {
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    successCount?: number;
    totalRows?: number;
  }

  export interface PieceLabel {
    coaInspectionResult?: string;
    expiryDate?: string;
    historical?: boolean;
    inspectionResult?: string;
    lastPrintTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    planNo?: string;
    printCount?: number;
    printStatus?: string;
    productionDate?: string;
    qualityStatus?: 'FROZEN' | 'NG' | 'OK' | string;
    recorderName?: string;
    recordStatus?: string;
    segmentBatchNo?: string;
    sliceBatchNo: string;
    sourceId: number;
    sourceType: 'CUT_ROUND_REPORT' | 'MANUAL_HISTORY' | string;
    workTime?: string;
  }

  export interface PieceLabelCandidatePageParams extends PageParam {
    businessStatus?: '' | 'IN_STOCK' | 'PACKED' | 'WAIT_PACKAGING';
    printStatus?: '' | 'PRINTED' | 'UNPRINTED';
    sliceBatchNo?: string;
    sourceType?: '' | 'CUT_ROUND_REPORT' | 'MANUAL_HISTORY';
  }

  export interface PieceLabelBatchQueryResult {
    failures?: string[];
    labels?: PieceLabel[];
  }

  export interface PieceLabelPrintedReq {
    labelContentJson?: string;
    operatorName?: string;
    printerName?: string;
    sourceId: number;
    sourceType: string;
  }

  export interface PieceLabelBatchPrintedReq {
    items: PieceLabelPrintedReq[];
  }

  export interface ConfirmInboundPackageReq {
    locationCode?: string;
    operatorName?: string;
    packageNo: string;
    remark?: string;
  }

  export interface ConfirmInboundPackageBatchReq {
    locationCode: string;
    operatorName?: string;
    packageIds: number[];
    remark?: string;
  }

  export interface InitInboundBoxesReq {
    boxCount: number;
    motherSegmentBatchNo: string;
    packageSpec: number;
    planId: number;
    planOperationId: number;
    recorderName?: string;
    remark?: string;
  }

  export interface ScanInboundPieceReq {
    boxId: number;
    scanUserName?: string;
    sliceBatchNo: string;
  }

  export interface BoxActionReq {
    id: number;
    operatorName?: string;
    reason?: string;
  }

  export interface CancelInboundPackageBatchReq {
    operatorName?: string;
    packageIds: number[];
    reason?: string;
  }

  export interface ManualOutboundPackageReq {
    id: number;
    operatorName?: string;
    reason: string;
  }

  export interface DownShelfInboundPackageBatchReq {
    operatorName?: string;
    packageIds: number[];
    reason?: string;
  }

  export interface ManualOutboundPackageBatchReq {
    operatorName?: string;
    packageIds: number[];
    reason: string;
  }

  export interface DirectOutboundPendingPackageBatchReq {
    operatorName?: string;
    packageIds: number[];
    reason: string;
  }

  export interface InboundPackageBatchActionResult {
    packageCount: number;
    packageNos: string[];
  }

  export interface ManualOutboundStockBatchReq {
    operatorName?: string;
    reason: string;
    stockIds: number[];
  }

  export interface ManualOutboundStockBatchResult {
    packageCount: number;
    sliceBatchNos: string[];
    stockCount: number;
  }

  export interface ManualOutboundRepackReturnReq {
    /** 已手工出库的库存历史流水 ID */
    txnLogId: number;
    /** 退回原因 */
    reason: string;
    /** 操作人已确认实物退回且原包装已拆开 */
    physicalReturned: boolean;
    operatorName?: string;
  }

  export interface ManualOutboundRepackReturnResult {
    sliceBatchNo: string;
    sourceInnerUnitNo: string;
    sourcePackageRemainingPieceCount: number;
  }

  export interface ShippingOrder {
    erpOrderNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    productSize?: string;
    shipQty?: number;
    shippingOrderNo?: string;
    shippingTime?: string;
    sourceSaleOrderId?: number;
  }

  export interface InitOutboundBoxesReq {
    boxCount: number;
    erpOrderNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packageSpec: number;
    productSize?: string;
    recorderName?: string;
    remark?: string;
    shipQty: number;
    shippingOrderNo: string;
    shippingTime?: string;
    sourceSaleOrderId?: number;
  }

  export interface InitOutboundBoxesFromNoticeReq {
    boxCount: number;
    packageSpec: number;
    recorderName?: string;
    remark?: string;
    sourceNoticeId: number;
  }

  export interface OutboundBoxItem {
    batchNo?: string;
    finishedStockId?: number;
    id?: number;
    inboundBoxNo?: string;
    inboundInnerUnitNo?: string;
    inboundNo?: string;
    materialCode?: string;
    modelCode?: string;
    outboundBoxId?: number;
    outboundBoxNo?: string;
    qualityStatus?: string;
    scanTime?: string;
    scanUserName?: string;
    sliceBatchNo?: string;
  }

  export interface OutboundBox {
    boxNo?: string;
    currentQty?: number;
    erpOrderNo?: string;
    id: number;
    items?: OutboundBoxItem[];
    labelNo?: string;
    lastPrintTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    outboundNo?: string;
    outboundOrderId?: number;
    printCount?: number;
    status?: string;
    targetQty?: number;
  }

  export interface OutboundOrder {
    boxCount?: number;
    boxes?: OutboundBox[];
    customerCode?: string;
    customerName?: string;
    erpOrderNo?: string;
    id: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    orderNo?: string;
    outboundNo?: string;
    outboundStatus?: string;
    pieceCount?: number;
    productType?: string;
    productSize?: string;
    shipQty?: number;
    shippingNoticeNo?: string;
    shippingOrderNo?: string;
    shippingTime?: string;
    sourceNoticeId?: number;
    sourceSaleOrderId?: number;
  }

  export interface ScanOutboundPieceReq {
    boxId: number;
    scanUserName?: string;
    sliceBatchNo: string;
  }
}

const BASE = '/mes/hc/package-fg';

export function getFgPackageBoxList(params: {
  boxType?: string;
  keyword?: string;
}) {
  return requestClient.get<MesHcFinishedPackagingApi.PackageBox[]>(
    `${BASE}/box/list`,
    { params },
  );
}

export function getFgPackageBoxInboundDetail(id: number) {
  return requestClient.get<MesHcFinishedPackagingApi.InboundBox>(
    `${BASE}/box/inbound-detail`,
    { params: { id } },
  );
}

export function getFgPackageBoxOutboundDetail(id: number) {
  return requestClient.get<MesHcFinishedPackagingApi.OutboundBox>(
    `${BASE}/box/outbound-detail`,
    { params: { id } },
  );
}

export function printFgPackageBox(
  data: MesHcFinishedPackagingApi.PackageBoxPrintReq,
) {
  return requestClient.post<number>(`${BASE}/box/print`, data);
}

export function getFgLocationGrid() {
  return requestClient.get<MesHcFinishedPackagingApi.FgLocationGrid[]>(
    `${BASE}/location/grid`,
  );
}

export function getFgLocationTree() {
  return requestClient.get<MesHcFinishedPackagingApi.FgLocationTreeWarehouse[]>(
    `${BASE}/location/tree`,
  );
}

export function saveFgWarehouse(
  data: MesHcFinishedPackagingApi.FgWarehouseSaveReq,
) {
  return requestClient.post<number>(`${BASE}/location/warehouse/save`, data);
}

export function updateFgWarehouseQualityScope(
  data: MesHcFinishedPackagingApi.FgWarehouseQualityScopeUpdateReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.FgWarehouseQualityScopeUpdateResult>(
    `${BASE}/location/warehouse/quality-scope/update`,
    data,
  );
}

export function deleteFgWarehouse(id: number) {
  return requestClient.delete<boolean>(
    `${BASE}/location/warehouse/delete?id=${id}`,
  );
}

export function saveFgRack(data: MesHcFinishedPackagingApi.FgRackSaveReq) {
  return requestClient.post<number>(`${BASE}/location/rack/save`, data);
}

export function deleteFgRack(id: number) {
  return requestClient.delete<boolean>(`${BASE}/location/rack/delete?id=${id}`);
}

export function saveFgLayer(data: MesHcFinishedPackagingApi.FgLayerSaveReq) {
  return requestClient.post<number>(`${BASE}/location/layer/save`, data);
}

export function deleteFgLayer(id: number) {
  return requestClient.delete<boolean>(
    `${BASE}/location/layer/delete?id=${id}`,
  );
}

export function saveFgLocation(
  data: MesHcFinishedPackagingApi.FgLocationSaveReq,
) {
  return requestClient.post<number>(`${BASE}/location/save`, data);
}

export function deleteFgLocation(id: number) {
  return requestClient.delete<boolean>(`${BASE}/location/delete?id=${id}`);
}

export function printFgLocation(
  data: MesHcFinishedPackagingApi.FgLocationPrintReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.FgLocationGrid>(
    `${BASE}/location/print`,
    data,
  );
}

export function getFgStockLocationOverview() {
  return requestClient.get<MesHcFinishedPackagingApi.FgStockLocationOverview[]>(
    `${BASE}/stock/location-overview`,
  );
}

export function getFgStockLocationDetail(locationCode: string) {
  return requestClient.get<MesHcFinishedPackagingApi.FgStockLocationDetail>(
    `${BASE}/stock/location-detail`,
    {
      params: { locationCode },
    },
  );
}

export function getFgStockLedgerPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcFinishedPackagingApi.FgStockLedger>>(
    `${BASE}/stock/ledger/page`,
    {
      params,
    },
  );
}

export function updateImportedFgStockData(
  data: MesHcFinishedPackagingApi.ImportedStockDataUpdateReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.FgStockLedger>(
    `${BASE}/stock/imported-data/update`,
    data,
  );
}

export function getFgStockHistoryLedgerPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.FgStockHistoryLedger>
  >(`${BASE}/stock/history-ledger/page`, { params });
}

export function getShippingNoticePage(params: PageParam & Record<string, any>) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.ShippingNotice>
  >(`${BASE}/shipping-notice/page`, {
    params,
  });
}

export function getShippingNotice(id: number) {
  return requestClient.get<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/get`,
    {
      params: { id },
    },
  );
}

export function getShippingNoticeStockCandidatePage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<MesHcFinishedPackagingApi.FgStockLedger>>(
    `${BASE}/shipping-notice/stock-candidate/page`,
    { params },
  );
}

export function getShippingNoticeBatchCandidatePage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.ShippingBatchCandidate>
  >(`${BASE}/shipping-notice/batch-candidate/page`, { params });
}

export function getShippingNoticePickCandidatePage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.ShippingNoticePickCandidate>
  >(`${BASE}/shipping-notice/pick-candidate/page`, { params });
}

export function getShippingNoticePickCandidateSegmentPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.ShippingNoticePickCandidateSegment>
  >(`${BASE}/shipping-notice/pick-candidate/segment/page`, { params });
}

export function getShippingNoticePickCandidateSegmentPieceList(
  params: Record<string, any>,
) {
  return requestClient.get<
    MesHcFinishedPackagingApi.ShippingNoticePickCandidate[]
  >(`${BASE}/shipping-notice/pick-candidate/segment-piece-list`, { params });
}

export function saveAndLockShippingNotice(
  data: MesHcFinishedPackagingApi.SaveShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/save-lock`,
    data,
  );
}

export function changeShippingNotice(
  data: MesHcFinishedPackagingApi.ChangeShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/change`,
    data,
  );
}

export function importShippingNoticeExcel(data: {
  file: File;
  fileUrl: string;
  operatorName?: string;
}) {
  return requestClient.upload<MesHcFinishedPackagingApi.ShippingNoticeExcelImportResult>(
    `${BASE}/shipping-notice/import-excel`,
    data,
  );
}

export function issueShippingNotice(
  data: MesHcFinishedPackagingApi.IssueShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/issue`,
    data,
  );
}

export function cancelShippingNotice(
  data: MesHcFinishedPackagingApi.CancelShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/cancel`,
    data,
  );
}

export function deleteShippingNotice(id: number) {
  return requestClient.delete<boolean>(
    `${BASE}/shipping-notice/delete?id=${id}`,
  );
}

export function pickShippingNotice(
  data: MesHcFinishedPackagingApi.PickShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/pick`,
    data,
  );
}

export function returnShippingNoticePick(
  data: MesHcFinishedPackagingApi.ReturnShippingNoticePickReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/release-pick`,
    data,
  );
}

export function confirmShippingNoticeForFqc(
  data: MesHcFinishedPackagingApi.ConfirmShippingNoticeForFqcReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/confirm-shipping`,
    data,
  );
}

export function inspectShippingNotice(
  data: MesHcFinishedPackagingApi.InspectShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/inspect`,
    data,
  );
}

export function packShippingNotice(
  data: MesHcFinishedPackagingApi.PackShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/pack`,
    data,
  );
}

export function pushShippingNoticeOqc(
  data: MesHcFinishedPackagingApi.PushShippingNoticeOqcReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/push-oqc`,
    data,
  );
}

export function completeShippingNotice(
  data: MesHcFinishedPackagingApi.CompleteShippingNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/shipping-notice/complete-shipping`,
    data,
  );
}

export function getPackageAuxStockPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.PackageAuxStock>
  >(`${BASE}/aux-stock/page`, {
    params,
  });
}

export function savePackageAuxStock(
  data: MesHcFinishedPackagingApi.SavePackageAuxStockReq,
) {
  return requestClient.post<number>(`${BASE}/aux-stock/save`, data);
}

export function getPackageAuxAvailableList() {
  return requestClient.get<MesHcFinishedPackagingApi.PackageAuxStock[]>(
    `${BASE}/aux-stock/available-list`,
  );
}

export function printPackageAuxStock(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.PackageAuxStock>(
    `${BASE}/aux-stock/print`,
    data,
  );
}

export function consumePackageAux(
  data: MesHcFinishedPackagingApi.PackageAuxConsumeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.PackageAuxConsumeRecord>(
    `${BASE}/aux-usage/create`,
    data,
  );
}

export function getPackageAuxTodaySummary(recordDate?: string) {
  return requestClient.get<
    MesHcFinishedPackagingApi.PackageAuxConsumeSummary[]
  >(`${BASE}/aux-usage/today-summary`, {
    params: { recordDate },
  });
}

export function getPackageAuxTodayList(recordDate?: string) {
  return requestClient.get<MesHcFinishedPackagingApi.PackageAuxConsumeRecord[]>(
    `${BASE}/aux-usage/today-list`,
    {
      params: { recordDate },
    },
  );
}

export function getPackageAuxBizList(bizType: string, bizNo: string) {
  return requestClient.get<MesHcFinishedPackagingApi.PackageAuxConsumeRecord[]>(
    `${BASE}/aux-usage/biz-list`,
    {
      params: { bizNo, bizType },
    },
  );
}

export function getFgMockInspectionPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.InspectionSlice>
  >(`${BASE}/inspection/mock/page`, {
    params,
  });
}

export function completeFgMockInspection(
  data: MesHcFinishedPackagingApi.CompleteMockInspectionReq,
) {
  return requestClient.post<number>(`${BASE}/inspection/mock/complete`, data);
}

export function getFgInspectionCompletedPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.InspectionSlice>
  >(`${BASE}/inspection/completed/page`, { params });
}

export function getFgInboundWaitPiecePage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.InspectionSlice>
  >(`${BASE}/inbound/wait-piece/page`, {
    params,
  });
}

export function getFgInboundWaitSegmentPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.InspectionSliceSegment>
  >(`${BASE}/inbound/wait-segment/page`, { params });
}

export function getFgInboundWaitSegmentPieceList(params: Record<string, any>) {
  return requestClient.get<MesHcFinishedPackagingApi.InspectionSlice[]>(
    `${BASE}/inbound/wait-segment/piece-list`,
    { params },
  );
}

export function createPackagingManualPiece(
  data: MesHcFinishedPackagingApi.PackagingManualPieceCreateReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InspectionSlice>(
    `${BASE}/inbound/manual-piece/create`,
    data,
  );
}

export function deletePackagingManualPiece(
  data: MesHcFinishedPackagingApi.PackagingManualPieceDeleteReq,
) {
  return requestClient.post<boolean>(
    `${BASE}/inbound/manual-piece/delete`,
    data,
  );
}

export function exportPackagingManualPieceImportTemplate() {
  return requestClient.download(`${BASE}/inbound/manual-piece/import-template`);
}

export function importPackagingManualPieces(file: File, operatorName?: string) {
  return requestClient.upload<MesHcFinishedPackagingApi.PackagingManualPieceImportResult>(
    `${BASE}/inbound/manual-piece/import-excel`,
    { file, operatorName },
  );
}

export function getPackagingPieceLabel(sliceBatchNo: string) {
  return requestClient.get<MesHcFinishedPackagingApi.PieceLabel>(
    `${BASE}/inbound/piece-label`,
    {
      params: { sliceBatchNo },
    },
  );
}

export function getPackagingPieceLabelCandidatePage(
  params: MesHcFinishedPackagingApi.PieceLabelCandidatePageParams,
) {
  return requestClient.get<PageResult<MesHcFinishedPackagingApi.PieceLabel>>(
    `${BASE}/inbound/piece-label/candidate/page`,
    { params },
  );
}

export function getPackagingPieceLabels(sliceBatchNos: string[]) {
  return requestClient.post<MesHcFinishedPackagingApi.PieceLabelBatchQueryResult>(
    `${BASE}/inbound/piece-label/batch-query`,
    { sliceBatchNos },
  );
}

export function markPackagingPieceLabelsPrinted(
  data: MesHcFinishedPackagingApi.PieceLabelBatchPrintedReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.PieceLabel[]>(
    `${BASE}/inbound/piece-label/batch-mark-printed`,
    data,
  );
}

export function getFgInboundPackageList(keyword?: string) {
  return requestClient.get<MesHcFinishedPackagingApi.InboundBox[]>(
    `${BASE}/inbound/package-list`,
    {
      params: { keyword },
    },
  );
}

export function getFgInboundPackagePage(
  params: MesHcFinishedPackagingApi.InboundPackagePageParams,
) {
  return requestClient.get<PageResult<MesHcFinishedPackagingApi.InboundBox>>(
    `${BASE}/inbound/package/page`,
    {
      params,
    },
  );
}

export function getFgInboundPackageSegmentPage(
  params: MesHcFinishedPackagingApi.InboundPackageSegmentPageParams,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.InboundPackageSegment>
  >(`${BASE}/inbound/package-segment/page`, { params });
}

export function getFgInboundPackageSegmentPackageList(params: {
  keyword?: string;
  qualityStatus?: 'FROZEN' | 'NG' | 'OK';
  segmentBatchNo: string;
  shelfStatus: MesHcFinishedPackagingApi.InboundPackageShelfStatus;
}) {
  return requestClient.get<MesHcFinishedPackagingApi.InboundBox[]>(
    `${BASE}/inbound/package-segment/package-list`,
    { params },
  );
}

export function getFgInboundPackedSegmentPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcFinishedPackagingApi.InboundPackageSegment>
  >(`${BASE}/inbound/packed-segment/page`, { params });
}

export function getFgInboundPackedSegmentPackageList(
  params: Record<string, any>,
) {
  return requestClient.get<MesHcFinishedPackagingApi.InboundBox[]>(
    `${BASE}/inbound/packed-segment/package-list`,
    { params },
  );
}

export function lockFgInboundPackage(
  data: MesHcFinishedPackagingApi.LockInboundPackageReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundPackageBatchResult>(
    `${BASE}/inbound/lock-package`,
    data,
  );
}

export function printFgInboundPackageCard(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundBox>(
    `${BASE}/inbound/print-card`,
    data,
  );
}

export function updateFgInboundPackageRemark(
  data: MesHcFinishedPackagingApi.InboundPackageRemarkUpdateReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundBox>(
    `${BASE}/inbound/update-remark`,
    data,
  );
}

export function cancelFgInboundPackageLock(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<number>(`${BASE}/inbound/cancel-lock`, data);
}

export function cancelFgInboundPackageLocks(
  data: MesHcFinishedPackagingApi.CancelInboundPackageBatchReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundPackageBatchActionResult>(
    `${BASE}/inbound/cancel-lock-batch`,
    data,
  );
}

export function confirmFgInboundPackage(
  data: MesHcFinishedPackagingApi.ConfirmInboundPackageReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundBox>(
    `${BASE}/inbound/confirm-package`,
    data,
  );
}

export function confirmFgInboundPackages(
  data: MesHcFinishedPackagingApi.ConfirmInboundPackageBatchReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundPackageBatchActionResult>(
    `${BASE}/inbound/confirm-package-batch`,
    data,
  );
}

export function downShelfFgInboundPackage(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundBox>(
    `${BASE}/inbound/down-shelf`,
    data,
  );
}

export function downShelfFgInboundPackages(
  data: MesHcFinishedPackagingApi.DownShelfInboundPackageBatchReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundPackageBatchActionResult>(
    `${BASE}/inbound/down-shelf-batch`,
    data,
  );
}

export function manualOutboundFgInboundPackage(
  data: MesHcFinishedPackagingApi.ManualOutboundPackageReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundBox>(
    `${BASE}/inbound/manual-outbound`,
    data,
  );
}

export function manualOutboundFgInboundPackages(
  data: MesHcFinishedPackagingApi.ManualOutboundPackageBatchReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundPackageBatchActionResult>(
    `${BASE}/inbound/manual-outbound-batch`,
    data,
  );
}

/** 不合格品库存查询专用：未上架不合格成品包装整盒出库。 */
export function manualOutboundPendingFgInboundPackages(
  data: MesHcFinishedPackagingApi.ManualOutboundPackageBatchReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundPackageBatchActionResult>(
    `${BASE}/inbound/manual-outbound-pending-batch`,
    data,
  );
}

/** 合格品待上架包装直接出库，不产生上架库存或库位占用。 */
export function directOutboundPendingFgInboundPackages(
  data: MesHcFinishedPackagingApi.DirectOutboundPendingPackageBatchReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.InboundPackageBatchActionResult>(
    `${BASE}/inbound/direct-outbound-pending-batch`,
    data,
  );
}

export function manualOutboundFgStocks(
  data: MesHcFinishedPackagingApi.ManualOutboundStockBatchReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ManualOutboundStockBatchResult>(
    `${BASE}/stock/manual-outbound`,
    data,
  );
}

/**
 * 将一片仍处于手工成品出库状态的成品拆出，退回包装台待重新包装。
 * 后续须按正常包装、待上架、入库流程生成新的内包装与标签。
 */
export function returnManualOutboundFgStockForRepack(
  data: MesHcFinishedPackagingApi.ManualOutboundRepackReturnReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ManualOutboundRepackReturnResult>(
    `${BASE}/stock/history-ledger/manual-outbound-repack-return`,
    data,
  );
}

export function getFgInboundTaskList(keyword?: string) {
  return requestClient.get<MesHcFinishedPackagingApi.InboundTask[]>(
    `${BASE}/inbound/task-list`,
    {
      params: { keyword },
    },
  );
}

export function getFgInboundSourceList(
  planId: number,
  motherSegmentBatchNo: string,
) {
  return requestClient.get<MesHcFinishedPackagingApi.SourcePiece[]>(
    `${BASE}/inbound/source-list`,
    {
      params: { motherSegmentBatchNo, planId },
    },
  );
}

export function getFgInboundBoxList(
  planOperationId: number,
  motherSegmentBatchNo: string,
) {
  return requestClient.get<MesHcFinishedPackagingApi.InboundBox[]>(
    `${BASE}/inbound/box-list`,
    {
      params: { motherSegmentBatchNo, planOperationId },
    },
  );
}

export function initFgInboundBoxes(
  data: MesHcFinishedPackagingApi.InitInboundBoxesReq,
) {
  return requestClient.post<number[]>(`${BASE}/inbound/init-boxes`, data);
}

export function scanFgInboundPiece(
  data: MesHcFinishedPackagingApi.ScanInboundPieceReq,
) {
  return requestClient.post<number>(`${BASE}/inbound/scan-piece`, data);
}

export function printFgInboundBox(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<number>(`${BASE}/inbound/print-box`, data);
}

export function confirmFgInboundBox(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<number>(`${BASE}/inbound/confirm-box`, data);
}

export function getFgShippingOrderList(keyword?: string) {
  return requestClient.get<MesHcFinishedPackagingApi.ShippingOrder[]>(
    `${BASE}/outbound/shipping-order-list`,
    {
      params: { keyword },
    },
  );
}

export function getFgOutboundShippingNoticeList(keyword?: string) {
  return requestClient.get<MesHcFinishedPackagingApi.ShippingNotice[]>(
    `${BASE}/outbound/shipping-notice-list`,
    {
      params: { keyword },
    },
  );
}

export function initFgOutboundBoxesFromNotice(
  data: MesHcFinishedPackagingApi.InitOutboundBoxesFromNoticeReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.OutboundOrder>(
    `${BASE}/outbound/init-boxes-from-notice`,
    data,
  );
}

export function initFgOutboundBoxes(
  data: MesHcFinishedPackagingApi.InitOutboundBoxesReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.OutboundOrder>(
    `${BASE}/outbound/init-boxes`,
    data,
  );
}

export function getFgOutboundOrder(outboundNo: string) {
  return requestClient.get<MesHcFinishedPackagingApi.OutboundOrder>(
    `${BASE}/outbound/order`,
    {
      params: { outboundNo },
    },
  );
}

export function getFgOutboundOrderByNotice(sourceNoticeId: number) {
  return requestClient.get<MesHcFinishedPackagingApi.OutboundOrder>(
    `${BASE}/outbound/order-by-notice`,
    {
      params: { sourceNoticeId },
    },
  );
}

export function getFgOutboundBoxList(outboundOrderId: number) {
  return requestClient.get<MesHcFinishedPackagingApi.OutboundBox[]>(
    `${BASE}/outbound/box-list`,
    {
      params: { outboundOrderId },
    },
  );
}

export function scanFgOutboundPiece(
  data: MesHcFinishedPackagingApi.ScanOutboundPieceReq,
) {
  return requestClient.post<number>(`${BASE}/outbound/scan-piece`, data);
}

export function printFgOutboundBox(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<number>(`${BASE}/outbound/print-box`, data);
}

export function confirmFgOutboundOrder(
  data: MesHcFinishedPackagingApi.BoxActionReq,
) {
  return requestClient.post<number>(`${BASE}/outbound/confirm-order`, data);
}

export function submitShippingNoticeDelivery(
  data: MesHcFinishedPackagingApi.SubmitShippingNoticeDeliveryReq,
) {
  return requestClient.post<MesHcFinishedPackagingApi.ShippingNotice>(
    `${BASE}/outbound/submit-delivery`,
    data,
  );
}
