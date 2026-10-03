import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcProductModelApi {
  export interface ProductModel {
    id?: number;
    modelCode?: string;
    modelName?: string;
    modelAlias?: string;
    modelLevel?: 'FAMILY' | 'MODEL';
    parentModelId?: number;
    parentModelCode?: string;
    parentModelName?: string;
    modelRuleId?: number;
    modelRuleCode?: string;
    modelRuleName?: string;
    productType?: string;
    prodType?: string;
    prodTypeName?: string;
    categoryId?: number;
    categoryCode?: string;
    categoryName?: string;
    sizeSpec?: string;
    sizeName?: string;
    retentionPeriodValue?: number;
    retentionPeriodUnit?: 'DAY' | 'MONTH';
    recipeId?: number;
    recipeCode?: string;
    recipeName?: string;
    defaultRouteId?: number;
    defaultRouteCode?: string;
    defaultRouteName?: string;
    segmentSnapshotJson?: string;
    sourceType?: string;
    status?: string;
    referencedFlag?: boolean;
    remark?: string;
    createTime?: string;
    modelSegments?: ProductModelSegment[];
    modelMaterials?: ProductModelMaterial[];
  }

  export interface ProductModelSegment {
    id?: number;
    modelId?: number;
    modelCode?: string;
    ruleId?: number;
    ruleCode?: string;
    ruleItemId?: number;
    itemCode?: string;
    itemName?: string;
    segmentValue?: string;
    segmentText?: string;
    dictId?: number;
    sort?: number;
  }

  export interface ProductModelMaterial {
    id?: number;
    modelId?: number;
    modelCode?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    isDefault?: boolean;
    remark?: string;
  }

  export interface GenerateCodeReq {
    modelRuleId: number;
    segmentValues?: Record<string, string>;
  }

  export interface GenerateCodeResp {
    generatedCode: string;
  }

  export interface SelectOption {
    value: number;
    label: string;
    code?: string;
    modelName?: string;
    modelLevel?: 'FAMILY' | 'MODEL';
    parentModelId?: number;
    parentModelCode?: string;
    parentModelName?: string;
    recipeId?: number;
    recipeCode?: string;
    recipeName?: string;
    sizeSpec?: string;
    sizeName?: string;
    status?: string;
  }
}

export async function getProductModelPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcProductModelApi.ProductModel>>(
    '/mes/hc/base/product-model/page',
    { params },
  );
}

export async function getProductModel(id: number) {
  return requestClient.get<MesHcProductModelApi.ProductModel>(
    `/mes/hc/base/product-model/get?id=${id}`,
  );
}

export async function getProductModelDetail(id: number) {
  return requestClient.get<MesHcProductModelApi.ProductModel>(
    `/mes/hc/base/product-model/get-detail?id=${id}`,
  );
}

export async function createProductModel(data: MesHcProductModelApi.ProductModel) {
  return requestClient.post<number>('/mes/hc/base/product-model/create', data);
}

export async function updateProductModel(data: MesHcProductModelApi.ProductModel) {
  return requestClient.put<boolean>('/mes/hc/base/product-model/update', data);
}

export async function deleteProductModel(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/product-model/delete?id=${id}`);
}

export async function deleteProductModelList(ids: number[]) {
  return requestClient.delete<boolean>(
    `/mes/hc/base/product-model/delete-list?ids=${ids.join(',')}`,
  );
}

export async function exportProductModel(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/product-model/export-excel', { params });
}

export async function generateProductModelCode(data: MesHcProductModelApi.GenerateCodeReq) {
  return requestClient.post<MesHcProductModelApi.GenerateCodeResp>(
    '/mes/hc/base/product-model/generate-code',
    data,
  );
}

export async function getProductModelSelectOptions(
  keyword?: string,
  modelLevel?: 'FAMILY' | 'MODEL',
) {
  return requestClient.get<MesHcProductModelApi.SelectOption[]>(
    '/mes/hc/base/product-model/select-options',
    { params: { keyword, modelLevel } },
  );
}

export async function getProductModelFamilyOptions(params?: {
  keyword?: string;
}) {
  return getProductModelSelectOptions(params?.keyword, 'FAMILY');
}
