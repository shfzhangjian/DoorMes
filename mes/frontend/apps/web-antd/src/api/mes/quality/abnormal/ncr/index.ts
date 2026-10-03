import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesNcrApi {
  export interface MrbReview {
    id?: number;
    ncRecordId?: number;
    ncNo?: string;
    deptId?: number;
    deptName?: string;
    handlerUserId?: number;
    handlerUserName?: string;
    delegateUserId?: number;
    delegateUserName?: string;
    delegateTime?: string;
    actualHandlerUserId?: number;
    actualHandlerUserName?: string;
    suggestedDisposition?: string;
    dispositionDetail?: string;
    rootCauseCategory?: string;
    causeAnalysis?: string;
    reviewOpinion?: string;
    reviewStatus?: string;
    handleTime?: string;
    sort?: number;
  }

  export interface NcDefect {
    id?: number;
    ncRecordId?: number;
    ncNo?: string;
    defectCodeId?: number;
    defectCode?: string;
    defectName?: string;
    defectPath?: string;
    sourceSectionName?: string;
    sourceInspectionItem?: string;
    sourceResult?: string;
    primaryFlag?: boolean;
    sort?: number;
  }

  export interface Relation {
    id?: number;
    ncRecordId?: number;
    ncNo?: string;
    relationType?: string;
    relatedObjectId?: number;
    relatedObjectNo?: string;
    relatedObjectName?: string;
    relationStatus?: string;
    primaryFlag?: boolean;
    relationTime?: string;
    relationUserId?: number;
    relationUserName?: string;
    remark?: string;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    batchNo?: string;
    arrivalDate?: string;
  }

  export interface FlowLog {
    id?: number;
    ncRecordId?: number;
    ncNo?: string;
    actionCode?: string;
    actionName?: string;
    fromStatus?: string;
    toStatus?: string;
    fromNodeCode?: string;
    fromNodeName?: string;
    toNodeCode?: string;
    toNodeName?: string;
    opinion?: string;
    handlerUserId?: number;
    handlerUserName?: string;
    handleTime?: string;
    businessSnapshot?: Record<string, any>;
  }

  export interface DispositionNotify {
    id?: number;
    ncRecordId?: number;
    ncNo?: string;
    executionId?: number;
    executionNo?: string;
    sourceNodeCode?: string;
    sourceNodeName?: string;
    dispositionType?: string;
    notifyUserId?: number;
    notifyUserName?: string;
    notifyStatus?: string;
    notifyTime?: string;
    replyConclusion?: string;
    replyUserId?: number;
    replyUserName?: string;
    replyTime?: string;
    remark?: string;
  }

  export interface NcrRecord {
    id?: number;
    ncNo?: string;
    sourceType?: string;
    sourceTypeName?: string;
    sourceBizType?: string;
    sourceBizTypeName?: string;
    sourceId?: number;
    sourceNcRecordId?: number;
    sourceNo?: string;
    happenTime?: string;
    happenDeptId?: number;
    happenDeptName?: string;
    subOrderId?: number;
    processId?: number;
    processName?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    unitCode?: string;
    lotNo?: string;
    defectCode?: string;
    defectName?: string;
    defectQty?: number;
    ncLevel?: string;
    ncLevelName?: string;
    responsibilityDeptCodes?: string | string[];
    responsibilityDeptNames?: string;
    rawMaterialAbnormalCategory?: string;
    rawMaterialAbnormalCategoryName?: string;
    isolatedFlag?: boolean;
    ncDescription?: string;
    status?: string;
    processInstanceId?: string;
    currentNodeCode?: string;
    currentNodeName?: string;
    currentHandlerUserId?: number;
    currentHandlerUserName?: string;
    applicantUserId?: number;
    applicantUserName?: string;
    applicantDeptId?: number;
    applicantDeptName?: string;
    contentConfirmUserId?: number;
    contentConfirmUserName?: string;
    contentConfirmTime?: string;
    qualityConfirmUserId?: number;
    qualityConfirmUserName?: string;
    qualityConfirmTime?: string;
    mrbDecision?: string;
    finalDisposition?: string;
    finalOpinion?: string;
    finalDisposeDescription?: string;
    finalApproverId?: number;
    finalApproverName?: string;
    finalApproveTime?: string;
    stockDisposeStatus?: string;
    stockDisposeQty?: number;
    stockDisposeUserId?: number;
    stockDisposeUserName?: string;
    stockDisposeResult?: string;
    stockDisposeTime?: string;
    effectConfirmResult?: string;
    effectConfirmUserId?: number;
    effectConfirmUserName?: string;
    effectConfirmTime?: string;
    relatedExceptionNo?: string;
    relatedExceptionId?: number;
    createExceptionFlag?: boolean;
    related8dNo?: string;
    closeTime?: string;
    closeUserId?: number;
    closeUserName?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
    reviews?: MrbReview[];
    defects?: NcDefect[];
    relations?: Relation[];
    flowLogs?: FlowLog[];
    dispositionNotifies?: DispositionNotify[];
    tabType?: string;
    canHandle?: boolean;
    canWithdraw?: boolean;
    listActionCode?: string;
    listActionName?: string;
    minePending?: boolean;
    mineDiscovered?: boolean;
    mineParticipated?: boolean;
  }

  export interface PackagingPiece {
    scopeLevel?: string;
    pieceNo?: string;
    segmentBatchNo?: string;
    motherBatchNo?: string;
    dispositionType?: string;
    scopeRole?: string;
    executionResult?: string;
    remark?: string;
    scopeConfirmed?: boolean;
    packaged?: boolean;
  }

  export interface NcrPageReq extends PageParam {
    inProgressOnly?: boolean;
    packagingPieceNo?: string;
    tabType?: string;
    pendingMine?: boolean;
    discoveredMine?: boolean;
    participatedMine?: boolean;
    ncNo?: string;
    sourceType?: string;
    sourceBizType?: string;
    lotNo?: string;
    happenDeptName?: string;
    materialCode?: string;
    materialName?: string;
    defectCode?: string;
    ncLevel?: string;
    rawMaterialAbnormalCategory?: string;
    status?: string;
    mrbDecision?: string;
    finalDisposition?: string;
    linkableExceptionOnly?: boolean;
    happenTime?: string[];
  }

  export interface SubmitReq {
    contentConfirmUserId?: number;
    contentConfirmUserName?: string;
    id: number;
    opinion?: string;
    reviews?: MrbReview[];
  }

  export interface WithdrawReq {
    id: number;
    reason: string;
  }

  export interface RawMaterialRestoreReq {
    id: number;
    confirmNcNo: string;
    reason: string;
  }

  export interface ProductRestoreReq {
    id: number;
    confirmNcNo: string;
    reason: string;
  }

  export interface RestorePreview {
    documentType?: string;
    hasWarnings?: boolean;
    id?: number;
    ncNo?: string;
    sourceBizType?: string;
    sourceObjectNo?: string;
    status?: string;
    warnings?: string[];
  }

  export interface MrbReviewDelegateReq {
    id: number;
    reviewId: number;
    delegateUserId: number;
    delegateUserName?: string;
  }

  export interface CreateFromProductEventReq {
    inspectionId: number;
    sourceType: string;
  }

  export interface BatchCreateFromProductEventReq {
    contentConfirmUserId?: number;
    contentConfirmUserName?: string;
    directSubmit?: boolean;
    items: Array<{
      defectCode?: string;
      defectName?: string;
      defectQty?: number | string;
      inspectionId: number;
      ncDescription?: string;
      sourceType: string;
    }>;
    ncLevel: string;
    remark?: string;
    responsibleDeptNames: string[];
  }

  export interface CreateFromProductEventResult {
    existed?: boolean;
    inspectionId?: number;
    ncNo?: string;
    ncRecordId?: number;
    sourceType?: string;
  }

  export interface RawMaterialInspection {
    inspectionType?: string;
    inspectionTypeName?: string;
    inspectionId?: number;
    inspectionNo?: string;
    inspectionTime?: string;
    supplierCode?: string;
    supplierName?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    lotNo?: string;
    quantity?: number;
    unitCode?: string;
    qaInspectorName?: string;
    qaTime?: string;
    judgment?: string;
    status?: string;
    rawMaterialAbnormalCategory?: string;
    rawMaterialAbnormalCategoryName?: string;
    abnormalSummary?: string;
    ncrGenerated?: boolean;
    ncrId?: number;
    ncrNo?: string;
    ncrStatus?: string;
    canGenerateNcr?: boolean;
    canRejectRecheck?: boolean;
    recheckCount?: number;
    recheckGroupId?: number;
    recheckLatestInspectionId?: number;
    recheckLatestInspectionNo?: string;
    recheckPrevInspectionId?: number;
    recheckPrevInspectionNo?: string;
    recheckResult?: string;
    recheckRootInspectionId?: number;
    recheckRootInspectionNo?: string;
    recheckRoundNo?: number;
    recheckStatus?: 'NONE' | 'RECHECKING' | 'RECHECK_NG' | 'RECHECK_OK';
    recheckStatusName?: string;
    rejectNextInspectionId?: number;
    rejectNextInspectionNo?: string;
    rejectReason?: string;
    rejectTime?: string;
    rejectUserName?: string;
  }

  export interface RawMaterialInspectionPageReq extends PageParam {
    inspectionType?: string;
    inspectionNo?: string;
    supplierName?: string;
    materialCode?: string;
    materialName?: string;
    lotNo?: string;
    ncrStatus?: string;
    inspectionTime?: string[];
  }

  export interface CreateFromInspectionReq {
    inspectionId: number;
    inspectionType: string;
  }

  export interface BatchCreateFromInspectionReq {
    contentConfirmUserId?: number;
    contentConfirmUserName?: string;
    directSubmit?: boolean;
    inspectionType: string;
    items: Array<{
      defectCode?: string;
      defectName?: string;
      defectQty?: number | string;
      inspectionId: number;
      inspectionType?: string;
      ncDescription?: string;
    }>;
    ncLevel: string;
    remark?: string;
    responsibleDeptNames: string[];
  }

  export interface CreateFromInspectionResult {
    existed?: boolean;
    inspectionId?: number;
    inspectionType?: string;
    ncNo?: string;
    ncRecordId?: number;
  }

  export interface HandleReq {
    createExceptionFlag?: boolean;
    finalDisposition?: string;
    finalOpinion?: string;
    id: number;
    ncDescription?: string;
    opinion?: string;
    nextHandlerUserId?: number;
    nextHandlerUserName?: string;
    nextHandlerUserIds?: number[];
    nextHandlerUserNames?: string[];
    transferRoute?: 'DIRECT_CLOSE' | 'DISPOSITION_EXECUTION' | 'FINAL_APPROVAL';
    finalDisposeDescription?: string;
    dispositionNotifyUserIds?: number[];
    dispositionNotifyUserNames?: string[];
    reviews?: MrbReview[];
    stockDisposeQty?: number;
  }

  export interface ReturnReq {
    id: number;
    targetNodeCode?: string;
    targetNodeName?: string;
    opinion: string;
  }

  export interface FinalApproveReq {
    id: number;
    finalDisposition: string;
    finalOpinion: string;
    createExceptionFlag?: boolean;
  }

  export interface LinkExceptionReq {
    id: number;
    exceptionId?: number;
    exceptionNo?: string;
    remark?: string;
  }

  export interface DispositionScopeCandidate {
    objectKey: string;
    scopeLevel: 'MOTHER_BATCH' | 'PIECE' | 'SEGMENT';
    scopeLevelName?: string;
    label?: string;
    motherBatchNo?: string;
    segmentBatchNo?: string;
    pieceNo?: string;
    sourceObjectType?: string;
    sourceObjectId?: number;
    sourceObjectNo?: string;
    quantity?: number;
    quantityUnit?: string;
    productionLength?: number;
    currentStatus?: string;
    currentStatusName?: string;
    selectable?: boolean;
  }

  export interface DispositionExecutionScope {
    objectKey?: string;
    scopeLevel?: string;
    dispositionType?: string;
    motherBatchNo?: string;
    segmentBatchNo?: string;
    pieceNo?: string;
    scopeRole?: string;
    quantity?: number;
    executionResult?: string;
    remark?: string;
  }

  export interface DispositionExecution {
    id?: number;
    executionNo?: string;
    ncRecordId?: number;
    ncNo?: string;
    dispositionType?: string;
    ngProcessCode?: string;
    ngProcessName?: string;
    targetWorkstationCode?: string;
    targetWorkstationName?: string;
    scopeLevel?: string;
    affectedQty?: number;
    selectedQty?: number;
    derivedScrapQty?: number;
    executionStatus?: string;
    confirmUserId?: number;
    confirmUserName?: string;
    confirmTime?: string;
    executionUserId?: number;
    executionUserName?: string;
    remark?: string;
    commandNo?: string;
    commandStatus?: string;
    scopes?: DispositionExecutionScope[];
  }

  export interface DispositionContext {
    ncRecordId?: number;
    ncNo?: string;
    finalDisposition?: string;
    finalDispositionName?: string;
    ngProcessCode?: string;
    ngProcessName?: string;
    targetWorkstationCode?: string;
    targetWorkstationName?: string;
    defaultScopeLevel?: 'MOTHER_BATCH' | 'PIECE' | 'SEGMENT';
    availableScopeLevels?: Array<'MOTHER_BATCH' | 'PIECE' | 'SEGMENT'>;
    affectedQty?: number;
    sourceLotNo?: string;
    candidateSourceDescription?: string;
    emptyReason?: string;
    pickQualification?: boolean;
    candidates?: DispositionScopeCandidate[];
    existingExecution?: DispositionExecution;
  }

  export interface DispositionScopeConfirmReq {
    id: number;
    scopeLevel: 'MOTHER_BATCH' | 'PIECE' | 'SEGMENT';
    selectedObjectKeys: string[];
    scopeRemarks?: Array<{
      objectKey: string;
      dispositionType?: string;
      remark?: string;
    }>;
    executionUserId: number;
    executionUserName?: string;
    dispositionNotifyUserIds?: number[];
    dispositionNotifyUserNames?: string[];
    remark?: string;
    recutTargetSize?: string;
    recutTolerance?: string;
    recutQty?: number;
    concessionReason?: string;
  }

  export interface StockDisposeReq {
    id: number;
    confirmPickQualified?: boolean;
    stockDisposeStatus?: string;
    stockResult?: string;
    opinion?: string;
    relations?: Relation[];
  }

  export interface DispositionNotifyReplyReq {
    id: number;
    replyConclusion: string;
  }

  export interface DispositionNotifyPageReq extends PageParam {
    lotNo?: string;
    ncNo?: string;
    notifyStatus?: 'PENDING' | 'REPLIED';
  }

  export interface DispositionNotifyWorkbench {
    id?: number;
    ncRecordId?: number;
    ncNo?: string;
    executionId?: number;
    executionNo?: string;
    sourceNodeCode?: string;
    sourceNodeName?: string;
    dispositionType?: string;
    notifyUserId?: number;
    notifyUserName?: string;
    notifyStatus?: string;
    notifyTime?: string;
    replyConclusion?: string;
    replyUserId?: number;
    replyUserName?: string;
    replyTime?: string;
    remark?: string;
    sourceType?: string;
    sourceTypeName?: string;
    sourceBizType?: string;
    sourceBizTypeName?: string;
    sourceNo?: string;
    happenTime?: string;
    processName?: string;
    happenDeptName?: string;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    lotNo?: string;
    defectName?: string;
    defectQty?: number | string;
    ncLevelName?: string;
    status?: string;
    currentNodeName?: string;
    currentHandlerUserName?: string;
    finalDisposition?: string;
    canReply?: boolean;
    listActionName?: string;
  }

  export interface ReviewConfig {
    id?: number;
    unitCode?: string;
    unitName?: string;
    deptId?: number;
    deptName?: string;
    handlerUserIds?: number[];
    handlerUserNames?: string[];
    status?: number;
    sort?: number;
    remark?: string;
    opinionTemplateJson?: string;
    createTime?: string;
  }

  export interface ReviewConfigPageReq extends PageParam {
    unitName?: string;
    status?: number;
  }
}

