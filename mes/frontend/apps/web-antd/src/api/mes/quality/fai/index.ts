import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

import {
  hasEntryRulePositions,
  parseEntryRuleParams,
  resolveEntryRuleExpectedSampleCount,
  resolveEntryRulePositions,
  resolveEntryRuleRepeatCount,
  resolveEntryRuleSampleSize,
} from '#/api/mes/quality/entry-rule';

export namespace MesFaiApi {
  export type Judgment = '-' | 'NG' | 'OK' | 'PENDING';
  export type AuditResult = 'FAIL' | 'PASS' | 'REJECT';
  export type ItemAuditResult = 'CONFIRM' | 'PENDING' | 'REJECT_RECHECK';
  export type ItemRecheckStatus =
    | 'CONFIRMED'
    | 'NONE'
    | 'WAIT_AUDIT'
    | 'WAIT_RECHECK';
  export type RetentionStatus = 'NOT_RETAINED' | 'RETAINED' | 'UNCONFIRMED';
  export type RetentionDestroyStatus = 'DESTROYED' | 'WAIT_DESTROY';
  export type RetentionPeriodUnit = 'DAY' | 'MONTH';
  export type SampleRole = 'OPERATOR' | 'QA';
  export type ValueTemplate =
    | 'COMPRESSION_CALC'
    | 'DENSITY_CALC'
    | 'SINGLE_VALUE';
  export type JudgmentMetric =
    | 'COMPRESSION_ELASTICITY_RATE'
    | 'COMPRESSION_RATE'
    | 'DENSITY_VALUE'
    | 'RESULT_VALUE'
    | string;

  export interface FaiSample {
    id?: number;
    inputComponent?: string;
    metricCode?: string;
    metricGroupCode?: string;
    sampleRole: SampleRole;
    sampleSeq: number;
    stepCode?: string;
    samplePosition?: string;
    rawValuesJson?: string;
    resultValue?: number;
    densityValue?: number;
    compressionRate?: number;
    compressionElasticityRate?: number;
    measuredValue?: number;
    qualitativeValue?: 'NG' | 'OK';
    sampleResult?: Judgment;
    sheetCellId?: number;
    sheetSectionCode?: string;
    sheetMetricCode?: string;
    sampleGroupNo?: number;
    sampleAxis?: string;
    sampleColumnNo?: number;
    importBatchNo?: string;
    valueSource?:
      | 'CALCULATED'
      | 'HISTORICAL_IMPORT'
      | 'ITEM_IMPORT'
      | 'MANUAL'
      | 'TEMPLATE_DEFAULT';
    defectCode?: string;
    defectName?: string;
    remark?: string;
    recheckItemFlag?: boolean;
  }

  export interface FaiItem {
    id?: number;
    abnormalSampleCount?: number;
    attachmentEnabled?: boolean;
    attachmentUrls?: string[];
    completedSampleCount?: number;
    inputComponent?: string;
    inputStatus?: 'ABNORMAL' | 'COMPLETE' | 'EMPTY' | 'FILLING';
    metricCode?: string;
    metricGroupCode?: string;
    requiredSampleCount?: number;
    standardItemId?: number;
    stepCode?: string;
    stepName?: string;
    inspectionItem: string;
    itemType: 'QUALITATIVE' | 'QUANTITATIVE';
    targetValue?: number;
    unit?: string;
    ruleDescription?: string;
    standardDesc: string;
    inspectionMethod?: string;
    testFrequencyJudgement?: string;
    valueTemplate?: ValueTemplate;
    valueTemplateName?: string;
    judgmentMetric?: JudgmentMetric;
    templateParams?: string;
    avgMinLimit?: number;
    avgMaxLimit?: number;
    stdMinLimit?: number;
    stdMaxLimit?: number;
    testTool?: string;
    sampleSize: number;
    minValueLimit?: number;
    maxValueLimit?: number;
    qaValues?: any[];
    operatorId?: number;
    operatorName?: string;
    operatorTime?: string;
    qaMax?: number;
    qaMin?: number;
    qaAvg?: number;
    qaResult: Judgment;
    qaInspectorId?: number;
    qaInspectorName?: string;
    qaTime?: string;
    calculatedAvg?: number;
    calculatedStd?: number;
    calculatedMin?: number;
    calculatedMax?: number;
    sheetSectionCode?: string;
    sheetSectionName?: string;
    sheetMetricCode?: string;
    sheetMetricName?: string;
    sheetFieldCode?: string;
    sheetTemplateId?: number;
    cellCompletedCount?: number;
    cellRequiredCount?: number;
    isSpc?: boolean;
    sort?: number;
    samples?: FaiSample[];
    recheckItemFlag?: boolean;
    auditGroups?: FaiGroupAudit[];
  }

  export interface FaiGroupAudit {
    id?: number;
    faiId?: number;
    faiItemId?: number;
    faiNo?: string;
    inspectionItem?: string;
    groupKey: string;
    samplePosition?: string;
    auditResult?: ItemAuditResult;
    auditRemark?: string;
    auditUserId?: number;
    auditUserName?: string;
    auditTime?: string;
    itemRecheckStatus?: ItemRecheckStatus;
    recheckItemFlag?: boolean;
  }

