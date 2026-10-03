import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcPrintFieldTemplateApi {
  export interface PrintFieldTemplateItem {
    id?: number;
    templateId?: number;
    fieldKey?: string;
    fieldLabel?: string;
    valueKey?: string;
    sort?: number;
    visible?: boolean;
    defaultValue?: string;
    formatType?: string;
    suffix?: string;
    remark?: string;
  }

  export interface PrintFieldTemplate {
    id?: number;
    templateCode?: string;
    templateName?: string;
    processCode?: string;
    processName?: string;
    documentType?: string;
    documentName?: string;
    usageScene?: string;
    status?: number;
    remark?: string;
    createTime?: string;
    updateTime?: string;
    items?: PrintFieldTemplateItem[];
  }
}

const BASE_URL = '/mes/hc/execution/print-field-template';

export async function getPrintFieldTemplatePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcPrintFieldTemplateApi.PrintFieldTemplate>>(`${BASE_URL}/page`, { params });
}

export async function getPrintFieldTemplateDetail(id: number) {
  return requestClient.get<MesHcPrintFieldTemplateApi.PrintFieldTemplate>(`${BASE_URL}/get-detail?id=${id}`);
}

export async function createPrintFieldTemplate(data: MesHcPrintFieldTemplateApi.PrintFieldTemplate) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export async function updatePrintFieldTemplate(data: MesHcPrintFieldTemplateApi.PrintFieldTemplate) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export async function deletePrintFieldTemplate(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete?id=${id}`);
}

export async function getActivePrintFieldTemplate(templateCode: string) {
  return requestClient.get<MesHcPrintFieldTemplateApi.PrintFieldTemplate>(`${BASE_URL}/active`, {
    params: { templateCode },
  });
}
