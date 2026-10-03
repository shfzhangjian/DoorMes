import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesOqcApi {
  export type Judgment = '-' | 'NA' | 'NG' | 'OK' | 'PENDING';
  export type AuditResult = 'FAIL' | 'PASS' | 'REJECT';

  export interface OqcSample {
    id?: number;
    sampleRole?: 'OPERATOR' | 'QA';
    sampleSeq: number;
    samplePosition?: string;
    rawValuesJson?: string;
    resultValue?: number;
    measuredValue?: number;
    qualitativeValue?: 'NA' | 'NG' | 'OK';
    sampleResult?: Judgment;
    sampleGroupNo?: number;
    valueSource?: 'MANUAL' | string;
    remark?: string;
    recheckItemFlag?: boolean;
  }

  export interface OqcItem {
    id?: number;
    oqcId?: number;
    oqcNo?: string;
    standardItemId?: number;
    category?: 'COA' | 'LABEL' | 'OTHER' | 'PACKING' | 'PRODUCT' | string;
    inspectionItem: string;
    itemType: 'QUALITATIVE' | 'QUANTITATIVE';
    targetValue?: number;
    standardDesc: string;
    unit?: string;
    ruleDescription?: string;
    inspectionMethod?: string;
    testFrequencyJudgement?: string;
    valueTemplate?: string;
    valueTemplateName?: string;
    judgmentMetric?: string;
    templateParams?: string;
    avgMinLimit?: number;
    avgMaxLimit?: number;
    stdMinLimit?: number;
    stdMaxLimit?: number;
    testTool?: string;
    sampleSize: number;
    minValueLimit?: number;
    maxValueLimit?: number;
    sampleValues?: any[];
    maxValue?: number;
    minValue?: number;
    averageValue?: number;
    itemResult: Judgment;
    qaResult?: Judgment;
    requiredSampleCount?: number;
    completedSampleCount?: number;
    abnormalSampleCount?: number;
    inputStatus?: 'ABNORMAL' | 'COMPLETE' | 'EMPTY' | 'FILLING';
    isSpc?: boolean;
    sort?: number;
    samples?: OqcSample[];
    recheckItemFlag?: boolean;
  }

  export interface OqcAbnormal {
    id?: number;
    oqcItemId?: number;
    sampleId?: number;
    abnormalRole?: 'OPERATOR' | 'QA' | 'SYSTEM';
    abnormalDesc: string;
    processStatus?: string;
    actionRequired?: string;
    ncRecordId?: number;
    relatedNcrNo?: string;
    dispositionStatus?: string;
  }

  export interface OqcRecord {
    id?: number;
    oqcNo: string;
    productType?: string;
    shippingNoticeId?: number;
    shippingNoticeItemId?: number;
    shippingNo: string;
    noticeNo?: string;
    customerId?: number;
    customerCode?: string;
    customerName: string;
    materialId?: number;
    materialCode: string;
    materialName: string;
    specification?: string;
    modelCode?: string;
    productSize?: string;
    batchNo: string;
    customerBatchNo?: string;
    shippingQty: number;
    shippingPieceQty?: number;
    aqlStandard?: string;
    sampleQty?: number;
    standardId?: number;
    standardNo?: string;
    standardVersion?: string;
    status:
      | 'CANCELED'
      | 'COMPLETED'
      | 'INSPECTING'
      | 'PENDING'
      | 'REJECTED'
      | 'SUSPENDED'
      | 'WAITING_QA';
    judgment: Judgment;
    relatedNcrNo?: string;
    ncrStatus?: string;
    inspectorId?: number;
    inspectorName?: string;
    inspectionTime?: string;
    qaInspectorId?: number;
    qaInspectorName?: string;
    qaTime?: string;
    releaseResult?: string;
    releaseTime?: string;
    entryMode?: 'MANUAL' | string;
    entryLayout?: 'PROGRAM_FORM' | string;
    entryProgress?: number;
    requiredItemCount?: number;
    completedItemCount?: number;
    abnormalItemCount?: number;
    lastSaveTime?: string;
    lastCalculateTime?: string;
    sheetLocked?: boolean;
    originalInspectionNo?: string;
    recheckFlag?: boolean;
    rejectPrevInspectionNo?: string;
    rejectRootInspectionNo?: string;
    remark?: string;
    items?: OqcItem[];
    abnormals?: OqcAbnormal[];
  }

  export interface OqcPendingTask {
    productType?: string;
    shippingNoticeId: number;
    shippingNoticeItemId: number;
    shippingNo: string;
    noticeNo?: string;
    customerCode?: string;
    customerId?: number;
    customerName: string;
    materialCode: string;
    materialName: string;
    specification?: string;
    modelCode?: string;
    productSize?: string;
    batchNo: string;
    customerBatchNo?: string;
    shippingQty: number;
    shippingPieceQty?: number;
    existingOqcId?: number;
    existingOqcNo?: string;
    existingStatus?: string;
  }

  export interface OqcStandard {
    standardId: number;
    standardNo: string;
    standardName: string;
    version: string;
    applyType: 'OQC' | string;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    productModelCode?: string;
    productModelName?: string;
    items: Array<{
      avgMaxLimit?: number;
      avgMinLimit?: number;
      category?: string;
      inspectionItem: string;
      inspectionMethod?: string;
      isSpc?: boolean;
      itemType: 'QUALITATIVE' | 'QUANTITATIVE';
      judgmentMetric?: string;
      maxValueLimit?: number;
      minValueLimit?: number;
      ruleDescription?: string;
      sampleSize: number;
      sort?: number;
      standardDesc: string;
      standardItemId?: number;
      stdMaxLimit?: number;
      stdMinLimit?: number;
      targetValue?: number;
      templateParams?: string;
      testFrequencyJudgement?: string;
      testTool?: string;
      unit?: string;
      valueTemplate?: string;
      valueTemplateName?: string;
    }>;
  }

  export interface OqcPageReq extends PageParam {
    oqcNo?: string;
    shippingNo?: string;
    noticeNo?: string;
    customerName?: string;
    materialCode?: string;
    batchNo?: string;
    status?: string;
    judgment?: string;
    inspectionTime?: string[];
    recheckFlag?: boolean;
  }

  export interface OqcAuditReq {
    id: number;
    auditResult: AuditResult;
    rejectReason?: string;
  }

  export interface OqcScanReq {
    scanCode: string;
    scanScene?: 'ENTRY_HEADER' | 'LEDGER_TOOLBAR';
    currentOqcId?: number;
    clientType?: string;
    terminalCode?: string;
  }

  export interface OqcScanCandidate {
    id?: number;
    oqcNo?: string;
    productType?: string;
    shippingNoticeId?: number;
    shippingNoticeItemId?: number;
    shippingNo?: string;
    noticeNo?: string;
    customerName?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    batchNo?: string;
    customerBatchNo?: string;
    shippingQty?: number;
    shippingPieceQty?: number;
    status?: OqcRecord['status'];
    judgment?: Judgment;
    lastSaveTime?: string;
    createTime?: string;
  }

  export interface OqcScanResp {
    scanCode: string;
    scanTargetType: string;
    scanScene: string;
    matchResult:
      | 'CREATED'
      | 'MATCHED_MULTIPLE'
      | 'MATCHED_SINGLE'
      | 'NOT_FOUND'
      | 'STATUS_BLOCKED';
    openTarget?: 'CANDIDATE_MODAL' | 'ENTRY' | 'REPORT';
    message?: string;
    matchedOqcId?: number;
    matchedOqcNo?: string;
    candidateCount?: number;
    record?: OqcRecord;
    candidates?: OqcScanCandidate[];
    scanTime?: string;
  }
}