  export interface FaiAbnormal {
    id?: number;
    faiItemId?: number;
    sampleId?: number;
    abnormalRole?: 'OPERATOR' | 'QA' | 'SYSTEM';
    defectCode?: string;
    defectName?: string;
    abnormalDesc: string;
    processStatus?: string;
    actionRequired?: string;
    ncRecordId?: number;
  }

  export interface FaiRecord {
    id?: number;
    faiNo: string;
    workOrderNo: string;
    planOrderId?: number;
    sourceReportNo?: string;
    operationCode?: string;
    operationName?: string;
    sourceOperationCode?: string;
    sourceOperationName?: string;
    machineId?: number;
    machineCode: string;
    machineName?: string;
    materialId?: number;
    materialCode: string;
    materialName: string;
    specification: string;
    productModelId?: number;
    productModel?: string;
    productBatchNo?: string;
    glueBoardStockId?: number;
    glueBoardModel?: string;
    glueBoardMaterialCode?: string;
    gluePlateBatchNo?: string;
    sampleLength?: number;
    inspectionQty?: number;
    processCategory?: string;
    sourceModule?: string;
    standardMatchMode?:
      | 'MATERIAL'
      | 'MATERIAL_PROCESS'
      | 'PROCESS'
      | 'PRODUCT_MODEL_PROCESS';
    standardMatchType?:
      | 'EXACT_MODEL'
      | 'FAMILY_MODEL'
      | 'MATERIAL'
      | 'MATERIAL_PROCESS'
      | 'OVERRIDE'
      | 'PROCESS';
    matchedModelId?: number;
    matchedModelCode?: string;
    standardMatchReason?: string;
    submissionType?: string;
    wetSampleType?: string;
    triggerReason:
      | 'NEW_ORDER'
      | 'ADDITIONAL_FIRST_INSPECTION'
      | 'PARAMETER_CHANGE'
      | 'PROCESS_CHECK_NG_RESTART'
      | 'REWORK_RECHECK'
      | 'SHIFT_CHANGE'
      | 'TOOL_CHANGE';
    qaInspector?: string;
    qaInspectorId?: number;
    qaInspectorName?: string;
    qaTime?: string;
    inspectionTime?: string;
    submissionTime?: string;
    submitterName?: string;
    retentionStatus?: RetentionStatus;
    retentionConfirmTime?: string;
    retentionConfirmUserId?: number;
    retentionConfirmUserName?: string;
    retentionPeriodValue?: number;
    retentionPeriodUnit?: RetentionPeriodUnit;
    retentionExpireTime?: string;
    retentionDestroyStatus?: RetentionDestroyStatus;
    retentionDestroyTime?: string;
    retentionDestroyUserId?: number;
    retentionDestroyUserName?: string;
    retentionDestroyRemark?: string;
    releaseResult?: string;
    releaseTime?: string;
    auditRemark?: string;
    judgment: Judgment;
    operatorId?: number;
    operatorName?: string;
    operatorTime?: string;
    status:
      | 'CANCELED'
      | 'COMPLETED'
      | 'INSPECTING'
      | 'PENDING'
      | 'REJECTED'
      | 'REWORKING'
      | 'SUSPENDED'
      | 'WAITING_QA';
    standardId?: number;
    standardNo?: string;
    standardVersion?: string;
    standardSnapshotHash?: string;
    standardContentChanged?: boolean;
    standardSwitchAllowed?: boolean;
    standardSelectionMessage?: string;
    sheetTemplateId?: number;
    sheetTemplateCode?: string;
    sheetTemplateName?: string;
    sheetTemplateVersion?: string;
    entryMode?:
      | 'EXCEL_IMPORT'
      | 'HISTORICAL_IMPORT'
      | 'MANUAL'
      | 'MIXED'
      | 'MIXED_REPAIR';
    entryLayout?: 'PROGRAM_FORM' | 'SHEET_GRID';
    currentStepCode?: string;
    entryProgress?: number;
    requiredItemCount?: number;
    completedItemCount?: number;
    abnormalItemCount?: number;
    lastSaveTime?: string;
    lastCalculateTime?: string;
    lastImportBatchNo?: string;
    lastScanCode?: string;
    lastScanScene?: string;
    lastScanTargetType?: string;
    lastScanTime?: string;
    lastScanUserId?: number;
    lastScanUserName?: string;
    historicalBackfill?: boolean;
    sheetLocked?: boolean;
    returnCount?: number;
    lastReturnReason?: string;
    originalInspectionNo?: string;
    recheckFlag?: boolean;
    rejectPrevInspectionNo?: string;
    rejectRootInspectionNo?: string;
    latestRecheckApplyId?: number;
    latestRecheckApplyNo?: string;
    latestRecheckApplyStatus?: RecheckApplyStatus;
    latestRecheckApplyReason?: string;
    latestRecheckAuditOpinion?: string;
    latestRecheckGeneratedFaiNo?: string;
    remark?: string;
    items?: FaiItem[];
    abnormals?: FaiAbnormal[];
  }

  export type RecheckApplyStatus =
    | 'APPROVED'
    | 'CANCELED'
    | 'PENDING_AUDIT'
    | 'REJECTED';