const BASE_URL = '/mes/qms-nc-record';
const REVIEW_CONFIG_BASE_URL = '/mes/qms-nc-review-config';

export function getNcrPage(params: MesNcrApi.NcrPageReq) {
  return requestClient.get<PageResult<MesNcrApi.NcrRecord>>(
    `${BASE_URL}/page`,
    {
      params,
    },
  );
}

export function getNcrRecord(id: number) {
  return requestClient.get<MesNcrApi.NcrRecord>(`${BASE_URL}/get?id=${id}`);
}

export function createNcrRecord(data: MesNcrApi.NcrRecord) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function relaunchNcrRecord(id: number) {
  return requestClient.post<number>(`${BASE_URL}/relaunch?id=${id}`);
}

export function restoreProductNcrRecord(data: MesNcrApi.ProductRestoreReq) {
  return requestClient.post<boolean>(`${BASE_URL}/restore`, data);
}

export function getProductNcrRestorePreview(id: number) {
  return requestClient.get<MesNcrApi.RestorePreview>(
    `${BASE_URL}/restore-preview?id=${id}`,
  );
}

export function createNcrFromProductEvent(
  data: MesNcrApi.CreateFromProductEventReq,
) {
  return requestClient.post<number>(
    `${BASE_URL}/create-from-product-event`,
    data,
  );
}

