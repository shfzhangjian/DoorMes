import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesWetPoreSelfCheckApi {
  export type ImageStatus = 'MISSING' | 'UPLOADED';
  export type SelfCheckResult = 'NG' | 'OK' | string;

  export interface Record {
    clientKey?: string;
    createTime?: string;
    id: string;
    imageStatus: ImageStatus;
    inspector?: string;
    motherBatchNo?: string;
    operationName?: string;
    /** 历史单图字段，新的页面统一读取 photos。 */
    photo?: string;
    photos?: string[];
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    productMaterialCode?: string;
    productModel?: string;
    recordIndex: number;
    recordStatus?: string;
    recordTime?: string;
    recordUserName?: string;
    remark?: string;
    selfCheckResult?: SelfCheckResult;
    selfCheckTime?: string;
    stationRecordId: number;
  }

  export interface PageReq extends PageParam {
    imageStatus?: string;
    keyword?: string;
    motherBatchNo?: string;
    planNo?: string;
    productMaterialCode?: string;
    productModel?: string;
    selfCheckResult?: string;
    selfCheckTime?: string[];
  }

  export interface PhotoReq {
    clientKey?: string;
    /** 历史单图入参，保留用于兼容旧调用方。 */
    photo?: string;
    photos?: string[];
    recordIndex: number;
    stationRecordId: number;
  }
}

export function getWetPoreSelfCheckPage(
  params: MesWetPoreSelfCheckApi.PageReq,
) {
  return requestClient.get<PageResult<MesWetPoreSelfCheckApi.Record>>(
    '/mes/quality/wet-pore-self-check/page',
    { params },
  );
}

export function updateWetPoreSelfCheckPhotos(
  data: MesWetPoreSelfCheckApi.PhotoReq,
) {
  return requestClient.put<MesWetPoreSelfCheckApi.Record>(
    '/mes/quality/wet-pore-self-check/photo',
    data,
  );
}
