import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcBomApi {
  export interface Bom {
    bomCode?: string;
    bomName?: string;
    productMaterialId?: number;
    productMaterialCode?: string;
    productMaterialName?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    productSpec?: string;
    recipeId?: number;
    recipeCode?: string;
    recipeName?: string;
    routeId?: number;
    routeCode?: string;
    versionNo?: string;
    bomType?: string;
    yieldRate?: number;
    pressSlotContinuousCheckCount?: number;
    status?: number | string;
    remark?: string;
    id: number;
    bomItems?: BomItem[];
  }

  export interface SimpleItem {
    id: number;
    code?: string;
    name?: string;
    status?: number | string;
  }

  export interface SelectOption {
    value: number;
    label: string;
    code?: string;
    status?: number;
  }

  export interface ProductModelOption {
    value: number;
    label: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    code?: string;
    modelName?: string;
    bomType?: string;
    recipeId?: number;
    recipeCode?: string;
    recipeName?: string;
    sizeSpec?: string;
    sizeName?: string;
    status?: number | string;
  }

  export interface BomItem {
    bomId?: number;
    bomCode?: string;
    lineNo?: number;
    componentMaterialId?: number;
    componentMaterialCode?: string;
    componentMaterialName?: string;
    componentType?: string;
    baseQty?: number;
    lossRate?: number;
    supplyMode?: string;
    issueOperationCode?: string;
    issueOperationName?: string;
    consumeGroupCode?: string;
    requiredFlag?: boolean;
    conditionJson?: string;
    uom?: string;
    remark?: string;
    id: number;
  }

  export interface ProductImportResp {
    totalRows?: number;
    skippedRows?: number;
    successCount?: number;
    itemCount?: number;
    failureCount?: number;
    messages?: string[];
    failures?: string[];
  }
}

export async function getBomPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcBomApi.Bom>>('/mes/hc/base/bom/page', { params });
}

export async function getBom(id: number) {
  return requestClient.get<MesHcBomApi.Bom>(`/mes/hc/base/bom/get?id=${id}`);
}

export async function getBomDetail(id: number) {
  return requestClient.get<MesHcBomApi.Bom>(`/mes/hc/base/bom/get-detail?id=${id}`);
}

export async function createBom(data: MesHcBomApi.Bom) {
  return requestClient.post<number>('/mes/hc/base/bom/create', data);
}

export async function updateBom(data: MesHcBomApi.Bom) {
  return requestClient.put<boolean>('/mes/hc/base/bom/update', data);
}

export async function deleteBom(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/bom/delete?id=${id}`);
}

export async function deleteBomList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/bom/delete-list?ids=${ids.join(',')}`);
}

export async function exportBom(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/bom/export-excel', { params });
}

export async function importProductBom(file: File, overwrite = true) {
  return requestClient.upload<MesHcBomApi.ProductImportResp>('/mes/hc/base/bom/import-product-bom', {
    file,
    overwrite,
  });
}

export async function getBomSimpleList() {
  return requestClient.get<MesHcBomApi.SimpleItem[]>('/mes/hc/base/bom/simple-list');
}

export async function getBomSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/bom/simple-map');
}

export async function getBomSelectOptions() {
  return requestClient.get<MesHcBomApi.SelectOption[]>('/mes/hc/base/bom/select-options');
}

export async function getBomItemListByParentId(parentId: number) {
  return requestClient.get<MesHcBomApi.BomItem[]>(`/mes/hc/base/bom/mes_md_bom_item/list-by-parent-id?parentId=${parentId}`);
}

export async function getBomSelectOptionsByMaterialId(materialId: number) {
  return requestClient.get<MesHcBomApi.SelectOption[]>(`/mes/hc/base/bom/select-options-by-material-id?materialId=${materialId}`);
}

export async function getBomProductModelOptions(params?: {
  keyword?: string;
  productMaterialId?: number;
}) {
  return requestClient.get<MesHcBomApi.ProductModelOption[]>(
    '/mes/hc/base/bom/product-model-options',
    { params },
  );
}