export function batchCreateNcrFromProductEvents(
  data: MesNcrApi.BatchCreateFromProductEventReq,
) {
  return requestClient.post<MesNcrApi.CreateFromProductEventResult[]>(
    `${BASE_URL}/batch-create-from-product-event`,
    data,
  );
}

export function updateNcrRecord(data: MesNcrApi.NcrRecord) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function submitNcr(data: MesNcrApi.SubmitReq) {
  return requestClient.post<boolean>(`${BASE_URL}/submit`, data);
}

export function withdrawNcr(data: MesNcrApi.WithdrawReq) {
  return requestClient.post<boolean>(`${BASE_URL}/withdraw`, data);
}

export function getNcrReviewConfigPage(params: MesNcrApi.ReviewConfigPageReq) {
  return requestClient.get<PageResult<MesNcrApi.ReviewConfig>>(
    `${REVIEW_CONFIG_BASE_URL}/page`,
    { params },
  );
}

export function getNcrReviewConfigSimpleList() {
  return requestClient.get<MesNcrApi.ReviewConfig[]>(
    `${REVIEW_CONFIG_BASE_URL}/simple-list`,
  );
}

export function createNcrReviewConfig(data: MesNcrApi.ReviewConfig) {
  return requestClient.post<number>(`${REVIEW_CONFIG_BASE_URL}/create`, data);
}

