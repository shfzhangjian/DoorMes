import type { MesHcPlanOrderApi } from '#/api/mes/hc/planorder';
import type { MesHcProcessAnalysisApi } from '#/api/mes/hc/process-analysis';

export type Aggregation = MesHcProcessAnalysisApi.Aggregation;

export interface AnalysisFact extends Record<string, unknown> {
  id?: number;
  pivotRowKey: string;
  planMergeKey: string;
  sourceRow: MesHcPlanOrderApi.ProcessPivotRow;
  stage: MesHcPlanOrderApi.ProcessPivotStage;
  stageCode: string;
  stageGrainKey: string;
  stageName: string;
}

export interface AnalysisMetric {
  aggregation: Aggregation;
  decimals: number;
  distinctKeyField?: string;
  field: string;
  id: string;
  label: string;
}

export interface AnalysisFormula {
  decimals: number;
  expression: string;
  format: 'NUMBER' | 'PERCENT';
  id: string;
  label: string;
}

export interface AnalysisFilter {
  field: string;
  id: string;
  operator: string;
  value?: string;
}

export interface AnalysisChart {
  categoryField: string;
  enabled: boolean;
  seriesField?: string;
  type: 'BAR' | 'LINE' | 'PIE';
  valueField: string;
}

export interface AnalysisQuery {
  dateRange: string[];
  keyword: string;
}

export interface AnalysisLayout {
  columnWidths: Record<string, number>;
  tableHeight?: number;
}

export interface AnalysisConfig {
  chart: AnalysisChart;
  columnDimensions: string[];
  columnValueFilters: Record<string, string[]>;
  filters: AnalysisFilter[];
  formulas: AnalysisFormula[];
  layout: AnalysisLayout;
  metrics: AnalysisMetric[];
  query: AnalysisQuery;
  rowDimensions: string[];
  version: number;
}

export interface DrillStep {
  field: string;
  label: string;
  value: string;
}

export interface PivotCellColumn {
  dataIndex: string;
  formulaId?: string;
  metricId?: string;
  title: string;
}

export interface PivotColumnGroup {
  children: PivotCellColumn[];
  key: string;
  title: string;
}

export interface PivotDisplayRow extends Record<string, unknown> {
  __facts: AnalysisFact[];
  __groupLabel: string;
  __groupValue: string;
  __key: string;
}

export interface PivotResult {
  columnGroups: PivotColumnGroup[];
  currentDimension?: string;
  facts: AnalysisFact[];
  rows: PivotDisplayRow[];
  valueColumns: PivotCellColumn[];
}
