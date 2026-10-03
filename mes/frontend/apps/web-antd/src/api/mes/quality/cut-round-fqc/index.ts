import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesCutRoundFqcApi {
  export interface QtimeInfo {
    elapsedMinutes?: number;
    message?: string;
    sourceEndTime?: string;
    standardMinutes?: number;
    status?: string;
    targetStarted?: boolean;
    targetStartTime?: string;
    timeout?: boolean;
  }

  export type Judgment = 'NG' | 'OK' | 'PENDING';
  export type RowJudgment = 'NG' | 'OK' | 'PENDING';
  export type Status =
    | 'CANCELED'
    | 'COMPLETED'
    | 'INSPECTING'
    | 'PENDING'
    | 'REJECTED'
    | 'WAITING_QA';

  export interface FqcSample {
    defects?: FqcSampleDefect[];
    qualitativeValue?: 'NG' | 'OK';
    rawValuesJson?: string;
    remark?: string;
    recheckItemFlag?: boolean;
    sampleResult?: Judgment;
    sampleSeq: number;
  }

  export interface FqcSampleDefect {
    defectCode?: string;
    defectCodeId?: number;
    defectLevel?: string;
    defectName?: string;
    sort?: number;
  }

  export interface FqcItem {
    id?: number;
    fqcId?: number;
    fqcNo?: string;
    submissionDetailId?: number;
    cutRoundInspectionDetailId?: number;
    sliceSeqNo?: number;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    standardItemId?: number;
    inspectionItem: string;
    itemType: 'QUALITATIVE' | 'QUANTITATIVE';
    standardDesc?: string;
    unit?: string;
    ruleDescription?: string;
    inspectionMethod?: string;
    testFrequencyJudgement?: string;
    sampleSize: number;
    minValueLimit?: number;
    maxValueLimit?: number;
    itemResult?: Judgment;
    qaResult?: Judgment;
    actualValueRequired?: boolean;
    actualValue?: string;
    inputStatus?: string;
    sort?: number;
    samples?: FqcSample[];
    recheckItemFlag?: boolean;
    qaValues?: Array<{
      defects?: FqcSampleDefect[];
      remark?: string;
      recheckItemFlag?: boolean;
      sampleResult: Judgment;
      value?: string;
    }>;
  }

  export interface SubmissionDetail {
    id: number;
    fqcId: number;
    fqcNo: string;
    cutRoundInspectionTaskId?: number;
    cutRoundInspectionTaskNo?: string;
    cutRoundInspectionDetailId?: number;
    cutRoundReportId?: number;
    seqNo?: number;
    planNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    sizeRule?: string;
    productionBatchNo: string;
    parentProductionBatchNo?: string;
    qualityRiskFlag?: 'ADHESIVE2_NG' | 'BOTH_NG' | 'CUT_ROUND_NG' | 'NONE' | string;
    qualityRiskSnapshotJson?: string;
    photoUrls?: string[];
    rowJudgment: RowJudgment;
    defectCode?: string;
    defectName?: string;
    ngReason?: string;
    inspectorName?: string;
    inspectionTime?: string;
    remark?: string;
    recheckDetailFlag?: boolean;
    entryProgress?: number;
    requiredItemCount?: number;
    completedItemCount?: number;
    abnormalItemCount?: number;
    items?: FqcItem[];
  }

  export interface Record {
    id: number;
    fqcNo: string;
    reportNo?: string;
    sourceReportNo?: string;
    workOrderNo?: string;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    productModel?: string;
    productBatchNo?: string;
    batchNo?: string;
    produceQty?: number;
    sampleQty?: number;
    submissionDetailCount?: number;
    okQty?: number;
    ngQty?: number;
    status: Status;
    judgment: Judgment;
    releaseResult?: string;
    submissionTime?: string;
    submitterName?: string;
    inspectorName?: string;
    inspectionTime?: string;
    qaInspectorName?: string;
    qaTime?: string;
    entryProgress?: number;
    requiredItemCount?: number;
    completedItemCount?: number;
    abnormalItemCount?: number;
    sheetLocked?: boolean;
    lastReturnReason?: string;
    originalInspectionNo?: string;
    recheckFlag?: boolean;
    rejectPrevInspectionNo?: string;
    rejectRootInspectionNo?: string;
    remark?: string;
    qtime?: QtimeInfo;
    submissionDetails?: SubmissionDetail[];
    items?: FqcItem[];
  }

  export interface DefectCause {
    id?: number;
    defectCodeId?: number;
    defectCode?: string;
    defectName?: string;
    reasonCode?: string;
    reasonName: string;
    reasonType?: string;
    reasonDesc?: string;
    sort?: number;
    status?: number;
    remark?: string;
  }

  export interface DefectCodeOption {
    id: number;
    code: string;
    name: string;
    level?: string;
    referencePicUrls?: string[];
    causes?: DefectCause[];
    remark?: string;
    sort?: number;
    status: number;
    type: 'CATEGORY' | 'ITEM';
  }

  export interface PageReq extends PageParam {
    fqcNo?: string;
    judgment?: string;
    materialCode?: string;
    planNo?: string;
    productionBatchNo?: string;
    productModel?: string;
    recheckFlag?: boolean;
    status?: string;
    submissionTime?: string[];
    taskNo?: string;
  }

  export interface SubmissionDetailSaveReq {
    fqcId: number;
    details: Array<{
      defectCode?: string;
      defectName?: string;
      id: number;
      ngReason?: string;
      remark?: string;
      rowJudgment?: RowJudgment;
    }>;
  }

  export interface SubmissionDetailPhotoSaveReq {
    fqcId: number;
    submissionDetailId: number;
    photoUrls?: string[];
  }

  export interface ItemPhoto {
    id: number;
    fqcId: number;
    fqcNo?: string;
    submissionDetailId: number;
    fqcItemId: number;
    sampleId?: number;
    productionBatchNo?: string;
    inspectionItem?: string;
    photoUrl: string;
    photoScene?: 'DEFECT' | 'RECHECK' | 'RESULT';
    defectCodeId?: number;
    defectCode?: string;
    defects?: ItemPhotoDefect[];
    remark?: string;
    sort?: number;
    capturedById?: number;
    capturedByName?: string;
    capturedTime?: string;
  }

  export interface ItemPhotoDefect {
    id?: number;
    defectCodeId: number;
    defectCode?: string;
    defectName?: string;
    defectLevel?: string;
    sort?: number;
  }

  export interface ItemPhotoSaveReq {
    fqcId: number;
    submissionDetailId: number;
    fqcItemId: number;
    sampleId?: number;
    photoUrl: string;
    photoScene?: 'DEFECT' | 'RECHECK' | 'RESULT';
    defectCodeId?: number;
    defectCode?: string;
    defects?: Array<{
      defectCodeId: number;
      sort?: number;
    }>;
    remark?: string;
    sort?: number;
  }

  export interface ScanReq {
    scanCode: string;
    scanScene?: string;
    currentFqcId?: number;
    clientType?: string;
    terminalCode?: string;
  }

  export interface ScanResp {
    scanCode: string;
    scanTargetType: string;
    matchResult: 'MATCHED_SINGLE' | 'MATCHED_MULTIPLE' | 'NOT_FOUND';
    message?: string;
    openTarget?: 'WORKBENCH';
    candidateCount?: number;
    record?: Record;
    detail?: SubmissionDetail;
    candidates?: ScanCandidate[];
  }

  export interface ScanCandidate {
    fqcId: number;
    fqcNo?: string;
    fqcStatus?: Status;
    submissionDetailId?: number;
    cutRoundInspectionTaskNo?: string;
    planNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    materialCode?: string;
    modelCode?: string;
  }
}

