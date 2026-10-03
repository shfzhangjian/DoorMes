import { requestClient } from '#/api/request';

export namespace MesDeviceCategoryApi {
  export interface Category {
    id?: number;
    parentId?: number;
    categoryCode?: string;
    categoryName?: string;
    description?: string;
    status?: number;
    sort?: number;
  }
}

export function getCategoryList(params?: any) {
  return requestClient.get<MesDeviceCategoryApi.Category[]>(
    '/mes/resource/device/category/list',
    { params },
  );
}

export function getCategory(id: number) {
  return requestClient.get<MesDeviceCategoryApi.Category>(
    `/mes/resource/device/category/get?id=${id}`,
  );
}

export function createCategory(data: MesDeviceCategoryApi.Category) {
  return requestClient.post('/mes/resource/device/category/create', data);
}

export function updateCategory(data: MesDeviceCategoryApi.Category) {
  return requestClient.put('/mes/resource/device/category/update', data);
}

export function deleteCategory(id: number) {
  return requestClient.delete(`/mes/resource/device/category/delete?id=${id}`);
}
