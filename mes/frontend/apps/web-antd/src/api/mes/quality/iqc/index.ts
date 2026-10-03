import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesIqcApi {
  export type Judgment = '-' | 'NG' | 'OK' | 'PENDING' | 'SKIP';
  export type RetentionDestroyStatus = 'DESTROYED' | 'WAIT_DESTROY';
  export type RetentionPeriodUnit = 'DAY' | 'MONTH';
  export type RetentionSampleUnit = 'L' | 'm';
  export type RetentionStatus = 'NOT_RETAINED' | 'RETAINED' | 'UNCONFIRMED';
  export type ItemType = 'DATE' | 'QUALITATIVE' | 'QUANTITATIVE';
  export type JudgmentMetric =
    | 'COMPRESSION_ELASTICITY_RATE'
    | 'COMPRESSION_RATE'
    | 'DENSITY_VALUE'
    | 'RESULT_VALUE';
  export type IqcStatus =
    | 'CANCELED'
    | 'COMPLETED'
    | 'FINISHED'
    | 'INSPECTING'
    | 'PENDING'
    | 'REJECTED'
    | 'SUSPENDED'
    | 'WAITING_CONFIRM';

  export interface IqcSample {
    id?: number;
    iqcId?: number;
    iqcItemId?: number;
    iqcNo?: string;
    sampleSeq: number;
    sampleBarcode?: string;
    rawValuesJson?: string;
    resultValue?: number;
    measuredValue?: number;
    qualitativeValue?: 'NG' | 'OK';
    dateValue?: string;
    evaluationDate?: string;
    sampleResult?: Judgment;
    defectCode?: string;
    defectName?: string;
    remark?: string;
    recheckItemFlag?: boolean;
  }

  export interface IqcItem {
    id?: number;
    iqcId?: number;
    iqcNo?: string;
    standardItemId?: number;
    inspectionItem: string;
    itemType: ItemType;
    expiryDays?: number;
    attachmentEnabled?: boolean;
    attachmentUrls?: string[];
    targetValue?: number;
    standardDesc?: string;
    unit?: string;
    inspectionMethod?: string;
    testFrequencyJudgement?: string;
    entryRuleType?: 'CUSTOM' | 'PRESET';
    ruleDescription?: string;
    valueTemplate?: string;
    valueTemplateName?: string;
    judgmentMetric?: JudgmentMetric;
    templateParams?: string;
    testTool?: string;
    sampleSize: number;
    minValueLimit?: number;
    maxValueLimit?: number;
    avgMinLimit?: number;
    avgMaxLimit?: number;
    judgmentReason?: string;
    sampleValues?: any[];
    maxValue?: number;
    minValue?: number;
    averageValue?: number;
    itemResult: Judgment;
    isSpc?: boolean;
    recheckItemFlag?: boolean;
    sort?: number;
    samples?: IqcSample[];
  }

  export interface IqcAbnormal {
    id?: number;
    iqcId?: number;
    iqcNo?: string;
    iqcItemId?: number;
    sampleId?: number;
    sampleBarcode?: string;
    defectCode?: string;
    defectName?: string;
    abnormalDesc: string;
    suggestedFlow: string;
    processStatus?: string;
    ncRecordId?: number;
  }

  export interface IqcRecord {
    id?: number;
    iqcNo?: string;
    receiptId?: number;
    receiptNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    receiverName?: string;
    materialId?: number;
    materialCode: string;
    materialName: string;
    specification?: string;
    modelNo?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    batchNo?: string;
    receiveQty: number;
    arrivalDate?: string;
    inspectionApplyTime?: string;
    inspectionApplyAttachmentUrls?: string[];
    productionDate?: string;
    expiryDate?: string;
    unit?: string;
    standardId?: number;
    standardNo?: string;
    standardName?: string;
    standardVersion?: string;
    standardSnapshotTime?: string;
    standardSnapshotHash?: string;
    standardContentChanged?: boolean;
    standardSwitchAllowed?: boolean;
    standardSelectionMessage?: string;
    standardMatchMode?: string;
    standardSnapshotLocked?: boolean;
    aqlStandard?: string;
    status?: IqcStatus;
    judgment: Judgment;
    retentionStatus?: RetentionStatus;
    retentionConfirmTime?: string;
    retentionConfirmUserId?: number;
    retentionConfirmUserName?: string;
    retentionMaterialCategoryId?: number;
    retentionMaterialCategoryName?: string;
    retentionQty?: number;
    retentionUnit?: RetentionSampleUnit;
    retentionRuleDesc?: string;
    retentionPeriodValue?: number;
    retentionPeriodUnit?: RetentionPeriodUnit;
    retentionExpireTime?: string;
    retentionDestroyStatus?: RetentionDestroyStatus;
    retentionDestroyTime?: string;
    retentionDestroyUserId?: number;
    retentionDestroyUserName?: string;
    retentionDestroyRemark?: string;
    inspectorId?: number;
    inspectorName?: string;
    inspectionTime?: string;
    qaInspectorId?: number;
    qaInspectorName?: string;
    qaTime?: string;
    disposalType?: string;
    purchaseContractNo?: string;
    returnCount?: number;
    lastReturnReason?: string;
    rejectFlag?: boolean;
    recheckFlag?: boolean;
    recheckGroupId?: number;
    recheckRoundNo?: number;
    rejectPrevInspectionId?: number;
    rejectPrevInspectionNo?: string;
    rejectNextInspectionId?: number;
    rejectNextInspectionNo?: string;
    rejectRootInspectionId?: number;
    rejectRootInspectionNo?: string;
    rejectRecheckResult?: string;
    rejectRecheckTime?: string;
    rejectReason?: string;
    rejectTime?: string;
    rejectUserId?: number;
    rejectUserName?: string;
    remark?: string;
    createTime?: string;
    items?: IqcItem[];
    abnormals?: IqcAbnormal[];
    returnRecords?: IqcReturnRecord[];
  }

  export interface IqcReturnRecord {
    afterStatus?: string;
    beforeStatus?: string;
    id?: number;
    iqcId?: number;
    iqcNo?: string;
    returnReason?: string;
    returnTime?: string;
    returnUserId?: number;
    returnUserName?: string;
  }

  export interface IqcImportResp {
    fileName?: string;
    status?: string;
    previewOnly?: boolean;
    totalCount?: number;
    successCount?: number;
    failureCount?: number;
    warningCount?: number;
    validateSummary?: string;
    record?: IqcRecord;
    messages?: string[];
  }

  export interface IqcPageReq extends PageParam {
    batchNo?: string;
    inspectionTime?: string[];
    iqcNo?: string;
    judgment?: string;
    materialCode?: string;
    receiptNo?: string;
    recheckFlag?: boolean;
    retentionDestroyStatus?: string;
    retentionExpireTime?: string[];
    retentionStatus?: string;
    standardMatchMode?: string;
    standardNo?: string;
    status?: string;
    supplierName?: string;
  }

  export interface RetentionConfirmReq {
    id: number;
  }

  export interface IqcScanReq {
    scanCode: string;
    scanScene?: 'LEDGER_TOOLBAR' | 'WORKBENCH_HEADER';
    clientType?: string;
    terminalCode?: string;
  }

  export interface IqcScanCandidate {
    id: number;
    iqcNo: string;
    receiptNo?: string;
    supplierName?: string;
    materialCode?: string;
    materialName?: string;
    batchNo?: string;
    status?: IqcStatus;
    judgment?: Judgment;
    createTime?: string;
  }

  export interface IqcScanResp {
    scanCode: string;
    scanTargetType: string;
    scanScene: string;
    matchResult:
      | 'MATCHED_MULTIPLE'
      | 'MATCHED_SINGLE'
      | 'NOT_FOUND'
      | 'STATUS_BLOCKED';
    openTarget?: 'CANDIDATE_MODAL' | 'REPORT' | 'WORKBENCH';
    message?: string;
    matchedIqcId?: number;
    matchedIqcNo?: string;
    candidateCount?: number;
    record?: IqcRecord;
    candidates?: IqcScanCandidate[];
    scanTime?: string;
  }

  export interface IqcAuditReq {
    auditRemark?: string;
    auditResult: 'APPROVE' | 'RETURN';
    id: number;
  }

  export interface IqcStandardResp {
    applyType?: string;
    items?: any[];
    materialCode?: string;
    materialId?: number;
    materialName?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    specification?: string;
    standardMatchMode?: string;
    standardId?: number;
    standardName?: string;
    standardNo?: string;
    version?: string;
  }

  export interface StandardCandidate {
    id: number;
    standardNo?: string;
    standardName?: string;
    version?: string;
    applyType?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    itemCount?: number;
    matchReason?: string;
    matchScore?: number;
    matchType?: string;
    recommended?: boolean;
  }

  export interface SelectStandardReq {
    id: number;
    standardId: number;
    reason?: string;
  }

  export interface RetentionDestroyReq {
    ids: number[];
    remark?: string;
  }

  export interface RetentionExpireTimeReq {
    id: number;
    retentionExpireTime: string;
  }
}

