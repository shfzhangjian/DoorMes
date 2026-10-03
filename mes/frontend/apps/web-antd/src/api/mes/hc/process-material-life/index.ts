import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcProcessMaterialLifeApi {
  export interface State {
    batchNo?: string;
    consumableType?: string;
    consumableTypeName?: string;
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
    changeLength?: number;
    changeUseCount?: number;
    consumableType?: string;
    consumableTypeName?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    eventTime?: string;
    eventType?: string;
    id?: number;
    motherBatchNo?: string;
    operatorId?: number;
    operatorName?: string;
    planNo?: string;
    processCode?: string;
    processName?: string;
    remark?: string;
    replaceReason?: string;
    stateId?: number;
  }

  export interface EventSaveReq {
    batchNo?: string;
    changeLength?: number;
    changeUseCount?: number;
    consumableType?: string;
    equipmentCode?: string;
    equipmentId?: number;
    equipmentName?: string;
    eventTime?: string;
    eventType?: string;
    motherBatchNo?: string;
    operatorName?: string;
    planNo?: string;
    processCode?: string;
    processName?: string;
    remark?: string;
    replaceReason?: string;
    stateId?: number;
  }
}

const BASE = '/mes/hc/base/process-material-life';

export function getProcessMaterialLifeStatePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcProcessMaterialLifeApi.State>>(`${BASE}/state-page`, { params });
}

export function getProcessMaterialLifeEventPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcProcessMaterialLifeApi.Event>>(`${BASE}/event-page`, { params });
}

export function createProcessMaterialLifeEvent(data: MesHcProcessMaterialLifeApi.EventSaveReq) {
  return requestClient.post<number>(`${BASE}/event/create`, data);
}
