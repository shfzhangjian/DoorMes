import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcLotRuleApi {
  export interface LotRule {
    ruleCode?: string;
    ruleName?: string;
    bizType?: string;
    productCategoryCode?: string;
    prodType?: string;
    modelMatchMode?: string;
    modelMatchValue?: string;
    priority?: number;
    versionNo?: number;
    counterGroupCode?: string;
    ruleConfigJson?: string;
    generationTrigger?: string;
    generationScope?: string;
    batchCardinality?: string;
    displayPolicy?: string;
    ruleMode?: string;
    prefix?: string;
    dateFormat?: string;
    yearCodeMode?: string;
    monthCodeMode?: string;
    seqLength?: number;
    seqStart?: number;
    seqStep?: number;
    resetCycle?: string;
    sampleSegmentRule?: string;
    status?: number;
    remark?: string;
    allowPreview?: boolean;
    allowParse?: boolean;
    allowManualOverride?: boolean;
    id?: number;
    lotRuleSegments?: LotRuleSegment[];
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

  export interface LotRuleSegment {
    ruleId?: number;
    ruleCode?: string;
    segmentCode?: string;
    segmentName?: string;
    segmentType?: string;
    segmentValue?: string;
    segmentLength?: number;
    counterType?: string;
    counterDimensionExpr?: string;
    sourceField?: string;
    consumeCounter?: boolean;
    valuePolicy?: string;
    sort?: number;
    delimiter?: string;
    enabled?: boolean;
    id?: number;
  }

  export interface LotRuleCounter {
    id?: number;
    ruleId?: number;
    ruleCode?: string;
    ruleName?: string;
    counterType?: string;
    bizDimensionKey?: string;
    bizDimensionJson?: string;
    counterKey?: string;
    resetKey?: string;
    currentSeq?: number;
    currentValue?: number;
    statisticalYear?: number;
    nextSeq?: number;
    nextLotNo?: string;
    initialized?: boolean;
    warning?: string;
    lastLotNo?: string;
    createTime?: number | string;
    updateTime?: number | string;
  }

  export interface LotRuleCounterSaveReq {
    id?: number;
    ruleId?: number;
    ruleCode?: string;
    counterType?: string;
    bizDimensionKey?: string;
    bizDimensionJson?: string;
    counterKey: string;
    resetKey: string;
    currentSeq: number;
    lastLotNo?: string;
  }

  export interface LotRuleCounterInitializeReq {
    ruleId?: number;
    ruleCode: string;
    counterType?: string;
    year: number;
    bizDimensionKey?: string;
    currentSeq: number;
    lastLotNo?: string;
    reason: string;
  }

  export interface LotRuleCounterAdjustReq {
    counterId: number;
    oldCurrentSeq: number;
    newCurrentSeq: number;
    lastLotNo?: string;
    reason: string;
  }

  export interface LotRuleCounterPreviewReq {
    ruleId?: number;
    ruleCode: string;
    counterType?: string;
    bizDate?: string;
    year?: number;
    bizDimensionKey?: string;
    currentSeq?: number;
    typeCode?: string;
    batchLineCode?: string;
  }

  export interface LotRuleCounterPreviewResp {
    ruleId?: number;
    ruleCode?: string;
    ruleName?: string;
    counterType?: string;
    bizDimensionKey?: string;
    counterKey?: string;
    resetKey?: string;
    currentSeq?: number;
    nextSeq?: number;
    maxUsedSeq?: number;
    nextLotNo?: string;
    warning?: string;
  }

  export interface GenerateReq {
    ruleId?: number;
    ruleCode?: string;
    ruleName?: string;
    bizDate?: string;
    ruleMode?: string;
    yearCodeMode?: string;
    monthCodeMode?: string;
    seqLength?: number;
    seqStart?: number;
    seqStep?: number;
    resetCycle?: string;
    sampleSegmentRule?: string;
    consumeSequence?: boolean;
    lotRuleSegments?: LotRuleSegment[];
    inputValues: Record<string, string>;
  }

  export interface GenerateResp {
    lotNo: string;
    currentSeq?: number;
    nextSeq?: number;
    segmentValues?: Record<string, string>;
  }

  export interface ParseReq {
    ruleId?: number;
    ruleCode?: string;
    ruleName?: string;
    seqLength?: number;
    yearCodeMode?: string;
    monthCodeMode?: string;
    lotRuleSegments?: LotRuleSegment[];
    lotNo: string;
  }

  export interface ParseResp {
    lotNo: string;
    segmentValues?: Record<string, string>;
  }

  export interface MatchReq {
    bizType: string;
    productCategoryCode?: string;
    prodType?: string;
    generationTrigger: string;
    generationScope: string;
    modelCode?: string;
  }

  export interface MatchResp {
    ruleId: number;
    ruleCode: string;
    ruleName: string;
    versionNo?: number;
    priority?: number;
    message?: string;
  }
}

export async function getLotRulePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcLotRuleApi.LotRule>>('/mes/hc/base/lot-rule/page', { params });
}