function toUiJudgment(value?: string): MesOqcApi.Judgment {
  return !value || value === 'PENDING' ? '-' : (value as MesOqcApi.Judgment);
}

function toApiJudgment(value?: MesOqcApi.Judgment) {
  return !value || value === '-' ? 'PENDING' : value;
}

function buildEmptyRows(item: Pick<MesOqcApi.OqcItem, 'sampleSize'>) {
  return Array.from({ length: item.sampleSize || 1 }).map((_, index) => ({
    sampleSeq: index + 1,
    samplePosition: `${index + 1}`,
    sampleResult: 'PENDING',
  }));
}

function buildValues(item: MesOqcApi.OqcItem) {
  const samples = (item.samples || []).toSorted(
    (a, b) => a.sampleSeq - b.sampleSeq,
  );
  if (samples.length === 0) return buildEmptyRows(item);
  const rows = samples.map((sample) => ({
    remark: sample.remark,
    sampleGroupNo: sample.sampleGroupNo,
    samplePosition: sample.samplePosition,
    sampleResult: sample.sampleResult || 'PENDING',
    sampleSeq: sample.sampleSeq,
    value:
      item.itemType === 'QUANTITATIVE'
        ? sample.measuredValue ?? sample.resultValue
        : sample.qualitativeValue,
  }));
  if (rows.length >= (item.sampleSize || 1)) return rows;
  return rows.concat(buildEmptyRows(item).slice(rows.length));
}