export function updateNcrReviewConfig(data: MesNcrApi.ReviewConfig) {
  return requestClient.put<boolean>(`${REVIEW_CONFIG_BASE_URL}/update`, data);
}

export function deleteNcrReviewConfig(id: number) {
  return requestClient.delete<boolean>(
    `${REVIEW_CONFIG_BASE_URL}/delete?id=${id}`,
  );
}

export function handleNcr(data: MesNcrApi.HandleReq) {
  return requestClient.post<boolean>(`${BASE_URL}/handle`, data);
}

export function linkNcrException(data: MesNcrApi.LinkExceptionReq) {
  return requestClient.post<boolean>(`${BASE_URL}/link-exception`, data);
}

export function replyNcrDispositionNotify(
  data: MesNcrApi.DispositionNotifyReplyReq,
) {
  return requestClient.post<boolean>(
    `${BASE_URL}/disposition-notify/reply`,
    data,
  );
}

export function getNcrDispositionNotifyPage(
  params: MesNcrApi.DispositionNotifyPageReq,
) {
  return requestClient.get<PageResult<MesNcrApi.DispositionNotifyWorkbench>>(
    `${BASE_URL}/disposition-notify/page`,
    { params },
  );
}

export function delegateNcrMrbReview(data: MesNcrApi.MrbReviewDelegateReq) {
  return requestClient.post<boolean>(`${BASE_URL}/mrb-review/delegate`, data);
}

