import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQmsCoaApi {
  export type ReportStatus =
    | 'ARCHIVED'
    | 'APPROVED'
    | 'DRAFT'
    | 'ISSUED'
    | 'PENDING_CONFIRM'
    | 'PENDING_REVIEW'
    | 'REJECTED'
    | 'VOIDED';

  export interface TemplateItem {
    allowCorrectionFlag?: boolean;
    coaDisplayFlag?: boolean;
    decimalPlaces?: number;
    fixedValue?: string;
    id?: number;
    itemGroup?: string;
    itemNameCn: string;
    itemNameEn?: string;
    lowerLimit?: number;
    metricCode: string;
    requiredFlag?: boolean;
    sortNo?: number;
    sourceInspectionItem?: string;
    sourceItemType?: string;
    sourceStandardApplyType?: 'FAI' | 'FQC';
    sourceProcessCode?: string;
    sourceProcessName?: string;
    sourceStandardId?: number;
    sourceStandardItemId?: number;
    sourceStandardNo?: string;
    sourceStandardVersion?: string;
    specSource?: string;
    specText?: string;
    coaSpecText?: string;
    targetValue?: number;
    unit?: string;
    upperLimit?: number;
    inspectionMethod?: string;
    valueSourceType: 'MANUAL_ENTRY' | 'PHOTO_UPLOAD' | 'PROCESS_INSPECTION';
    valueStrategy: string;
  }

  export interface Template {
    auditOpinion?: string;
    auditStatus?: string;
    auditTime?: string;
    auditorName?: string;
    customerCode?: string;
    customerId?: number;
    customerName?: string;
    customerProductCode?: string;
    customerProductName?: string;
    footerStatement?: string;
    id?: number;
    items: TemplateItem[];
    languageType: string;
    materialCode?: string;
    materialId?: number;
    materialName?: string;
    productModelCode?: string;
    productModelId?: number;
    productModelName?: string;
    remark?: string;
    reportTitle: string;
    status?: number;
    templateCode?: string;
    templateName: string;
    versionNo: string;
  }

  export interface FaiStandardItem {
    id: number;
    inspectionItem: string;
    inspectionMethod?: string;
    itemType?: string;
    maxValue?: number;
    minValue?: number;
    sampleSize?: number;
    sheetMetricCode?: string;
    sort?: number;
    standardDesc?: string;
    targetValue?: number;
    unit?: string;
  }

  export interface FaiStandard {
    id: number;
    items: FaiStandardItem[];
    materialCode?: string;
    materialName?: string;
    processCode?: string;
    processName?: string;
    productModelCode?: string;
    productModelName?: string;
    standardName: string;
    standardNo: string;
    version: string;
  }

  export interface StandardItem {
    id: number;
    standardId: number;
    standardNo: string;
    standardName: string;
    standardVersion: string;
    standardApplyType: 'FAI' | 'FQC';
    processId?: number;
    processCode?: string;
    processName?: string;
    productModelCode?: string;
    productModelName?: string;
    sort?: number;
    inspectionItem: string;
    itemType?: string;
    targetValue?: number;
    minValue?: number;
    maxValue?: number;
    standardDesc?: string;
    unit?: string;
    inspectionMethod?: string;
    sheetMetricCode?: string;
    ruleDescription?: string;
  }

  export interface StandardItemPageReq extends PageParam {
    keyword?: string;
    processCode?: string;
    productModelCode: string;
    standardApplyType?: 'FAI' | 'FQC';
  }

  export interface ReportItem {
    actualValue?: string;
    allowCorrectionFlag?: boolean;
    correctedFlag?: boolean;
    correctionCount?: number;
    decimalPlaces?: number;
    displayValue?: string;
    id: number;
    itemGroup?: string;
    itemNameCn: string;
    itemNameEn?: string;
    valueSourceType?: 'MANUAL_ENTRY' | 'PHOTO_UPLOAD' | 'PROCESS_INSPECTION';
    sourceStandardApplyType?: 'FAI' | 'FQC';
    lastCorrectionReason?: string;
    lowerLimit?: number;
    metricCode: string;
    requiredFlag?: boolean;
    result?: string;
    sourceDataCompleteFlag?: boolean;
    sourceFaiId?: number;
    sourceFaiItemId?: number;
    sourceFaiNo?: string;
    sourceProcessCode?: string;
    sourceProcessName?: string;
    sourceQaTime?: string;
    sourceStandardItemId?: number;
    sourceStandardNo?: string;
    sourceStandardVersion?: string;
    sourceValue?: string;
    specText?: string;
    coaSpecText?: string;
    targetValue?: number;
    unit?: string;
    upperLimit?: number;
    inspectionMethod?: string;
    rawValueJson?: string;
    attachmentUrls?: string;
    sourceType?: 'FAI' | 'FQC' | 'MANUAL' | 'PHOTO';
    sourceOrderId?: number;
    sourceOrderNo?: string;
    sourceOrderItemId?: number;
  }

  export interface MotherBatch {
    productionBatchNo: string;
    processInstanceId?: string;
    materialCode?: string;
    materialName?: string;
    productModelCode?: string;
    manufactureDate?: string;
    latestInspectionTime?: string;
    inspectionCount?: number;
  }

  export interface ReportItemValueCandidate {
    candidateKey: string;
    sourceType: 'FAI' | 'FQC';
    sourceOrderId: number;
    sourceOrderNo: string;
    sourceItemId: number;
    sourceBatchNo?: string;
    processCode?: string;
    processName?: string;
    standardNo?: string;
    standardVersion?: string;
    inspectionItem?: string;
    actualValue?: string;
    averageValue?: number;
    minValue?: number;
    maxValue?: number;
    result?: string;
    rawValuesJson?: string;
    inspectionTime?: string;
  }

  export interface ReportSource {
    effectiveReason?: string;
    historicalBackfill?: boolean;
    id: number;
    processCode?: string;
    processName?: string;
    qaInspectorName?: string;
    qaTime?: string;
    recheckFlag?: boolean;
    recheckRoundNo?: number;
    releaseResult?: string;
    selectedFlag?: boolean;
    sourceBatchNo?: string;
    sourceId: number;
    sourceNo: string;
    sourceResult?: string;
    sourceStatus?: string;
    standardNo?: string;
    standardVersion?: string;
  }

  export interface CorrectionLog {
    afterActualValue?: string;
    afterDisplayValue?: string;
    afterResult?: string;
    beforeActualValue?: string;
    beforeDisplayValue?: string;
    beforeResult?: string;
    correctionReason: string;
    correctionTime?: string;
    correctorName?: string;
    id: number;
    itemName?: string;
    metricCode?: string;
    sourceFaiNo?: string;
    sourceValue?: string;
  }

  export interface AuditLog {
    actionTime?: string;
    actionType: string;
    afterStatus?: string;
    beforeStatus?: string;
    id: number;
    operatorName?: string;
    opinion?: string;
    reason?: string;
  }

  export interface ShippingRelation {
    actualSliceBatchNo?: string;
    customerBatchNo?: string;
    id: number;
    oqcStatus?: string;
    outerBoxNo?: string;
    qualityStatus?: string;
    shippingInspectionResult?: string;
    shippingNoticeItemId?: number;
    shippingNoticeNo?: string;
    shippingQuantity?: number;
    sliceBatchNo?: string;
    stockNo?: string;
  }

  export interface Report {
    coaNo: string;
    correctedItemCount?: number;
    correctionLogs?: CorrectionLog[];
    createTime?: string;
    customerBatchNo?: string;
    customerName?: string;
    customerProductCode?: string;
    customerProductName?: string;
    dataCompleteFlag?: boolean;
    dataSnapshotHash?: string;
    externalProductCode?: string;
    externalProductModel?: string;
    footerStatement?: string;
    id: number;
    issuedByName?: string;
    issuedTime?: string;
    items?: ReportItem[];
    materialCode?: string;
    materialName?: string;
    productSize?: string;
    shelfLife?: string;
    issueDate?: string;
    overallResult?: string;
    productModelCode?: string;
    productModelName?: string;
    productionBatchNo: string;
    remark?: string;
    reportItemCount?: number;
    reportStatus: ReportStatus;
    reportTitle?: string;
    requiredIncompleteCount?: number;
    reviewOpinion?: string;
    inspectorName?: string;
    inspectorTime?: string;
    confirmerName?: string;
    confirmTime?: string;
    confirmOpinion?: string;
    reviewTime?: string;
    reviewerName?: string;
    revisionNo: number;
    rootReportId?: number;
    shippingNoticeNo?: string;
    shippingQuantity?: number;
    shippingRelations?: ShippingRelation[];
    sources?: ReportSource[];
    templateCode?: string;
    templateName?: string;
    templateVersion?: string;
    verificationCode?: string;
    voidReason?: string;
    voidedByName?: string;
    voidedTime?: string;
    auditLogs?: AuditLog[];
  }

  export interface TemplatePageReq extends PageParam {
    auditStatus?: string;
    customerName?: string;
    keyword?: string;
    materialCode?: string;
    productModelCode?: string;
    status?: number;
  }

  export interface ReportPageReq extends PageParam {
    customerName?: string;
    keyword?: string;
    productionBatchNo?: string;
    reportStatus?: string;
    reviewQueue?: string;
    shippingNoticeNo?: string;
  }
}