  export interface RecheckApply {
    id: number;
    applyNo?: string;
    sourceFaiId?: number;
    sourceFaiNo?: string;
    sourceInspectionType?: 'ADDITIONAL' | 'ORIGINAL' | 'RECHECK';
    applyReason?: string;
    applyUserId?: number;
    applyUserName?: string;
    applyTime?: string;
    status?: RecheckApplyStatus;
    auditUserId?: number;
    auditUserName?: string;
    auditTime?: string;
    auditOpinion?: string;
    generatedFaiId?: number;
    generatedFaiNo?: string;
  }

  export interface FaiPageReq extends PageParam {
    faiNo?: string;
    workOrderNo?: string;
    machineCode?: string;
    materialCode?: string;
    productModel?: string;
    productBatchNo?: string;
    glueBoardModel?: string;
    glueBoardMaterialCode?: string;
    gluePlateBatchNo?: string;
    processCategory?: string;
    sourceModule?: string;
    submissionType?: string;
    wetSampleType?: string;
    submitterName?: string;
    submissionTime?: string[];
    inspectionTime?: string[];
    status?: string;
    judgment?: string;
    recheckFlag?: boolean;
    itemRecheckStatusFilter?: ItemRecheckStatus;
    retentionStatus?: string;
    retentionDestroyStatus?: string;
    retentionExpireTime?: string[];
    triggerReason?: string;
    qaTime?: string[];
  }

  export interface RetentionExpireTimeReq {
    id: number;
    retentionExpireTime: string;
  }

  export interface RetentionDestroyReq {
    ids: number[];
    remark?: string;
  }

  export interface BindStandardReq {
    id: number;
    standardId: number;
    overrideReason?: string;
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
    glueBoardModel?: string;
    processCode?: string;
    processName?: string;
    itemCount?: number;
    matchType?:
      | 'EXACT_MODEL'
      | 'FAMILY_MODEL'
      | 'MATERIAL'
      | 'MATERIAL_PROCESS'
      | 'PROCESS';
    matchScore?: number;
    matchReason?: string;
    recommended?: boolean;
  }

  export interface SelectStandardReq {
    id: number;
    standardId: number;
    reason?: string;
    selectedStandardItemIds?: number[];
  }

  export interface FaiStandardItemCandidate {
    standardItemId: number;
    inspectionItem?: string;
    standardDesc?: string;
    unit?: string;
    inspectionMethod?: string;
    testFrequencyJudgement?: string;
    sampleSize?: number;
    sort?: number;
    /** 原检NG仅用于提示，绝不代表该项目已被选择。 */
    originalNg?: boolean;
  }

  export interface FaiStandard {
    standardId: number;
    standardNo: string;
    standardName: string;
    version: string;
    applyType: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    specification?: string;
    processCode?: string;
    processName?: string;
    items: Array<{
      attachmentEnabled?: boolean;
      avgMaxLimit?: number;
      avgMinLimit?: number;
      inspectionItem: string;
      inspectionMethod?: string;
      isSpc?: boolean;
      itemType: 'QUALITATIVE' | 'QUANTITATIVE';
      judgmentMetric?: JudgmentMetric;
      maxValueLimit?: number;
      minValueLimit?: number;
      ruleDescription?: string;
      sampleSize: number;
      sheetFieldCode?: string;
      sheetMetricCode?: string;
      sheetSectionCode?: string;
      sheetTemplateId?: number;
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
      valueTemplate?: ValueTemplate;
      valueTemplateName?: string;
    }>;
  }

  export interface FaiSheetTemplate {
    id: number;
    templateCode: string;
    templateName: string;
    templateVersion: string;
    productModel?: string;
    sheetName?: string;
    status: string;
    remark?: string;
    sections?: Array<{
      excelRange?: string;
      expectedColumns?: number;
      expectedRows: number;
      fields?: Array<{
        avgMaxLimit?: number;
        avgMinLimit?: number;
        excelColumn?: string;
        fieldCode: string;
        fieldName: string;
        fieldRole: string;
        formulaExpr?: string;
        id: number;
        metricCode: string;
        metricName: string;
        sort?: number;
        stdMaxLimit?: number;
        stdMinLimit?: number;
        unit?: string;
      }>;
      id: number;
      sectionCode: string;
      sectionName: string;
      sectionType: string;
      sort?: number;
    }>;
  }

  export interface FaiImportResp {
    failureCount?: number;
    fileName?: string;
    importBatchNo?: string;
    previewOnly?: boolean;
    successCount?: number;
    status: string;
    totalCount?: number;
    validateSummary?: string;
    warningCount?: number;
    record?: FaiRecord;
    messages?: string[];
  }

  export interface FaiScanReq {
    scanCode: string;
    scanScene?:
      | 'ITEM_MODAL'
      | 'ITEM_OVERVIEW'
      | 'LEDGER_TOOLBAR'
      | 'WORKBENCH_HEADER';
    currentFaiId?: number;
    sourceModule?: string;
    clientType?: string;
    terminalCode?: string;
  }

  export interface FaiScanCandidate {
    id: number;
    faiNo: string;
    workOrderNo?: string;
    productModel?: string;
    productBatchNo?: string;
    machineCode?: string;
    status?: FaiRecord['status'];
    judgment?: Judgment;
    currentStepCode?: string;
    lastSaveTime?: string;
    createTime?: string;
  }

