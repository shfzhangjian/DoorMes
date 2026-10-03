import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcCustomerPrintTemplateApi {
  export interface CustomerPrintTemplateVar {
    id?: number;
    templateId?: number;
    variableName?: string;
    variableExpr?: string;
    variableScope?: 'CONST' | 'DETAIL' | 'HEAD' | 'PRINT' | string;
    sourceField?: string;
    sourceLabel?: string;
    sort?: number;
    required?: boolean;
    defaultValue?: string;
    remark?: string;
  }

  export interface CustomerPrintTemplate {
    id?: number;
    templateCode?: string;
    templateName?: string;
    customerCode?: string;
    customerName?: string;
    templateType?: 'PACKAGE' | 'PIECE' | string;
    templateFormat?: 'ZPL' | 'NLBL' | string;
    fileName?: string;
    fileUrl?: string;
    fileSize?: number;
    templateContent?: string;
    variableJson?: string;
    status?: number;
    remark?: string;
    createTime?: string;
    updateTime?: string;
    vars?: CustomerPrintTemplateVar[];
  }

  export interface FieldOption {
    scope?: string;
    sourceField?: string;
    sourceLabel?: string;
    variableExpr?: string;
  }
}

const BASE_URL = '/mes/hc/execution/customer-print-template';

export function buildCustomerPrintTemplatePublicDownloadPath(id?: number) {
  return `${BASE_URL}/public/download?id=${encodeURIComponent(String(id || ''))}`;
}

export async function getCustomerPrintTemplatePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcCustomerPrintTemplateApi.CustomerPrintTemplate>>(`${BASE_URL}/page`, {
    params,
  });
}

export async function getCustomerPrintTemplateDetail(id: number) {
  return requestClient.get<MesHcCustomerPrintTemplateApi.CustomerPrintTemplate>(`${BASE_URL}/get-detail?id=${id}`);
}

export async function createCustomerPrintTemplate(data: MesHcCustomerPrintTemplateApi.CustomerPrintTemplate) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export async function updateCustomerPrintTemplate(data: MesHcCustomerPrintTemplateApi.CustomerPrintTemplate) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export async function deleteCustomerPrintTemplate(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete?id=${id}`);
}

export async function getActiveCustomerPrintTemplateList(params: {
  customerCode?: string;
  customerName?: string;
  templateType: string;
}) {
  return requestClient.get<MesHcCustomerPrintTemplateApi.CustomerPrintTemplate[]>(`${BASE_URL}/active-list`, {
    params,
  });
}

export async function getCustomerPrintTemplateFieldOptions(scope?: string) {
  return requestClient.get<MesHcCustomerPrintTemplateApi.FieldOption[]>(`${BASE_URL}/field-options`, {
    params: { scope },
  });
}

export async function parseCustomerPrintTemplateVariables(data: {
  templateContent: string;
  templateFormat?: string;
  templateType: string;
}) {
  return requestClient.post<MesHcCustomerPrintTemplateApi.CustomerPrintTemplateVar[]>(
    `${BASE_URL}/parse-variables`,
    data,
  );
}
