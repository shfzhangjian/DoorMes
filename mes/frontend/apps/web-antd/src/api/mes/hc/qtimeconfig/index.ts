import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcQtimeConfigApi {
  export interface QtimeConfig {
    createTime?: string;
    adhesive2ToCutRoundMinutes?: number;
    adhesiveToSlittingMinutes?: number;
    cutRoundToFqcMinutes?: number;
    firstGrindingToSecondMinutes?: number;
    formulaToWetMinutes?: number;
    grindingToAdhesiveMinutes?: number;
    id?: number;
    modelPrefix: string;
    remark?: string;
    pressSlotToAdhesive2Minutes?: number;
    slittingToPressSlotMinutes?: number;
    status?: 'DISABLED' | 'ENABLED' | string;
    updateTime?: string;
    wetToGrindingMinutes?: number;
  }

  export interface PageReq extends PageParam {
    modelPrefix?: string;
    status?: string;
  }
}

const baseUrl = '/mes/hc/base/qtime-config';

export async function getQtimeConfigPage(params: MesHcQtimeConfigApi.PageReq) {
  return requestClient.get<PageResult<MesHcQtimeConfigApi.QtimeConfig>>(`${baseUrl}/page`, { params });
}

export async function getQtimeConfig(id: number) {
  return requestClient.get<MesHcQtimeConfigApi.QtimeConfig>(`${baseUrl}/get`, { params: { id } });
}

export async function createQtimeConfig(data: MesHcQtimeConfigApi.QtimeConfig) {
  return requestClient.post<number>(`${baseUrl}/create`, data);
}

export async function updateQtimeConfig(data: MesHcQtimeConfigApi.QtimeConfig) {
  return requestClient.put<boolean>(`${baseUrl}/update`, data);
}

export async function deleteQtimeConfig(id: number) {
  return requestClient.delete<boolean>(`${baseUrl}/delete`, { params: { id } });
}
