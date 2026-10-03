import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcLotInstanceApi {
  export interface LotInstance {
    id: number;
    lotNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    parentLotNo?: string;
    batchLevel?: string;
    instanceStatus?: string;
    generateSource?: string;
    batchStage?: string;
    ruleId?: number;
    ruleCode?: string;
    ruleName?: string;
    ruleVersion?: number;
    bizType?: string;
    productCategoryCode?: string;
    prodType?: string;
    modelCode?: string;
    ruleFormatSummary?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    operationCode?: string;
    operationName?: string;
    materialCode?: string;
    materialName?: string;
    lineCode?: string;
    lineName?: string;
    batchLineCode?: string;
    yearCode?: string;
    monthCode?: string;
    annualBatchSeq?: number;
    generationTrigger?: string;
    generationScope?: string;
    operatorName?: string;
    bizDate?: string;
    generatedTime?: string;
    createTime?: string;
    ruleFormatSnapshot?: Record<string, any>;
    segmentValues?: Record<string, any>;
    generationContext?: Record<string, any>;
    productionAttributes?: Record<string, any>;
  }
}

export async function getLotInstancePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcLotInstanceApi.LotInstance>>('/mes/hc/base/lot-instance/page', { params });
}

export async function getLotInstanceDetail(id: number) {
  return requestClient.get<MesHcLotInstanceApi.LotInstance>(`/mes/hc/base/lot-instance/get-detail?id=${id}`);
}
