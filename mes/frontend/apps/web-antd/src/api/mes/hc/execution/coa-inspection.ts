import type { PageResult } from '@vben/request';

import type { MesFaiApi } from '#/api/mes/quality/fai';

import { requestClient } from '#/api/request';

const BASE = '/mes/hc/execution/coa-inspection';

export namespace MesPackagingCoaInspectionApi {
  export type SourceType = 'CUT_ROUND_REPORT' | 'MANUAL_HISTORY';

  export interface SubmitSample {
    sourceRecordId: number;
    sourceType: SourceType;
  }

  export interface SubmitReq {
    remark?: string;
    samples: SubmitSample[];
    segmentBatchNo: string;
  }

  export interface SubmitResp {
    faiId?: number;
    faiIds?: number[];
    faiJudgment?: string;
    faiNo?: string;
    faiNos?: string[];
    faiStatus?: string;
    sampleCount?: number;
    segmentBatchNo?: string;
  }

  export interface FaiRecord extends MesFaiApi.FaiRecord {
    coaSampleBatchNo?: string;
  }
}

export function submitPackagingCoaInspection(
  data: MesPackagingCoaInspectionApi.SubmitReq,
) {
  return requestClient.post<MesPackagingCoaInspectionApi.SubmitResp>(
    `${BASE}/submit`,
    data,
  );
}

export function getPackagingCoaInspectionFaiPage(
  params: MesFaiApi.FaiPageReq,
) {
  return requestClient.get<PageResult<MesPackagingCoaInspectionApi.FaiRecord>>(
    `${BASE}/fai-page`,
    { params },
  );
}
