import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQmsYieldAnalysisApi {
  export type ProcessCode =
    | 'ADHESIVE1'
    | 'ADHESIVE2'
    | 'ALL'
    | 'CUT_ROUND'
    | 'FINAL_INSPECTION'
    | 'FORMULA'
    | 'PRESS_SLOT'
    | 'ROUGH_GRINDING'
    | 'SLITTING'
    | 'WET';

  export interface QueryParams extends PageParam {
    defectName?: string;
    endDate?: string;
    keyword?: string;
    materialKeyword?: string;
    metricKey?: string;
    modelCode?: string;
    motherRollNo?: string;
    pieceNo?: string;
    planNo?: string;
    processCode?: ProcessCode | string;
    segmentNo?: string;
    statDate?: string;
    startDate?: string;
  }

  export interface Overview {
    confirmedTotal: number;
    goodTotal: number;
    inputTotal: number;
    inspectionTotal: number;
    ngTotal: number;
    outputGoodTotal: number;
    outputNgTotal: number;
    primaryDefectCount: number;
    primaryDefectName: string;
    selfCheckNgTotal: number;
    submissionNgTotal: number;
    targetModelCode?: string;
    targetProcessName?: string;
    targetQualifiedQty?: number;
    targetType?: string;
    targetUnit?: string;
    yieldRate: number;
  }

  export interface SummaryRow {
    blackDotCount: number;
    blueDotCount: number;
    confirmedTotal: number;
    defectSummary?: string;
    goodTotal: number;
    inputTotal: number;
    inspectionTotal: number;
    groupKey: string;
    modelCode: string;
    motherRollNo: string;
    ngTotal: number;
    otherCount: number;
    outputGoodTotal: number;
    outputNgTotal: number;
    pinholeCount: number;
    planNo: string;
    processCode: string;
    processName: string;
    redDotCount: number;
    segmentNo: string;
    selfCheckNgTotal: number;
    stripeCount: number;
    submissionNgTotal: number;
    targetAchievementRate?: number;
    targetDifference?: number;
    targetMatched?: boolean;
    targetQualifiedQty?: number;
    targetReached?: boolean;
    targetType?: string;
    targetUnit?: string;
    waveCount: number;
    wrinkleCount: number;
    yieldRate: number;
    yellowDotCount: number;
  }

  export interface SegmentRow {
    groupKey: string;
    modelCode: string;
    motherRollNo: string;
    processMetrics: ProcessMetric[];
    segmentNo: string;
  }

  export interface ProcessMetric {
    blackDotCount: number;
    blueDotCount: number;
    confirmedTotal: number;
    defectSummary?: string;
    goodTotal: number;
    inputTotal: number;
    inspectionTotal: number;
    ngTotal: number;
    otherCount: number;
    outputGoodTotal: number;
    outputNgTotal: number;
    pinholeCount: number;
    planNo: string;
    processCode: string;
    processName: string;
    redDotCount: number;
    selfCheckNgTotal: number;
    stripeCount: number;
    submissionNgTotal: number;
    targetAchievementRate?: number;
    targetDifference?: number;
    targetMatched?: boolean;
    targetQualifiedQty?: number;
    targetReached?: boolean;
    targetType?: string;
    targetUnit?: string;
    waveCount: number;
    wrinkleCount: number;
    yieldRate: number;
    yellowDotCount: number;
  }

  export interface DefectDistribution {
    defectCount: number;
    defectName: string;
    ratio: number;
  }

  export interface TrendPoint {
    confirmedTotal: number;
    inputTotal: number;
    inspectionTotal: number;
    ngTotal: number;
    outputGoodTotal: number;
    outputNgTotal: number;
    statDate: string;
    yieldRate: number;
  }

  export interface OverviewResult {
    defectDistribution: DefectDistribution[];
    formulaExample: string;
    formulaText: string;
    overview: Overview;
    segmentRows: SegmentRow[];
    summaryRows: SummaryRow[];
    trendRows: TrendPoint[];
  }

  export interface DetailRow {
    actualReportModelCode?: string;
    actualSizeSpec?: string;
    blackDotCount: number;
    blueDotCount: number;
    confirmTime: string;
    defectSummary: string;
    groupKey: string;
    inputCount: number;
    inspectionCount: number;
    inspectionId?: number;
    inspectionNo?: string;
    inspectionSourceType?: string;
    materialCode: string;
    materialName: string;
    modelCode: string;
    motherRollNo: string;
    ngCount: number;
    otherCount: number;
    outputGoodCount: number;
    outputNgCount: number;
    pieceNo: string;
    pinholeCount: number;
    planNo: string;
    processCode: string;
    processName: string;
    redDotCount: number;
    segmentNo: string;
    selfCheck: string;
    selfCheckNgCount: number;
    sizeSpec?: string;
    sourceId: number;
    sourceKey: string;
    sourceRoute: string;
    sourceTable: string;
    stripeCount: number;
    submissionNgCount: number;
    submissionResult: string;
    waveCount: number;
    wrinkleCount: number;
    yieldRate: number;
    yellowDotCount: number;
  }

  export interface SourcePreviewField {
    label: string;
    value: string;
    valueType?: string;
  }

  export interface SourcePreviewGroup {
    items: SourcePreviewField[];
    title: string;
  }

  export interface SourcePreviewParams {
    processCode?: string;
    sourceId?: number | string;
    sourceTable?: string;
  }

  export interface SourcePreviewResp {
    confirmTime?: string;
    defectItems: SourcePreviewField[];
    defectSummary?: string;
    fieldGroups: SourcePreviewGroup[];
    found: boolean;
    motherRollNo?: string;
    pieceNo?: string;
    planNo?: string;
    processCode?: string;
    processName?: string;
    rawItems: SourcePreviewField[];
    segmentNo?: string;
    selfCheck?: string;
    sourceId?: number;
    sourceKey?: string;
    sourceTable?: string;
    submissionResult?: string;
    title?: string;
  }
}

const BASE_URL = '/mes/quality/statistics/yield-analysis';

export function getYieldAnalysisOverview(
  params: MesQmsYieldAnalysisApi.QueryParams,
) {
  return requestClient.get<MesQmsYieldAnalysisApi.OverviewResult>(
    `${BASE_URL}/overview`,
    {
      params,
    },
  );
}

export function getYieldAnalysisTargetModelOptions() {
  return requestClient.get<string[]>(`${BASE_URL}/target-model-options`);
}

export function getYieldAnalysisDetailPage(
  params: MesQmsYieldAnalysisApi.QueryParams,
) {
  return requestClient.get<PageResult<MesQmsYieldAnalysisApi.DetailRow>>(
    `${BASE_URL}/detail-page`,
    { params },
  );
}

export function getYieldAnalysisSourcePreview(
  params: MesQmsYieldAnalysisApi.SourcePreviewParams,
) {
  return requestClient.get<MesQmsYieldAnalysisApi.SourcePreviewResp>(
    `${BASE_URL}/source-preview`,
    { params },
  );
}

export function exportYieldAnalysisExcel(
  params: MesQmsYieldAnalysisApi.QueryParams,
) {
  return requestClient.download(`${BASE_URL}/export-excel`, { params });
}
