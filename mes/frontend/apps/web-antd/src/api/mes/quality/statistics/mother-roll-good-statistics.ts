import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQmsMotherRollGoodStatisticsApi {
  export interface ProcessPivotPiece {
    sourceSlittingOkFlag?: boolean;
    actualModelCode?: string;
    actualSizeSpec?: string;
    coaFlag?: boolean;
    defectFlag?: boolean;
    lastReportTime?: string;
    outputActualSizeSpec?: string;
    outputBatchNo?: string;
    pieceNo?: string;
    remark?: string;
    reportConfirmed?: boolean;
    sourceBatchNo?: string;
    stageCode?: string;
    status?: 'DEFECT' | 'DONE' | 'PENDING' | string;
  }

  export interface ProcessPivotInspection {
    sourceSlittingOkFlag?: boolean;
    firstInspectionSampleFlag?: boolean;
    coaInspectionFlag?: boolean;
    defectSummary?: string;
    inspectionId?: number;
    inspectionNgQty?: number;
    inspectionNo?: string;
    inspectionQty?: number;
    inspectionTime?: string;
    inspectionType?: string;
    judgment?: string;
    productBatchNo?: string;
    remark?: string;
    sourceType?: string;
    stageCode?: string;
    status?: string;
  }

  export interface ProcessPivotStage {
    stageCode?: string;
    stageName?: string;
    stageStatus?: 'FINISHED' | 'NOT_STARTED' | 'PENDING' | 'RUNNING' | string;
    sourceBatchNos?: string;
    outputBatchNos?: string;
    inputQty?: number;
    inputQtyMergeKey?: string;
    segmentInputQty?: number;
    reportQty?: number;
    doneQty?: number;
    pendingQty?: number;
    pendingUnit?: string;
    defectQty?: number;
    segmentDefectQty?: number;
    finalInspectionOutputMergeKey?: string;
    finalInspectionOutputQty?: number;
    finalInspectionOutputUnit?: string;
    finalInspectionYieldRate?: number;
    finalInspectionYieldRateMergeKey?: string;
    goodYieldRate?: number;
    goodYieldRateMergeKey?: string;
    goodTargetRate?: number;
    goodTargetRateMergeKey?: string;
    coaInspectionQty?: number;
    coaInspectionNgQty?: number;
    glueBoardInspectionQty?: number;
    inspectionQty?: number;
    processProductionInspectionQty?: number;
    inspectionNgQty?: number;
    confirmedQty?: number;
    motherOutputMergeKey?: string;
    motherOutputQty?: number;
    motherOutputUnit?: string;
    lengthQty?: number;
    lengthUnit?: string;
    processLength?: number;
    reportUnit?: string;
    startPosition?: number;
    theoreticalOutputMatched?: boolean;
    theoreticalOutputMergeKey?: string;
    theoreticalOutputModelCode?: string;
    theoreticalOutputProcessCode?: string;
    theoreticalOutputQty?: number;
    theoreticalOutputUnit?: string;
    lastReportTime?: string;
    pieceDetails?: ProcessPivotPiece[];
    inspectionDetails?: ProcessPivotInspection[];
    remark?: string;
  }

  export interface ProcessPivotRow {
    id?: number;
    planNo?: string;
    postProcessFlag?: boolean;
    planNoTagText?: string;
    planDate?: string;
    planStatus?: string;
    productionStartDate?: string;
    productionEndDate?: string;
    materialCode?: string;
    materialName?: string;
    motherMaterialCode?: string;
    motherMaterialName?: string;
    motherModelCode?: string;
    motherModelName?: string;
    modelCode?: string;
    modelName?: string;
    modelSeriesCode?: string;
    sizeSpec?: string;
    sizeName?: string;
    targetQty?: number;
    netPlanQty?: number;
    targetUom?: string;
    batchNo?: string;
    productionBatchNo?: string;
    parentProductionBatchNo?: string;
    motherRollBatchNo?: string;
    segmentBatchNo?: string;
    pivotRowKey?: string;
    planMergeKey?: string;
    segmentMergeKey?: string;
    modelSizeMergeKey?: string;
    actualModelCode?: string;
    actualSizeSpec?: string;
    variationStartStageCode?: string;
    variationStartStageName?: string;
    stageMergeKeys?: Record<string, string>;
    totalDefectQty?: number;
    latestReportTime?: string;
    stages?: Record<string, ProcessPivotStage>;
  }


  export interface PageReqVO extends PageParam {
    planNo?: string;
    keyword?: string;
    planStatuses?: string[];
    planMode?: string;
    sourceType?: string;
    salesOrderNo?: string;
    materialKeyword?: string;
    motherMaterialCode?: string;
    motherModelCode?: string;
    motherRollBatchNo?: string;
    prodType?: string;
    categoryCode?: string;
    modelCode?: string;
    sizeSpec?: string;
    recipeCode?: string;
    routeKeyword?: string;
    planDateStart?: string;
    planDateEnd?: string;
    productionStartDateStart?: string;
    productionStartDateEnd?: string;
    productionEndDateStart?: string;
    productionEndDateEnd?: string;
    createTimeStart?: string;
    createTimeEnd?: string;
  }

  export interface ProcessPivotPageReqVO extends PageReqVO {
    exportBaseColumns?: string[];
    exportStageColumns?: string[];
    motherSegmentBatchNo?: string;
  }

}

export async function getMotherRollGoodStatisticsPage(
  params: MesQmsMotherRollGoodStatisticsApi.ProcessPivotPageReqVO,
) {
  return requestClient.get<PageResult<MesQmsMotherRollGoodStatisticsApi.ProcessPivotRow>>(
    '/mes/quality/statistics/mother-roll-good-statistics/page',
    { params },
  );
}

export async function exportMotherRollGoodStatistics(
  params: MesQmsMotherRollGoodStatisticsApi.ProcessPivotPageReqVO,
) {
  return requestClient.download(
    '/mes/quality/statistics/mother-roll-good-statistics/export-excel',
    { params },
  );
}
