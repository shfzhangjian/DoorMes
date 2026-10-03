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

export namespace MesFqcApi {
  export type Judgment = '-' | 'NG' | 'OK' | 'PENDING';
  export type AuditResult = 'FAIL' | 'PASS' | 'REJECT';
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

  export interface FqcSampleDefect {
    id?: number;
    defectCodeId?: number;
    defectCode: string;
    defectName: string;
    defectLevel?: string;
    sort?: number;
  }

  export interface FqcSample {
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
    defects?: FqcSampleDefect[];
    remark?: string;
    recheckItemFlag?: boolean;
  }

  export interface FqcDefectCodeOption {
    id: number;
    code: string;
    name: string;
    level?: string;
    referencePicUrls?: string[];
    causes?: Array<{
      reasonCode?: string;
      reasonName?: string;
      reasonType?: string;
      reasonDesc?: string;
      sort?: number;
      status?: number;
      remark?: string;
    }>;
    remark?: string;
    sort?: number;
    status: number;
    type: 'CATEGORY' | 'ITEM';
  }

  export interface FqcItem {
    id?: number;
    abnormalSampleCount?: number;
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
    samples?: FqcSample[];
    recheckItemFlag?: boolean;
  }

  export interface FqcAbnormal {
    id?: number;
    fqcItemId?: number;
    sampleId?: number;
    abnormalRole?: 'OPERATOR' | 'QA' | 'SYSTEM';
    defectCode?: string;
    defectName?: string;
    abnormalDesc: string;
    processStatus?: string;
    actionRequired?: string;
    ncRecordId?: number;
  }

  export interface FqcRecord {
    id?: number;
    fqcNo: string;
    reportNo?: string;
    workOrderNo: string;
    planOrderId?: number;
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
    productModel?: string;
    productBatchNo?: string;
    batchNo?: string;
    produceQty?: number;
    sampleQty?: number;
    glueBoardModel?: string;
    gluePlateBatchNo?: string;
    processCategory?: string;
    sourceModule?: string;
    standardMatchMode?:
      | 'MATERIAL'
      | 'MATERIAL_PROCESS'
      | 'PROCESS'
      | 'PRODUCT_MODEL_PROCESS';
    submissionType?: string;
    triggerReason:
      | 'NEW_ORDER'
      | 'PARAMETER_CHANGE'
      | 'REWORK_RECHECK'
      | 'SHIFT_CHANGE'
      | 'TOOL_CHANGE';
    inspectorId?: number;
    inspectorName?: string;
    inspectionTime?: string;
    qaInspector?: string;
    qaInspectorId?: number;
    qaInspectorName?: string;
    qaTime?: string;
    submissionTime?: string;
    submitterName?: string;
    releaseResult?: string;
    releaseTime?: string;
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
    remark?: string;
    items?: FqcItem[];
    abnormals?: FqcAbnormal[];
  }

  export interface FqcPageReq extends PageParam {
    fqcNo?: string;
    workOrderNo?: string;
    machineCode?: string;
    materialCode?: string;
    productModel?: string;
    productBatchNo?: string;
    glueBoardModel?: string;
    gluePlateBatchNo?: string;
    processCategory?: string;
    sourceModule?: string;
    submissionType?: string;
    submitterName?: string;
    submissionTime?: string[];
    status?: string;
    judgment?: string;
    recheckFlag?: boolean;
    triggerReason?: string;
    qaTime?: string[];
  }

  export interface FqcStandard {
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

  export interface FqcSheetTemplate {
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

  export interface FqcImportResp {
    failureCount?: number;
    fileName?: string;
    importBatchNo?: string;
    previewOnly?: boolean;
    successCount?: number;
    status: string;
    totalCount?: number;
    validateSummary?: string;
    warningCount?: number;
    record?: FqcRecord;
    messages?: string[];
  }

  export interface FqcScanReq {
    scanCode: string;
    scanScene?:
      | 'ITEM_MODAL'
      | 'ITEM_OVERVIEW'
      | 'LEDGER_TOOLBAR'
      | 'WORKBENCH_HEADER';
    currentFqcId?: number;
    clientType?: string;
    terminalCode?: string;
  }

  export interface FqcScanCandidate {
    id: number;
    fqcNo: string;
    workOrderNo?: string;
    productModel?: string;
    productBatchNo?: string;
    machineCode?: string;
    status?: FqcRecord['status'];
    judgment?: Judgment;
    currentStepCode?: string;
    lastSaveTime?: string;
    createTime?: string;
  }