  export interface FaiScanResp {
    scanRecordId?: number;
    scanCode: string;
    scanTargetType: string;
    scanScene: string;
    matchResult:
      | 'CURRENT_DIRTY'
      | 'MATCHED_MULTIPLE'
      | 'MATCHED_SINGLE'
      | 'NOT_FOUND'
      | 'PERMISSION_DENIED'
      | 'STATUS_BLOCKED'
      | 'TEMPLATE_MISSING';
    openTarget?: 'CANDIDATE_MODAL' | 'ITEM_MODAL' | 'REPORT' | 'WORKBENCH';
    message?: string;
    matchedFaiId?: number;
    matchedFaiNo?: string;
    matchedFaiItemId?: number;
    matchedStepCode?: string;
    candidateCount?: number;
    record?: FaiRecord;
    candidates?: FaiScanCandidate[];
    scanTime?: string;
  }

  export interface FaiAuditReq {
    id: number;
    auditResult: AuditResult;
    rejectReason?: string;
    groups?: FaiGroupAuditReq[];
  }

  export interface FaiGroupAuditReq {
    itemId: number;
    groupKey: string;
    samplePosition?: string;
    auditResult: Exclude<ItemAuditResult, 'PENDING'>;
    auditRemark?: string;
  }
}

function toUiJudgment(value?: string): MesFaiApi.Judgment {
  return !value || value === 'PENDING' ? '-' : (value as MesFaiApi.Judgment);
}

function toApiJudgment(value?: MesFaiApi.Judgment) {
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

function buildEmptyRows(
  item: Pick<MesFaiApi.FaiItem, 'itemType' | 'sampleSize' | 'templateParams'>,
) {
  const sampleSize = resolveEntryRuleSampleSize(item);
  const positions = resolveEntryRulePositions(item);
  const repeatCount = resolveEntryRuleRepeatCount(item);
  const rows = positions.flatMap((position) =>
    Array.from({ length: repeatCount }).map((_, repeatIndex) => ({
      sampleGroupNo: repeatIndex + 1,
      samplePosition: position.name,
      samplePositionCode: position.code,
      sampleResult: 'PENDING',
    })),
  );
  const expectedSize = hasEntryRulePositions(item)
    ? resolveEntryRuleExpectedSampleCount(item)
    : sampleSize;
  return Array.from({ length: expectedSize }).map((_, index) => ({
    ...(rows[index] || {
      sampleGroupNo: (index % repeatCount) + 1,
      samplePosition: `${index + 1}`,
      samplePositionCode: `P${index + 1}`,
      sampleResult: 'PENDING',
    }),
    sampleSeq: index + 1,
  }));
}

function buildValues(item: any, role: MesFaiApi.SampleRole) {
  const samples = (item.samples || [])
    .filter((sample: MesFaiApi.FaiSample) => sample.sampleRole === role)
    .toSorted(
      (a: MesFaiApi.FaiSample, b: MesFaiApi.FaiSample) =>
        a.sampleSeq - b.sampleSeq,
    );
  if (samples.length === 0) return buildEmptyRows(item);
  return samples.map((sample: MesFaiApi.FaiSample) => {
    const base = {
      defectCode: sample.defectCode,
      defectName: sample.defectName,
      importBatchNo: sample.importBatchNo,
      metricCode: sample.metricCode,
      metricGroupCode: sample.metricGroupCode,
      remark: sample.remark,
      sampleAxis: sample.sampleAxis,
      sampleColumnNo: sample.sampleColumnNo,
      sampleGroupNo: sample.sampleGroupNo,
      samplePosition: sample.samplePosition,
      sampleResult: sample.sampleResult || 'PENDING',
      sampleSeq: sample.sampleSeq,
      sheetMetricCode: sample.sheetMetricCode,
      sheetSectionCode: sample.sheetSectionCode,
      stepCode: sample.stepCode,
      valueSource: sample.valueSource,
    };
    if (item.itemType !== 'QUANTITATIVE') {
      return {
        ...base,
        value: sample.qualitativeValue,
      };
    }
    if (sample.rawValuesJson) {
      try {
        return {
          ...base,
          ...JSON.parse(sample.rawValuesJson),
          resultValue: sample.resultValue,
          densityValue: sample.densityValue,
          compressionRate: sample.compressionRate,
          compressionElasticityRate: sample.compressionElasticityRate,
        };
      } catch {
        return {
          ...base,
          value: sample.measuredValue,
        };
      }
    }
    return {
      ...base,
      resultValue: sample.resultValue,
      value: sample.measuredValue ?? sample.resultValue,
    };
  });
}

function resolveDataRuleResultCodes(item: MesFaiApi.FaiItem) {
  try {
    const params = parseEntryRuleParams(item.templateParams);
    const dataRule = params.dataRule || params;
    return Array.isArray(dataRule.resultFields)
      ? dataRule.resultFields.map((field: any) => field.code).filter(Boolean)
      : [];
  } catch {
    return [];
  }
}

function normalizeItem(item: any): MesFaiApi.FaiItem {
  return {
    ...item,
    attachmentUrls: item.attachmentUrls || [],
    qaResult: toUiJudgment(item.qaResult),
    qaValues: item.qaValues || buildValues(item, 'QA'),
  };
}

function normalizeRecord(record: any): MesFaiApi.FaiRecord {
  return {
    ...record,
    materialName: record.materialName || record.productModel,
    productModel: record.productModel || record.materialName,
    qaInspector: record.qaInspectorName || record.qaInspector,
    judgment: toUiJudgment(record.judgment),
    items: (record.items || []).map((item: any) => normalizeItem(item)),
  };
}

function resolveRecordList(response: any): MesFaiApi.FaiRecord[] {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.list)) return response.list;
  if (Array.isArray(response?.records)) return response.records;
  if (Array.isArray(response?.data)) return response.data;
  if (Array.isArray(response?.data?.list)) return response.data.list;
  if (Array.isArray(response?.data?.records)) return response.data.records;
  return [];
}

