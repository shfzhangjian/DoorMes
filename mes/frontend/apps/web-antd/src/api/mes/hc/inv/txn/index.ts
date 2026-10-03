import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcInvTxnApi {
  export interface TxnLog {
    id: number;
    txnNo?: string;
    txnType?: string;
    txnTime?: string;
    warehouseCode?: string;
    warehouseName?: string;
    locationCode?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    modelNo?: string;
    batchNo?: string;
    txnQty?: number;
    beforeQty?: number;
    afterQty?: number;
    uom?: string;
    refDocType?: string;
    refDocId?: number;
    creatorName?: string;
    remark?: string;
    createTime?: string;
  }

  export interface PageReqVO extends PageParam {
    warehouseCode?: string;
    materialCode?: string;
    modelNo?: string;
    batchNo?: string;
    txnTypes?: string[];
    txnNo?: string;
    txnTimeStart?: string;
    txnTimeEnd?: string;
  }
}

export async function getInvTxnLogPage(params: MesHcInvTxnApi.PageReqVO) {
  return requestClient.get<PageResult<MesHcInvTxnApi.TxnLog>>('/mes/hc/inv/txn/page', { params });
}

export async function exportInvTxnLog(params: MesHcInvTxnApi.PageReqVO) {
  return requestClient.download('/mes/hc/inv/txn/export-excel', { params });
}