export function returnNcr(data: MesNcrApi.ReturnReq) {
  return requestClient.post<boolean>(`${BASE_URL}/return`, data);
}

export function finalApproveNcr(data: MesNcrApi.FinalApproveReq) {
  return requestClient.post<boolean>(`${BASE_URL}/final-approve`, data);
}

export function getNcrDispositionContext(id: number) {
  return requestClient.get<MesNcrApi.DispositionContext>(
    `${BASE_URL}/disposition-context?id=${id}`,
  );
}

export function confirmNcrDispositionScope(
  data: MesNcrApi.DispositionScopeConfirmReq,
) {
  return requestClient.post<MesNcrApi.DispositionExecution>(
    `${BASE_URL}/disposition-scope/confirm`,
    data,
  );
}

export function stockDisposeNcr(data: MesNcrApi.StockDisposeReq) {
  return requestClient.post<boolean>(`${BASE_URL}/stock-dispose`, data);
}

export function getNcrFlowLogs(ncRecordId: number) {
  return requestClient.get<MesNcrApi.FlowLog[]>(`${BASE_URL}/flow-log/list`, {
    params: { ncRecordId },
  });
}

export function exportNcrExcel(params: MesNcrApi.NcrPageReq) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}

export interface ClosedCorrectionPiece {
  scopeId: number;
  pieceNo: string;
  originalDisposition: string;
  dispositionType: string;
  editable: boolean;
  blockedReason?: string;
}
export interface ClosedCorrectionRequest {
  id: number;
  reason: string;
  changes: Array<
    Pick<
      ClosedCorrectionPiece,
      'scopeId' | 'originalDisposition' | 'dispositionType'
    >
  >;
  previewToken?: string;
}
export interface ClosedCorrectionResponse {
  id: number;
  ncNo: string;
  pieces: ClosedCorrectionPiece[];
  previewToken?: string;
}
export function getClosedNcrCorrection(id: number) {
  return requestClient.get<ClosedCorrectionResponse>(
    '/mes/qms-nc-record/closed-correction',
    { params: { id } },
  );
}
export function previewClosedNcrCorrection(data: ClosedCorrectionRequest) {
  return requestClient.post<ClosedCorrectionResponse>(
    '/mes/qms-nc-record/closed-correction/preview',
    data,
  );
}
export function saveClosedNcrCorrection(data: ClosedCorrectionRequest) {
  return requestClient.post<boolean>(
    '/mes/qms-nc-record/closed-correction/save',
    data,
  );
}

/** 包装入口只读查询，不触发处置确认或包装自动推进。 */
export function getNcrPackagingPieces(id: number) {
  return requestClient.get<MesNcrApi.PackagingPiece[]>(`${BASE_URL}/packaging-flow/pieces`, {
    params: { id },
  });
}

export function getNcrPackagingFlowRecord(id: number) {
  return requestClient.get<MesNcrApi.NcrRecord>(`${BASE_URL}/packaging-flow/get`, { params: { id } });
}
