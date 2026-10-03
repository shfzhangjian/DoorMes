import type { PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

import type { MesNcrApi } from '../ncr';

const BASE_URL = '/mes/qms-raw-material-nc-record';

export function getRawMaterialNcrPage(params: MesNcrApi.NcrPageReq) {
  return requestClient.get<PageResult<MesNcrApi.NcrRecord>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getRawMaterialNcrRecord(id: number) {
  return requestClient.get<MesNcrApi.NcrRecord>(`${BASE_URL}/get?id=${id}`);
}

export function createRawMaterialNcrRecord(data: MesNcrApi.NcrRecord) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function updateRawMaterialNcrRecord(data: MesNcrApi.NcrRecord) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function relaunchRawMaterialNcrRecord(id: number) {
  return requestClient.post<number>(`${BASE_URL}/relaunch?id=${id}`);
}

export function restoreRawMaterialNcrRecord(
  data: MesNcrApi.RawMaterialRestoreReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/restore`, data);
}

export function getRawMaterialNcrRestorePreview(id: number) {
  return requestClient.get<MesNcrApi.RestorePreview>(
    `${BASE_URL}/restore-preview?id=${id}`,
  );
}

export function getRawMaterialNcrSourceInspectionPage(
  params: MesNcrApi.RawMaterialInspectionPageReq,
) {
  return requestClient.get<PageResult<MesNcrApi.RawMaterialInspection>>(
    `${BASE_URL}/source-inspection/page`,
    { params },
  );
}

export function createRawMaterialNcrFromInspection(
  data: MesNcrApi.CreateFromInspectionReq,
) {
  return requestClient.post<number>(`${BASE_URL}/create-from-inspection`, data);
}

export function batchCreateRawMaterialNcrFromInspections(
  data: MesNcrApi.BatchCreateFromInspectionReq,
) {
  return requestClient.post<MesNcrApi.CreateFromInspectionResult[]>(
    `${BASE_URL}/batch-create-from-inspection`,
    data,
  );
}

export function mergeCreateRawMaterialNcrFromInspections(
  data: MesNcrApi.BatchCreateFromInspectionReq,
) {
  return requestClient.post<number>(
    `${BASE_URL}/merge-create-from-inspection`,
    data,
  );
}

export function submitRawMaterialNcr(data: MesNcrApi.SubmitReq) {
  return requestClient.post<boolean>(`${BASE_URL}/submit`, data);
}

export function withdrawRawMaterialNcr(data: MesNcrApi.WithdrawReq) {
  return requestClient.post<boolean>(`${BASE_URL}/withdraw`, data);
}

export function handleRawMaterialNcr(data: MesNcrApi.HandleReq) {
  return requestClient.post<boolean>(`${BASE_URL}/handle`, data);
}

export function linkRawMaterialNcrException(data: MesNcrApi.LinkExceptionReq) {
  return requestClient.post<boolean>(`${BASE_URL}/link-exception`, data);
}

export function replyRawMaterialNcrDispositionNotify(
  data: MesNcrApi.DispositionNotifyReplyReq,
) {
  return requestClient.post<boolean>(
    `${BASE_URL}/disposition-notify/reply`,
    data,
  );
}

export function delegateRawMaterialNcrMrbReview(
  data: MesNcrApi.MrbReviewDelegateReq,
) {
  return requestClient.post<boolean>(`${BASE_URL}/mrb-review/delegate`, data);
}

export function returnRawMaterialNcr(data: MesNcrApi.ReturnReq) {
  return requestClient.post<boolean>(`${BASE_URL}/return`, data);
}

export function finalApproveRawMaterialNcr(data: MesNcrApi.FinalApproveReq) {
  return requestClient.post<boolean>(`${BASE_URL}/final-approve`, data);
}

export function stockDisposeRawMaterialNcr(data: MesNcrApi.StockDisposeReq) {
  return requestClient.post<boolean>(`${BASE_URL}/stock-dispose`, data);
}

export function getRawMaterialNcrFlowLogs(ncRecordId: number) {
  return requestClient.get<MesNcrApi.FlowLog[]>(`${BASE_URL}/flow-log/list`, {
    params: { ncRecordId },
  });
}

export function exportRawMaterialNcrExcel(params: MesNcrApi.NcrPageReq) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}
