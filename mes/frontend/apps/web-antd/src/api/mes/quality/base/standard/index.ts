import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQualityStandardApi {
  export interface EntryRuleInputField {
    code: string;
    name: string;
    unit?: string;
    type?: 'NUMBER' | 'TEXT';
    required?: boolean;
    precision?: number;
  }

  export interface EntryRuleResultField {
    code: string;
    name: string;
    formula: string;
    unit?: string;
    judgment?: boolean;
    precision?: number;
  }

  export interface EntryRuleDataRule {
    inputFields?: EntryRuleInputField[];
    resultFields?: EntryRuleResultField[];
    judgmentMetric?: string;
  }

  export interface StandardItem {
    id?: number;
    standardId?: number;
    sort?: number;
    inspectionItem: string;
    itemType: 'DATE' | 'QUALITATIVE' | 'QUANTITATIVE';
    expiryDays?: number;
    attachmentEnabled?: boolean;
    actualValueRequired?: boolean;
    targetValue?: number;
    maxValue?: number;
    maxValueScale?: number;
    minValue?: number;
    minValueScale?: number;
    standardDesc: string;
    unit?: string;
    inspectionMethod?: string;
    testFrequencyJudgement?: string;
    entryRuleType?: 'CUSTOM' | 'PRESET';
    entryRuleTemplateId?: number;
    entryRuleTemplateName?: string;
    ruleDescription?: string;
    valueTemplate?: 'COMPRESSION_CALC' | 'DENSITY_CALC' | 'SINGLE_VALUE';
    judgmentMetric?:
      | 'COMPRESSION_ELASTICITY_RATE'
      | 'COMPRESSION_RATE'
      | 'DENSITY_VALUE'
      | 'RESULT_VALUE'
      | string;
    templateParams?: string;
    avgMinLimit?: number;
    avgMinLimitScale?: number;
    avgMaxLimit?: number;
    avgMaxLimitScale?: number;
    stdMinLimit?: number;
    stdMinLimitScale?: number;
    stdMaxLimit?: number;
    stdMaxLimitScale?: number;
    sheetTemplateId?: number;
    sheetSectionCode?: string;
    sheetMetricCode?: string;
    sheetFieldCode?: string;
    processId?: number;
    processCode?: string;
    processName?: string;
    testTool?: string;
    sampleSize: number;
    isSpc: boolean;
  }

  export interface Standard {
    id?: number;
    standardNo?: string;
    standardName: string;
    applyScope?: Array<'MATERIAL' | 'PROCESS'>;
    iqcScopeType?: 'MATERIAL' | 'UNIVERSAL';
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    prodType?: string;
    prodTypeName?: string;
    glueBoardModel?: string;
    processId?: number;
    processCode?: string;
    processName?: string;
    version: string;
    applyType: 'FAI' | 'FQC' | 'GLUE_BOARD_FAI' | 'IPQC' | 'IQC' | 'OQC';
    status: number;
    auditStatus?: number;
    auditorId?: number;
    auditorName?: string;
    auditTime?: string;
    remark?: string;
    createTime?: string;
    items?: StandardItem[];
  }

  export interface StandardPageReq extends PageParam {
    standardName?: string;
    materialCode?: string;
    materialName?: string;
    glueBoardModel?: string;
    productModelCode?: string;
    productModelKeyword?: string;
    processId?: number;
    processKeyword?: string;
    applyType?: string;
    status?: number;
    auditStatus?: number;
  }

  export interface AuditReq {
    id: number;
    auditResult: 'PASS' | 'REJECT';
    rejectReason?: string;
  }

  export interface EntryRuleTemplate {
    id?: number;
    templateName: string;
    itemType: 'DATE' | 'QUALITATIVE' | 'QUANTITATIVE';
    testFrequencyJudgement: string;
    templateParams: string;
    ruleDescription?: string;
    sampleSize: number;
    status?: number;
    createTime?: string;
  }

  export interface ChangeLog {
    id: number;
    standardId: number;
    applyType: 'FAI' | 'FQC' | 'GLUE_BOARD_FAI' | 'IPQC' | 'IQC' | 'OQC';
    changeScope: 'ITEM' | 'MAIN';
    itemKey?: string;
    itemLabel?: string;
    fieldName: string;
    fieldLabel: string;
    beforeValue?: string;
    afterValue?: string;
    operatorId?: number;
    operatorName?: string;
    changeTime: string;
  }
}

function getStandardBaseUrl(applyType?: string) {
  const scope = applyType?.toLowerCase().replace(/_/g, '-');
  return scope
    ? `/mes/quality/base/standard/${scope}`
    : '/mes/quality/base/standard';
}

export function getStandardPage(
  params: MesQualityStandardApi.StandardPageReq,
  applyType?: string,
) {
  return requestClient.get<PageResult<MesQualityStandardApi.Standard>>(
    `${getStandardBaseUrl(applyType)}/page`,
    { params },
  );
}

export function getStandard(id: number, applyType?: string) {
  return requestClient.get<MesQualityStandardApi.Standard>(
    `${getStandardBaseUrl(applyType)}/get?id=${id}`,
  );
}

export function getStandardChangeLogs(standardId: number, applyType?: string) {
  return requestClient.get<MesQualityStandardApi.ChangeLog[]>(
    `${getStandardBaseUrl(applyType)}/change-log`,
    { params: { standardId, applyType } },
  );
}

export function createStandard(
  data: MesQualityStandardApi.Standard,
  applyType?: string,
) {
  return requestClient.post<number>(
    `${getStandardBaseUrl(applyType)}/create`,
    data,
  );
}

export function updateStandard(
  data: MesQualityStandardApi.Standard,
  applyType?: string,
) {
  return requestClient.put<boolean>(
    `${getStandardBaseUrl(applyType)}/update`,
    data,
  );
}

export function auditStandard(
  data: MesQualityStandardApi.AuditReq,
  applyType?: string,
) {
  return requestClient.put<boolean>(
    `${getStandardBaseUrl(applyType)}/audit`,
    data,
  );
}

export function deleteStandard(id: number, applyType?: string) {
  return requestClient.delete<boolean>(
    `${getStandardBaseUrl(applyType)}/delete?id=${id}`,
  );
}

export function deleteStandardList(ids: number[], applyType?: string) {
  return requestClient.delete<boolean>(
    `${getStandardBaseUrl(applyType)}/delete-list?ids=${ids.join(',')}`,
  );
}

export function exportStandard(
  params: Record<string, any>,
  applyType?: string,
) {
  return requestClient.download(
    `${getStandardBaseUrl(applyType)}/export-excel`,
    { params },
  );
}

export function getEntryRuleTemplateList(params?: { itemType?: string }) {
  return requestClient.get<MesQualityStandardApi.EntryRuleTemplate[]>(
    '/mes/quality/base/standard/entry-rule-template/list',
    { params },
  );
}

export function createEntryRuleTemplate(
  data: MesQualityStandardApi.EntryRuleTemplate,
) {
  return requestClient.post<number>(
    '/mes/quality/base/standard/entry-rule-template/create',
    data,
  );
}

export function updateEntryRuleTemplate(
  data: MesQualityStandardApi.EntryRuleTemplate,
) {
  return requestClient.put<boolean>(
    '/mes/quality/base/standard/entry-rule-template/update',
    data,
  );
}

export function deleteEntryRuleTemplate(id: number) {
  return requestClient.delete<boolean>(
    `/mes/quality/base/standard/entry-rule-template/delete?id=${id}`,
  );
}
