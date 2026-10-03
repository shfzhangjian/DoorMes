import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesFgShippingFqcApi {
  export type AlignmentStatus = 'ALIGNED' | 'MISMATCH' | 'NOT_REQUIRED' | 'PENDING';
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

  export interface ShippingDetail {
    id: number;
    fqcId: number;
    fqcNo: string;
    shippingNoticeId?: number;
    shippingNoticeNo?: string;
    shippingNoticeItemId?: number;
    shippingPickItemId?: number;
    finishedStockId?: number;
    stockNo?: string;
    actualSliceBatchNo?: string;
    sliceBatchNo?: string;
    customerName?: string;
    erpOrderNo?: string;
    customerProductBatchNo?: string;
    packageSliceNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    internalItemCode?: string;
    productSize?: string;
    shippingQty?: number;
    rowJudgment: RowJudgment;
    defectCode?: string;
    defectName?: string;
    ngReason?: string;
    alignmentStatus?: AlignmentStatus;
    mismatchReason?: string;
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

  export interface AlignmentCandidate {
    actualSliceBatchNo?: string;
    alignmentStatus?: AlignmentStatus;
    customerProductBatchNo?: string;
    finishedStockId?: number;
    internalItemCode?: string;
    internalModelCode?: string;
    lockStatus?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    packageNo?: string;
    productSize?: string;
    qualityStatus?: string;
    rowJudgment?: RowJudgment;
    shippingDetailId?: number;
    shippingPickItemId: number;
    sliceBatchNo?: string;
    sourceNoticeItemId?: number;
    stockNo?: string;
  }

  export interface AlignmentRow {
    actualSliceBatchNo?: string;
    alignmentStatus?: AlignmentStatus;
    customerProductBatchNo?: string;
    finishedStockId?: number;
    id: number;
    internalItemCode?: string;
    materialCode?: string;
    materialName?: string;
    mismatchReason?: string;
    modelCode?: string;
    packageSliceNo?: string;
    productSize?: string;
    rowJudgment?: RowJudgment;
    shippingDetailId?: number;
    shippingNoticeItemId?: number;
    shippingPickItemId?: number;
    shippingQty?: number;
    sliceBatchNo?: string;
    stockNo?: string;
  }

  export interface Record {
    productType?: string;
    id: number;
    fqcNo: string;
    reportNo?: string;
    sourceReportNo?: string;
    sourceReportId?: number;
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
    shippingNoticeId?: number;
    shippingNoticeNo?: string;
    customerName?: string;
    erpOrderNo?: string;
    alignmentStatus?: AlignmentStatus;
    mismatchReason?: string;
    alignmentRows?: AlignmentRow[];
    shippingDetails?: ShippingDetail[];
    alignmentCandidates?: AlignmentCandidate[];
    items?: FqcItem[];
  }

  export interface PendingTask {
    shippingNoticeId: number;
    shippingNoticeNo: string;
    customerName?: string;
    erpOrderNo?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    productSize?: string;
    noticeStatus?: string;
    shippingTime?: string;
    pickedQty?: number;
    inspectedQty?: number;
    existingFqcId?: number;
    existingFqcNo?: string;
    existingFqcStatus?: Status;
    existingFqcJudgment?: Judgment;
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
    actualSliceBatchNo?: string;
    alignmentStatus?: string;
    customerName?: string;
    erpOrderNo?: string;
    fqcNo?: string;
    judgment?: string;
    materialCode?: string;
    modelCode?: string;
    recheckFlag?: boolean;
    shippingNoticeNo?: string;
    status?: string;
    submissionTime?: string[];
  }

  export interface ShippingDetailSaveReq {
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
    matchResult: 'MATCHED_SINGLE' | 'NOT_FOUND';
    message?: string;
    openTarget?: 'WORKBENCH';
    record?: Record;
    detail?: ShippingDetail;
  }
}

function normalizeRecord(record: MesFgShippingFqcApi.Record) {
  const normalizeItem = (item: MesFgShippingFqcApi.FqcItem) => ({
    ...item,
    actualValueRequired: !!item.actualValueRequired,
    itemResult: item.itemResult || 'PENDING',
    qaResult: item.qaResult || item.itemResult || 'PENDING',
  });
  return {
    ...record,
    alignmentStatus: record.alignmentStatus,
    judgment: record.judgment || 'PENDING',
    items: (record.items || []).map((item) => normalizeItem(item)),
    shippingDetails: (record.shippingDetails || []).map((item) => ({
      ...item,
      alignmentStatus: item.alignmentStatus || 'PENDING',
      items: (item.items || []).map((detailItem) => normalizeItem(detailItem)),
      rowJudgment: item.rowJudgment || 'PENDING',
    })),
    alignmentRows: record.alignmentRows?.map((item) => ({
      ...item,
      alignmentStatus: item.alignmentStatus || 'PENDING',
      rowJudgment: item.rowJudgment || 'PENDING',
    })),
    alignmentCandidates: record.alignmentCandidates,
  };
}

function buildItemSamples(item: MesFgShippingFqcApi.FqcItem) {
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
        : Array.from({ length: sampleSize }).map((_, index) => ({
            sampleResult: item.qaResult || item.itemResult || 'PENDING',
            sampleSeq: index + 1,
            value: item.qaResult || item.itemResult || 'PENDING',
          }));
  return source.slice(0, sampleSize).map((row, index) => ({
    sampleSeq: index + 1,
    qualitativeValue:
      row.value === 'NG' || row.value === 'OK' ? row.value : undefined,
    defects: row.defects || [],
    remark: row.remark,
    sampleResult: row.sampleResult || 'PENDING',
  }));
}

function collectProgramItems(record: MesFgShippingFqcApi.Record) {
  const keyedItems = new Map<number, MesFgShippingFqcApi.FqcItem>();
  const unkeyedItems: MesFgShippingFqcApi.FqcItem[] = [];

  const collect = (item: MesFgShippingFqcApi.FqcItem) => {
    if (item.id) {
      keyedItems.set(item.id, {
        ...(keyedItems.get(item.id) || {}),
        ...item,
      });
      return;
    }
    unkeyedItems.push(item);
  };

  (record.shippingDetails || []).forEach((detail) => {
    (detail.items || []).forEach(collect);
  });
  (record.items || []).forEach(collect);

  return [...keyedItems.values(), ...unkeyedItems];
}

function buildProgramPayload(
  record: MesFgShippingFqcApi.Record,
  scopedItems?: MesFgShippingFqcApi.FqcItem[],
) {
  const items =
    scopedItems === undefined ? collectProgramItems(record) : scopedItems;
  return {
    id: record.id,
    fqcNo: record.fqcNo,
    reportNo: record.reportNo || record.shippingNoticeNo || record.fqcNo,
    workOrderNo:
      record.workOrderNo ||
      record.erpOrderNo ||
      record.reportNo ||
      record.fqcNo,
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

export async function getFgShippingFqcPendingList(keyword?: string) {
  return requestClient.get<MesFgShippingFqcApi.PendingTask[]>(
    '/mes/quality/fg-shipping-fqc/pending-list',
    { params: { keyword } },
  );
}

export async function createFgShippingFqcFromShippingNotice(
  shippingNoticeId: number,
) {
  const result = await requestClient.post<MesFgShippingFqcApi.Record>(
    '/mes/quality/fg-shipping-fqc/create-from-shipping-notice',
    undefined,
    { params: { shippingNoticeId } },
  );
  return normalizeRecord(result);
}

export async function getFgShippingFqcPage(
  params: MesFgShippingFqcApi.PageReq,
) {
  const page = await requestClient.get<PageResult<MesFgShippingFqcApi.Record>>(
    '/mes/quality/fg-shipping-fqc/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getFgShippingFqcDetail(id: number) {
  const record = await requestClient.get<MesFgShippingFqcApi.Record>(
    `/mes/quality/fg-shipping-fqc/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function getFgShippingFqcDefectCodeOptions() {
  return requestClient.get<MesFgShippingFqcApi.DefectCodeOption[]>(
    '/mes/quality/fg-shipping-fqc/defect-code-options',
  );
}

export async function saveFgShippingFqcProgramEntry(
  record: MesFgShippingFqcApi.Record,
  scopedItems?: MesFgShippingFqcApi.FqcItem[],
) {
  const result = await requestClient.put<MesFgShippingFqcApi.Record>(
    '/mes/quality/fg-shipping-fqc/program-entry/save',
    buildProgramPayload(record, scopedItems),
  );
  return normalizeRecord(result);
}

export async function submitFgShippingFqcProgramEntry(
  record: MesFgShippingFqcApi.Record,
) {
  const result = await requestClient.put<MesFgShippingFqcApi.Record>(
    '/mes/quality/fg-shipping-fqc/program-entry/submit',
    buildProgramPayload(record),
  );
  return normalizeRecord(result);
}

export async function saveFgShippingFqcShippingDetails(
  data: MesFgShippingFqcApi.ShippingDetailSaveReq,
) {
  const result = await requestClient.put<MesFgShippingFqcApi.Record>(
    '/mes/quality/fg-shipping-fqc/shipping-detail/save',
    data,
  );
  return normalizeRecord(result);
}

export async function auditFgShippingFqc(
  id: number,
  auditResult: 'PASS' | 'REJECT',
  rejectReason?: string,
) {
  const result = await requestClient.put<MesFgShippingFqcApi.Record>(
    '/mes/quality/fg-shipping-fqc/program-entry/audit',
    { auditResult, id, rejectReason },
  );
  return normalizeRecord(result);
}

export async function resolveFgShippingFqcScan(
  data: MesFgShippingFqcApi.ScanReq,
) {
  const result = await requestClient.post<MesFgShippingFqcApi.ScanResp>(
    '/mes/quality/fg-shipping-fqc/scan/resolve',
    data,
  );
  return {
    ...result,
    record: result.record ? normalizeRecord(result.record) : undefined,
  };
}
