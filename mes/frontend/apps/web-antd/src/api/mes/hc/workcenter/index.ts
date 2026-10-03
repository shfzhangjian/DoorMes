import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcWorkCenterApi {
  export interface WorkCenter {
    wcCode?: string;
    wcName?: string;
    processStage?: string;
    processId?: number;
    processCode?: string;
    processName?: string;
    lineCode?: string;
    lineName?: string;
    lineShortCode?: string;
    batchLineCode?: string;
    lineSort?: number;
    terminalIps?: string;
    capacityPerHour?: number;
    capacityUom?: string;
    defaultShiftMode?: string;
    status?: number;
    remark?: string;
    id: number;
  }

  export interface SimpleItem {
    id: number;
    wcCode?: string;
    wcName?: string;
    processStage?: string;
    processId?: number;
    processCode?: string;
    processName?: string;
    code?: string;
    name?: string;
    lineCode?: string;
    lineName?: string;
    lineShortCode?: string;
    batchLineCode?: string;
    status?: number;
  }

  export interface TerminalMatch {
    ip?: string;
    matched?: boolean;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    processCode?: string;
    processName?: string;
    lineCode?: string;
    lineName?: string;
    lineShortCode?: string;
    batchLineCode?: string;
    terminalIps?: string;
  }

  export interface SelectOption {
    value: number;
    label: string;
    code?: string;
    processStage?: string;
    processId?: number;
    processCode?: string;
    processName?: string;
    lineCode?: string;
    lineName?: string;
    lineShortCode?: string;
    batchLineCode?: string;
    status?: number;
  }
}

export function getWorkCenterLineLabel(lineCode?: string, lineName?: string) {
  if (lineName) return lineName;
  return (
    {
      BLACK: '黑垫线',
      WHITE: '白垫线',
    } as Record<string, string>
  )[String(lineCode || '').toUpperCase()] || lineCode || '-';
}

function formatWorkCenterOption(item: MesHcWorkCenterApi.SelectOption) {
  const lineName = getWorkCenterLineLabel(item.lineCode, item.lineName);
  return {
    ...item,
    lineName,
    label: `${lineName} / ${item.code || ''} / ${item.label || ''}`,
  };
}

export async function getWorkCenterPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcWorkCenterApi.WorkCenter>>('/mes/hc/base/work-center/page', { params });
}

export async function getWorkCenter(id: number) {
  return requestClient.get<MesHcWorkCenterApi.WorkCenter>(`/mes/hc/base/work-center/get?id=${id}`);
}

export async function getWorkCenterDetail(id: number) {
  return requestClient.get<MesHcWorkCenterApi.WorkCenter>(`/mes/hc/base/work-center/get-detail?id=${id}`);
}

export async function createWorkCenter(data: MesHcWorkCenterApi.WorkCenter) {
  return requestClient.post<number>('/mes/hc/base/work-center/create', data);
}

export async function updateWorkCenter(data: MesHcWorkCenterApi.WorkCenter) {
  return requestClient.put<boolean>('/mes/hc/base/work-center/update', data);
}

export async function deleteWorkCenter(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/work-center/delete?id=${id}`);
}

export async function deleteWorkCenterList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/work-center/delete-list?ids=${ids.join(',')}`);
}

export async function exportWorkCenter(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/work-center/export-excel', { params });
}

export async function getWorkCenterSimpleList() {
  const list = await requestClient.get<MesHcWorkCenterApi.SimpleItem[]>('/mes/hc/base/work-center/simple-list');
  return (list || []).map((item) => ({
    ...item,
    code: item.code || item.wcCode || '',
    name: item.name || item.wcName || '',
    lineName: getWorkCenterLineLabel(item.lineCode, item.lineName),
  }));
}

export async function getWorkCenterSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/work-center/simple-map');
}

export async function getWorkCenterSelectOptions() {
  const list = await requestClient.get<MesHcWorkCenterApi.SelectOption[]>('/mes/hc/base/work-center/select-options');
  return (list || []).map(formatWorkCenterOption);
}

export async function getWorkCenterByTerminalIp(ip?: string) {
  return requestClient.get<MesHcWorkCenterApi.TerminalMatch>('/mes/hc/base/work-center/public/by-ip', {
    params: ip ? { ip } : undefined,
  });
}
