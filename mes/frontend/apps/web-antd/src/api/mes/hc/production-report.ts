import type { PageResult } from '@vben/request';

import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';

import { requestClient } from '#/api/request';

export namespace MesHcProductionReportApi {
  export type PageReqVO = MesHcPlanOrderApi.ProcessPivotPageReqVO;
  export type OverviewRow = MesHcPlanOrderApi.ProcessPivotRow;

  export interface FormItem {
    id?: number;
    recordId?: number;
    templateItemId?: number;
    itemSeq?: number;
    fieldKey?: string;
    fieldLabel?: string;
    itemName?: string;
    itemCategory?: string;
    stepNode?: string;
    standardText?: string;
    unit?: string;
    valueMode?: string;
    controlType?: string;
    dualLabel1?: string;
    dualLabel2?: string;
    actualValue?: string;
    actualValue2?: string;
    actualNumber?: number;
    actualTime?: string;
    resultFlag?: string;
    abnormalRemark?: string;
    sourceRowJson?: string;
  }

  export interface FormRecord {
    sourceType?: string;
    id?: number;
    mirrorRecordId?: number;
    sourceProcessFormRecordId?: number;
    planOperationId?: number;
    bizType?: string;
    bizId?: number;
    recordNo?: string;
    templateId?: number;
    versionId?: number;
    templateCode?: string;
    templateName?: string;
    processCode?: string;
    processName?: string;
    formType?: string;
    formTypeName?: string;
    recordScope?: string;
    triggerTimingCode?: string;
    triggerTimingName?: string;
    modelCode?: string;
    modelName?: string;
    batchNo?: string;
    equipmentCode?: string;
    equipmentName?: string;
    recordStatus?: string;
    docStatus?: string;
    resultStatus?: string;
    inspectionResult?: string;
    headerDataJson?: string;
    contextJson?: string;
    formRemark?: string;
    confirmRemark?: string;
    fillUserName?: string;
    recordUserName?: string;
    confirmUserName?: string;
    recordDate?: string;
    fillTime?: string;
    recordTime?: string;
    confirmTime?: string;
    items?: FormItem[];
  }

  export interface ReportRecord {
    id?: number;
    planOperationId?: number;
    sourceType?: string;
    sourceTable?: string;
    reportDate?: string;
    modelCode?: string;
    padType?: string;
    materialCode?: string;
    recordRole?: string;
    sourceBizType?: string;
    sourceDetailId?: number;
    segmentMark?: string;
    reportType?: string;
    sourceMenuCode?: string;
    operationStatus?: string;
    reportStatus?: string;
    batchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    feedQty?: number;
    inputUom?: string;
    goodQty?: number;
    scrapQty?: number;
    reportQty?: number;
    reportUom?: string;
    outputQty?: number;
    outputUom?: string;
    outputPostStatus?: string;
    inputLength?: number;
    startPosition?: number;
    endPosition?: number;
    outputLength?: number;
    lossLength?: number;
    selfCheck?: string;
    defectCode?: string;
    productQualityStatus?: string;
    qualityLockReason?: string;
    inspectionStatus?: string;
    inspectionResult?: string;
    inspectorName?: string;
    innerUnitCount?: number;
    innerPieceCount?: number;
    outerBoxCount?: number;
    outerPieceCount?: number;
    inboundPieceCount?: number;
    faiNo?: string;
    faiStatus?: string;
    faiJudgment?: string;
    extraJson?: string;
    remark?: string;
    recorderName?: string;
    confirmerName?: string;
    reportTime?: string;
    startTime?: string;
    endTime?: string;
    productionData?: Record<string, unknown>;
    detailJson?: string;
  }

  export interface OperationSummary {
    id?: number;
    opSeq?: number;
    opCode?: string;
    opName?: string;
    operationStatus?: string;
    operationStatusText?: string;
    workCenterCode?: string;
    workCenterName?: string;
    equipmentCode?: string;
    equipmentName?: string;
    requiredQty?: number;
    uom?: string;
    finishTime?: string;
    finishRemark?: string;
    reportCount?: number;
    productionRecordCount?: number;
    startReportCount?: number;
    endReportCount?: number;
    reportedQty?: number;
    goodQty?: number;
    scrapQty?: number;
    outputPostStatus?: string;
    latestReportTime?: string;
    latestRecorderName?: string;
    latestConfirmerName?: string;
    formCount?: number;
    confirmedFormCount?: number;
    abnormalFormCount?: number;
    mirroredFormCount?: number;
    forms?: FormRecord[];
    productionRecords?: ReportRecord[];
    reports?: ReportRecord[];
  }

  export interface Detail {
    plan?: {
      batchNo?: string;
      id?: number;
      materialCode?: string;
      materialName?: string;
      modelCode?: string;
      modelName?: string;
      motherMaterialCode?: string;
      motherMaterialName?: string;
      motherModelCode?: string;
      motherModelName?: string;
      parentProductionBatchNo?: string;
      planDate?: string;
      planNo?: string;
      planStatus?: string;
      productionBatchNo?: string;
      productionEndDate?: string;
      productionStartDate?: string;
      sizeSpec?: string;
      targetQty?: number;
      targetUom?: string;
    };
    operations?: OperationSummary[];
    unassignedForms?: FormRecord[];
    unassignedReports?: ReportRecord[];
  }
}

export function getProcessReportOverviewPage(
  params: MesHcProductionReportApi.PageReqVO,
) {
  return requestClient.get<PageResult<MesHcProductionReportApi.OverviewRow>>(
    '/mes/hc/plan/production-report/page',
    { params, timeout: 60_000 },
  );
}

export function getProcessReportOverviewDetail(planId: number) {
  return requestClient.get<MesHcProductionReportApi.Detail>(
    `/mes/hc/plan/production-report/detail?planId=${planId}`,
    { timeout: 60_000 },
  );
}
