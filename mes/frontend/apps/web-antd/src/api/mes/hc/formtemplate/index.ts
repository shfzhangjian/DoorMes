import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcFormTemplateApi {
  export interface FormTemplate {
    templateCode?: string;
    templateName?: string;
    templateType?: string;
    businessStage?: string;
    formStyle?: string;
    status?: string;
    remark?: string;
    id: number;
    templateVersions?: FormTemplateVersion[];
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

  export interface FormTemplateVersion {
    templateId?: number;
    templateCode?: string;
    versionNo?: string;
    isCurrent?: boolean;
    effectiveDate?: string;
    sourceFileName?: string;
    remark?: string;
    id: number;
  }
}

export async function getFormTemplatePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcFormTemplateApi.FormTemplate>>('/mes/hc/base/form-template/page', { params });
}

export async function getFormTemplate(id: number) {
  return requestClient.get<MesHcFormTemplateApi.FormTemplate>(`/mes/hc/base/form-template/get?id=${id}`);
}

export async function getFormTemplateDetail(id: number) {
  return requestClient.get<MesHcFormTemplateApi.FormTemplate>(`/mes/hc/base/form-template/get-detail?id=${id}`);
}

export async function createFormTemplate(data: MesHcFormTemplateApi.FormTemplate) {
  return requestClient.post<number>('/mes/hc/base/form-template/create', data);
}

export async function updateFormTemplate(data: MesHcFormTemplateApi.FormTemplate) {
  return requestClient.put<boolean>('/mes/hc/base/form-template/update', data);
}

export async function deleteFormTemplate(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/form-template/delete?id=${id}`);
}

export async function deleteFormTemplateList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/form-template/delete-list?ids=${ids.join(',')}`);
}

export async function exportFormTemplate(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/form-template/export-excel', { params });
}

export async function getFormTemplateSimpleList() {
  return requestClient.get<MesHcFormTemplateApi.SimpleItem[]>('/mes/hc/base/form-template/simple-list');
}

export async function getFormTemplateSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/form-template/simple-map');
}

export async function getFormTemplateSelectOptions() {
  return requestClient.get<MesHcFormTemplateApi.SelectOption[]>('/mes/hc/base/form-template/select-options');
}

export async function getFormTemplateVersionListByParentId(parentId: number) {
  return requestClient.get<MesHcFormTemplateApi.FormTemplateVersion[]>(`/mes/hc/base/form-template/mes_form_template_version/list-by-parent-id?parentId=${parentId}`);
}
