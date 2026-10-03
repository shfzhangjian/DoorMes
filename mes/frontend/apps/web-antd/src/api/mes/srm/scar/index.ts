import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmScarApi {
  export interface Scar {
    id?: number;
    scarNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    materialModel?: string;
    batchNo?: string;
    quantity?: number;
    issueDate?: string;
    issueDesc?: string;
    replyDate?: string;
    replyDesc?: string;
    status?: 'WAIT_SUPPLIER' | 'CLOSED';
    applicantId?: number;
    applicantName?: string;
    applyTime?: string; // 登记时间，yyyy-MM-dd HH:mm:ss
    remark?: string;
    version?: number;
    createTime?: string;
    updateTime?: string;
  }
}

export function getScarPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<SrmScarApi.Scar>>(
    '/mes/srm/scar/page',
    { params },
  );
}

export function getScar(id: number) {
  return requestClient.get<SrmScarApi.Scar>(`/mes/srm/scar/get?id=${id}`);
}

export function createScar(data: SrmScarApi.Scar) {
  return requestClient.post<number>('/mes/srm/scar/create', data);
}

export function updateScar(data: SrmScarApi.Scar) {
  return requestClient.put<boolean>('/mes/srm/scar/update', data);
}

export function deleteScar(id: number) {
  return requestClient.delete<boolean>(`/mes/srm/scar/delete?id=${id}`);
}