import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

import { normalizeMesDateTime } from '../shared/date-time';

export namespace MesHcToolingConsumableLedgerApi {
  export interface Ledger {
    batchNo?: string;
    balanceQty?: number;
    consumableType?: string;
    consumableTypeName?: string;
    consumedQty?: number;
    createTime?: string;
    erpMaterialCode?: string;
    id?: number;
    model?: string;
    processCode?: string;
    processName?: string;
    receiveQty?: number;
    receiveTime?: string;
    receiverId?: number;
    receiverName?: string;
    remark?: string;
    returnAuthTime?: string;
    returnAuthUserId?: number;
    returnAuthUserName?: string;
    returnQty?: number;
    returnReason?: string;
    uomCode?: string;
    uomId?: number;
    uomName?: string;
    uom?: string;
    usageStatus?: string;
    usedUpActualDate?: string;
    usedUpAuthTime?: string;
    usedUpAuthUserId?: number;
    usedUpAuthUserName?: string;
    usedUpRemainQty?: number;
    usedUpRemark?: string;
    updateTime?: string;
  }

  export interface MarkUsedUpReq {
    id: number;
    usedUpActualDate?: string;
    usedUpAuthUserId?: number;
    usedUpAuthUserName: string;
    usedUpRemainQty?: number;
    balanceQty?: number;
    usedUpRemark?: string;
  }

  export interface ReturnReq {
    id: number;
    returnAuthUserId?: number;
    returnAuthUserName: string;
    returnReason: string;
  }

  export interface Consume {
    batchNo?: string;
    consumeQty?: number;
    consumeTime?: string;
    consumableType?: string;
    consumableTypeName?: string;
    createTime?: string;
    creator?: string;
    creatorName?: string;
    consumeSource?: string;
    consumeType?: string;
    glueBoardStockId?: number;
    glueBoardUsageId?: number;
    id?: number;
    ledgerId?: number;
    model?: string;
    planNo?: string;
    planOperationId?: number;
    processCode?: string;
    processName?: string;
    productBatchNo?: string;
    productInputQty?: number;
    productMaterialCode?: string;
    productModelCode?: string;
    productOutputQty?: number;
    productionBatchNo?: string;
    remark?: string;
    uom?: string;
    uomCode?: string;
    uomId?: number;
    uomName?: string;
    updateTime?: string;
  }

  export interface Balance {
    balanceQty?: number;
    batchNo?: string;
    consumedQty?: number;
    consumableType?: string;
    consumableTypeName?: string;
    erpMaterialCode?: string;
    ledgerId?: number;
    model?: string;
    processCode?: string;
    processName?: string;
    receiveQty?: number;
    receiveTime?: string;
    receiverId?: number;
    receiverName?: string;
    remark?: string;
    uom?: string;
    uomCode?: string;
    uomId?: number;
    uomName?: string;
  }

  export interface ImportResp {
    createdConsumeCount?: number;
    createdLedgerCount?: number;
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    totalRows?: number;
    updatedConsumeCount?: number;
    updatedLedgerCount?: number;
  }

  export interface ProcessConsumable {
    consumableType?: string;
    consumableTypeName?: string;
    createTime?: string;
    defaultBatchNo?: string;
    defaultErpMaterialCode?: string;
    defaultUomCode?: string;
    defaultUomId?: number;
    defaultUomName?: string;
    id?: number;
    processCode?: string;
    processName?: string;
    remark?: string;
    status?: number;
    updateTime?: string;
  }
}

const BASE = '/mes/hc/base/tooling-consumable-ledger';

function normalizeLedgerPayload(data: MesHcToolingConsumableLedgerApi.Ledger) {
  return {
    ...data,
    receiveTime: normalizeMesDateTime(data.receiveTime),
  };
}

function normalizeConsumePayload(data: MesHcToolingConsumableLedgerApi.Consume) {
  return {
    ...data,
    consumeTime: normalizeMesDateTime(data.consumeTime),
  };
}

export function getToolingConsumableLedgerPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcToolingConsumableLedgerApi.Ledger>>(`${BASE}/page`, { params });
}

export function getToolingConsumableBalancePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcToolingConsumableLedgerApi.Balance>>(`${BASE}/balance/page`, { params });
}

