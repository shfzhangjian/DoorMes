import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcStationRecordApi {
  export interface RecordItem {
    id?: number;
    recordId?: number;
    itemSeq?: number;
    itemCategory?: string;
    stepNode?: string;
    itemName?: string;
    standardText?: string;
    valueMode?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    fieldDefinitionsJson?: string;
    fieldValuesJson?: string;
    actualValue?: string;
    actualValue2?: string;
    resultFlag?: string;
    abnormalRemark?: string;
  }

  export interface Record {
    id?: number;
    planId?: number;
    planNo?: string;
    batchNo?: string;
    modelCode?: string;
    modelName?: string;
    planOperationId?: number;
    operationCode?: string;
    operationName?: string;
    formId?: number;
    formCode?: string;
    formName?: string;
    triggerTimingCode?: string;
    triggerTimingName?: string;
    docStatus?: string;
    resultStatus?: string;
    inspectionResult?: string;
    equipmentId?: number;
    equipmentCode?: string;
    equipmentName?: string;
    mixerEquipmentId?: number;
    mixerEquipmentCode?: string;
    mixerEquipmentName?: string;
    foamingEquipmentId?: number;
    foamingEquipmentCode?: string;
    foamingEquipmentName?: string;
    workCenterId?: number;
    workCenterCode?: string;
    workCenterName?: string;
    recordScope?: string;
    recordDate?: string;
    bizType?: string;
    bizId?: number;
    recordUserName?: string;
    recordTime?: string;
    createUserName?: string;
    creator?: string;
    createTime?: string;
    confirmUserName?: string;
    confirmTime?: string;
    headerDataJson?: string;
    formRemark?: string;
    confirmRemark?: string;
    updater?: string;
    updateTime?: string;
    items?: RecordItem[];
  }

  export interface PageReqVO extends PageParam {
    keyword?: string;
    planNo?: string;
    operationName?: string;
    formName?: string;
    formCode?: string;
    excludeFormCode?: string;
    formCodePrefix?: string;
    equipmentCode?: string;
    equipmentName?: string;
    recordScope?: string;
    createTimeStart?: string;
    createTimeEnd?: string;
    createUserName?: string;
    confirmUserName?: string;
    confirmTimeStart?: string;
    confirmTimeEnd?: string;
  }
}

export function getStationRecordPage(params: MesHcStationRecordApi.PageReqVO) {
  return requestClient.get<PageResult<MesHcStationRecordApi.Record>>(
    '/mes/hc/plan/station-record-query/page',
    { params },
  );
}

export function getStationRecordDetail(id: number) {
  return requestClient.get<MesHcStationRecordApi.Record>(
    `/mes/hc/plan/station-record-query/get-detail?id=${id}`,
  );
}

export function updateStationRecord(data: MesHcStationRecordApi.Record) {
  return requestClient.put<boolean>('/mes/hc/plan/station-record-query/update', data);
}
