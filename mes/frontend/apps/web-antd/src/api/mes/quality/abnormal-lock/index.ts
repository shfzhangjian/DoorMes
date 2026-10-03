import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQmsAbnormalLockApi {
  export interface AbnormalLockRecord {
    id?: number;
    lockNo?: string;
    lockSourceType?: string;
    inspectionOrderType?: string;
    inspectionOrderId?: number;
    inspectionOrderNo?: string;
    inspectionStatus?: string;
    inspectionResult?: string;
    sourceModule?: string;
    sourceReportId?: number;
    sourceReportNo?: string;
    sourceOperationCode?: string;
    sourceOperationName?: string;
    lockScope?: string;
    affectedObjectType?: string;
    affectedSourceTable?: string;
    affectedSourceId?: number;
    affectedSourceKey?: string;
    rootBatchNo?: string;
    sourceBatchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    affectedBatchNo?: string;
    planId?: number;
    planNo?: string;
    planOperationId?: number;
    operationCode?: string;
    operationName?: string;
    processOrder?: number;
    glueBoardStockId?: number;
    glueBoardUsageId?: number;
    glueBoardBatchNo?: string;
    glueBoardStartPosition?: number;
    glueBoardUseLength?: number;
    inspectionSubmitTime?: string;
    ngConfirmTime?: string;
    lockTime?: string;
    lockStatus?: string;
    lockReason?: string;
    releaseUserId?: number;
    releaseUserName?: string;
    releaseTime?: string;
    releaseReason?: string;
    traceSnapshotJson?: string;
    remark?: string;
    createTime?: string;
    updateTime?: string;
  }

  export interface AbnormalLockPageReqVO extends PageParam {
    lockNo?: string;
    inspectionOrderType?: string;
    inspectionOrderNo?: string;
    lockSourceType?: string;
    lockScope?: string;
    lockStatus?: string;
    affectedBatchNo?: string;
    sourceBatchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    glueBoardBatchNo?: string;
    planNo?: string;
    operationCode?: string;
    operationName?: string;
    lockTime?: string[];
  }
}

export function getAbnormalLockPage(params: MesQmsAbnormalLockApi.AbnormalLockPageReqVO) {
  return requestClient.get<PageResult<MesQmsAbnormalLockApi.AbnormalLockRecord>>(
    '/mes/quality/abnormal-lock/page',
    { params },
  );
}
