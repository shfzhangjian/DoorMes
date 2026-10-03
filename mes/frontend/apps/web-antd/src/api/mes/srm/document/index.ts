import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmDocumentApi {
  export interface Document {
    id?: number;
    bizType?: string;
    docNo?: string;
    title?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    materialName?: string;
    status?: string;
    applicantId?: number;
    applicantName?: string;
    applyDept?: string;
    applyTime?: string;
    dueDate?: string;
    totalScore?: number;
    evalGrade?: string;
    bizCategory?: string;
    bizLevel?: string;
    periodType?: string;
    evalYear?: number;
    evalQuarter?: number;
    payloadJson?: string;
    remark?: string;
    version?: number;
    createTime?: string;
    updateTime?: string;
  }
}

export function getDocumentPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<SrmDocumentApi.Document>>(
    '/mes/srm/document/page',
    { params },
  );
}

export function getDocument(id: number) {
  return requestClient.get<SrmDocumentApi.Document>(
    `/mes/srm/document/get?id=${id}`,
  );
}

export function createDocument(data: SrmDocumentApi.Document) {
  return requestClient.post<number>('/mes/srm/document/create', data);
}

export function updateDocument(data: SrmDocumentApi.Document) {
  return requestClient.put<boolean>('/mes/srm/document/update', data);
}

export function deleteDocument(id: number) {
  return requestClient.delete<boolean>(`/mes/srm/document/delete?id=${id}`);
}