function buildSamples(
  item: MesFaiApi.FaiItem,
  role: MesFaiApi.SampleRole,
): MesFaiApi.FaiSample[] {
  const values = item.qaValues;
  return (values || []).map((value, index) => {
    const row = value && typeof value === 'object' ? value : { value };
    const resultCodes = resolveDataRuleResultCodes(item);
    const rawValues =
      item.itemType === 'QUANTITATIVE'
        ? Object.fromEntries(
            Object.entries(row).filter(
              ([key, val]) =>
                ![
                  'compressionElasticityRate',
                  'compressionRate',
                  'densityValue',
                  'inputComponent',
                  'importBatchNo',
                  'metricCode',
                  'metricGroupCode',
                  'remark',
                  'resultValue',
                  'sampleAxis',
                  'sampleColumnNo',
                  'sampleGroupNo',
                  'samplePosition',
                  'samplePositionCode',
                  'sampleResult',
                  'sampleSeq',
                  'sheetMetricCode',
                  'sheetSectionCode',
                  'stepCode',
                  'valueSource',
                  ...resultCodes,
                ].includes(key) &&
                val !== undefined &&
                val !== null &&
                val !== '',
            ),
          )
        : undefined;
    const measuredValue =
      item.itemType === 'QUANTITATIVE'
        ? Number(
            row.resultValue ??
              row.densityValue ??
              row.compressionRate ??
              row.compressionElasticityRate ??
              row.value ??
              value,
          )
        : undefined;
    const qualitativeValue = row.value ?? value;
    return {
      sampleRole: role,
      sampleSeq: index + 1,
      stepCode: row.stepCode ?? item.stepCode,
      metricCode: row.metricCode ?? item.metricCode,
      metricGroupCode: row.metricGroupCode ?? item.metricGroupCode,
      inputComponent: row.inputComponent ?? item.inputComponent,
      samplePosition: row.samplePosition,
      rawValuesJson: rawValues ? JSON.stringify(rawValues) : undefined,
      resultValue: row.resultValue,
      densityValue: row.densityValue,
      compressionRate: row.compressionRate,
      compressionElasticityRate: row.compressionElasticityRate,
      measuredValue: Number.isFinite(measuredValue as number)
        ? measuredValue
        : undefined,
      qualitativeValue:
        item.itemType === 'QUALITATIVE' ? qualitativeValue : undefined,
      sampleResult:
        row.sampleResult ||
        (item.itemType === 'QUALITATIVE' ? qualitativeValue : undefined),
      sheetSectionCode: row.sheetSectionCode ?? item.sheetSectionCode,
      sheetMetricCode: row.sheetMetricCode ?? item.sheetMetricCode,
      sampleGroupNo: row.sampleGroupNo ?? index + 1,
      sampleAxis: row.sampleAxis,
      sampleColumnNo: row.sampleColumnNo,
      importBatchNo: row.importBatchNo,
      remark: row.remark,
    };
  });
}

