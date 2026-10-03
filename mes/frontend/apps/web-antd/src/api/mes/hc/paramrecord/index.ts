import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcParamRecordApi {
  export interface ParamRecord {
    id?: number;
    reportId?: number;
    reportNo?: string;
    paramCode?: string;
    paramName?: string;
    paramValue?: string;
    valueNum?: number | string | null;
    uom?: string;
    judgeResult?: string;
    createTime?: string;
  }

  export interface ParamRecordContext {
    reportId?: number;
    reportNo?: string;
    workOrderId?: number;
    workOrderNo?: string;
    operationCode?: string;
    operationName?: string;
    routeId?: number;
    routeCode?: string;
    routeName?: string;
    productMaterialId?: number;
    productMaterialCode?: string;
    productMaterialName?: string;
    lotNo?: string;
    paramTemplateJson?: string;
    records?: ParamRecord[];
  }

  export interface ParamRecordItemSubmit {
    paramCode: string;
    paramName: string;
    paramValue: string;
    valueNum?: number | string | null;
    uom?: string;
    judgeResult?: string;
  }

  export interface ParamRecordSubmitReq {
    reportId: number;
    reportNo: string;
    items: ParamRecordItemSubmit[];
  }
}

export async function getParamRecordContext(params: { reportId?: number; reportNo?: string }) {
  return requestClient.get<MesHcParamRecordApi.ParamRecordContext>('/mes/hc/execution/param-record/context', {
    params,
  });
}

export async function submitParamRecord(data: MesHcParamRecordApi.ParamRecordSubmitReq) {
  return requestClient.post<boolean>('/mes/hc/execution/param-record/submit', data);
}

export async function getParamRecordPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcParamRecordApi.ParamRecord>>('/mes/hc/execution/param-record/page', {
    params,
  });
}

