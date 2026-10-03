import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmPerformanceActualReportApi {
  export type PeriodType = 'MONTH' | 'QUARTER';
  export type ReportStatus = 'CONFIRMED' | 'DRAFT' | 'REJECTED' | 'SUBMITTED';

  export interface Value {
    id?: number;
    reportId?: number;
    metricCode?: string;
    metricName?: string;
    valueType?: 'NUMBER' | 'PERCENT' | 'TEXT';
    numericValue?: number;
    textValue?: string;
    unit?: string;
    sourceNodeKey?: string;
    sourceIndicatorCode?: string;
    sourceIndicatorName?: string;
    calcDescription?: string;
    formulaExpr?: string;
    scoreFormulaExpr?: string;
    evidenceRequired?: boolean;
    valueStatus?: string;
    remark?: string;
  }

  export interface Report {
    id?: number;
    reportNo?: string;
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    supplierSourceType?: string;
    periodType?: PeriodType;
    evalYear?: number;
    evalQuarter?: number;
    evalMonth?: number;
    status?: ReportStatus;
    reporterUserId?: number;
    reporterUserName?: string;
    confirmUserId?: number;
    confirmUserName?: string;
    submitTime?: string;
    confirmTime?: string;
    confirmOpinion?: string;
    remark?: string;
    version?: number;
    canEdit?: boolean;
    canSubmit?: boolean;
    canConfirm?: boolean;
    values?: Value[];
    createTime?: string;
    updateTime?: string;
  }

  export interface AvailableSupplier {
    supplierId?: number;
    supplierCode?: string;
    supplierName?: string;
    supplierSourceType?: string;
    configId?: number;
    templateVersionId?: number;
    templateCodeSnapshot?: string;
    templateNameSnapshot?: string;
    templateVersionNoSnapshot?: string;
  }

  export interface BatchCreateReq {
    periodType: PeriodType;
    evalYear: number;
    evalQuarter?: number;
    evalMonth?: number;
    supplierIds: number[];
  }
}

export function getActualReportPage(
  params: PageParam & Record<string, unknown>,
) {
  return requestClient.get<PageResult<SrmPerformanceActualReportApi.Report>>(
    '/mes/srm/performance-actual-report/page',
    { params },
  );
}

export function getActualReport(id: number) {
  return requestClient.get<SrmPerformanceActualReportApi.Report>(
    '/mes/srm/performance-actual-report/get',
    { params: { id } },
  );
}

export function createActualReport(data: SrmPerformanceActualReportApi.Report) {
  return requestClient.post<number>(
    '/mes/srm/performance-actual-report/create',
    data,
  );
}

export function getActualReportAvailableSuppliers(params: {
  evalMonth?: number;
  evalQuarter?: number;
  evalYear: number;
  periodType: SrmPerformanceActualReportApi.PeriodType;
  supplierInfo?: string;
}) {
  return requestClient.get<SrmPerformanceActualReportApi.AvailableSupplier[]>(
    '/mes/srm/performance-actual-report/available-suppliers',
    { params },
  );
}

export function batchCreateActualReport(
  data: SrmPerformanceActualReportApi.BatchCreateReq,
) {
  return requestClient.post<number[]>(
    '/mes/srm/performance-actual-report/batch-create',
    data,
  );
}

export function updateActualReport(data: SrmPerformanceActualReportApi.Report) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-actual-report/update',
    data,
  );
}

export function deleteActualReport(id: number) {
  return requestClient.delete<boolean>(
    '/mes/srm/performance-actual-report/delete',
    { params: { id } },
  );
}

export function submitActualReport(id: number) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-actual-report/submit',
    null,
    { params: { id } },
  );
}

export function confirmActualReport(reportId: number, confirmOpinion?: string) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-actual-report/confirm',
    { confirmOpinion, reportId },
  );
}

export function rejectActualReport(reportId: number, confirmOpinion?: string) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-actual-report/reject',
    { confirmOpinion, reportId },
  );
}

export function pullActualReportMonthlyValues(id: number) {
  return requestClient.put<boolean>(
    '/mes/srm/performance-actual-report/pull-monthly-values',
    null,
    { params: { id } },
  );
}