function toSubmitPayload(
  data: MesFaiApi.FaiRecord,
  entryLayout: MesFaiApi.FaiRecord['entryLayout'] = 'PROGRAM_FORM',
) {
  return {
    id: data.id,
    faiNo: data.faiNo,
    workOrderNo: data.workOrderNo,
    planOrderId: data.planOrderId,
    operationCode: data.operationCode,
    operationName: data.operationName,
    machineId: data.machineId,
    machineCode: data.machineCode,
    machineName: data.machineName,
    materialId: data.materialId,
    materialCode: data.materialCode,
    materialName: data.materialName,
    specification: data.specification,
    productModel: data.productModel,
    productBatchNo: data.productBatchNo,
    glueBoardStockId: data.glueBoardStockId,
    glueBoardModel: data.glueBoardModel,
    glueBoardMaterialCode: data.glueBoardMaterialCode,
    gluePlateBatchNo: data.gluePlateBatchNo,
    sampleLength: data.sampleLength,
    inspectionQty: data.inspectionQty,
    processCategory: data.processCategory,
    sourceModule: data.sourceModule,
    submissionType: data.submissionType,
    wetSampleType: data.wetSampleType,
    triggerReason: data.triggerReason,
    standardId: data.standardId,
    standardNo: data.standardNo,
    standardVersion: data.standardVersion,
    status: data.status,
    judgment: toApiJudgment(data.judgment),
    inspectionTime: toLocalDateTimeMillis(data.inspectionTime),
    submissionTime: data.submissionTime,
    submitterName: data.submitterName,
    retentionStatus: data.retentionStatus,
    retentionConfirmTime: data.retentionConfirmTime,
    retentionConfirmUserId: data.retentionConfirmUserId,
    retentionConfirmUserName: data.retentionConfirmUserName,
    remark: data.remark,
    sheetTemplateId: data.sheetTemplateId,
    sheetTemplateCode: data.sheetTemplateCode,
    sheetTemplateName: data.sheetTemplateName,
    sheetTemplateVersion: data.sheetTemplateVersion,
    currentStepCode: data.currentStepCode,
    entryLayout,
    entryMode: 'MANUAL',
    lastReturnReason: data.lastReturnReason,
    items: (data.items || []).map((item) => ({
      id: item.id,
      standardItemId: item.standardItemId,
      stepCode: item.stepCode,
      stepName: item.stepName,
      metricCode: item.metricCode,
      metricGroupCode: item.metricGroupCode,
      inputComponent: item.inputComponent,
      inspectionItem: item.inspectionItem,
      itemType: item.itemType,
      attachmentEnabled: item.attachmentEnabled,
      attachmentUrls: item.attachmentUrls || [],
      targetValue: item.targetValue,
      standardDesc: item.standardDesc,
      unit: item.unit,
      ruleDescription: item.ruleDescription,
      inspectionMethod: item.inspectionMethod,
      testFrequencyJudgement: item.testFrequencyJudgement,
      valueTemplate: item.valueTemplate,
      valueTemplateName: item.valueTemplateName,
      judgmentMetric: item.judgmentMetric,
      templateParams: item.templateParams,
      avgMinLimit: item.avgMinLimit,
      avgMaxLimit: item.avgMaxLimit,
      stdMinLimit: item.stdMinLimit,
      stdMaxLimit: item.stdMaxLimit,
      sheetSectionCode: item.sheetSectionCode,
      sheetSectionName: item.sheetSectionName,
      sheetMetricCode: item.sheetMetricCode,
      sheetMetricName: item.sheetMetricName,
      sheetFieldCode: item.sheetFieldCode,
      testTool: item.testTool,
      sampleSize: item.sampleSize,
      minValueLimit: item.minValueLimit,
      maxValueLimit: item.maxValueLimit,
      qaResult: toApiJudgment(item.qaResult),
      requiredSampleCount: item.requiredSampleCount,
      completedSampleCount: item.completedSampleCount,
      abnormalSampleCount: item.abnormalSampleCount,
      inputStatus: item.inputStatus,
      isSpc: item.isSpc,
      sort: item.sort,
      samples: buildSamples(item, 'QA'),
    })),
    abnormals: data.abnormals,
  };
}

/**
 * 包装段 COA 送检的 productBatchNo 保存的是段批次；页面展示时应优先使用实际送检片号。
 * 其他来源的 FAI 保持原产品批次展示，避免改变既有业务语义。
 */
export function resolveFaiDisplayBatchNo(
  record?: Pick<
    MesFaiApi.FaiRecord,
    'productBatchNo' | 'remark' | 'sourceModule' | 'sourceReportNo'
  > | null,
) {
  const productBatchNo = String(record?.productBatchNo || '').trim();
  if (String(record?.sourceModule || '').trim().toUpperCase() !== 'PACKAGING_COA') {
    return productBatchNo || '-';
  }

  const remark = String(record?.remark || '');
  const sampleMatch = remark.match(/(?:样片|片号)\s*[：:]\s*([^；;，,\s]+)/);
  if (sampleMatch?.[1]) return sampleMatch[1];

  const sourceReportNo = String(record?.sourceReportNo || '').trim();
  const sourcePrefix = productBatchNo
    ? `PACKAGING-COA-${productBatchNo}-`
    : '';
  if (sourcePrefix && sourceReportNo.startsWith(sourcePrefix)) {
    const sampleBatchNo = sourceReportNo
      .slice(sourcePrefix.length)
      .replace(/-\d{17}$/, '');
    if (sampleBatchNo) return sampleBatchNo;
  }
  return productBatchNo || '-';
}

