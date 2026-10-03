import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcGuideClothRecordApi {
  export interface GuideClothRecord {
    id?: number;
    lineName?: string;
    lineCode?: string;
    equipmentId?: number;
    replaceTime?: string;
    replacePlanNo?: string;
    petBatchNo?: string;
    guideClothBatchNo?: string;
    petModel?: string;
    replaceReason?: string;
    useCount?: number;
    currentFlag?: number;
    remark?: string;
    createTime?: string;
  }

  export interface RuntimeInfo {
    lineName?: string;
    lineCode?: string;
    nextUseCount?: number;
    currentUseCount?: number;
    replaceTime?: string;
    replacePlanNo?: string;
    petBatchNo?: string;
    guideClothBatchNo?: string;
    petModel?: string;
    replaceReason?: string;
    consumableStateId?: number;
    currentUsedLength?: number;
    nextUsedLength?: number;
    limitLength?: number;
    warningFlag?: number;
    warningText?: string;
  }

  export interface ImportResp {
    clearedCount?: number;
    failureCount?: number;
    failures?: string[];
    messages?: string[];
    skippedRows?: number;
    successCount?: number;
    totalRows?: number;
  }

  export interface RndConsumeReq {
    guideClothRecordId: number;
    guideClothChanged?: boolean;
    /** 更换导布时选择的湿法导布耗材领用台账 ID */
    guideClothLedgerId?: number;
    guideClothNewBatchNo?: string;
    guideClothReplaceReason?: string;
    productModelCode: string;
    productMaterialCode: string;
    productBatchNo: string;
    /** 选择的湿法 PET 耗材领用台账 ID */
    petLedgerId: number;
    petModel: string;
    petBatchNo: string;
    wetInputKg: number;
    wetOutputMeter: number;
    consumeTime: string;
    remark: string;
  }
}

export async function getGuideClothRecordPage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesHcGuideClothRecordApi.GuideClothRecord>>(
    '/mes/hc/base/guide-cloth-record/page',
    { params },
  );
}

export async function getGuideClothRecord(id: number) {
  return requestClient.get<MesHcGuideClothRecordApi.GuideClothRecord>(
    `/mes/hc/base/guide-cloth-record/get?id=${id}`,
  );
}

export async function createGuideClothRecord(data: MesHcGuideClothRecordApi.GuideClothRecord) {
  return requestClient.post<number>('/mes/hc/base/guide-cloth-record/create', data);
}

export async function updateGuideClothRecord(data: MesHcGuideClothRecordApi.GuideClothRecord) {
  return requestClient.put<boolean>('/mes/hc/base/guide-cloth-record/update', data);
}

export async function deleteGuideClothRecord(id: number) {
  return requestClient.delete<boolean>(`/mes/hc/base/guide-cloth-record/delete?id=${id}`);
}

export async function deleteGuideClothRecordList(ids: number[]) {
  return requestClient.delete<boolean>(
    `/mes/hc/base/guide-cloth-record/delete-list?ids=${ids.join(',')}`,
  );
}

export async function getGuideClothRuntime(motherModelCode?: string, equipmentId?: number) {
  return requestClient.get<MesHcGuideClothRecordApi.RuntimeInfo>(
    '/mes/hc/base/guide-cloth-record/runtime',
    { params: { equipmentId, motherModelCode } },
  );
}

export async function getGuideClothRndRuntime(guideClothRecordId: number) {
  return requestClient.get<MesHcGuideClothRecordApi.RuntimeInfo>(
    '/mes/hc/base/guide-cloth-record/rnd-runtime',
    { params: { guideClothRecordId } },
  );
}

export async function saveGuideClothRndConsume(data: MesHcGuideClothRecordApi.RndConsumeReq) {
  return requestClient.post<string>('/mes/hc/base/guide-cloth-record/rnd-consume', data);
}

export async function exportGuideClothRecord(params: Record<string, any>) {
  return requestClient.download('/mes/hc/base/guide-cloth-record/export-excel', { params });
}

export async function importGuideClothRecord(file: File, confirmClear = true) {
  return requestClient.upload<MesHcGuideClothRecordApi.ImportResp>('/mes/hc/base/guide-cloth-record/import', {
    confirmClear,
    file,
  });
}