function normalizeItem(item: MesCutRoundFqcApi.FqcItem) {
  return {
    ...item,
    actualValueRequired: !!item.actualValueRequired,
    itemResult: item.itemResult || 'PENDING',
    qaResult: item.qaResult || item.itemResult || 'PENDING',
  };
}

function normalizeRecord(record: MesCutRoundFqcApi.Record) {
  return {
    ...record,
    judgment: record.judgment || 'PENDING',
    items: (record.items || []).map((item) => normalizeItem(item)),
    submissionDetails: (record.submissionDetails || []).map((item) => ({
      ...item,
      items: (item.items || []).map((detailItem) => normalizeItem(detailItem)),
      photoUrls: item.photoUrls || [],
      rowJudgment: item.rowJudgment || 'PENDING',
    })),
  };
}

function buildItemSamples(item: MesCutRoundFqcApi.FqcItem) {
  const sampleSize = Math.max(1, item.sampleSize || 1);
  const source =
    item.qaValues && item.qaValues.length > 0
      ? item.qaValues
      : item.samples && item.samples.length > 0
        ? item.samples.map((sample) => ({
            defects: sample.defects,
            remark: sample.remark,
            sampleResult: sample.sampleResult || 'PENDING',
            value:
              sample.qualitativeValue ||
              sample.sampleResult ||
              (sample.rawValuesJson as string | undefined),
          }))
        : Array.from({ length: Math.max(1, item.sampleSize || 1) }).map(
            (_, index) => ({
              sampleResult: item.qaResult || item.itemResult || 'PENDING',
              value: item.qaResult || item.itemResult || 'PENDING',
              sampleSeq: index + 1,
            }),
          );
  return source.slice(0, sampleSize).map((row, index) => ({
    sampleSeq: index + 1,
    qualitativeValue:
      row.value === 'NG' || row.value === 'OK' ? row.value : undefined,
    defects: row.defects || [],
    remark: row.remark,
    sampleResult: row.sampleResult || 'PENDING',
  }));
}