function toUiJudgment(value?: string): MesIqcApi.Judgment {
  return !value || value === 'PENDING' ? '-' : (value as MesIqcApi.Judgment);
}

function toApiJudgment(value?: MesIqcApi.Judgment) {
  return !value || value === '-' ? 'PENDING' : value;
}

function toLocalDateTimeMillis(value?: number | string) {
  if (value === undefined || value === null || value === '') return undefined;
  if (typeof value === 'number') return value;
  const numericValue = Number(value);
  if (Number.isFinite(numericValue) && /^\d+$/.test(value.trim())) {
    return numericValue;
  }
  const match = value
    .trim()
    .match(/^(\d{4})-(\d{2})-(\d{2})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?/);
  if (!match) return undefined;
  const [, year, month, day, hour = '0', minute = '0', second = '0'] = match;
  // Keep the selected GMT+8 wall-clock time stable for the backend epoch parser.
  const time = Date.UTC(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hour) - 8,
    Number(minute),
    Number(second),
  );
  return Number.isFinite(time) ? time : undefined;
}

function normalizeItem(item: MesIqcApi.IqcItem): MesIqcApi.IqcItem {
  const samples = (item.samples || []).toSorted(
    (a, b) => (a.sampleSeq || 0) - (b.sampleSeq || 0),
  );
  const sampleValues =
    samples.length > 0
      ? samples.map((sample) => normalizeSampleValue(item, sample))
      : Array.from({ length: item.sampleSize || 1 });
  return {
    ...item,
    itemResult: toUiJudgment(item.itemResult),
    sampleValues,
    samples,
  };
}

