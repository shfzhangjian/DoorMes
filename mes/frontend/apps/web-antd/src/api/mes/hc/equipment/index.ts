import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcEquipmentApi {
  export interface Equipment {
    equipmentCode?: string;
    equipmentName?: string;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    equipmentType?: string;
    applicablePadType?: string;
    applicablePadTypeName?: string;
    assetNo?: string;
    enableQcChecklist?: boolean;
    enableCleanChecklist?: boolean;
    status?: number;
    workStatus?: string;
    currentPlanNo?: string;
    currentOperationCode?: string;
    currentOperationName?: string;
    currentStartTime?: string;
    currentEndTime?: string;
    currentOperatorName?: string;
    currentRecordTime?: string;
    remark?: string;
    id: number;
  }

  export interface WorkStateAdjustReq {
    id: number;
    workStatus: string;
    clearOperationBinding?: boolean;
    remark?: string;
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
    name?: string;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    applicablePadType?: string;
    applicablePadTypeName?: string;
    currentOperationCode?: string;
    currentOperationName?: string;
    status?: number;
    workStatus?: string;
  }
}

export async function getEquipmentPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcEquipmentApi.Equipment>>('/mes/hc/base/equipment/page', { params });
}

export async function getEquipment(id: number) {
  return requestClient.get<MesHcEquipmentApi.Equipment>(`/mes/hc/base/equipment/get?id=${id}`);
}

export async function getEquipmentDetail(id: number) {
  return requestClient.get<MesHcEquipmentApi.Equipment>(`/mes/hc/base/equipment/get-detail?id=${id}`);
}

export async function createEquipment(data: MesHcEquipmentApi.Equipment) {
  return requestClient.post<number>('/mes/hc/base/equipment/create', data);
}

export async function updateEquipment(data: MesHcEquipmentApi.Equipment) {
  return requestClient.put<boolean>('/mes/hc/base/equipment/update', data);
}

export async function adjustEquipmentWorkState(data: MesHcEquipmentApi.WorkStateAdjustReq) {
  return requestClient.put<boolean>('/mes/hc/base/equipment/adjust-work-state', data);
}

export async function deleteEquipment(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/equipment/delete?id=${id}`);
}

export async function deleteEquipmentList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/equipment/delete-list?ids=${ids.join(',')}`);
}

export async function exportEquipment(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/equipment/export-excel', { params });
}

export async function getEquipmentSimpleList() {
  return requestClient.get<MesHcEquipmentApi.SimpleItem[]>('/mes/hc/base/equipment/simple-list');
}

export async function getEquipmentSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/equipment/simple-map');
}

export async function getEquipmentSelectOptions() {
  return requestClient.get<MesHcEquipmentApi.SelectOption[]>('/mes/hc/base/equipment/select-options');
}
