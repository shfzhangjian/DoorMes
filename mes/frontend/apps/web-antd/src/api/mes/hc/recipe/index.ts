import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcRecipeApi {
  export interface Recipe {
    recipeCode?: string;
    recipeName?: string;
    recipeType?: string;
    modelCode?: string;
    usageCount?: number;
    productMaterialId?: number;
    productMaterialCode?: string;
    productMaterialName?: string;
    versionNo?: string;
    solidContentStd?: number;
    viscosityStd?: number;
    yieldRate?: number;
    effectiveDate?: string;
    expireDate?: string;
    status?: number;
    remark?: string;
    id: number;
    recipeItems?: RecipeItem[];
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
    recipeType?: string;
    modelCode?: string;
    status?: number;
  }

  export interface RecipeItem {
    recipeId?: number;
    recipeCode?: string;
    lineNo?: number;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    materialType?: string;
    theoryQty?: number;
    lossRate?: number;
    uom?: string;
    keyControlFlag?: boolean;
    remark?: string;
    id: number;
  }
}

export async function getRecipePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcRecipeApi.Recipe>>('/mes/hc/base/recipe/page', { params });
}

export async function getRecipe(id: number) {
  return requestClient.get<MesHcRecipeApi.Recipe>(`/mes/hc/base/recipe/get?id=${id}`);
}

export async function getRecipeDetail(id: number) {
  return requestClient.get<MesHcRecipeApi.Recipe>(`/mes/hc/base/recipe/get-detail?id=${id}`);
}

export async function createRecipe(data: MesHcRecipeApi.Recipe) {
  return requestClient.post<number>('/mes/hc/base/recipe/create', data);
}

export async function updateRecipe(data: MesHcRecipeApi.Recipe) {
  return requestClient.put<boolean>('/mes/hc/base/recipe/update', data);
}

export async function deleteRecipe(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/recipe/delete?id=${id}`);
}

export async function deleteRecipeList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/recipe/delete-list?ids=${ids.join(',')}`);
}

export async function exportRecipe(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/recipe/export-excel', { params });
}

export async function getRecipeSimpleList() {
  return requestClient.get<MesHcRecipeApi.SimpleItem[]>('/mes/hc/base/recipe/simple-list');
}

export async function getRecipeSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/recipe/simple-map');
}

export async function getRecipeSelectOptions() {
  return requestClient.get<MesHcRecipeApi.SelectOption[]>('/mes/hc/base/recipe/select-options');
}

export async function getRecipeItemListByParentId(parentId: number) {
  return requestClient.get<MesHcRecipeApi.RecipeItem[]>(
    `/mes/hc/base/recipe/mes_md_recipe_item/list-by-parent-id?parentId=${parentId}`,
  );
}

export async function getRecipeSelectOptionsByMaterialId(materialId: number) {
  return requestClient.get<MesHcRecipeApi.SelectOption[]>(
    `/mes/hc/base/recipe/select-options-by-material-id?materialId=${materialId}`,
  );
}
