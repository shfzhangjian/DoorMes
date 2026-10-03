import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

import { normalizePressSlotSpareSavePayload } from './payload';

export namespace MesHcPressSlotSpareApi {
  export interface Spare {
    availableQuantity?: number;
    batchNo?: string;
    createTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    id?: number;
    lastCleanRemark?: string;
    lastCleanTime?: string;
    lastEventTime?: string;
    lastOperatorId?: number;
    lastOperatorName?: string;
    lastReplaceReason?: string;
    lastReplaceTime?: string;
    limitCount?: number;
    limitDays?: number;
    materialCode?: string;
    materialName?: string;
    onlineQuantity?: number;
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
    afterAvailableQuantity?: number;
    afterBatchNo?: string;
    afterMaterialCode?: string;
    afterUseCount?: number;
    beforeAvailableQuantity?: number;
    beforeBatchNo?: string;
    beforeMaterialCode?: string;
    beforeUseCount?: number;
    bizId?: number;
    bizType?: string;
    changeQuantity?: number;
    changeUseCount?: number;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    eventTime?: string;
    eventType?: string;
    id?: number;
    operationName?: string;
    operatorId?: number;
    operatorName?: string;
    planNo?: string;
    prodType?: string;
    productMaterialCode?: string;
    productMaterialName?: string;
    productionBatchNo?: string;
    pressSlotInputPcs?: number;
    pressSlotOutputPcs?: number;
    recordGroupNo?: string;
    recordSource?: string;
    remark?: string;
    replaceReason?: string;
    spareId?: number;
    spareType?: string;
  }

  export interface SaveReq extends Spare {
    operatorId?: number;
    operatorName?: string;
    replaceReason?: string;
  }

  export interface RndConsumeReq {
    consumeTime: string;
    equipmentId: number;
    modelCode: string;
    pressSlotInputPcs: number;
    pressSlotOutputPcs: number;
    productionBatchNo: string;
    remark: string;
  }

  export interface ImportResp {
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

const BASE = '/mes/hc/base/press-slot-spare';

export function getPressSlotSparePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcPressSlotSpareApi.Spare>>(`${BASE}/page`, { params });
}

export function getPressSlotSpareRecordPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcPressSlotSpareApi.Record>>(`${BASE}/record-page`, { params });
}

export function getPressSlotSpare(id: number) {
  return requestClient.get<MesHcPressSlotSpareApi.Spare>(`${BASE}/get?id=${id}`);
}

export function savePressSlotSpare(data: MesHcPressSlotSpareApi.SaveReq) {
  return requestClient.post<number>(`${BASE}/save`, normalizePressSlotSpareSavePayload(data));
}

export function savePressSlotSpareRndConsume(data: MesHcPressSlotSpareApi.RndConsumeReq) {
  return requestClient.post<string>(`${BASE}/rnd-consume`, data);
}

export function exportPressSlotSpare(params: Record<string, any>) {
  return requestClient.download(`${BASE}/export-excel`, { params });
}

export function importPressSlotSpare(file: File, confirmClear = true) {
  return requestClient.upload<MesHcPressSlotSpareApi.ImportResp>(`${BASE}/import`, {
    confirmClear,
    file,
  });
}
