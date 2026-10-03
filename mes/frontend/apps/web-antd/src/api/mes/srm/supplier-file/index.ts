import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSupplierFileApi {
  export interface SupplierFile {
    id?: number;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    fileType?: string;
    fileName?: string;
    providedProduct?: string;
    productModel?: string;
    inspectionAgency?: string;
    reportCode?: string;
    effectDate?: string;
    expiryDate?: string;
    fileStatus?: 'EXPIRED' | 'VALID' | 'WARNING';
    warningDays?: number;
    daysLeft?: number;
    standardCompliant?: 'N' | 'Y';
    validityMonths?: number;
    expiryRule?: string;
    payloadJson?: string;
    attachmentName?: string;
    attachmentUrl?: string;
    sourceBizType?: string;
    sourceBizId?: number;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }
}

export function getSupplierFilePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<SrmSupplierFileApi.SupplierFile>>(
    '/mes/srm/supplier-file/page',
    { params },
  );
}

export function getSupplierFile(id: number) {
  return requestClient.get<SrmSupplierFileApi.SupplierFile>(
    `/mes/srm/supplier-file/get?id=${id}`,
  );
}

export function createSupplierFile(data: SrmSupplierFileApi.SupplierFile) {
  return requestClient.post<number>('/mes/srm/supplier-file/create', data);
}

export function updateSupplierFile(data: SrmSupplierFileApi.SupplierFile) {
  return requestClient.put<boolean>('/mes/srm/supplier-file/update', data);
}

export function deleteSupplierFile(id: number) {
  return requestClient.delete<boolean>(
    `/mes/srm/supplier-file/delete?id=${id}`,
  );
}
