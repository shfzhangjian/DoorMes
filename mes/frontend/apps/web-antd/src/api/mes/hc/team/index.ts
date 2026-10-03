import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcTeamApi {
  export interface Team {
    teamCode?: string;
    teamName?: string;
    workCenterId?: number;
    workCenterCode?: string;
    leaderUserId?: number;
    leaderName?: string;
    status?: number;
    remark?: string;
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

export async function getTeamPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcTeamApi.Team>>('/mes/hc/base/team/page', { params });
}

export async function getTeam(id: number) {
  return requestClient.get<MesHcTeamApi.Team>(`/mes/hc/base/team/get?id=${id}`);
}

export async function getTeamDetail(id: number) {
  return requestClient.get<MesHcTeamApi.Team>(`/mes/hc/base/team/get-detail?id=${id}`);
}

export async function createTeam(data: MesHcTeamApi.Team) {
  return requestClient.post<number>('/mes/hc/base/team/create', data);
}

export async function updateTeam(data: MesHcTeamApi.Team) {
  return requestClient.put<boolean>('/mes/hc/base/team/update', data);
}

export async function deleteTeam(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/team/delete?id=${id}`);
}

export async function deleteTeamList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/team/delete-list?ids=${ids.join(',')}`);
}

export async function exportTeam(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/team/export-excel', { params });
}

export async function getTeamSimpleList() {
  return requestClient.get<MesHcTeamApi.SimpleItem[]>('/mes/hc/base/team/simple-list');
}

export async function getTeamSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/team/simple-map');
}

export async function getTeamSelectOptions() {
  return requestClient.get<MesHcTeamApi.SelectOption[]>('/mes/hc/base/team/select-options');
}
