import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcFinishedGlueBoardMapApi {
  export interface FinishedGlueBoardMap {
    id?: number;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    productSpec?: string;
    sizeSpec?: string;
    sizeName?: string;
    adhesive1Summary?: string;
    adhesive2Summary?: string;
    status?: string;
    remark?: string;
    createTime?: string;
    items?: FinishedGlueBoardMapItem[];
  }

  export interface FinishedGlueBoardMapItem {
    id?: number;
    mapId?: number;
    productModelCode?: string;
    glueProcess?: string;
    glueProcessName?: string;
    glueBoardMaterialId?: number;
    glueBoardMaterialCode?: string;
    glueBoardMaterialName?: string;
    glueBoardModel?: string;
    glueBoardSpec?: string;
    preferredFlag?: boolean;
    sort?: number;
    remark?: string;
  }
}

export async function getFinishedGlueBoardMapPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap>>(
    '/mes/hc/base/finished-glue-board-map/page',
    { params },
  );
}

export async function getFinishedGlueBoardMap(id: number) {
  return requestClient.get<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap>(
    `/mes/hc/base/finished-glue-board-map/get?id=${id}`,
  );
}

export async function getFinishedGlueBoardMapDetail(id: number) {
  return requestClient.get<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap>(
    `/mes/hc/base/finished-glue-board-map/get-detail?id=${id}`,
  );
}

export async function getMatchedFinishedGlueBoardMapItems(params: {
  glueProcess: string;
  productModelCode: string;
}) {
  return requestClient.get<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[]>(
    '/mes/hc/base/finished-glue-board-map/match-items',
    { params },
  );
}

export async function getFinishedGlueBoardMapModelOptions(params?: {
  glueBoardMaterialCode?: string;
  glueProcess?: string;
}) {
  return requestClient.get<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem[]>(
    '/mes/hc/base/finished-glue-board-map/model-options',
    { params },
  );
}

export async function createFinishedGlueBoardMap(data: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap) {
  return requestClient.post<number>('/mes/hc/base/finished-glue-board-map/create', data);
}

export async function updateFinishedGlueBoardMap(data: MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap) {
  return requestClient.put<boolean>('/mes/hc/base/finished-glue-board-map/update', data);
}

export async function deleteFinishedGlueBoardMap(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/finished-glue-board-map/delete?id=${id}`);
}

export async function deleteFinishedGlueBoardMapList(ids: number[]) {
  return requestClient.delete<boolean>(
    `/mes/hc/base/finished-glue-board-map/delete-list?ids=${ids.join(',')}`,
  );
}

export async function exportFinishedGlueBoardMap(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/finished-glue-board-map/export-excel', { params });
}
