import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcTerminalApi {
  export interface Terminal {
    terminalCode?: string;
    terminalName?: string;
    workCenterId?: number;
    workCenterCode?: string;
    terminalMode?: string;
    status?: number;
    id: number;
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
    status?: number;
  }
}

export async function getTerminalPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcTerminalApi.Terminal>>('/mes/hc/base/terminal/page', { params });
}

export async function getTerminal(id: number) {
  return requestClient.get<MesHcTerminalApi.Terminal>(`/mes/hc/base/terminal/get?id=${id}`);
}

export async function getTerminalDetail(id: number) {
  return requestClient.get<MesHcTerminalApi.Terminal>(`/mes/hc/base/terminal/get-detail?id=${id}`);
}

export async function createTerminal(data: MesHcTerminalApi.Terminal) {
  return requestClient.post<number>('/mes/hc/base/terminal/create', data);
}

export async function updateTerminal(data: MesHcTerminalApi.Terminal) {
  return requestClient.put<boolean>('/mes/hc/base/terminal/update', data);
}

export async function deleteTerminal(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/terminal/delete?id=${id}`);
}

export async function deleteTerminalList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/terminal/delete-list?ids=${ids.join(',')}`);
}

export async function exportTerminal(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/terminal/export-excel', { params });
}

export async function getTerminalSimpleList() {
  return requestClient.get<MesHcTerminalApi.SimpleItem[]>('/mes/hc/base/terminal/simple-list');
}

export async function getTerminalSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/terminal/simple-map');
}

export async function getTerminalSelectOptions() {
  return requestClient.get<MesHcTerminalApi.SelectOption[]>('/mes/hc/base/terminal/select-options');
}
