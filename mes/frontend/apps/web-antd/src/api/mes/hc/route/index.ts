import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcRouteApi {
  export interface Route {
    routeCode?: string;
    routeName?: string;
    applicableScope?: string;
    productMaterialId?: number;
    productMaterialCode?: string;
    productMaterialName?: string;
    productLevel?: string;
    versionNo?: string;
    routeType?: string;
    status?: number;
    remark?: string;
    id: number;
    routeOperations?: RouteOperation[];
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

  export interface RouteOperation {
    routeId?: number;
    routeCode?: string;
    operationCode?: string;
    operationName?: string;
    seqNo?: number;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    reportRequired?: boolean;
    conversionRate?: number;
    outputUnitId?: number;
    outputUnitCode?: string;
    outputUnitName?: string;
    outputUom?: string;
    qcRequired?: boolean;
    batchSplitMode?: string;
    paramTemplateJson?: string;
    remark?: string;
    id: number;
  }
}

export async function getRoutePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcRouteApi.Route>>('/mes/hc/base/route/page', { params });
}

export async function getRoute(id: number) {
  return requestClient.get<MesHcRouteApi.Route>(`/mes/hc/base/route/get?id=${id}`);
}

export async function getRouteDetail(id: number) {
  return requestClient.get<MesHcRouteApi.Route>(`/mes/hc/base/route/get-detail?id=${id}`);
}

export async function createRoute(data: MesHcRouteApi.Route) {
  return requestClient.post<number>('/mes/hc/base/route/create', data);
}

export async function updateRoute(data: MesHcRouteApi.Route) {
  return requestClient.put<boolean>('/mes/hc/base/route/update', data);
}

export async function deleteRoute(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/route/delete?id=${id}`);
}

export async function deleteRouteList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/route/delete-list?ids=${ids.join(',')}`);
}

export async function exportRoute(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/route/export-excel', { params });
}

export async function getRouteSimpleList() {
  return requestClient.get<MesHcRouteApi.SimpleItem[]>('/mes/hc/base/route/simple-list');
}

export async function getRouteSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/route/simple-map');
}

export async function getRouteSelectOptions() {
  return requestClient.get<MesHcRouteApi.SelectOption[]>('/mes/hc/base/route/select-options');
}

export async function getRouteSelectOptionsByMaterialId(materialId: number) {
  return requestClient.get<MesHcRouteApi.SelectOption[]>(
    `/mes/hc/base/route/select-options-by-material-id?materialId=${materialId}`,
  );
}

export async function getRouteOperationListByParentId(parentId: number) {
  return requestClient.get<MesHcRouteApi.RouteOperation[]>(`/mes/hc/base/route/mes_md_route_operation/list-by-parent-id?parentId=${parentId}`);
}