const BASE_URL = '/mes/quality/coa';

export const getCoaTemplatePage = (params: MesQmsCoaApi.TemplatePageReq) =>
  requestClient.get<PageResult<MesQmsCoaApi.Template>>(
    `${BASE_URL}/template/page`,
    { params },
  );
export const getCoaTemplate = (id: number) =>
  requestClient.get<MesQmsCoaApi.Template>(`${BASE_URL}/template/get`, {
    params: { id },
  });
export const createCoaTemplate = (data: MesQmsCoaApi.Template) =>
  requestClient.post<number>(`${BASE_URL}/template/create`, data);
export const updateCoaTemplate = (data: MesQmsCoaApi.Template) =>
  requestClient.put<boolean>(`${BASE_URL}/template/update`, data);
export const submitCoaTemplate = (id: number) =>
  requestClient.put<boolean>(`${BASE_URL}/template/submit`, undefined, {
    params: { id },
  });
export const auditCoaTemplate = (data: {
  id: number;
  opinion?: string;
  result: 'PASS' | 'REJECT';
}) => requestClient.put<boolean>(`${BASE_URL}/template/audit`, data);
export const changeCoaTemplateStatus = (data: { id: number; status: number }) =>
  requestClient.put<boolean>(`${BASE_URL}/template/status`, data);
