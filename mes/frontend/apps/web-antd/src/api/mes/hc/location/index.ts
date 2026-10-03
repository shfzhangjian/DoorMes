import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcLocationApi {
  export interface Location {
    locationCode?: string;
    locationName?: string;
    warehouseCode?: string;
    warehouseName?: string;
    locationType?: string;
    mixBatchFlag?: boolean;
    mixModelFlag?: boolean;
    status?: string;
    id: number;
  }

  export interface SimpleItem {
    id: number;
    code?: string;
    name?: string;
    status?: string;
  }

  export interface SelectOption {
    value: number;
    label: string;
    code?: string;
    status?: string;
  }
}

export async function getLocationPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcLocationApi.Location>>('/mes/hc/base/location/page', { params });
}

export async function getLocation(id: number) {
  return requestClient.get<MesHcLocationApi.Location>(`/mes/hc/base/location/get?id=${id}`);
}

export async function getLocationDetail(id: number) {
  return requestClient.get<MesHcLocationApi.Location>(`/mes/hc/base/location/get-detail?id=${id}`);
}

export async function createLocation(data: MesHcLocationApi.Location) {
  return requestClient.post<number>('/mes/hc/base/location/create', data);
}

export async function updateLocation(data: MesHcLocationApi.Location) {
  return requestClient.put<boolean>('/mes/hc/base/location/update', data);
}

export async function deleteLocation(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/location/delete?id=${id}`);
}

export async function deleteLocationList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/location/delete-list?ids=${ids.join(',')}`);
}

export async function exportLocation(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/location/export-excel', { params });
}

export async function getLocationSimpleList() {
  return requestClient.get<MesHcLocationApi.SimpleItem[]>('/mes/hc/base/location/simple-list');
}

export async function getLocationSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/location/simple-map');
}

export async function getLocationSelectOptions() {
  return requestClient.get<MesHcLocationApi.SelectOption[]>('/mes/hc/base/location/select-options');
}
