import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcModelRuleApi {
  export interface ModelRule {
    ruleCode?: string;
    ruleName?: string;
    ruleCategory?: string;
    targetLevel?: string;
    ruleDesc?: string;
    effectiveVersion?: string;
    status?: number;
    id: number;
    modelRuleItems?: ModelRuleItem[];
    modelRuleDicts?: ModelRuleDict[];
  }

  export interface SimpleItem {
    id: number;
    code?: string;
    name?: string;
    status?: number;
  }

  export interface SelectOption {
    value: number;
    label: string;
    code?: string;
    status?: number;
  }

  export interface ModelRuleItem {
    ruleId?: number;
    ruleCode?: string;
    itemCode?: string;
    itemName?: string;
    parseType?: string;
    segmentLength?: number;
    fixedValue?: string;
    dataSourceType?: string;
    sort?: number;
    requiredFlag?: boolean;
    id: number;
  }

  export interface ModelRuleDict {
    ruleId?: number;
    ruleItemId?: number;
    itemCode?: string;
    dictCode?: string;
    dictValue?: string;
    extAttrJson?: string;
    sort?: number;
    enabled?: boolean;
    id: number;
  }

  export interface GenerateCodeReq {
    modelRuleItems: ModelRuleItem[];
    modelRuleDicts?: ModelRuleDict[];
    testValues?: Record<string, string>;
  }

  export interface GenerateCodeResp {
    generatedCode: string;
  }
}

export async function getModelRulePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcModelRuleApi.ModelRule>>('/mes/hc/base/model-rule/page', { params });
}

export async function getModelRule(id: number) {
  return requestClient.get<MesHcModelRuleApi.ModelRule>(`/mes/hc/base/model-rule/get?id=${id}`);
}

export async function getModelRuleDetail(id: number) {
  return requestClient.get<MesHcModelRuleApi.ModelRule>(`/mes/hc/base/model-rule/get-detail?id=${id}`);
}

export async function createModelRule(data: MesHcModelRuleApi.ModelRule) {
  return requestClient.post<number>('/mes/hc/base/model-rule/create', data);
}

export async function updateModelRule(data: MesHcModelRuleApi.ModelRule) {
  return requestClient.put<boolean>('/mes/hc/base/model-rule/update', data);
}

export async function deleteModelRule(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/model-rule/delete?id=${id}`);
}

export async function deleteModelRuleList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/model-rule/delete-list?ids=${ids.join(',')}`);
}

export async function exportModelRule(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/model-rule/export-excel', { params });
}

export async function getModelRuleSimpleList() {
  return requestClient.get<MesHcModelRuleApi.SimpleItem[]>('/mes/hc/base/model-rule/simple-list');
}

export async function getModelRuleSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/model-rule/simple-map');
}

export async function getModelRuleSelectOptions() {
  return requestClient.get<MesHcModelRuleApi.SelectOption[]>('/mes/hc/base/model-rule/select-options');
}

export async function getModelRuleItemListByParentId(parentId: number) {
  return requestClient.get<MesHcModelRuleApi.ModelRuleItem[]>(`/mes/hc/base/model-rule/mes_md_model_rule_item/list-by-parent-id?parentId=${parentId}`);
}

export async function getModelRuleDictListByParentId(parentId: number) {
  return requestClient.get<MesHcModelRuleApi.ModelRuleDict[]>(`/mes/hc/base/model-rule/mes_md_model_rule_dict/list-by-parent-id?parentId=${parentId}`);
}

export async function generateModelRuleCode(data: MesHcModelRuleApi.GenerateCodeReq) {
  return requestClient.post<MesHcModelRuleApi.GenerateCodeResp>(
    '/mes/hc/base/model-rule/generate-code',
    data,
  );
}