function normalizeSampleValue(
  item: MesIqcApi.IqcItem,
  sample: MesIqcApi.IqcSample,
) {
  if (item.itemType === 'QUALITATIVE') return sample.qualitativeValue;
  const base = {
    remark: sample.remark,
    resultValue: sample.resultValue,
    sampleBarcode: sample.sampleBarcode,
    sampleResult: sample.sampleResult || 'PENDING',
  };
  if (item.itemType === 'DATE') {
    return {
      ...base,
      evaluationDate: sample.evaluationDate,
      value: sample.dateValue,
    };
  }
  if (sample.rawValuesJson) {
    try {
      return {
        ...base,
        ...JSON.parse(sample.rawValuesJson),
        value: sample.measuredValue ?? sample.resultValue,
      };
    } catch {
      return {
        ...base,
        value: sample.measuredValue ?? sample.resultValue,
      };
    }
  }
  return {
    ...base,
    value: sample.measuredValue ?? sample.resultValue,
  };
}

function normalizeRecord(record: MesIqcApi.IqcRecord): MesIqcApi.IqcRecord {
  return {
    ...record,
    inspectionApplyAttachmentUrls: record.inspectionApplyAttachmentUrls || [],
    judgment: toUiJudgment(record.judgment),
    items: (record.items || []).map((item) => normalizeItem(item)),
  };
}