export async function getFaiPage(params: MesFaiApi.FaiPageReq) {
  const page = await requestClient.get<PageResult<MesFaiApi.FaiRecord>>(
    '/mes/quality/fai/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getFaiRetentionPage(params: MesFaiApi.FaiPageReq) {
  const page = await requestClient.get<PageResult<MesFaiApi.FaiRecord>>(
    '/mes/quality/fai/retention/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getFaiRetentionExpiredPage(
  params: MesFaiApi.FaiPageReq,
) {
  const page = await requestClient.get<PageResult<MesFaiApi.FaiRecord>>(
    '/mes/quality/fai/retention/expired/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function updateFaiRetentionExpireTime(
  data: MesFaiApi.RetentionExpireTimeReq,
) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/retention/expire-time',
    data,
  );
  return normalizeRecord(record);
}

export async function destroyFaiRetention(data: MesFaiApi.RetentionDestroyReq) {
  return requestClient.put<boolean>('/mes/quality/fai/retention/destroy', data);
}

export async function getPendingFaiTasks() {
  const response = await requestClient.get<MesFaiApi.FaiRecord[]>(
    '/mes/quality/fai/pending-list',
  );
  return resolveRecordList(response).map((item) => normalizeRecord(item));
}

export async function resolveFaiScan(data: MesFaiApi.FaiScanReq) {
  const resp = await requestClient.post<MesFaiApi.FaiScanResp>(
    '/mes/quality/fai/scan/resolve',
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

export async function getStandardByMaterial(
  materialCode: string,
  operationCode?: string,
  operationName?: string,
  existingItems?: MesFaiApi.FaiItem[],
) {
  if (existingItems && existingItems.length > 0)
    return existingItems.map((item) => normalizeItem(item));
  const standard = await requestClient.get<MesFaiApi.FaiStandard>(
    '/mes/quality/fai/standard-by-material',
    {
      params: { materialCode, operationCode, operationName },
    },
  );
  return (standard.items || []).map((item) => ({
    ...item,
    qaResult: '-',
    qaValues: buildEmptyRows(item),
  }));
}

export async function getFaiDetail(id: number) {
  const record = await requestClient.get<MesFaiApi.FaiRecord>(
    `/mes/quality/fai/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export function getFaiStandardCandidates(id: number) {
  return requestClient.get<MesFaiApi.StandardCandidate[]>(
    '/mes/quality/fai/standard-candidates',
    { params: { id } },
  );
}

export function getFaiStandardItemCandidates(id: number, standardId: number) {
  return requestClient.get<MesFaiApi.FaiStandardItemCandidate[]>(
    '/mes/quality/fai/standard-item-candidates',
    { params: { id, standardId } },
  );
}

export async function selectFaiStandard(data: MesFaiApi.SelectStandardReq) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/select-standard',
    data,
  );
  return normalizeRecord(record);
}

export async function getFaiRetentionDetail(id: number) {
  const record = await requestClient.get<MesFaiApi.FaiRecord>(
    `/mes/quality/fai/retention/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function getFaiSheetTemplateList(productModel?: string) {
  return requestClient.get<MesFaiApi.FaiSheetTemplate[]>(
    '/mes/quality/fai/sheet-template/list',
    {
      params: { productModel },
    },
  );
}

export async function getFaiSheetTemplate(id: number) {
  return requestClient.get<MesFaiApi.FaiSheetTemplate>(
    `/mes/quality/fai/sheet-template/get?id=${id}`,
  );
}

export async function selectFaiSheetTemplate(id: number, templateId: number) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    `/mes/quality/fai/select-sheet-template?id=${id}&templateId=${templateId}`,
  );
  return normalizeRecord(record);
}

export async function importFaiOriginSheet(
  id: number,
  templateId: number,
  file: File,
  mode = 'OVERWRITE',
) {
  const resp = await requestClient.upload<MesFaiApi.FaiImportResp>(
    '/mes/quality/fai/import-origin-sheet',
    {
      id,
      templateId,
      mode,
      file,
    },
  );
  return {
    ...resp,
    record: resp.record ? normalizeRecord(resp.record) : undefined,
  };
}

export function downloadFaiItemTemplate(id: number) {
  return requestClient.download('/mes/quality/fai/item-template/download', {
    params: { id },
  });
}

export function exportFaiItemValues(id: number) {
  return requestClient.download('/mes/quality/fai/item-export', {
    params: { id },
  });
}

export async function previewFaiItemImport(id: number, file: File) {
  return requestClient.upload<MesFaiApi.FaiImportResp>(
    '/mes/quality/fai/item-import/preview',
    { file, id },
  );
}

export async function confirmFaiItemImport(
  id: number,
  file: File,
  allowOverwrite = false,
) {
  const resp = await requestClient.upload<MesFaiApi.FaiImportResp>(
    '/mes/quality/fai/item-import/confirm',
    { allowOverwrite, file, id },
  );
  return {
    ...resp,
    record: resp.record ? normalizeRecord(resp.record) : undefined,
  };
}

export async function saveFaiSheetEntry(data: MesFaiApi.FaiRecord) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/save-sheet-entry',
    toSubmitPayload(data, 'SHEET_GRID'),
  );
  return normalizeRecord(record);
}

export async function saveFaiProgramEntry(data: MesFaiApi.FaiRecord) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/program-entry/save',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function bindFaiStandard(data: MesFaiApi.BindStandardReq) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/bind-standard',
    data,
  );
  return normalizeRecord(record);
}

export async function recalculateFaiProgramEntry(data: MesFaiApi.FaiRecord) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/program-entry/recalculate',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function auditFaiProgramEntryItem(data: MesFaiApi.FaiRecord) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/program-entry/item-audit',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function submitFaiProgramEntry(data: MesFaiApi.FaiRecord) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/program-entry/submit',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function auditFaiProgramEntry(data: MesFaiApi.FaiAuditReq) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/program-entry/audit',
    data,
  );
  return normalizeRecord(record);
}

export async function oneClickPassFai(id: number) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/one-click-pass',
    undefined,
    { params: { id } },
  );
  return normalizeRecord(record);
}

export async function oneClickFailFai(id: number) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/one-click-fail',
    undefined,
    { params: { id } },
  );
  return normalizeRecord(record);
}

export function exportFaiExcel(params: MesFaiApi.FaiPageReq) {
  return requestClient.download('/mes/quality/fai/export-excel', { params });
}

export function exportFaiRetentionExcel(params: MesFaiApi.FaiPageReq) {
  return requestClient.download('/mes/quality/fai/retention/export-excel', {
    params,
  });
}

