import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcTestOuterPackagePrintApi {
  export type LabelKind = 'boxFront' | 'cleanBag' | 'customerSide' | 'padBack';

  export interface WaitSegment {
    expiryDate?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packagingQualityStatus?: string;
    pieceCount?: number;
    productionDateEnd?: string;
    productionDateStart?: string;
    sampleSliceBatchNo?: string;
    sampleSliceBatchNos?: string[];
    segmentBatchNo: string;
  }

  export interface WaitPiece {
    coaInspectionResult?: string;
    expiryDate?: string;
    inspectionResult?: string;
    inspectionTaskNo?: string;
    inspectionTime?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packagingQualityStatus?: string;
    parentProductionBatchNo?: string;
    productionBatchNo?: string;
    productionDate?: string;
    remark?: string;
    segmentBatchNo?: string;
    sliceBatchNo: string;
    sourceCutRoundReportId?: number;
    sourceId?: number;
    sourceManualPieceId?: number;
    sourceType?: string;
  }

  export interface CustomerProduct {
    customer?: string;
    customerInfoId: number;
    customerSideMethod?: string;
    customerSideSize?: string;
    customerSideTemplate?: string;
    deliveryNote?: string;
    designId?: number;
    hasMark?: string;
    labelImageFile?: string;
    labelImageId?: string;
    needEcoa?: string;
    needPaperCoa?: string;
    productItemId: number;
    productType?: string;
    rowKey?: string;
    serialNo?: string;
    shipmentFilePackageMethod?: string;
    shippingMethod?: string;
    sizeMm?: string;
    sourceRow?: number;
    specialRemark?: string;
  }

  export interface PrintDesign {
    designJson?: string;
    dpi?: number;
    heightMm?: number;
    id?: number;
    imageFile?: string;
    imageId?: string;
    labelKind?: string;
    labelName?: string;
    templateSource?: string;
    widthMm?: number;
  }

  export interface VariableCheck {
    fieldKey: string;
    fieldLabel?: string;
    message?: string;
    required?: boolean;
    source?: string;
    status?: 'ERROR' | 'OK' | 'WARNING' | string;
    value?: any;
  }

  export interface PrintPayloadItem {
    data: Record<string, any>;
    labelKind?: string;
    labelName?: string;
    requestId?: string;
    segmentBatchNo?: string;
    sliceBatchNo?: string;
    templateName?: string;
    variableChecks?: VariableCheck[];
  }

  export interface PrintPayload {
    customerProduct?: CustomerProduct;
    design?: PrintDesign;
    errorCount?: number;
    items?: PrintPayloadItem[];
    rendererTemplate?: Record<string, any>;
    warningCount?: number;
  }
}

const BASE_URL = '/mes/hc/barcode-management/test-outer-package-print';

export async function getTestOuterWaitSegmentPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcTestOuterPackagePrintApi.WaitSegment>>(`${BASE_URL}/wait-segment/page`, {
    params,
  });
}

export async function getTestOuterWaitSegmentPieceList(params: Record<string, any>) {
  return requestClient.get<MesHcTestOuterPackagePrintApi.WaitPiece[]>(`${BASE_URL}/wait-segment/piece-list`, {
    params,
  });
}

export async function getTestOuterCustomerProductPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcTestOuterPackagePrintApi.CustomerProduct>>(
    `${BASE_URL}/customer-product/page`,
    { params },
  );
}

export async function getTestOuterTemplateList(params: {
  customerInfoId: number;
  labelKind: MesHcTestOuterPackagePrintApi.LabelKind | string;
  productItemId: number;
}) {
  return requestClient.get<MesHcTestOuterPackagePrintApi.PrintDesign[]>(`${BASE_URL}/template-list`, {
    params,
  });
}

export async function buildTestOuterPrintPayload(params: {
  customerInfoId: number;
  designId: number;
  labelKind: MesHcTestOuterPackagePrintApi.LabelKind | string;
  productItemId: number;
  sliceBatchNos: string;
}) {
  return requestClient.get<MesHcTestOuterPackagePrintApi.PrintPayload>(`${BASE_URL}/print-payload`, {
    params,
  });
}
