import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcVisualPrintDesignerApi {
  export type LabelKind = 'boxFront' | 'cleanBag' | 'customerSide' | 'padBack';

  export interface CustomerInfo {
    id?: number;
    masterSourceRow?: number;
    productItemId?: number;
    rowKey?: string;
    sourceRow?: number;
    serialNo?: string;
    customer?: string;
    productType?: string;
    sizeMm?: string;
    padBackLabelImageId?: string;
    padBackLabelImageFile?: string;
    padBackDesignId?: number;
    padBackSharedCount?: number;
    cleanBagLabelImageId?: string;
    cleanBagLabelImageFile?: string;
    cleanBagDesignId?: number;
    cleanBagSharedCount?: number;
    boxFrontLabelImageId?: string;
    boxFrontLabelImageFile?: string;
    boxFrontDesignId?: number;
    boxFrontSharedCount?: number;
    customerSideLabelImageId?: string;
    customerSideLabelImageFile?: string;
    customerSideDesignId?: number;
    customerSideSharedCount?: number;
    customerSideSize?: string;
    customerSideMethod?: string;
    shippingMethod?: string;
    needPaperCoa?: string;
    needEcoa?: string;
    hasMark?: string;
    deliveryNote?: string;
    shipmentFilePackageMethod?: string;
    customerSideTemplate?: string;
    specialRemark?: string;
    status?: number;
    importBatchNo?: string;
    createTime?: string;
    updateTime?: string;
    designs?: Design[];
    productItems?: ProductItem[];
  }

  export interface ProductItem {
    id?: number;
    customerInfoId?: number;
    sourceRow?: number;
    productType?: string;
    sizeMm?: string;
    padBackLabelImageId?: string;
    padBackLabelImageFile?: string;
    cleanBagLabelImageId?: string;
    cleanBagLabelImageFile?: string;
    boxFrontLabelImageId?: string;
    boxFrontLabelImageFile?: string;
    customerSideLabelImageId?: string;
    customerSideLabelImageFile?: string;
    status?: number;
    importBatchNo?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface Design {
    id?: number;
    forceNew?: boolean;
    bindingId?: number;
    customerInfoId?: number;
    productItemId?: number;
    labelKind?: LabelKind | string;
    labelName?: string;
    widthMm?: number;
    heightMm?: number;
    dpi?: number;
    imageId?: string;
    imageFile?: string;
    imageWidthPx?: number;
    imageHeightPx?: number;
    btwTemplateRootDir?: string;
    btwCallFile?: string;
    designJson?: string;
    sharedCount?: number;
    linkedProductItems?: ProductItem[];
    status?: number;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface FieldOption {
    excelColumn?: string;
    fieldKey: string;
    fieldLabel: string;
  }

  export interface ImportResp {
    importBatchNo?: string;
    totalCount?: number;
    createCount?: number;
    updateCount?: number;
    skipCount?: number;
    messages?: string[];
  }

  export interface DesignAttachReq {
    customerInfoId: number;
    designId: number;
    labelKind: LabelKind | string;
    productItemIds: number[];
  }
}

const BASE_URL = '/mes/hc/execution/visual-print-designer';

export async function getVisualPrintCustomerInfoPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcVisualPrintDesignerApi.CustomerInfo>>(`${BASE_URL}/page`, {
    params,
  });
}

export async function getVisualPrintCustomerInfoList(params: PageParam & Record<string, any>) {
  return requestClient.get<MesHcVisualPrintDesignerApi.CustomerInfo[]>(`${BASE_URL}/list`, {
    params,
  });
}

export async function getVisualPrintCustomerInfoDetail(id: number) {
  return requestClient.get<MesHcVisualPrintDesignerApi.CustomerInfo>(`${BASE_URL}/get-detail?id=${id}`);
}

export async function createVisualPrintCustomerInfo(data: MesHcVisualPrintDesignerApi.CustomerInfo) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export async function updateVisualPrintCustomerInfo(data: MesHcVisualPrintDesignerApi.CustomerInfo) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export async function deleteVisualPrintCustomerInfo(id: number, productItemId?: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete`, {
    params: { id, productItemId },
  });
}

export async function importVisualPrintCustomerInfoExcel(file: File) {
  return requestClient.upload<MesHcVisualPrintDesignerApi.ImportResp>(`${BASE_URL}/import-excel`, {
    file,
  });
}

export async function getVisualPrintFieldOptions() {
  return requestClient.get<MesHcVisualPrintDesignerApi.FieldOption[]>(`${BASE_URL}/field-options`);
}

export async function getVisualPrintDesigns(customerInfoId: number, productItemId?: number) {
  return requestClient.get<MesHcVisualPrintDesignerApi.Design[]>(`${BASE_URL}/designs`, {
    params: { customerInfoId, productItemId },
  });
}

export async function getVisualPrintDesign(customerInfoId: number, labelKind: string, productItemId?: number) {
  return requestClient.get<MesHcVisualPrintDesignerApi.Design>(`${BASE_URL}/design`, {
    params: { customerInfoId, labelKind, productItemId },
  });
}

export async function saveVisualPrintDesign(data: MesHcVisualPrintDesignerApi.Design) {
  return requestClient.post<number>(`${BASE_URL}/design/save`, data);
}

export async function attachVisualPrintDesign(data: MesHcVisualPrintDesignerApi.DesignAttachReq) {
  return requestClient.post<MesHcVisualPrintDesignerApi.Design>(`${BASE_URL}/design/attach`, data);
}
