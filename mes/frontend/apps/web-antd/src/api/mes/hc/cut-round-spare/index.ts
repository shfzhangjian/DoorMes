import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcCutRoundSpareApi {
  export interface Spare {
    createTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    id?: number;
    lastEventTime?: string;
    lastOperatorId?: number;
    lastOperatorName?: string;
    lastReplaceReason?: string;
    lastReplaceTime?: string;
    limitCount?: number;
    limitDays?: number;
    remark?: string;
    spareType?: string;
    status?: string;
    useCount?: number;
    warningFlag?: number;
    workCenterCode?: string;
    workCenterId?: number;
    workCenterName?: string;
  }

  export interface Record {
    ledgerId?: number;
    consumeId?: number;
    replaceQuantity?: number;
    afterBatchNo?: string;
    afterUseCount?: number;
    beforeUseCount?: number;
    bizId?: number;
    bizType?: string;
    changeUseCount?: number;
    cutInputPcs?: number;
    cutOutputPcs?: number;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    eventTime?: string;
    eventType?: string;
    feltUseDays?: number;
    id?: number;
    operationName?: string;
    operatorId?: number;
    operatorName?: string;
    planNo?: string;
    productionBatchNo?: string;
    modelCode?: string;
    cutSizeMm?: string;
    recordGroupNo?: string;
    recordSource?: string;
    remark?: string;
    replaceReason?: string;
    spareId?: number;
    spareType?: string;
  }

  export interface SaveReq extends Spare {
    correctHistory?: boolean;
    operatorId?: number;
    operatorName?: string;
    replaceReason?: string;
  }

  export interface RndConsumeReq {
    consumeTime: string;
    cutInputPcs: number;
    cutOutputPcs: number;
    cutSizeMm: string;
    equipmentId: number;
    modelCode: string;
    padType: 'BLACK_PAD' | 'WHITE_PAD';
    productionBatchNo: string;
    remark: string;
  }

  export interface StateImportResp {
    clearedRecordCount?: number;
    clearedStateCount?: number;
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    successCount?: number;
    totalRows?: number;
  }
}

const BASE = '/mes/hc/base/cut-round-spare';

export function getCutRoundSparePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcCutRoundSpareApi.Spare>>(
    `${BASE}/page`,
    { params },
  );
}

export function getCutRoundSpareRecordPage(
  params: PageParam & Record<string, any>,
) {
  return requestClient.get<PageResult<MesHcCutRoundSpareApi.Record>>(
    `${BASE}/record-page`,
    { params },
  );
}

export function getCutRoundSpare(id: number) {
  return requestClient.get<MesHcCutRoundSpareApi.Spare>(`${BASE}/get?id=${id}`);
}

export function saveCutRoundSpare(data: MesHcCutRoundSpareApi.SaveReq) {
  return requestClient.post<number>(`${BASE}/save`, data);
}

export function saveCutRoundSpareRndConsume(
  data: MesHcCutRoundSpareApi.RndConsumeReq,
) {
  return requestClient.post<string>(`${BASE}/rnd-consume`, data);
}

export function exportCutRoundSpare(params: Record<string, any>) {
  return requestClient.download(`${BASE}/export-excel`, { params });
}

export function importCutRoundSpare(file: File, confirmClear = true) {
  return requestClient.upload<MesHcCutRoundSpareApi.StateImportResp>(
    `${BASE}/import`,
    {
      confirmClear,
      file,
    },
  );
}