function collectProgramItems(record: MesCutRoundFqcApi.Record) {
  const keyedItems = new Map<number, MesCutRoundFqcApi.FqcItem>();
  const unkeyedItems: MesCutRoundFqcApi.FqcItem[] = [];

  const collect = (item: MesCutRoundFqcApi.FqcItem) => {
    if (item.id) {
      keyedItems.set(item.id, {
        ...(keyedItems.get(item.id) || {}),
        ...item,
      });
      return;
    }
    unkeyedItems.push(item);
  };

  (record.submissionDetails || []).forEach((detail) => {
    (detail.items || []).forEach(collect);
  });
  (record.items || []).forEach(collect);

  return [...keyedItems.values(), ...unkeyedItems];
}

function buildProgramPayload(
  record: MesCutRoundFqcApi.Record,
  scopedItems?: MesCutRoundFqcApi.FqcItem[],
) {
  const items = scopedItems === undefined ? collectProgramItems(record) : scopedItems;
  return {
    id: record.id,
    fqcNo: record.fqcNo,
    reportNo: record.reportNo || record.fqcNo,
    workOrderNo: record.workOrderNo || record.reportNo || record.fqcNo,
    materialCode: record.materialCode,
    materialName: record.materialName,
    specification: record.specification,
    productModel: record.productModel,
    productBatchNo: record.productBatchNo,
    batchNo: record.batchNo || record.productBatchNo || record.fqcNo,
    produceQty: record.produceQty,
    sampleQty: record.sampleQty,
    status: record.status,
    judgment: record.judgment || 'PENDING',
    entryLayout: 'PROGRAM_FORM',
    entryMode: 'MANUAL',
    remark: record.remark,
    items: items.map((item) => ({
      id: item.id,
      submissionDetailId: item.submissionDetailId,
      cutRoundInspectionDetailId: item.cutRoundInspectionDetailId,
      sliceSeqNo: item.sliceSeqNo,
      productionBatchNo: item.productionBatchNo,
      parentProductionBatchNo: item.parentProductionBatchNo,
      standardItemId: item.standardItemId,
      inspectionItem: item.inspectionItem,
      itemType: item.itemType,
      standardDesc: item.standardDesc,
      unit: item.unit,
      ruleDescription: item.ruleDescription,
      inspectionMethod: item.inspectionMethod,
      testFrequencyJudgement: item.testFrequencyJudgement,
      sampleSize: item.sampleSize,
      minValueLimit: item.minValueLimit,
      maxValueLimit: item.maxValueLimit,
      itemResult: item.itemResult,
      qaResult: item.qaResult,
      actualValueRequired: item.actualValueRequired,
      actualValue: item.actualValue,
      inputStatus: item.inputStatus,
      sort: item.sort,
      samples: buildItemSamples(item),
    })),
  };
}

