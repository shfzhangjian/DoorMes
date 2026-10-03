import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcEquipmentConsumableApi {
  export interface State {
    batchNo?: string;
    consumableType?: string;
    createTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    id?: number;
    lastEventTime?: string;
    lastOperatorId?: number;
    lastOperatorName?: string;
    lastReplacePlanNo?: string;
    lastReplaceReason?: string;
    lastReplaceTime?: string;
    limitCount?: number;
    limitLength?: number;
    processCode?: string;
    processName?: string;
    remark?: string;
    status?: string;
    useCount?: number;
    usedLength?: number;
    warningFlag?: number;
    workCenterCode?: string;
    workCenterId?: number;
    workCenterName?: string;
  }

  export interface Event {
    afterBatchNo?: string;
    afterUseCount?: number;
    afterUsedLength?: number;
    beforeBatchNo?: string;
    beforeUseCount?: number;
    beforeUsedLength?: number;
    bizId?: number;
    bizType?: string;
    changeLength?: number;
    changeUseCount?: number;
    consumableType?: string;
    createTime?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    eventTime?: string;
    eventType?: string;
    guideClothRecordId?: number;
    grindingStage?: string;
    id?: number;
    operationCode?: string;
    operationName?: string;
    operatorId?: number;
    operatorName?: string;
    petBatchNo?: string;
    petModel?: string;
    planNo?: string;
    productBatchNo?: string;
    productMaterialCode?: string;
    productModelCode?: string;
    processCode?: string;
    processName?: string;
    remark?: string;
    recordGroupNo?: string;
    recordSource?: string;
    replaceReason?: string;
    stateId?: number;
    wetInputKg?: number;
    wetOutputMeter?: number;
  }

  export interface AdjustReq extends State {
    eventTime?: string;
    eventType?: string;
    operatorId?: number;
    operatorName?: string;
    replaceReason?: string;
  }

  export interface StateImportResp {
    clearedEventCount?: number;
    clearedStateCount?: number;
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    successCount?: number;
    totalRows?: number;
  }
}

const BASE = '/mes/hc/base/equipment-consumable';

export function getEquipmentConsumableStatePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcEquipmentConsumableApi.State>>(`${BASE}/state-page`, { params });
}

export function getEquipmentConsumableEventPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcEquipmentConsumableApi.Event>>(`${BASE}/event-page`, { params });
}

export function getEquipmentConsumableState(id: number) {
  return requestClient.get<MesHcEquipmentConsumableApi.State>(`${BASE}/get-state?id=${id}`);
}

export function adjustEquipmentConsumable(data: MesHcEquipmentConsumableApi.AdjustReq) {
  return requestClient.post<number>(`${BASE}/adjust`, data);
}

export function exportEquipmentConsumableState(params: Record<string, any>) {
  return requestClient.download(`${BASE}/state-export-excel`, { params });
}

export function importEquipmentConsumableState(file: File, confirmClear = true) {
  return requestClient.upload<MesHcEquipmentConsumableApi.StateImportResp>(`${BASE}/state-import`, {
    confirmClear,
    file,
  });
}