export async function returnFaiProgramEntry(data: MesFaiApi.FaiRecord) {
  const payload = toSubmitPayload(data);
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/fai/program-entry/return',
    {
      id: data.id,
      currentStepCode: data.currentStepCode,
      lastReturnReason: data.lastReturnReason,
      items: payload.items,
    },
  );
  return normalizeRecord(record);
}

export async function createFaiRecord(data: MesFaiApi.FaiRecord) {
  const payload = { ...data };
  delete payload.items;
  delete payload.abnormals;
  const id = await requestClient.post<number>(
    '/mes/quality/fai/create',
    payload,
  );
  return getFaiDetail(id);
}

export async function applyFaiRecheck(sourceFaiId: number, applyReason?: string) {
  return await requestClient.post<MesFaiApi.RecheckApply>(
    '/mes/quality/fai/recheck/apply',
    { applyReason, sourceFaiId },
  );
}

export async function auditFaiRecheckApply(data: {
  approved: boolean;
  auditOpinion?: string;
  id: number;
}) {
  return await requestClient.post<MesFaiApi.RecheckApply>(
    '/mes/quality/fai/recheck/audit',
    data,
  );
}

export async function getGlueBoardFaiPage(params: MesFaiApi.FaiPageReq) {
  const page = await requestClient.get<PageResult<MesFaiApi.FaiRecord>>(
    '/mes/quality/glue-board-fai/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getPendingGlueBoardFaiTasks() {
  const response = await requestClient.get<MesFaiApi.FaiRecord[]>(
    '/mes/quality/glue-board-fai/pending-list',
  );
  return resolveRecordList(response).map((item) => normalizeRecord(item));
}

export async function getGlueBoardFaiDetail(id: number) {
  const record = await requestClient.get<MesFaiApi.FaiRecord>(
    `/mes/quality/glue-board-fai/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export function getGlueBoardFaiStandardCandidates(id: number) {
  return requestClient.get<MesFaiApi.StandardCandidate[]>(
    '/mes/quality/glue-board-fai/standard-candidates',
    { params: { id } },
  );
}

export async function selectGlueBoardFaiStandard(
  data: MesFaiApi.SelectStandardReq,
) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/glue-board-fai/select-standard',
    data,
  );
  return normalizeRecord(record);
}

export async function saveGlueBoardFaiProgramEntry(data: MesFaiApi.FaiRecord) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/glue-board-fai/program-entry/save',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function bindGlueBoardFaiStandard(
  data: MesFaiApi.BindStandardReq,
) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/glue-board-fai/bind-standard',
    data,
  );
  return normalizeRecord(record);
}

export async function recalculateGlueBoardFaiProgramEntry(
  data: MesFaiApi.FaiRecord,
) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/glue-board-fai/program-entry/recalculate',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function submitGlueBoardFaiProgramEntry(
  data: MesFaiApi.FaiRecord,
) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/glue-board-fai/program-entry/submit',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function auditGlueBoardFaiProgramEntry(
  data: MesFaiApi.FaiAuditReq,
) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/glue-board-fai/program-entry/audit',
    data,
  );
  return normalizeRecord(record);
}

export async function oneClickPassGlueBoardFai(id: number) {
  const record = await requestClient.put<MesFaiApi.FaiRecord>(
    '/mes/quality/glue-board-fai/one-click-pass',
    undefined,
    { params: { id } },
  );
  return normalizeRecord(record);
}

export async function createGlueBoardFaiRecord(
  data: Partial<MesFaiApi.FaiRecord>,
) {
  const id = await requestClient.post<number>(
    '/mes/quality/glue-board-fai/create',
    data,
  );
  return getGlueBoardFaiDetail(id);
}

export async function createGlueBoardFaiRecordFromAdhesive1(
  data: Partial<MesFaiApi.FaiRecord> & {
    glueBoardMaterialCode?: string;
    glueBoardMaterialName?: string;
    glueBoardUsageId?: number;
    sampleStartPosition?: number;
    sourceProductionBatchNo?: string;
  },
) {
  const id = await requestClient.post<number>(
    '/mes/quality/glue-board-fai/create-from-adhesive1',
    data,
  );
  return getGlueBoardFaiDetail(id);
}

export async function createGlueBoardFaiRecordFromAdhesive2(
  data: Partial<MesFaiApi.FaiRecord> & {
    glueBoardMaterialCode?: string;
    glueBoardMaterialName?: string;
    glueBoardUsageId?: number;
    sampleStartPosition?: number;
    sourceProductionBatchNo?: string;
  },
) {
  const id = await requestClient.post<number>(
    '/mes/quality/glue-board-fai/create-from-adhesive2',
    data,
  );
  return getGlueBoardFaiDetail(id);
}

export function downloadGlueBoardFaiItemTemplate(id: number) {
  return requestClient.download(
    '/mes/quality/glue-board-fai/item-template/download',
    { params: { id } },
  );
}

export function exportGlueBoardFaiItemValues(id: number) {
  return requestClient.download('/mes/quality/glue-board-fai/item-export', {
    params: { id },
  });
}

export async function suspendFaiRecord(id: number) {
  return requestClient.put<boolean>(`/mes/quality/fai/suspend?id=${id}`);
}