export async function getLotRuleCounterPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcLotRuleApi.LotRuleCounter>>('/mes/hc/base/lot-rule/counter/page', { params });
}

export async function getLotRule(id: number) {
  return requestClient.get<MesHcLotRuleApi.LotRule>(`/mes/hc/base/lot-rule/get?id=${id}`);
}

export async function getLotRuleDetail(id: number) {
  return requestClient.get<MesHcLotRuleApi.LotRule>(`/mes/hc/base/lot-rule/get-detail?id=${id}`);
}

export async function createLotRule(data: MesHcLotRuleApi.LotRule) {
  return requestClient.post<number>('/mes/hc/base/lot-rule/create', data);
}

export async function updateLotRule(data: MesHcLotRuleApi.LotRule) {
  return requestClient.put<boolean>('/mes/hc/base/lot-rule/update', data);
}

export async function deleteLotRule(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/lot-rule/delete?id=${id}`);
}

export async function deleteLotRuleList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/lot-rule/delete-list?ids=${ids.join(',')}`);
}

export async function publishLotRule(id: number) {
  return requestClient.post<boolean>(`/mes/hc/base/lot-rule/publish?id=${id}`);
}

export async function disableLotRule(id: number) {
  return requestClient.post<boolean>(`/mes/hc/base/lot-rule/disable?id=${id}`);
}

export async function copyLotRuleAsNewVersion(id: number) {
  return requestClient.post<number>(`/mes/hc/base/lot-rule/copy-new-version?id=${id}`);
}

export async function matchLotRule(data: MesHcLotRuleApi.MatchReq) {
  return requestClient.post<MesHcLotRuleApi.MatchResp>('/mes/hc/base/lot-rule/match', data);
}

export async function exportLotRule(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/lot-rule/export-excel', { params });
}

export async function getLotRuleSimpleList() {
  return requestClient.get<MesHcLotRuleApi.SimpleItem[]>('/mes/hc/base/lot-rule/simple-list');
}

export async function getLotRuleSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/lot-rule/simple-map');
}

export async function getLotRuleSelectOptions() {
  return requestClient.get<MesHcLotRuleApi.SelectOption[]>('/mes/hc/base/lot-rule/select-options');
}

export async function getLotRuleSegmentListByParentId(parentId: number) {
  return requestClient.get<MesHcLotRuleApi.LotRuleSegment[]>(`/mes/hc/base/lot-rule/mes_md_lot_rule_segment/list-by-parent-id?parentId=${parentId}`);
}

export async function getLotRuleCounterListByRuleId(ruleId: number) {
  return requestClient.get<MesHcLotRuleApi.LotRuleCounter[]>(`/mes/hc/base/lot-rule/counter/list-by-rule-id?ruleId=${ruleId}`);
}

export async function getLotRuleCounterSummaryList(ruleId: number, year?: number) {
  return requestClient.get<MesHcLotRuleApi.LotRuleCounter[]>('/mes/hc/base/lot-rule/counter/summary-list', {
    params: { ruleId, year },
  });
}

export async function createLotRuleCounter(data: MesHcLotRuleApi.LotRuleCounterSaveReq) {
  return requestClient.post<number>('/mes/hc/base/lot-rule/counter/create', data);
}

export async function updateLotRuleCounter(data: MesHcLotRuleApi.LotRuleCounterSaveReq) {
  return requestClient.put<boolean>('/mes/hc/base/lot-rule/counter/update', data);
}

export async function deleteLotRuleCounter(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/lot-rule/counter/delete?id=${id}`);
}

export async function initializeLotRuleCounter(data: MesHcLotRuleApi.LotRuleCounterInitializeReq) {
  return requestClient.post<MesHcLotRuleApi.LotRuleCounter>('/mes/hc/base/lot-rule/counter/initialize', data);
}

export async function adjustLotRuleCounter(data: MesHcLotRuleApi.LotRuleCounterAdjustReq) {
  return requestClient.post<MesHcLotRuleApi.LotRuleCounter>('/mes/hc/base/lot-rule/counter/adjust', data);
}

export async function previewNextLotRuleCounter(params: MesHcLotRuleApi.LotRuleCounterPreviewReq) {
  return requestClient.get<MesHcLotRuleApi.LotRuleCounterPreviewResp>('/mes/hc/base/lot-rule/counter/preview-next', { params });
}

export async function generateLotNo(data: MesHcLotRuleApi.GenerateReq) {
  return requestClient.post<MesHcLotRuleApi.GenerateResp>('/mes/hc/base/lot-rule/generate', data);
}

export async function parseLotNo(data: MesHcLotRuleApi.ParseReq) {
  return requestClient.post<MesHcLotRuleApi.ParseResp>('/mes/hc/base/lot-rule/parse', data);
}
