import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQmsYieldTargetConfigApi {
  export interface PageReqVO extends PageParam {
    modelCode?: string;
    processCode?: string;
    segmentCount?: number;
    status?: number;
  }

  export interface TargetConfig {
    createTime?: string;
    id?: number;
    measureUnit: string;
    modelCode: string;
    processCode: string;
    processName: string;
    remark?: string;
    segmentCount: number;
    sort?: number;
    status: number;
    targetType: string;
    targetQualifiedQty: number;
  }
}

const BASE_URL = '/mes/quality/statistics/yield-target-config';

export function getYieldTargetConfigPage(
  params: MesQmsYieldTargetConfigApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesQmsYieldTargetConfigApi.TargetConfig>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getYieldTargetConfig(id: number) {
  return requestClient.get<MesQmsYieldTargetConfigApi.TargetConfig>(
    `${BASE_URL}/get`,
    { params: { id } },
  );
}

export function createYieldTargetConfig(
  data: MesQmsYieldTargetConfigApi.TargetConfig,
) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function updateYieldTargetConfig(
  data: MesQmsYieldTargetConfigApi.TargetConfig,
) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function deleteYieldTargetConfig(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete`, { params: { id } });
}

export function exportYieldTargetConfigExcel(
  params: MesQmsYieldTargetConfigApi.PageReqVO,
) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}