export async function getCutRoundFqcPage(params: MesCutRoundFqcApi.PageReq) {
  const page = await requestClient.get<PageResult<MesCutRoundFqcApi.Record>>(
    '/mes/quality/cut-round-fqc/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getCutRoundFqcDetail(id: number) {
  const record = await requestClient.get<MesCutRoundFqcApi.Record>(
    `/mes/quality/cut-round-fqc/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function getCutRoundFqcLightDetail(id: number) {
  const record = await requestClient.get<MesCutRoundFqcApi.Record>(
    `/mes/quality/cut-round-fqc/get-light?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function getCutRoundFqcSubmissionDetailItems(
  id: number,
  submissionDetailId: number,
) {
  const items = await requestClient.get<MesCutRoundFqcApi.FqcItem[]>(
    '/mes/quality/cut-round-fqc/submission-detail/items',
    { params: { id, submissionDetailId } },
  );
  return (items || []).map((item) => normalizeItem(item));
}

export async function getCutRoundFqcItemPhotos(
  id: number,
  submissionDetailId: number,
  fqcItemId: number,
) {
  return requestClient.get<MesCutRoundFqcApi.ItemPhoto[]>(
    '/mes/quality/cut-round-fqc/submission-detail/item-photos',
    { params: { fqcItemId, id, submissionDetailId } },
  );
}

export async function saveCutRoundFqcItemPhoto(
  data: MesCutRoundFqcApi.ItemPhotoSaveReq,
) {
  return requestClient.put<MesCutRoundFqcApi.ItemPhoto>(
    '/mes/quality/cut-round-fqc/submission-detail/item-photo/save',
    data,
  );
}

export async function deleteCutRoundFqcItemPhoto(id: number, photoId: number) {
  return requestClient.delete<boolean>(
    '/mes/quality/cut-round-fqc/submission-detail/item-photo/delete',
    { params: { id, photoId } },
  );
}

export async function getCutRoundFqcDefectCodeOptions() {
  return requestClient.get<MesCutRoundFqcApi.DefectCodeOption[]>(
    '/mes/quality/cut-round-fqc/defect-code-options',
  );
}

export async function startCutRoundFqcProgramEntry(id: number) {
  const result = await requestClient.put<MesCutRoundFqcApi.Record>(
    '/mes/quality/cut-round-fqc/program-entry/start',
    undefined,
    { params: { id } },
  );
  return normalizeRecord(result);
}

export async function saveCutRoundFqcProgramEntry(
  record: MesCutRoundFqcApi.Record,
  scopedItems?: MesCutRoundFqcApi.FqcItem[],
) {
  const result = await requestClient.put<MesCutRoundFqcApi.Record>(
    '/mes/quality/cut-round-fqc/program-entry/save',
    buildProgramPayload(record, scopedItems),
  );
  return normalizeRecord(result);
}

export async function submitCutRoundFqcProgramEntry(
  record: MesCutRoundFqcApi.Record,
) {
  const result = await requestClient.put<MesCutRoundFqcApi.Record>(
    '/mes/quality/cut-round-fqc/program-entry/submit',
    buildProgramPayload(record),
  );
  return normalizeRecord(result);
}

export async function saveCutRoundFqcSubmissionDetails(
  data: MesCutRoundFqcApi.SubmissionDetailSaveReq,
) {
  const result = await requestClient.put<MesCutRoundFqcApi.Record>(
    '/mes/quality/cut-round-fqc/submission-detail/save',
    data,
  );
  return normalizeRecord(result);
}

export async function saveCutRoundFqcSubmissionDetailPhotos(
  data: MesCutRoundFqcApi.SubmissionDetailPhotoSaveReq,
) {
  const result = await requestClient.put<MesCutRoundFqcApi.Record>(
    '/mes/quality/cut-round-fqc/submission-detail/photos/save',
    data,
  );
  return normalizeRecord(result);
}

export async function auditCutRoundFqc(
  id: number,
  auditResult: 'PASS' | 'REJECT',
  rejectReason?: string,
) {
  const result = await requestClient.put<MesCutRoundFqcApi.Record>(
    '/mes/quality/cut-round-fqc/program-entry/audit',
    { id, auditResult, rejectReason },
  );
  return normalizeRecord(result);
}

export async function revokeCutRoundFqcAudit(id: number, revokeReason: string) {
  const result = await requestClient.put<MesCutRoundFqcApi.Record>(
    '/mes/quality/cut-round-fqc/program-entry/revoke-audit',
    { id, revokeReason },
  );
  return normalizeRecord(result);
}

export async function resolveCutRoundFqcScan(data: MesCutRoundFqcApi.ScanReq) {
  const result = await requestClient.post<MesCutRoundFqcApi.ScanResp>(
    '/mes/quality/cut-round-fqc/scan/resolve',
    data,
  );
  return {
    ...result,
    record: result.record ? normalizeRecord(result.record) : undefined,
  };
}
