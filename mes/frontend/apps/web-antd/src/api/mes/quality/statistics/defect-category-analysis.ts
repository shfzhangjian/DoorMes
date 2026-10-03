import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQmsDefectCategoryAnalysisApi {
  export interface QueryParams extends PageParam {
    defectCategory?: string;
    endDate?: string;
    groupLevel?: 'MOTHER' | 'SEGMENT' | string;
    inspectionType?: 'ALL' | 'FINAL_INSPECTION' | 'SELF_CHECK' | 'SUBMISSION' | string;
    keyword?: string;
    modelCode?: string;
    motherRollBatchNo?: string;
    pieceNo?: string;
    processCode?: 'ADHESIVE2' | 'ALL' | 'CUT_ROUND' | 'PRESS_SLOT' | 'SLITTING' | string;
    segmentBatchNo?: string;
    startDate?: string;
  }

  export interface Overview {
    motherRollCount: number;
    pieceCount: number;
    primaryDefectCategory: string;
    primaryDefectCount: number;
    segmentCount: number;
    selfCheckCount: number;
    submissionCount: number;
    totalCount: number;
  }

  export interface DefectColumn {
    key: string;
    label: string;
    totalCount: number;
  }

  export interface PivotRow {
    defectCounts: Record<string, number>;
    groupKey: string;
    modelCode: string;
    modelSeriesCode: string;
    motherRollBatchNo: string;
    segmentBatchNo?: string;
    totalCount: number;
  }

  export interface ParetoRow {
    cumulativeRatio: number;
    defectCategory: string;
    defectCount: number;
    ratio: number;
  }

  export interface OverviewResult {
    defectColumns: DefectColumn[];
    groupLevel: string;
    overview: Overview;
    paretoRows: ParetoRow[];
    rows: PivotRow[];
  }

  export interface DetailRow {
    checkResult: string;
    defectCode: string;
    defectCount: number;
    defectLevel: string;
    defectName: string;
    eventKey: string;
    eventSource: string;
    eventTime: string;
    inspectionCategory: string;
    inspectionId?: number;
    inspectionNo: string;
    inspectionTime: string;
    inspectionType: string;
    inspectionTypeName: string;
    inspectorName: string;
    materialCode: string;
    materialName: string;
    modelCode: string;
    modelSeriesCode: string;
    motherRollBatchNo: string;
    planNo: string;
    processCode: string;
    processName: string;
    processPieceNo: string;
    remark: string;
    scanConfirmPieceNo: string;
    scanConfirmTime: string;
    segmentBatchNo: string;
    sourceId?: number;
    sourceTable: string;
  }
}

const BASE_URL = '/mes/quality/statistics/defect-category-analysis';

export function getDefectCategoryAnalysisOverview(
  params: MesQmsDefectCategoryAnalysisApi.QueryParams,
) {
  return requestClient.get<MesQmsDefectCategoryAnalysisApi.OverviewResult>(
    `${BASE_URL}/overview`,
    { params },
  );
}

export function getDefectCategoryAnalysisDetailPage(
  params: MesQmsDefectCategoryAnalysisApi.QueryParams,
) {
  return requestClient.get<PageResult<MesQmsDefectCategoryAnalysisApi.DetailRow>>(
    `${BASE_URL}/detail-page`,
    { params },
  );
}

export function getDefectCategoryOptions(
  params: MesQmsDefectCategoryAnalysisApi.QueryParams,
) {
  return requestClient.get<string[]>(`${BASE_URL}/defect-category-options`, {
    params,
  });
}
