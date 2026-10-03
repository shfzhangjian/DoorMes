import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesSaleOrderApi {
  export interface MesSaleOrder {
    id?: number;
    orderNo: string;
    erpNo?: string;
    orderLineNo?: string;
    customerId?: number;
    customerName: string;
    productId?: number;
    productCode?: string;
    productName?: string;
    productSpec?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    sizeSpec?: string;
    sizeName?: string;
    quantity: number;
    plannedQty?: number;
    remainQty?: number;
    unitId?: number;
    unitCode?: string;
    unitName?: string;
    unit?: string;
    deliveryDate: string;
    status?: string;
    statusName?: string;
    auditorId?: number;
    auditorName?: string;
    auditTime?: string;
    remark?: string;
    createTime?: string;
  }

  export interface PageReqVO extends PageParam {
    orderNo?: string;
    erpNo?: string;
    customerName?: string;
    productCode?: string;
    productName?: string;
    productSpec?: string;
    keyword?: string;
    status?: string;
    planSelectable?: boolean;
  }
}

export async function getSaleOrderPage(params: MesSaleOrderApi.PageReqVO) {
  return requestClient.get<PageResult<MesSaleOrderApi.MesSaleOrder>>(
    '/mes/plan/sale-order/page',
    { params },
  );
}

export async function getSaleOrder(id: number) {
  return requestClient.get<MesSaleOrderApi.MesSaleOrder>(
    `/mes/plan/sale-order/get?id=${id}`,
  );
}

export async function createSaleOrder(_data: MesSaleOrderApi.MesSaleOrder) {
  return requestClient.post<number>('/mes/plan/sale-order/create', _data);
}

export async function updateSaleOrder(_data: MesSaleOrderApi.MesSaleOrder) {
  return requestClient.put<boolean>('/mes/plan/sale-order/update', _data);
}

export async function deleteSaleOrder(id: number) {
  return requestClient.delete<boolean>(`/mes/plan/sale-order/delete?id=${id}`);
}

export async function auditSaleOrder(id: number) {
  return requestClient.put<boolean>(`/mes/plan/sale-order/audit?id=${id}`);
}

export async function syncErpOrders() {
  return requestClient.post<boolean>('/mes/plan/sale-order/sync-erp');
}