export function getToolingConsumableLedger(id: number) {
  return requestClient.get<MesHcToolingConsumableLedgerApi.Ledger>(`${BASE}/get?id=${id}`);
}

export function createToolingConsumableLedger(data: MesHcToolingConsumableLedgerApi.Ledger) {
  return requestClient.post<number>(`${BASE}/create`, normalizeLedgerPayload(data));
}

export function updateToolingConsumableLedger(data: MesHcToolingConsumableLedgerApi.Ledger) {
  return requestClient.put<boolean>(`${BASE}/update`, normalizeLedgerPayload(data));
}

export function updateToolingConsumableLedgerUsageStatus(id: number, usageStatus: string) {
  return requestClient.put<boolean>(
    `${BASE}/usage-status/update?id=${id}&usageStatus=${encodeURIComponent(usageStatus)}`,
  );
}

export function markToolingConsumableLedgerUsedUp(data: MesHcToolingConsumableLedgerApi.MarkUsedUpReq) {
  return requestClient.put<boolean>(`${BASE}/used-up/mark`, data);
}

export function returnToolingConsumableLedger(data: MesHcToolingConsumableLedgerApi.ReturnReq) {
  return requestClient.put<boolean>(`${BASE}/return/register`, data);
}

export function deleteToolingConsumableLedger(id: number) {
  return requestClient.delete<boolean>(`${BASE}/delete?id=${id}`);
}

export function deleteToolingConsumableLedgerList(ids: number[]) {
  return requestClient.delete<boolean>(`${BASE}/delete-list?ids=${ids.join(',')}`);
}

export function getToolingConsumableConsumePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcToolingConsumableLedgerApi.Consume>>(`${BASE}/consume/page`, { params });
}

export function getToolingConsumableConsume(id: number) {
  return requestClient.get<MesHcToolingConsumableLedgerApi.Consume>(`${BASE}/consume/get?id=${id}`);
}

export function createToolingConsumableConsume(data: MesHcToolingConsumableLedgerApi.Consume) {
  return requestClient.post<number>(`${BASE}/consume/create`, normalizeConsumePayload(data));
}

export function updateToolingConsumableConsume(data: MesHcToolingConsumableLedgerApi.Consume) {
  return requestClient.put<boolean>(`${BASE}/consume/update`, normalizeConsumePayload(data));
}

export function deleteToolingConsumableConsume(id: number) {
  return requestClient.delete<boolean>(`${BASE}/consume/delete?id=${id}`);
}

export function deleteToolingConsumableConsumeList(ids: number[]) {
  return requestClient.delete<boolean>(`${BASE}/consume/delete-list?ids=${ids.join(',')}`);
}

export function exportToolingConsumableLedger(params: Record<string, any>) {
  return requestClient.download(`${BASE}/export-excel`, { params });
}

export function importToolingConsumableLedger(file: File, processCode?: string) {
  return requestClient.upload<MesHcToolingConsumableLedgerApi.ImportResp>(`${BASE}/import`, { file, processCode });
}

export function getToolingProcessConsumablePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcToolingConsumableLedgerApi.ProcessConsumable>>(`${BASE}/config/page`, {
    params,
  });
}

export function getToolingProcessConsumable(id: number) {
  return requestClient.get<MesHcToolingConsumableLedgerApi.ProcessConsumable>(`${BASE}/config/get?id=${id}`);
}

export function createToolingProcessConsumable(data: MesHcToolingConsumableLedgerApi.ProcessConsumable) {
  return requestClient.post<number>(`${BASE}/config/create`, data);
}

export function updateToolingProcessConsumable(data: MesHcToolingConsumableLedgerApi.ProcessConsumable) {
  return requestClient.put<boolean>(`${BASE}/config/update`, data);
}

export function deleteToolingProcessConsumable(id: number) {
  return requestClient.delete<boolean>(`${BASE}/config/delete?id=${id}`);
}

export function deleteToolingProcessConsumableList(ids: number[]) {
  return requestClient.delete<boolean>(`${BASE}/config/delete-list?ids=${ids.join(',')}`);
}

export function getToolingProcessConsumableListByProcess(processCode?: string) {
  return requestClient.get<MesHcToolingConsumableLedgerApi.ProcessConsumable[]>(
    `${BASE}/config/list-by-process`,
    { params: { processCode } },
  );
}
