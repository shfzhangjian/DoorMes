import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcFifoPolicyApi {
  export interface FifoPolicy {
    policyCode?: string;
    policyName?: string;
    warehouseCode?: string;
    warehouseName?: string;
    ownerCode?: string;
    matchScope?: string;
    issueRule?: string;
    priorityFields?: string;
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

export async function getFifoPolicyPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcFifoPolicyApi.FifoPolicy>>('/mes/hc/base/fifo-policy/page', { params });
}

export async function getFifoPolicy(id: number) {
  return requestClient.get<MesHcFifoPolicyApi.FifoPolicy>(`/mes/hc/base/fifo-policy/get?id=${id}`);
}

export async function getFifoPolicyDetail(id: number) {
  return requestClient.get<MesHcFifoPolicyApi.FifoPolicy>(`/mes/hc/base/fifo-policy/get-detail?id=${id}`);
}

export async function createFifoPolicy(data: MesHcFifoPolicyApi.FifoPolicy) {
  return requestClient.post<number>('/mes/hc/base/fifo-policy/create', data);
}

export async function updateFifoPolicy(data: MesHcFifoPolicyApi.FifoPolicy) {
  return requestClient.put<boolean>('/mes/hc/base/fifo-policy/update', data);
}

export async function deleteFifoPolicy(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/fifo-policy/delete?id=${id}`);
}

export async function deleteFifoPolicyList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/fifo-policy/delete-list?ids=${ids.join(',')}`);
}

export async function exportFifoPolicy(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/fifo-policy/export-excel', { params });
}

export async function getFifoPolicySimpleList() {
  return requestClient.get<MesHcFifoPolicyApi.SimpleItem[]>('/mes/hc/base/fifo-policy/simple-list');
}

export async function getFifoPolicySimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/fifo-policy/simple-map');
}

export async function getFifoPolicySelectOptions() {
  return requestClient.get<MesHcFifoPolicyApi.SelectOption[]>('/mes/hc/base/fifo-policy/select-options');
}