function normalizeItem(item: any): MesOqcApi.OqcItem {
  return {
    ...item,
    itemResult: toUiJudgment(item.itemResult),
    qaResult: toUiJudgment(item.qaResult),
    sampleValues: item.sampleValues || buildValues(item),
  };
}

function normalizeDateTime(value?: string) {
  if (!value) return value;
  const time = new Date(value.replace(' ', 'T')).getTime();
  if (!Number.isFinite(time)) return value;
  return time < new Date('2000-01-01T00:00:00').getTime() ? undefined : value;
}

function normalizeRecord(record: any): MesOqcApi.OqcRecord {
  return {
    ...record,
    judgment: toUiJudgment(record.judgment),
    inspectionTime: normalizeDateTime(record.inspectionTime),
    lastCalculateTime: normalizeDateTime(record.lastCalculateTime),
    lastSaveTime: normalizeDateTime(record.lastSaveTime),
    qaTime: normalizeDateTime(record.qaTime),
    releaseTime: normalizeDateTime(record.releaseTime),
    items: (record.items || []).map((item: any) => normalizeItem(item)),
  };
}

function buildSamples(item: MesOqcApi.OqcItem): MesOqcApi.OqcSample[] {
  return (item.sampleValues || [])
    .map((value, index) => {
    const row = value && typeof value === 'object' ? value : { value };
    const primaryValue =
      row.resultValue ?? row.measuredValue ?? row.value ?? value;
    const measuredValue =
      item.itemType === 'QUANTITATIVE' ? toOptionalNumber(primaryValue) : undefined;
    const resultValue =
      item.itemType === 'QUANTITATIVE'
        ? toOptionalNumber(row.resultValue)
        : undefined;
    const rawValues =
      item.itemType === 'QUANTITATIVE'
        ? Object.fromEntries(
            Object.entries(row).filter(
              ([key, val]) =>
                ![
                  'inputComponent',
                  'remark',
                  'resultValue',
                  'sampleGroupNo',
                  'samplePosition',
                  'sampleResult',
                  'sampleSeq',
                  'value',
                  'valueSource',
                ].includes(key) &&
                val !== undefined &&
                val !== null &&
                val !== '',
            ),
          )
        : undefined;
    const qualitativeValue = row.value ?? value;
    return {
      sampleRole: 'OPERATOR',
      sampleSeq: row.sampleSeq ?? index + 1,
      samplePosition: row.samplePosition,
      rawValuesJson:
        rawValues && Object.keys(rawValues).length > 0
          ? JSON.stringify(rawValues)
          : row.rawValuesJson,
      measuredValue,
      resultValue,
      qualitativeValue:
        item.itemType === 'QUALITATIVE' ? qualitativeValue : undefined,
      sampleResult:
        row.sampleResult ||
        (item.itemType === 'QUALITATIVE' ? qualitativeValue : undefined),
      sampleGroupNo: row.sampleGroupNo ?? index + 1,
      valueSource: row.valueSource || 'MANUAL',
      remark: row.remark,
    };
  })
    .filter((sample) => isFilledSample(sample));
}

function isBlankValue(value: unknown) {
  return value === undefined || value === null || value === '';
}

function toOptionalNumber(value: unknown) {
  if (isBlankValue(value)) return undefined;
  const num = Number(value);
  return Number.isFinite(num) ? num : undefined;
}

