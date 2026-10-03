import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesMaintStandardApi {
  export interface Standard {
    id?: number;
    code?: string;
    name?: string;
    categoryId?: number;
    categoryName?: string;
    deviceType?: string;
    frequency?: string;
    maintType?: string;
    estimatedMinutes?: number;
    status?: number;
    remark?: string;
    version?: number;
    createTime?: string;
    items?: StandardItem[];
  }

  export interface StandardItem {
    id?: number;
    standardId?: number;
    itemGroup?: string;
    itemName?: string;
    method?: string;
    requirement?: string;
    frequency?: string;
    tool?: string;
    resultType?: string;
    requiredFlag?: boolean;
    sort?: number;
  }
}

export function getStandardPage(params: PageParam) {
  return requestClient.get<PageResult<MesMaintStandardApi.Standard>>(
    '/mes/resource/device/maint-standard/page',
    { params },
  );
}

export function getStandard(id: number) {
  return requestClient.get<MesMaintStandardApi.Standard>(
    `/mes/resource/device/maint-standard/get?id=${id}`,
  );
}

export function createStandard(data: MesMaintStandardApi.Standard) {
  return requestClient.post('/mes/resource/device/maint-standard/create', data);
}

export function updateStandard(data: MesMaintStandardApi.Standard) {
  return requestClient.put('/mes/resource/device/maint-standard/update', data);
}

export function deleteStandardList(ids: number[]) {
  return requestClient.delete(
    `/mes/resource/device/maint-standard/delete-list?ids=${ids.join(',')}`,
  );
}

export function exportStandard(params: any) {
  return requestClient.download(
    '/mes/resource/device/maint-standard/export-excel',
    { params },
  );
}

export function getStandardItemListById(standardId: number) {
  return requestClient.get<MesMaintStandardApi.StandardItem[]>(
    '/mes/resource/device/maint-standard/item/list',
    { params: { standardId } },
  );
}
