import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmPerformanceSupplierConfigApi {
  export type ConfigStatus = 'DISABLED' | 'ENABLED';
  export type IndicatorType = 'CALCULATED_SCORE' | 'MANUAL_SCORE' | 'MIXED';

  export interface CalcNode {
    id?: number;
    ruleId?: number;
    parentNodeId?: number;
    nodeKey?: string;
    nodeName?: string;
    nodeType?: 'CONSTANT' | 'FORMULA' | 'RESULT' | 'SOURCE_METRIC';
    sourceMetricCode?: string;
    sourceMetricName?: string;
    operator?: string;
    aggregateMethod?: 'AVG' | 'MAX' | 'MIN' | 'SUM';
    unit?: string;
    sortNo?: number;
    requiredFlag?: boolean;
  }

  export interface CalcRule {
    id?: number;
    configId?: number;
    configItemId?: number;
    templateItemId?: number;
    ruleCode?: string;
    ruleName?: string;
    formulaExpr?: string;
    scoreFormulaExpr?: string;
    periodScope?: string;
    aggregateMethod?: 'AVG' | 'MAX' | 'MIN' | 'SUM';
    missingPolicy?: 'BLOCK' | 'ZERO';
    enabled?: boolean;
    remark?: string;
    nodes?: CalcNode[];
  }

  export interface Item {
    id?: number;
    configId?: number;
    templateId?: number;
    templateVersionId?: number;
    templateItemId?: number;
    groupCodeSnapshot?: string;
    groupNameSnapshot?: string;
    groupSort?: number;
    groupMaxScoreSnapshot?: number;
    indicatorCodeSnapshot?: string;
    indicatorNameSnapshot?: string;
    indicatorSort?: number;
    scoringRuleSnapshot?: string;
    maxScoreSnapshot?: number;
    defaultDeptNames?: string;
    indicatorType?: IndicatorType;
    targetValue?: number;
    targetUnit?: string;
    redlineScore?: number;
    scorerCandidateUserIds?: string;
    scorerCandidateUserNames?: string;
    scorerUserId?: number;
    scorerUserName?: string;
    reporterCandidateUserIds?: string;
    reporterCandidateUserNames?: string;
    reporterUserId?: number;
    reporterUserName?: string;
    calcRuleId?: number;
    remark?: string;
    calcRule?: CalcRule;
  }

  export interface Config {
    id?: number;
    configNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    supplierSourceType?: string;
    currentTemplateId?: number;
    currentTemplateVersionId?: number;
    templateCodeSnapshot?: string;
    templateNameSnapshot?: string;
    templateVersionNoSnapshot?: string;
    status?: ConfigStatus;
    remark?: string;
    version?: number;
    items?: Item[];
    createTime?: string;
    updateTime?: string;
  }

  export interface ItemConfigSaveReq {
    configId: number;
    templateVersionId: number;
    items: Array<{
      calcRule?: CalcRule;
      indicatorType?: IndicatorType;
      redlineScore?: number;
      remark?: string;
      reporterCandidateUserIds?: number[];
      reporterUserId?: number;
      scorerCandidateUserIds?: number[];
      scorerUserId?: number;
      targetUnit?: string;
      targetValue?: number;
      templateItemId: number;
    }>;
  }
}

export function getSupplierConfigPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmPerformanceSupplierConfigApi.Config>>(
    '/mes/srm/performance-supplier-config/page',
    { params },
  );
}

export function getEnabledSupplierConfigList() {
  return requestClient.get<SrmPerformanceSupplierConfigApi.Config[]>(
    '/mes/srm/performance-supplier-config/enabled-list',
  );
}

export function getSupplierConfigDetail(id: number) {
  return requestClient.get<SrmPerformanceSupplierConfigApi.Config>(
    '/mes/srm/performance-supplier-config/get',
    { params: { id } },
  );
}

export function createSupplierConfig(
  data: SrmPerformanceSupplierConfigApi.Config,
) {
  return requestClient.post<number>(
    '/mes/srm/performance-supplier-config/create',
    data,
  );
}

export function updateSupplierConfig(
  data: SrmPerformanceSupplierConfigApi.Config,
) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-supplier-config/update',
    data,
  );
}

export function deleteSupplierConfig(id: number) {
  return requestClient.delete<boolean>(
    '/mes/srm/performance-supplier-config/delete',
    { params: { id } },
  );
}

export function getSupplierItemConfig(
  configId: number,
  templateVersionId: number,
) {
  return requestClient.get<SrmPerformanceSupplierConfigApi.Config>(
    '/mes/srm/performance-supplier-config/item-config',
    { params: { configId, templateVersionId } },
  );
}

export function saveSupplierItemConfig(
  data: SrmPerformanceSupplierConfigApi.ItemConfigSaveReq,
) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-supplier-config/item-config',
    data,
  );
}