  export interface FqcScanResp {
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
    matchedFqcId?: number;
    matchedFqcNo?: string;
    matchedFqcItemId?: number;
    matchedStepCode?: string;
    candidateCount?: number;
    record?: FqcRecord;
    candidates?: FqcScanCandidate[];
    scanTime?: string;
  }

  export interface FqcAuditReq {
    id: number;
    auditResult: AuditResult;
    rejectReason?: string;
  }
}

function toUiJudgment(value?: string): MesFqcApi.Judgment {
  return !value || value === 'PENDING' ? '-' : (value as MesFqcApi.Judgment);
}

function toApiJudgment(value?: MesFqcApi.Judgment) {
  return !value || value === '-' ? 'PENDING' : value;
}

function buildEmptyRows(
  item: Pick<MesFqcApi.FqcItem, 'itemType' | 'sampleSize' | 'templateParams'>,
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

function buildValues(
  item: any,
  role: MesFqcApi.SampleRole,
  fallbackRole?: MesFqcApi.SampleRole,
) {
  let samples = (item.samples || [])
    .filter((sample: MesFqcApi.FqcSample) => sample.sampleRole === role)
    .toSorted(
      (a: MesFqcApi.FqcSample, b: MesFqcApi.FqcSample) =>
        a.sampleSeq - b.sampleSeq,
    );
  if (samples.length === 0 && fallbackRole) {
    samples = (item.samples || [])
      .filter(
        (sample: MesFqcApi.FqcSample) => sample.sampleRole === fallbackRole,
      )
      .toSorted(
        (a: MesFqcApi.FqcSample, b: MesFqcApi.FqcSample) =>
          a.sampleSeq - b.sampleSeq,
      );
  }
  if (samples.length === 0) return buildEmptyRows(item);
  return samples.map((sample: MesFqcApi.FqcSample) => {
    const base = {
      defectCode: sample.defectCode,
      defectName: sample.defectName,
      defects: sample.defects || [],
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

function normalizeDefects(row: any): MesFqcApi.FqcSampleDefect[] {
  if (!Array.isArray(row?.defects)) return [];
  return row.defects
    .filter((defect: any) => defect?.defectCode && defect?.defectName)
    .map((defect: any, index: number) => ({
      defectCodeId: defect.defectCodeId,
      defectCode: defect.defectCode,
      defectName: defect.defectName,
      defectLevel: defect.defectLevel,
      sort: defect.sort ?? (index + 1) * 10,
    }));
}

function resolveDataRuleResultCodes(item: MesFqcApi.FqcItem) {
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

function normalizeItem(item: any): MesFqcApi.FqcItem {
  return {
    ...item,
    qaResult: toUiJudgment(item.qaResult),
    qaValues: item.qaValues || buildValues(item, 'OPERATOR', 'QA'),
  };
}

function normalizeRecord(record: any): MesFqcApi.FqcRecord {
  return {
    ...record,
    materialName: record.materialName || record.productModel,
    productModel: record.productModel || record.materialName,
    qaInspector: record.qaInspectorName || record.qaInspector,
    judgment: toUiJudgment(record.judgment),
    items: (record.items || []).map((item: any) => normalizeItem(item)),
  };
}

function resolveRecordList(response: any): MesFqcApi.FqcRecord[] {
  if (Array.isArray(response)) return response;
  if (Array.isArray(response?.list)) return response.list;
  if (Array.isArray(response?.records)) return response.records;
  if (Array.isArray(response?.data)) return response.data;
  if (Array.isArray(response?.data?.list)) return response.data.list;
  if (Array.isArray(response?.data?.records)) return response.data.records;
  return [];
}

function buildSamples(
  item: MesFqcApi.FqcItem,
  role: MesFqcApi.SampleRole,
): MesFqcApi.FqcSample[] {
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
                  'defectCode',
                  'defectCodeIds',
                  'defectName',
                  'defects',
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
    const sampleResult =
      row.sampleResult ||
      (item.itemType === 'QUALITATIVE' ? qualitativeValue : undefined);
    const defects =
      sampleResult === 'NG' || qualitativeValue === 'NG'
        ? normalizeDefects(row)
        : [];
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
      sampleResult,
      sheetSectionCode: row.sheetSectionCode ?? item.sheetSectionCode,
      sheetMetricCode: row.sheetMetricCode ?? item.sheetMetricCode,
      sampleGroupNo: row.sampleGroupNo ?? index + 1,
      sampleAxis: row.sampleAxis,
      sampleColumnNo: row.sampleColumnNo,
      importBatchNo: row.importBatchNo,
      defectCode: defects[0]?.defectCode,
      defectName: defects[0]?.defectName,
      defects,
      remark: row.remark,
    };
  });
}

function toSubmitPayload(
  data: MesFqcApi.FqcRecord,
  entryLayout: MesFqcApi.FqcRecord['entryLayout'] = 'PROGRAM_FORM',
) {
  return {
    id: data.id,
    fqcNo: data.fqcNo,
    reportNo: data.reportNo || data.fqcNo || `FQC-MANUAL-${Date.now()}`,
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
    batchNo: data.batchNo || data.productBatchNo,
    produceQty: data.produceQty,
    sampleQty: data.sampleQty,
    glueBoardModel: data.glueBoardModel,
    gluePlateBatchNo: data.gluePlateBatchNo,
    processCategory: data.processCategory,
    sourceModule: data.sourceModule,
    submissionType: data.submissionType,
    triggerReason: data.triggerReason,
    standardId: data.standardId,
    standardNo: data.standardNo,
    standardVersion: data.standardVersion,
    status: data.status,
    judgment: toApiJudgment(data.judgment),
    submissionTime: data.submissionTime,
    submitterName: data.submitterName,
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
      samples: buildSamples(item, 'OPERATOR'),
    })),
    abnormals: data.abnormals,
  };
}

export async function getFqcPage(params: MesFqcApi.FqcPageReq) {
  const page = await requestClient.get<PageResult<MesFqcApi.FqcRecord>>(
    '/mes/quality/fqc/page',
    { params },
  );
  return {
    ...page,
    list: (page.list || []).map((item) => normalizeRecord(item)),
  };
}

export async function getPendingFqcTasks() {
  const response = await requestClient.get<MesFqcApi.FqcRecord[]>(
    '/mes/quality/fqc/pending-list',
  );
  return resolveRecordList(response).map((item) => normalizeRecord(item));
}

export async function getFqcDefectCodeOptions() {
  return requestClient.get<MesFqcApi.FqcDefectCodeOption[]>(
    '/mes/quality/fqc/defect-code-options',
  );
}

export async function resolveFqcScan(data: MesFqcApi.FqcScanReq) {
  const resp = await requestClient.post<MesFqcApi.FqcScanResp>(
    '/mes/quality/fqc/scan/resolve',
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
  existingItems?: MesFqcApi.FqcItem[],
) {
  if (existingItems && existingItems.length > 0)
    return existingItems.map((item) => normalizeItem(item));
  const standard = await requestClient.get<MesFqcApi.FqcStandard>(
    '/mes/quality/fqc/standard-by-product',
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

export async function getFqcDetail(id: number) {
  const record = await requestClient.get<MesFqcApi.FqcRecord>(
    `/mes/quality/fqc/get?id=${id}`,
  );
  return normalizeRecord(record);
}

export async function getFqcSheetTemplateList(productModel?: string) {
  return requestClient.get<MesFqcApi.FqcSheetTemplate[]>(
    '/mes/quality/fqc/sheet-template/list',
    {
      params: { productModel },
    },
  );
}

export async function getFqcSheetTemplate(id: number) {
  return requestClient.get<MesFqcApi.FqcSheetTemplate>(
    `/mes/quality/fqc/sheet-template/get?id=${id}`,
  );
}

export async function selectFqcSheetTemplate(id: number, templateId: number) {
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    `/mes/quality/fqc/select-sheet-template?id=${id}&templateId=${templateId}`,
  );
  return normalizeRecord(record);
}

export async function importFqcOriginSheet(
  id: number,
  templateId: number,
  file: File,
  mode = 'OVERWRITE',
) {
  const resp = await requestClient.upload<MesFqcApi.FqcImportResp>(
    '/mes/quality/fqc/import-origin-sheet',
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

export function downloadFqcItemTemplate(id: number) {
  return requestClient.download('/mes/quality/fqc/item-template/download', {
    params: { id },
  });
}

export function exportFqcItemValues(id: number) {
  return requestClient.download('/mes/quality/fqc/item-export', {
    params: { id },
  });
}

export async function previewFqcItemImport(id: number, file: File) {
  return requestClient.upload<MesFqcApi.FqcImportResp>(
    '/mes/quality/fqc/item-import/preview',
    { file, id },
  );
}

export async function confirmFqcItemImport(
  id: number,
  file: File,
  allowOverwrite = false,
) {
  const resp = await requestClient.upload<MesFqcApi.FqcImportResp>(
    '/mes/quality/fqc/item-import/confirm',
    { allowOverwrite, file, id },
  );
  return {
    ...resp,
    record: resp.record ? normalizeRecord(resp.record) : undefined,
  };
}

export async function saveFqcSheetEntry(data: MesFqcApi.FqcRecord) {
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    '/mes/quality/fqc/save-sheet-entry',
    toSubmitPayload(data, 'SHEET_GRID'),
  );
  return normalizeRecord(record);
}

export async function saveFqcProgramEntry(data: MesFqcApi.FqcRecord) {
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    '/mes/quality/fqc/program-entry/save',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function recalculateFqcProgramEntry(data: MesFqcApi.FqcRecord) {
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    '/mes/quality/fqc/program-entry/recalculate',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function auditFqcProgramEntryItem(data: MesFqcApi.FqcRecord) {
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    '/mes/quality/fqc/program-entry/item-audit',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function submitFqcProgramEntry(data: MesFqcApi.FqcRecord) {
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    '/mes/quality/fqc/program-entry/submit',
    toSubmitPayload(data),
  );
  return normalizeRecord(record);
}

export async function auditFqcProgramEntry(data: MesFqcApi.FqcAuditReq) {
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    '/mes/quality/fqc/program-entry/audit',
    data,
  );
  return normalizeRecord(record);
}

export async function returnFqcProgramEntry(data: MesFqcApi.FqcRecord) {
  const payload = toSubmitPayload(data);
  const record = await requestClient.put<MesFqcApi.FqcRecord>(
    '/mes/quality/fqc/program-entry/return',
    {
      id: data.id,
      currentStepCode: data.currentStepCode,
      lastReturnReason: data.lastReturnReason,
      items: payload.items,
    },
  );
  return normalizeRecord(record);
}

export async function createFqcRecord(data: MesFqcApi.FqcRecord) {
  const payload = {
    ...data,
    batchNo: data.batchNo || data.productBatchNo || '',
    reportNo: data.reportNo || `FQC-MANUAL-${Date.now()}`,
  };
  delete payload.items;
  delete payload.abnormals;
  delete (payload as Record<string, any>).wetSampleType;
  const id = await requestClient.post<number>(
    '/mes/quality/fqc/create',
    payload,
  );
  return getFqcDetail(id);
}

export async function getGlueBoardFqcPage(params: MesFqcApi.FqcPageReq) {
  return getFqcPage(params);
}

export async function getPendingGlueBoardFqcTasks() {
  return getPendingFqcTasks();
}

export async function getGlueBoardFqcDetail(id: number) {
  return getFqcDetail(id);
}

export async function saveGlueBoardFqcProgramEntry(data: MesFqcApi.FqcRecord) {
  return saveFqcProgramEntry(data);
}

export async function recalculateGlueBoardFqcProgramEntry(
  data: MesFqcApi.FqcRecord,
) {
  return recalculateFqcProgramEntry(data);
}

export async function submitGlueBoardFqcProgramEntry(
  data: MesFqcApi.FqcRecord,
) {
  return submitFqcProgramEntry(data);
}

export async function auditGlueBoardFqcProgramEntry(
  data: MesFqcApi.FqcAuditReq,
) {
  return auditFqcProgramEntry(data);
}

export async function createGlueBoardFqcRecord(data: Partial<MesFqcApi.FqcRecord>) {
  return createFqcRecord(data as MesFqcApi.FqcRecord);
}

export function downloadGlueBoardFqcItemTemplate(id: number) {
  return downloadFqcItemTemplate(id);
}

export function exportGlueBoardFqcItemValues(id: number) {
  return exportFqcItemValues(id);
}

export async function suspendFqcRecord(id: number) {
  return requestClient.put<boolean>(`/mes/quality/fqc/suspend?id=${id}`);
}
