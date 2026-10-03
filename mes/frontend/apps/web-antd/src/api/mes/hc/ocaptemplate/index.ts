import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcOcapTemplateApi {
  export interface OcapTemplate {
    ocapCode?: string;
    ocapName?: string;
    businessStage?: string;
    triggerItemCode?: string;
    triggerCondition?: string;
    actionSteps?: string;
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

export async function getOcapTemplatePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcOcapTemplateApi.OcapTemplate>>('/mes/hc/base/ocap-template/page', { params });
}

export async function getOcapTemplate(id: number) {
  return requestClient.get<MesHcOcapTemplateApi.OcapTemplate>(`/mes/hc/base/ocap-template/get?id=${id}`);
}

export async function getOcapTemplateDetail(id: number) {
  return requestClient.get<MesHcOcapTemplateApi.OcapTemplate>(`/mes/hc/base/ocap-template/get-detail?id=${id}`);
}

export async function createOcapTemplate(data: MesHcOcapTemplateApi.OcapTemplate) {
  return requestClient.post<number>('/mes/hc/base/ocap-template/create', data);
}

export async function updateOcapTemplate(data: MesHcOcapTemplateApi.OcapTemplate) {
  return requestClient.put<boolean>('/mes/hc/base/ocap-template/update', data);
}

export async function deleteOcapTemplate(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/ocap-template/delete?id=${id}`);
}

export async function deleteOcapTemplateList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/ocap-template/delete-list?ids=${ids.join(',')}`);
}

export async function exportOcapTemplate(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/ocap-template/export-excel', { params });
}

export async function getOcapTemplateSimpleList() {
  return requestClient.get<MesHcOcapTemplateApi.SimpleItem[]>('/mes/hc/base/ocap-template/simple-list');
}

export async function getOcapTemplateSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/ocap-template/simple-map');
}

export async function getOcapTemplateSelectOptions() {
  return requestClient.get<MesHcOcapTemplateApi.SelectOption[]>('/mes/hc/base/ocap-template/select-options');
}