function resolveSampleValue(value: any) {
  if (value && typeof value === 'object') {
    return value.resultValue ?? value.value;
  }
  return value;
}

function resolveSampleRemark(value: any) {
  return value && typeof value === 'object' ? value.remark : undefined;
}

function resolveSampleBarcode(value: any) {
  return value && typeof value === 'object' ? value.sampleBarcode : undefined;
}

function resolveSampleResultValue(value: any) {
  return value && typeof value === 'object' ? value.resultValue : undefined;
}

function resolveRawValuesJson(item: MesIqcApi.IqcItem, value: any) {
  if (item.itemType !== 'QUANTITATIVE') return undefined;
  const row = value && typeof value === 'object' ? value : { value };
  const structuredTemplate =
    item.valueTemplate === 'DENSITY_CALC' ||
    item.valueTemplate === 'COMPRESSION_CALC' ||
    hasDataRule(item);
  const rawValues = Object.fromEntries(
    Object.entries(row).filter(([key, val]) => {
      if (
        ['remark', 'resultValue', 'sampleBarcode', 'sampleResult'].includes(key)
      ) {
        return false;
      }
      if (structuredTemplate && key === 'value') return false;
      return val !== undefined && val !== null && val !== '';
    }),
  );
  return Object.keys(rawValues).length > 0
    ? JSON.stringify(rawValues)
    : undefined;
}

function isStructuredTemplate(item: MesIqcApi.IqcItem) {
  return (
    item.itemType === 'QUANTITATIVE' &&
    (item.valueTemplate === 'DENSITY_CALC' ||
      item.valueTemplate === 'COMPRESSION_CALC' ||
      hasDataRule(item))
  );
}

function hasStructuredInputValue(item: MesIqcApi.IqcItem, value: any) {
  if (!isStructuredTemplate(item)) return false;
  const row = value && typeof value === 'object' ? value : {};
  let inputCodes: string[] = [];
  if (item.valueTemplate === 'DENSITY_CALC') {
    inputCodes = ['thicknessMm', 'weightG'];
  } else if (item.valueTemplate === 'COMPRESSION_CALC') {
    inputCodes = ['t1Mm', 't2Mm', 't3Mm'];
  } else {
    try {
      const params = item.templateParams ? JSON.parse(item.templateParams) : {};
      const dataRule = params.dataRule || params;
      inputCodes = (dataRule.inputFields || [])
        .map((field: any) =>
          String(field.code || field.fieldCode || field.key || ''),
        )
        .filter(Boolean);
    } catch {
      return false;
    }
  }
  return inputCodes.some((code) => {
    const input = row[code];
    return input !== undefined && input !== null && input !== '';
  });
}

function hasDataRule(item: MesIqcApi.IqcItem) {
  try {
    const params = item.templateParams ? JSON.parse(item.templateParams) : {};
    const dataRule = params.dataRule || params;
    return (
      Array.isArray(dataRule.inputFields) &&
      dataRule.inputFields.length > 0 &&
      Array.isArray(dataRule.resultFields) &&
      dataRule.resultFields.length > 0
    );
  } catch {
    return false;
  }
}

function resolveSampleResult(
  item: MesIqcApi.IqcItem,
  value: any,
): MesIqcApi.Judgment | undefined {
  if (value && typeof value === 'object' && value.sampleResult) {
    return value.sampleResult;
  }
  if (item.itemType === 'QUALITATIVE') {
    return resolveSampleValue(value) as MesIqcApi.Judgment;
  }
  return item.itemResult;
}

