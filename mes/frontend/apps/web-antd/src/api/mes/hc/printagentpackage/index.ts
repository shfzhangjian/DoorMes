import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcPrintAgentPackageApi {
  export interface PrintAgentPackage {
    id?: number;
    packageCode?: string;
    versionNo?: string;
    packageName?: string;
    packageUrl?: string;
    packageSha256?: string;
    packageSize?: number;
    status?: string;
    currentFlag?: boolean;
    publishTime?: string;
    releaseNote?: string;
    remark?: string;
    createTime?: string;
  }
}

const BASE_URL = '/mes/hc/print-agent-package';

export async function getPrintAgentPackagePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcPrintAgentPackageApi.PrintAgentPackage>>(`${BASE_URL}/page`, { params });
}

export async function createPrintAgentPackage(data: MesHcPrintAgentPackageApi.PrintAgentPackage) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export async function updatePrintAgentPackage(data: MesHcPrintAgentPackageApi.PrintAgentPackage) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export async function deletePrintAgentPackage(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete?id=${id}`);
}

export async function publishPrintAgentPackage(id: number) {
  return requestClient.put<boolean>(`${BASE_URL}/publish?id=${id}`);
}

export function buildPrintAgentLatestPath(packageCode = 'HC_MES_PRINT_AGENT') {
  return `${BASE_URL}/latest?packageCode=${encodeURIComponent(packageCode)}`;
}

export function buildPrintAgentLatestDownloadPath(packageCode = 'HC_MES_PRINT_AGENT') {
  return `${BASE_URL}/latest/download?packageCode=${encodeURIComponent(packageCode)}`;
}
