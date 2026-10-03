import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

const BASE_URL = '/mes/quality/sample-abnormal-recheck';

export namespace MesQmsSampleAbnormalRecheckApi {
  export interface Record {
    id?: number;
    lockNo?: string;
    lockStatus?: string;
    objectType?: string;
    objectNo?: string;
    sourceProcessCode?: string;
    sourceProcessName?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    operationCode?: string;
    operationName?: string;
    motherBatchNo?: string;
    segmentNo?: string;
    productionBatchNo?: string;
    abnormalInspectionId?: number;
    abnormalInspectionNo?: string;
    abnormalInspectionType?: string;
    abnormalOperationCode?: string;
    abnormalOperationName?: string;
    abnormalObjectNo?: string;
    abnormalResult?: string;
    abnormalSubmitTime?: string;
    abnormalFeedbackTime?: string;
    abnormalSourceModule?: string;
    abnormalSourceReportId?: number;
    abnormalSourceReportNo?: string;
    recheckInspectionId?: number;
    recheckInspectionNo?: string;
    recheckInspectionType?: string;
    recheckOperationCode?: string;
    recheckOperationName?: string;
    recheckObjectNo?: string;
    recheckResult?: string;
    recheckSubmitTime?: string;
    recheckFeedbackTime?: string;
    recheckSourceModule?: string;
    recheckSourceReportId?: number;
    recheckSourceReportNo?: string;
    recheckCount?: number;
    lastRecheckApplyTime?: string;
    lockReason?: string;
    releaseTime?: string;
    releaseReason?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface PageReqVO extends PageParam {
    lockNo?: string;
    lockStatus?: string;
    objectType?: string;
    objectNo?: string;
    sourceProcessCode?: string;
    planNo?: string;
    abnormalInspectionNo?: string;
    abnormalResult?: string;
    abnormalFeedbackTime?: string[];
    recheckInspectionNo?: string;
    recheckResult?: string;
    recheckFeedbackTime?: string[];
  }

  export interface ActiveReqVO {
    qualificationObjectNo?: string;
    objectNo?: string;
    objectType?: string;
    sourceProcessCode?: string;
  }
}

export function getSampleAbnormalRecheckPage(
  params: MesQmsSampleAbnormalRecheckApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesQmsSampleAbnormalRecheckApi.Record>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function createSampleAbnormalRecheck(id: number) {
  return requestClient.post<MesQmsSampleAbnormalRecheckApi.Record>(
    `${BASE_URL}/${id}/recheck`,
  );
}

export function getActiveSampleAbnormalLock(
  params: MesQmsSampleAbnormalRecheckApi.ActiveReqVO,
) {
  return requestClient
    .get<MesQmsSampleAbnormalRecheckApi.Record | null>(`${BASE_URL}/active`, {
      params,
    })
    .then((lock) => {
      if (!lock) return null;
      const abnormalResult = String(lock.abnormalResult || '').toUpperCase();
      return abnormalResult === 'NG' && !!lock.abnormalFeedbackTime
        ? lock
        : null;
    });
}