function toSubmitItems(record: MesIqcApi.IqcRecord) {
  return (record.items || []).map((item) => ({
    id: item.id,
    standardItemId: item.standardItemId,
    inspectionItem: item.inspectionItem,
    itemType: item.itemType,
    attachmentUrls: item.attachmentUrls || [],
    samples: (item.sampleValues || [])
      .map((value, index) => ({ index, value }))
      .filter(({ value }) => {
        const sampleValue = resolveSampleValue(value);
        if (isStructuredTemplate(item)) {
          return hasStructuredInputValue(item, value);
        }
        return (
          sampleValue !== undefined &&
          sampleValue !== null &&
          sampleValue !== ''
        );
      })
      .map(({ index, value }) => {
        const sampleValue = resolveSampleValue(value);
        const sampleResult = resolveSampleResult(item, value);
        const resultValue = resolveSampleResultValue(value);
        const structuredTemplate = isStructuredTemplate(item);
        return {
          sampleBarcode: resolveSampleBarcode(value),
          sampleSeq: index + 1,
          rawValuesJson: resolveRawValuesJson(item, value),
          resultValue:
            item.itemType === 'QUANTITATIVE'
              ? (resultValue ?? sampleValue)
              : undefined,
          measuredValue:
            item.itemType === 'QUANTITATIVE' && !structuredTemplate
              ? sampleValue
              : undefined,
          qualitativeValue:
            item.itemType === 'QUALITATIVE' ? sampleValue : undefined,
          dateValue: item.itemType === 'DATE' ? sampleValue : undefined,
          remark: resolveSampleRemark(value),
          sampleResult: toApiJudgment(sampleResult),
        };
      }),
  }));
}

function toCreatePayload(record: MesIqcApi.IqcRecord) {
  return {
    aqlStandard: record.aqlStandard,
    batchNo: record.batchNo,
    materialCode: record.materialCode,
    materialId: record.materialId,
    materialName: record.materialName,
    modelNo: record.modelNo,
    productModelId: record.productModelId,
    productModelCode: record.productModelCode,
    productModelName: record.productModelName,
    receiverName: record.receiverName,
    receiptId: record.receiptId,
    receiptNo: record.receiptNo,
    receiveQty: record.receiveQty,
    arrivalDate: record.arrivalDate,
    inspectionApplyTime: toLocalDateTimeMillis(record.inspectionApplyTime),
    inspectionApplyAttachmentUrls: record.inspectionApplyAttachmentUrls || [],
    productionDate: record.productionDate,
    expiryDate: record.expiryDate,
    purchaseContractNo: record.purchaseContractNo,
    remark: record.remark,
    retentionStatus: record.retentionStatus,
    specification: record.specification,
    standardId: record.standardId,
    supplierId: record.supplierId,
    supplierCode: record.supplierCode,
    supplierName: record.supplierName,
    unit: record.unit,
  };
}

function requireRecordId(record: MesIqcApi.IqcRecord) {
  if (!record.id) {
    throw new Error('IQC记录ID不能为空');
  }
  return record.id;
}