export const upgradeCoaTemplate = (id: number) =>
  requestClient.post<number>(`${BASE_URL}/template/upgrade`, undefined, {
    params: { id },
  });
export const getCoaFaiStandards = (keyword?: string) =>
  requestClient.get<MesQmsCoaApi.FaiStandard[]>(
    `${BASE_URL}/template/fai-standard-list`,
    { params: { keyword } },
  );
export const getCoaStandardItemPage = (
  params: MesQmsCoaApi.StandardItemPageReq,
) =>
  requestClient.get<PageResult<MesQmsCoaApi.StandardItem>>(
    `${BASE_URL}/template/standard-item-page`,
    { params },
  );

export const getCoaReportPage = (params: MesQmsCoaApi.ReportPageReq) =>
  requestClient.get<PageResult<MesQmsCoaApi.Report>>(
    `${BASE_URL}/report/page`,
    { params },
  );
export const exportCoaReportExcel = (params: MesQmsCoaApi.ReportPageReq) =>
  requestClient.download(`${BASE_URL}/report/export-excel`, { params });
export const exportCoaReportItemExcel = (id: number) =>
  requestClient.download(`${BASE_URL}/report/item/export-excel`, { params: { id } });
export const getCoaReport = (id: number) =>
  requestClient.get<MesQmsCoaApi.Report>(`${BASE_URL}/report/get`, {
    params: { id },
  });
export const getCoaMotherBatchPage = (params: {
  keyword?: string;
  pageNo: number;
  pageSize: number;
  templateId: number;
}) => requestClient.get<PageResult<MesQmsCoaApi.MotherBatch>>(
  `${BASE_URL}/report/mother-batch-page`,
  { params },
);
export const generateCoaReport = (data: {
  customerBatchNo?: string;
  issueDate?: string;
  productSize?: string;
  productionBatchNo: string;
  remark?: string;
  shelfLife?: string;
  shippingNoticeNo?: string;
  templateId: number;
}) => requestClient.post<number>(`${BASE_URL}/report/generate`, data);
export const refreshCoaReport = (id: number) =>
  requestClient.put<boolean>(`${BASE_URL}/report/refresh`, undefined, {
    params: { id },
  });
export const correctCoaReportItem = (data: {
  coaSpecText?: string;
  correctedResult?: string;
  correctedValue: string;
  correctionReason: string;
  reportId: number;
  reportItemId: number;
}) => requestClient.put<boolean>(`${BASE_URL}/report/item/correct`, data);
export const getCoaReportItemValueCandidatePage = (params: {
  pageNo: number;
  pageSize: number;
  reportId: number;
  reportItemId: number;
}) => requestClient.get<PageResult<MesQmsCoaApi.ReportItemValueCandidate>>(
  `${BASE_URL}/report/item/value-candidate-page`,
  { params },
);
export const applyCoaReportItemValue = (data: {
  attachmentUrls?: string[];
  candidateKeys?: string[];
  coaSpecText?: string;
  manualValue?: string;
  reportId: number;
  reportItemId: number;
  valueStrategy?: string;
}) => requestClient.put<boolean>(`${BASE_URL}/report/item/apply-value`, data);
export const submitCoaReport = (id: number) =>
  requestClient.put<boolean>(`${BASE_URL}/report/submit`, undefined, {
    params: { id },
  });
export const confirmCoaReport = (data: {
  id: number;
  opinion?: string;
  result: 'PASS' | 'REJECT';
}) => requestClient.put<boolean>(`${BASE_URL}/report/confirm`, data);
export const auditCoaReport = (data: {
  id: number;
  opinion?: string;
  result: 'PASS' | 'REJECT';
}) => requestClient.put<boolean>(`${BASE_URL}/report/audit`, data);
export const issueCoaReport = (data: { id: number; reason?: string }) =>
  requestClient.put<boolean>(`${BASE_URL}/report/issue`, data);
export const createCoaReportRevision = (data: { id: number; reason: string }) =>
  requestClient.post<number>(`${BASE_URL}/report/revision`, data);
export const voidCoaReport = (data: { id: number; reason: string }) =>
  requestClient.put<boolean>(`${BASE_URL}/report/void`, data);
