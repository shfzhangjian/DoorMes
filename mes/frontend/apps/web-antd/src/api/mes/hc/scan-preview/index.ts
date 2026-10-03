import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesHcScanPreviewApi {
  export interface Record {
    id?: number;
    operationReportId?: number;
    sourceType?: string;
    sourceTypeName?: string;
    bizNo?: string;
    planNo?: string;
    operationName?: string;
    materialCode?: string;
    materialName?: string;
    modelCode?: string;
    modelName?: string;
    productionBatchNo?: string;
    sourceBatchNo?: string;
    reportDate?: string;
    startTime?: string;
    endTime?: string;
    reportQty?: number;
    goodQty?: number;
    reportUom?: string;
    reportStatus?: string;
    recorderName?: string;
    confirmerName?: string;
    printStatus?: string;
    printCount?: number;
    lastPrintTime?: string;
    scanStatus?: string;
    inspectionNo?: string;
    inspectionStatus?: string;
    inspectionResult?: string;
    inspectionApplyTime?: string;
    inspectionReturnTime?: string;
    inspectionRemark?: string;
    reportQrValue?: string;
    reportQrText?: string;
    inspectionQrValue?: string;
    hasInspection?: boolean;
    remark?: string;
    createTime?: string;
  }

  export interface PageReqVO extends PageParam {
    keyword?: string;
    sourceType?: string;
    operationName?: string;
    onlyWithInspection?: boolean;
    reportDateStart?: string;
    reportDateEnd?: string;
  }
}

export function getScanPreviewPage(params: MesHcScanPreviewApi.PageReqVO) {
  return requestClient.get<PageResult<MesHcScanPreviewApi.Record>>('/mes/hc/plan/scan-preview/page', {
    params,
  });
}