export async function getIqcPage(params: MesIqcApi.IqcPageReq) {
  const page = await requestClient.get<PageResult<MesIqcApi.IqcRecord>>(
    '/mes/quality/iqc/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getIqcRetentionPage(params: MesIqcApi.IqcPageReq) {
  const page = await requestClient.get<PageResult<MesIqcApi.IqcRecord>>(
    '/mes/quality/iqc/retention/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getIqcRetentionExpiredPage(params: MesIqcApi.IqcPageReq) {
  const page = await requestClient.get<PageResult<MesIqcApi.IqcRecord>>(
    '/mes/quality/iqc/retention/expired/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getPendingReceipts() {
  const list = await requestClient.get<MesIqcApi.IqcRecord[]>(
    '/mes/quality/iqc/pending-list',
  );
  return (list || []).map((item) => normalizeRecord(item));
}

export async function getIqcDetail(id: number) {
  const record = await requestClient.get<MesIqcApi.IqcRecord>(
    `/mes/quality/iqc/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function getIqcRetentionDetail(id: number) {
  const record = await requestClient.get<MesIqcApi.IqcRecord>(
    `/mes/quality/iqc/retention/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export function getIqcStandardCandidates(id: number) {
  return requestClient.get<MesIqcApi.StandardCandidate[]>(
    '/mes/quality/iqc/standard-candidates',
    { params: { id } },
  );
}

export function getIqcStandardCandidatesByMaterial(params: {
  materialCode: string;
  materialId?: number;
}) {
  return requestClient.get<MesIqcApi.StandardCandidate[]>(
    '/mes/quality/iqc/standard-candidates-by-material',
    { params },
  );
}

export async function selectIqcStandard(data: MesIqcApi.SelectStandardReq) {
  const record = await requestClient.put<MesIqcApi.IqcRecord>(
    '/mes/quality/iqc/select-standard',
    data,
  );
  return normalizeRecord(record);
}

export async function resolveIqcScan(data: MesIqcApi.IqcScanReq) {
  const resp = await requestClient.post<MesIqcApi.IqcScanResp>(
    '/mes/quality/iqc/scan/resolve',
    data,
  );
  return {
    ...resp,
    record: resp.record ? normalizeRecord(resp.record) : undefined,
    candidates: (resp.candidates || []).map((item) => ({
      ...item,
      judgment: toUiJudgment(item.judgment),
    })),
  };
}

export async function getStandardByMaterial(materialCode: string) {
  return requestClient.get<MesIqcApi.IqcStandardResp>(
    '/mes/quality/iqc/standard-by-material',
    {
      params: { materialCode },
    },
  );
}

export async function getIqcPrintDetail(id: number) {
  const record = await requestClient.get<MesIqcApi.IqcRecord>(
    `/mes/quality/iqc/print-detail?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function updateIqcRetentionExpireTime(
  data: MesIqcApi.RetentionExpireTimeReq,
) {
  const record = await requestClient.put<MesIqcApi.IqcRecord>(
    '/mes/quality/iqc/retention/expire-time',
    data,
  );
  return normalizeRecord(record);
}

export async function confirmIqcRetention(data: MesIqcApi.RetentionConfirmReq) {
  const resp = await requestClient.put<MesIqcApi.IqcRecord>(
    '/mes/quality/iqc/retention/confirm',
    data,
  );
  return normalizeRecord(resp);
}

export async function destroyIqcRetention(data: MesIqcApi.RetentionDestroyReq) {
  return requestClient.put<boolean>('/mes/quality/iqc/retention/destroy', data);
}

export function exportIqcRetentionExcel(params: MesIqcApi.IqcPageReq) {
  return requestClient.download('/mes/quality/iqc/retention/export-excel', {
    params,
  });
}

export function buildIqcCoaWordFileName(
  record?: null | Pick<MesIqcApi.IqcRecord, 'id' | 'iqcNo'>,
) {
  const fileKey = sanitizeDownloadFileName(
    String(record?.iqcNo || record?.id || 'UNKNOWN'),
  );
  return `IQC_COA_${fileKey}.docx`;
}

function sanitizeDownloadFileName(value: string) {
  return value.replaceAll(/[\\/:*?"<>|]/g, '_').trim() || 'UNKNOWN';
}

export async function createIqcRecord(record: MesIqcApi.IqcRecord) {
  const id = await requestClient.post<number>(
    '/mes/quality/iqc/create',
    toCreatePayload(record),
  );
  return getIqcDetail(id);
}

export async function deleteIqcRecord(id: number) {
  return requestClient.delete<boolean>(`/mes/quality/iqc/delete?id=${id}`);
}

export async function saveIqcRecord(record: MesIqcApi.IqcRecord) {
  const id = requireRecordId(record);
  await requestClient.put<boolean>('/mes/quality/iqc/update', {
    ...toCreatePayload(record),
    id,
    items: toSubmitItems(record),
    judgment: toApiJudgment(record.judgment),
  });
  return getIqcDetail(id);
}

export async function saveIqcProgramEntry(record: MesIqcApi.IqcRecord) {
  const resp = await requestClient.put<MesIqcApi.IqcRecord>(
    '/mes/quality/iqc/program-entry/save',
    {
      id: record.id,
      items: toSubmitItems(record),
      judgment: toApiJudgment(record.judgment),
      remark: record.remark,
    },
  );
  return normalizeRecord(resp);
}

export async function recalculateIqcProgramEntry(record: MesIqcApi.IqcRecord) {
  const resp = await requestClient.put<MesIqcApi.IqcRecord>(
    '/mes/quality/iqc/program-entry/recalculate',
    {
      id: record.id,
      items: toSubmitItems(record),
      judgment: toApiJudgment(record.judgment),
      remark: record.remark,
    },
  );
  return normalizeRecord(resp);
}

export async function submitIqcProgramEntry(record: MesIqcApi.IqcRecord) {
  const resp = await requestClient.put<MesIqcApi.IqcRecord>(
    '/mes/quality/iqc/program-entry/submit',
    {
      disposalType: record.disposalType,
      id: record.id,
      items: toSubmitItems(record),
      judgment: toApiJudgment(record.judgment),
      remark: record.remark,
      retentionStatus: record.retentionStatus,
    },
  );
  return normalizeRecord(resp);
}

export async function submitIqcRecord(record: MesIqcApi.IqcRecord) {
  const id = requireRecordId(record);
  await requestClient.put<boolean>('/mes/quality/iqc/submit', {
    disposalType: record.disposalType,
    id,
    items: toSubmitItems(record),
    judgment: toApiJudgment(record.judgment),
    remark: record.remark,
    retentionStatus: record.retentionStatus,
  });
  return getIqcDetail(id);
}

export function resolveIqcRetentionRule(record?: Partial<MesIqcApi.IqcRecord>) {
  const text = [
    record?.retentionMaterialCategoryName,
    record?.materialName,
    record?.materialCode,
  ]
    .filter(Boolean)
    .join(' ')
    .toUpperCase();
  const build = (
    materialCategoryName: string,
    qty: number,
    unit: MesIqcApi.RetentionSampleUnit,
  ) => ({
    materialCategoryName,
    qty,
    ruleDesc: `${qty}${unit}/每来料批次`,
    unit,
  });
  if (text.includes('砂纸')) return build('砂纸', 5, 'm');
  if (text.includes('PET')) return build('PET', 1, 'm');
  if (text.includes('胶板')) return build('胶板', 1, 'm');
  if (text.includes('DMF')) return build('DMF', 1, 'L');
  if (text.includes('助剂')) return build('助剂', 1, 'L');
  return build(
    record?.retentionMaterialCategoryName || record?.materialName || '浆料',
    1,
    'L',
  );
}

export async function auditIqcRecord(data: MesIqcApi.IqcAuditReq) {
  await requestClient.put<boolean>('/mes/quality/iqc/audit', data);
  return getIqcDetail(data.id);
}

export async function suspendIqcRecord(id: number) {
  return requestClient.put<boolean>(`/mes/quality/iqc/suspend?id=${id}`);
}

export function downloadIqcItemTemplate(id: number) {
  return requestClient.download('/mes/quality/iqc/item-template/download', {
    params: { id },
  });
}

export function exportIqcItemValues(id: number) {
  return requestClient.download('/mes/quality/iqc/item-export', {
    params: { id },
  });
}

export function exportIqcCoaWord(id: number) {
  return requestClient.download('/mes/quality/iqc/coa-word', {
    params: { id },
  });
}

export async function previewIqcItemImport(id: number, file: File) {
  return requestClient.upload<MesIqcApi.IqcImportResp>(
    '/mes/quality/iqc/item-import/preview',
    { file, id },
  );
}

export async function confirmIqcItemImport(
  id: number,
  file: File,
  allowOverwrite = false,
) {
  const resp = await requestClient.upload<MesIqcApi.IqcImportResp>(
    '/mes/quality/iqc/item-import/confirm',
    { allowOverwrite, file, id },
  );
  return {
    ...resp,
    record: resp.record ? normalizeRecord(resp.record) : undefined,
  };
}
