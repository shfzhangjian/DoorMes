import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcOwnerApi {
  export interface Owner {
    ownerCode?: string;
    ownerName?: string;
    ownerType?: string;
    contactName?: string;
    contactPhone?: string;
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

export async function getOwnerPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcOwnerApi.Owner>>('/mes/hc/base/owner/page', { params });
}

export async function getOwner(id: number) {
  return requestClient.get<MesHcOwnerApi.Owner>(`/mes/hc/base/owner/get?id=${id}`);
}

export async function getOwnerDetail(id: number) {
  return requestClient.get<MesHcOwnerApi.Owner>(`/mes/hc/base/owner/get-detail?id=${id}`);
}

export async function createOwner(data: MesHcOwnerApi.Owner) {
  return requestClient.post<number>('/mes/hc/base/owner/create', data);
}

export async function updateOwner(data: MesHcOwnerApi.Owner) {
  return requestClient.put<boolean>('/mes/hc/base/owner/update', data);
}

export async function deleteOwner(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/owner/delete?id=${id}`);
}

export async function deleteOwnerList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/owner/delete-list?ids=${ids.join(',')}`);
}

export async function exportOwner(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/owner/export-excel', { params });
}

export async function getOwnerSimpleList() {
  return requestClient.get<MesHcOwnerApi.SimpleItem[]>('/mes/hc/base/owner/simple-list');
}

export async function getOwnerSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/owner/simple-map');
}

export async function getOwnerSelectOptions() {
  return requestClient.get<MesHcOwnerApi.SelectOption[]>('/mes/hc/base/owner/select-options');
}
