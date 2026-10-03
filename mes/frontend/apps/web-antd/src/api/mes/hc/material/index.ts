import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcMaterialApi {
  export interface Material {
    materialCode?: string;
    materialName?: string;
    materialShortName?: string;
    materialType?: number;
    materialCategoryId?: number;
    materialCategoryName?: string;
    productLevel?: string;
    specModel?: string;
    materialCodeRuleId?: number;
    modelCodeRuleId?: number;
    productModelId?: number;
    modelCode?: string;
    productModelName?: string;
    modelSegmentsJson?: string;
    baseUnitId?: number;
    baseUnitCode?: string;
    baseUnitName?: string;
    baseUom?: string;
    stockUnitId?: number;
    stockUnitCode?: string;
    stockUnitName?: string;
    stockUom?: string;
    produceUnitId?: number;
    produceUnitCode?: string;
    produceUnitName?: string;
    produceUom?: string;
    batchManaged?: boolean;
    barcodeManaged?: boolean;
    defaultRouteId?: number;
    defaultRouteCode?: string;
    defaultRouteName?: string;
    defaultRecipeId?: number;
    defaultRecipeCode?: string;
    defaultRecipeName?: string;
    defaultBomId?: number;
    defaultBomCode?: string;
    defaultBomName?: string;
    qualityControlMode?: string;
    materialStatus?: number;
    mesSelectVisible?: boolean;
    remark?: string;
    id: number;
    materialExtAttrs?: MaterialExtAttr[];
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

  export interface MaterialExtAttr {
    materialId?: number;
    materialCode?: string;
    attrCode?: string;
    attrName?: string;
    attrValue?: string;
    valueType?: string;
    sort?: number;
    id: number;
  }

  export interface MaterialBindingPreviewBomItem {
    bomItemId?: number;
    lineNo?: number;
    issueOperationCode?: string;
    componentMaterialId?: number;
    componentMaterialCode?: string;
    componentMaterialName?: string;
    baseQty?: number;
    lossRate?: number;
    uom?: string;
    consumeGroupCode?: string;
    conditionJson?: string;
    requiredFlag?: boolean;
    consumeGroupMatched?: boolean;
    conditionMatched?: boolean;
    matched?: boolean;
  }

  export interface MaterialBindingPreviewBom {
    bomId?: number;
    bomCode?: string;
    bomName?: string;
    bomType?: string;
    status?: number;
    productMaterialId?: number;
    productMaterialCode?: string;
    routeId?: number;
    routeCode?: string;
    materialMatched?: boolean;
    routeMatched?: boolean;
    matched?: boolean;
    matchedItemCount?: number;
    bomItems?: MaterialBindingPreviewBomItem[];
  }

  
  export interface MaterialCodeGenerateResp {
    materialCode?: string;
  }

  export interface MaterialBindingPreview {
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    modelCodeRuleId?: number;
    modelCode?: string;
    modelSegmentsJson?: string;
    defaultRecipeId?: number;
    defaultRecipeCode?: string;
    defaultRecipeName?: string;
    suggestedRecipeId?: number;
    suggestedRecipeCode?: string;
    suggestedRecipeName?: string;
    defaultRouteId?: number;
    defaultRouteCode?: string;
    defaultRouteName?: string;
    suggestedRouteId?: number;
    suggestedRouteCode?: string;
    suggestedRouteName?: string;
    consumeGroupCodes?: string[];
    bomCandidates?: MaterialBindingPreviewBom[];
  }
}

export async function getMaterialPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcMaterialApi.Material>>('/mes/hc/base/material/page', { params });
}

export async function getMaterial(id: number) {
  return requestClient.get<MesHcMaterialApi.Material>(`/mes/hc/base/material/get?id=${id}`);
}

export async function getMaterialDetail(id: number) {
  return requestClient.get<MesHcMaterialApi.Material>(`/mes/hc/base/material/get-detail?id=${id}`);
}

export async function getMaterialBindingPreview(id: number) {
  return requestClient.get<MesHcMaterialApi.MaterialBindingPreview>(`/mes/hc/base/material/binding-preview?id=${id}`);
}

export async function previewMaterialBinding(data: MesHcMaterialApi.Material) {
  return requestClient.post<MesHcMaterialApi.MaterialBindingPreview>('/mes/hc/base/material/binding-preview', data);
}

export async function generateMaterialCode(data: MesHcMaterialApi.Material) {
  return requestClient.post<MesHcMaterialApi.MaterialCodeGenerateResp>('/mes/hc/base/material/generate-material-code', data);
}

export async function createMaterial(data: MesHcMaterialApi.Material) {
  return requestClient.post<number>('/mes/hc/base/material/create', data);
}

export async function updateMaterial(data: MesHcMaterialApi.Material) {
  return requestClient.put<boolean>('/mes/hc/base/material/update', data);
}

export async function deleteMaterial(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/material/delete?id=${id}`);
}

export async function deleteMaterialList(ids: number[]) {
  return requestClient.delete<boolean>(`/mes/hc/base/material/delete-list?ids=${ids.join(',')}`);
}

export async function exportMaterial(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/material/export-excel', { params });
}

export async function getMaterialSimpleList() {
  return requestClient.get<MesHcMaterialApi.SimpleItem[]>('/mes/hc/base/material/simple-list');
}

export async function getMaterialSimpleMap() {
  return requestClient.get<Record<string, string>>('/mes/hc/base/material/simple-map');
}

export async function getMaterialSelectOptions() {
  return requestClient.get<MesHcMaterialApi.SelectOption[]>('/mes/hc/base/material/select-options');
}

export async function getMaterialExtAttrListByParentId(parentId: number) {
  return requestClient.get<MesHcMaterialApi.MaterialExtAttr[]>(`/mes/hc/base/material/mes_md_material_ext_attr/list-by-parent-id?parentId=${parentId}`);
}