function isFilledSample(sample: MesOqcApi.OqcSample) {
  return (
    sample.measuredValue !== undefined ||
    sample.resultValue !== undefined ||
    sample.qualitativeValue === 'NA' ||
    sample.qualitativeValue === 'OK' ||
    sample.qualitativeValue === 'NG' ||
    sample.sampleResult === 'NA' ||
    sample.sampleResult === 'OK' ||
    sample.sampleResult === 'NG'
  );
}

function toSubmitPayload(data: MesOqcApi.OqcRecord) {
  return {
    ...data,
    judgment: toApiJudgment(data.judgment),
    entryLayout: data.entryLayout || 'PROGRAM_FORM',
    entryMode: data.entryMode || 'MANUAL',
    items: (data.items || []).map((item) => ({
      ...item,
      itemResult: toApiJudgment(item.itemResult),
      qaResult: toApiJudgment(item.qaResult),
      samples: buildSamples(item),
    })),
  };
}

export async function getOqcPage(params: MesOqcApi.OqcPageReq) {
  const page = await requestClient.get<PageResult<MesOqcApi.OqcRecord>>(
    '/mes/quality/oqc/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getPendingOqcTasks(keyword?: string) {
  return requestClient.get<MesOqcApi.OqcPendingTask[]>(
    '/mes/quality/oqc/pending-list',
    { params: { keyword } },
  );
}

export async function resolveOqcScan(data: MesOqcApi.OqcScanReq) {
  const resp = await requestClient.post<MesOqcApi.OqcScanResp>(
    '/mes/quality/oqc/scan/resolve',
    data,
  );
  return {
    ...resp,
    record: resp.record ? normalizeRecord(resp.record) : undefined,
    candidates: (resp.candidates || []).map((item) => ({
      ...item,
      judgment: toUiJudgment(item.judgment),
      createTime: normalizeDateTime(item.createTime),
      lastSaveTime: normalizeDateTime(item.lastSaveTime),
    })),
  };
}

export async function getOqcDetail(id: number) {
  const record = await requestClient.get<MesOqcApi.OqcRecord>(
    `/mes/quality/oqc/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function getOqcStandardByProduct(
  materialCode?: string,
  modelCode?: string,
  standardId?: number,
) {
  return requestClient.get<MesOqcApi.OqcStandard>(
    '/mes/quality/oqc/standard-by-product',
    {
      params: { materialCode, modelCode, standardId },
    },
  );
}

export async function createOqcFromShippingNotice(
  shippingNoticeItemId: number,
  standardId?: number,
) {
  const id = await requestClient.post<number>(
    '/mes/quality/oqc/create-from-shipping-notice',
    undefined,
    { params: { shippingNoticeItemId, standardId } },
  );
  return getOqcDetail(id);
}

export async function createOqcRecord(data: MesOqcApi.OqcRecord) {
  const payload = { ...data };
  delete payload.items;
  delete payload.abnormals;
  const id = await requestClient.post<number>(
    '/mes/quality/oqc/create',
    payload,
  );
  return getOqcDetail(id);
}

export async function saveOqcProgramEntry(data: MesOqcApi.OqcRecord) {
  const record = await requestClient.put<MesOqcApi.OqcRecord>(
    '/mes/quality/oqc/program-entry/save',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function recalculateOqcProgramEntry(data: MesOqcApi.OqcRecord) {
  const record = await requestClient.put<MesOqcApi.OqcRecord>(
    '/mes/quality/oqc/program-entry/recalculate',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function submitOqcProgramEntry(data: MesOqcApi.OqcRecord) {
  const record = await requestClient.put<MesOqcApi.OqcRecord>(
    '/mes/quality/oqc/program-entry/submit',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function auditOqcProgramEntry(data: MesOqcApi.OqcAuditReq) {
  const record = await requestClient.put<MesOqcApi.OqcRecord>(
    '/mes/quality/oqc/program-entry/audit',
    data,
  );
  return normalizeRecord(record);
}

export async function suspendOqcRecord(id: number) {
  return requestClient.put<boolean>(`/mes/quality/oqc/suspend?id=${id}`);
}
