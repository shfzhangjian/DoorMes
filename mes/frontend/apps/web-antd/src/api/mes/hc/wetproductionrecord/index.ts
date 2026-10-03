import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcWetProductionRecordApi {
  export interface WetProductionRecord {
    dataSource?: string;
    id?: number;
    recordDate?: string;
    modelCode?: string;
    padType?: 'BLACK_PAD' | 'WHITE_PAD';
    materialCode?: string;
    batchNo?: string;
    inputKg?: number;
    outputMeter?: number;
    petModel?: string;
    petBatchNo?: string;
    guideClothBatchNo?: string;
    guideClothUseCount?: number;
    guideClothChanged?: string;
    changeDesc?: string;
    recorderName?: string;
    recordTime?: string;
    confirmerName?: string;
    confirmTime?: string;
    status?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }
}

const baseUrl = '/mes/hc/execution/wet-production-record';

export async function getWetProductionRecordPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<
    PageResult<MesHcWetProductionRecordApi.WetProductionRecord>
  >(`${baseUrl}/page`, { params });
}

export async function exportWetProductionRecord(params: Record<string, any>) {
  return requestClient.download(`${baseUrl}/export-excel`, { params });
}
