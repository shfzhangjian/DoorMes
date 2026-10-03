import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcMaterialCategoryApi {
  export interface MaterialCategory {
    parentId?: number;
    categoryCode?: string;
    categoryName?: string;
    categoryCodePath?: string;
    categoryNamePath?: string;
    categoryPath?: string;
    sort?: number;
    remark?: string;
    status?: number;
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

  export interface TreeNode extends MaterialCategory {
    children?: TreeNode[];
  }
}

export async function getMaterialCategoryPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcMaterialCategoryApi.MaterialCategory>>('/mes/hc/base/material-category/page', { params });
}

export async function getMaterialCategory(id: number) {
  return requestClient.get<MesHcMaterialCategoryApi.MaterialCategory>(`/mes/hc/base/material-category/get?id=${id}`);
}

export async function getMaterialCategoryDetail(id: number) {
  return requestClient.get<MesHcMaterialCategoryApi.MaterialCategory>(`/mes/hc/base/material-category/get-detail?id=${id}`);
}

export async function createMaterialCategory(data: MesHcMaterialCategoryApi.MaterialCategory) {
  return requestClient.post<number>('/mes/hc/base/material-category/create', data);
}

export async function updateMaterialCategory(data: MesHcMaterialCategoryApi.MaterialCategory) {
  return requestClient.put<boolean>('/mes/hc/base/material-category/update', data);
}

export async function moveUpMaterialCategory(id: number) {
  return requestClient.put<boolean>(`/mes/hc/base/material-category/move-up?id=${id}`);
}

export async function moveDownMaterialCategory(id: number) {
  return requestClient.put<boolean>(`/mes/hc/base/material-category/move-down?id=${id}`);
}

export async function deleteMaterialCategory(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/material-category/delete?id=${id}`);
}

export async function deleteMaterialCategoryList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/material-category/delete-list?ids=${ids.join(',')}`);
}

export async function exportMaterialCategory(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/material-category/export-excel', { params });
}

export async function getMaterialCategorySimpleList() {
  return requestClient.get<MesHcMaterialCategoryApi.SimpleItem[]>('/mes/hc/base/material-category/simple-list');
}

export async function getMaterialCategorySimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/material-category/simple-map');
}

export async function getMaterialCategorySelectOptions() {
  return requestClient.get<MesHcMaterialCategoryApi.SelectOption[]>('/mes/hc/base/material-category/select-options');
}

export async function getMaterialCategoryTree() {
  return requestClient.get<MesHcMaterialCategoryApi.TreeNode[]>('/mes/hc/base/material-category/tree');
}
