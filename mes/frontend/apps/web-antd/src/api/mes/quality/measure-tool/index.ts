import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace QmsMeasureToolApi {
  export type ToolStatus =
    | 'CALIBRATING'
    | 'IDLE'
    | 'IN_USE'
    | 'REPAIRING'
    | 'SCRAPPED'
    | 'STOPPED';
  export type WarningStatus =
    | 'DUE_SOON'
    | 'NORMAL'
    | 'NOT_CALIBRATED'
    | 'NOT_REQUIRED'
    | 'OVERDUE';
  export type ApplyStatus =
    | 'APPROVED'
    | 'APPROVING'
    | 'CANCELLED'
    | 'DRAFT'
    | 'REJECTED';
  export type TaskStatus =
    | 'CANCELLED'
    | 'COMPLETED'
    | 'IN_PROGRESS'
    | 'OVERDUE'
    | 'PENDING';
  export type CalibrationResult = 'LIMITED' | 'QUALIFIED' | 'UNQUALIFIED';
  export type RecordSource = 'IMPORT' | 'MANUAL' | 'TASK';

  export interface Category {
    id?: number;
    parentId?: number;
    categoryCode?: string;
    categoryName?: string;
    parentName?: string;
    levelName?: string;
    description?: string;
    status?: number;
    sort?: number;
    createTime?: string;
  }

  export interface Ledger {
    id?: number;
    toolCode?: string;
    bodyNo?: string;
    toolName?: string;
    categoryId?: number;
    categoryName?: string;
    model?: string;
    specification?: string;
    accuracy?: string;
    measureRange?: string;
    manufacturer?: string;
    purchaseDate?: string;
    calibrationCycleMonths?: number;
    warningDays?: number;
    calibrationType?: 'EXTERNAL' | 'INTERNAL';
    lastCalibrationDate?: string;
    nextCalibrationDate?: string;
    calibrationMethod?: string;
    calibrationOrg?: string;
    calibrator?: string;
    calibrationResult?: CalibrationResult;
    certificateNo?: string;
    calibrationReport?: string;
    usingDepartment?: string;
    maintainerName?: string;
    keeperName?: string;
    storageLocation?: string;
    status?: ToolStatus;
    calibrationStatus?: WarningStatus;
    displayStatus?: ToolStatus;
    calibrationOverdue?: boolean;
    calibrationMissedCount?: number;
    recentCalibrationMissedDate?: string;
    msaEnabled?: number;
    msaCycleMonths?: number;
    msaWarningDays?: number;
    lastMsaDate?: string;
    nextMsaDate?: string;
    msaStatus?: WarningStatus;
    msaResult?: 'QUALIFIED' | 'UNQUALIFIED';
    msaReport?: string;
    msaAnalyst?: string;
    msaMissedCount?: number;
    recentMsaMissedDate?: string;
    responsiblePerson?: string;
    externalOpen?: number;
    remark?: string;
    version?: number;
    createTime?: string;
  }

  export interface LedgerSelectOptions {
    personnelNames?: string[];
    usingDepartments?: string[];
    calibrationOrgs?: string[];
  }

  export interface StatusRecord {
    id?: number;
    ledgerId?: number;
    previousStatus?: ToolStatus;
    status?: ToolStatus;
    handler?: string;
    handleTime?: string;
    oaProcessNo?: string;
    handleRemark?: string;
    attachments?: string;
    createTime?: string;
  }

  export interface LedgerStatusUpdate {
    attachments?: string;
    handleRemark: string;
    handleTime: string;
    handler: string;
    id: number;
    oaProcessNo?: string;
    status: ToolStatus;
    version: number;
  }

  export interface Apply {
    id?: number;
    applyNo?: string;
    toolName?: string;
    categoryId?: number;
    categoryName?: string;
    model?: string;
    specification?: string;
    accuracy?: string;
    measureRange?: string;
    manufacturer?: string;
    purchaseDate?: string;
    calibrationCycleMonths?: number;
    warningDays?: number;
    usingDepartment?: string;
    keeperName?: string;
    storageLocation?: string;
    applyDepartment?: string;
    applicantName?: string;
    applyReason?: string;
    status?: ApplyStatus;
    approvalOpinion?: string;
    approvedBy?: string;
    approvedTime?: string;
    ledgerId?: number;
    assignedToolCode?: string;
    remark?: string;
    version?: number;
    createTime?: string;
  }

  export interface ApplyAction {
    id: number;
    operatorName?: string;
    opinion?: string;
  }

  export interface CalibrationTask {
    id?: number;
    taskNo?: string;
    ledgerId?: number;
    toolCode?: string;
    toolName?: string;
    categoryId?: number;
    categoryName?: string;
    usingDepartment?: string;
    keeperName?: string;
    dueDate?: string;
    warningDays?: number;
    warningStatus?: WarningStatus;
    taskStatus?: TaskStatus;
    sourceType?: string;
    generatedTime?: string;
    completedTime?: string;
    recordId?: number;
    handlerName?: string;
    remark?: string;
    version?: number;
  }

  export interface CalibrationTaskCandidate {
    ledgerId?: number;
    toolCode?: string;
    toolName?: string;
    categoryId?: number;
    categoryName?: string;
    usingDepartment?: string;
    keeperName?: string;
    lastCalibrationDate?: string;
    nextCalibrationDate?: string;
    taskDueDate?: string;
    dueInSelectedMonth?: boolean;
    overdue?: boolean;
    warningStatus?: WarningStatus;
  }

  export interface CalibrationDueHint {
    month?: string;
    dueSoonCount?: number;
    overdueCount?: number;
    totalCount?: number;
  }

  export interface CalibrationRecord {
    id?: number;
    recordNo?: string;
    taskId?: number;
    ledgerId?: number;
    toolCode?: string;
    toolName?: string;
    categoryId?: number;
    categoryName?: string;
    usingDepartment?: string;
    keeperName?: string;
    calibrationDate?: string;
    calibrationMethod?: string;
    calibrationOrg?: string;
    calibrator?: string;
    calibrationResult?: CalibrationResult;
    certificateNo?: string;
    certificateAttachment?: string;
    validUntil?: string;
    nextCalibrationDate?: string;
    cost?: number;
    sourceType?: RecordSource;
    remark?: string;
    version?: number;
    createTime?: string;
  }

  export interface MonthlySummary {
    month?: string;
    categoryId?: number;
    categoryName?: string;
    usingDepartment?: string;
    taskCount?: number;
    recordCount?: number;
    qualifiedCount?: number;
    unqualifiedCount?: number;
    limitedCount?: number;
    overdueCompletedCount?: number;
    completionRate?: number;
  }

  export interface MsaRecord {
    id?: number;
    recordNo?: string;
    ledgerId?: number;
    toolCode?: string;
    toolName?: string;
    categoryName?: string;
    msaDate?: string;
    nextMsaDate?: string;
    msaResult?: 'QUALIFIED' | 'UNQUALIFIED';
    msaReport?: string;
    analyst?: string;
    missedCount?: number;
    overdueFlag?: number;
    overdueDueDate?: string;
    remark?: string;
  }
}

export function getMeasureToolCategoryList(params?: any) {
  return requestClient.get<QmsMeasureToolApi.Category[]>(
    '/mes/quality/measure-tool/category/list',
    { params },
  );
}

export function getMeasureToolCategory(id: number) {
  return requestClient.get<QmsMeasureToolApi.Category>(
    `/mes/quality/measure-tool/category/get?id=${id}`,
  );
}

export function createMeasureToolCategory(data: QmsMeasureToolApi.Category) {
  return requestClient.post('/mes/quality/measure-tool/category/create', data);
}

export function updateMeasureToolCategory(data: QmsMeasureToolApi.Category) {
  return requestClient.put('/mes/quality/measure-tool/category/update', data);
}

export function deleteMeasureToolCategory(id: number) {
  return requestClient.delete(
    `/mes/quality/measure-tool/category/delete?id=${id}`,
  );
}

export function getMeasureToolLedgerPage(params: PageParam) {
  return requestClient.get<PageResult<QmsMeasureToolApi.Ledger>>(
    '/mes/quality/measure-tool/ledger/page',
    { params },
  );
}

export function getMeasureToolLedger(id: number) {
  return requestClient.get<QmsMeasureToolApi.Ledger>(
    `/mes/quality/measure-tool/ledger/get?id=${id}`,
  );
}

export function getMeasureToolLedgerSelectOptions() {
  return requestClient.get<QmsMeasureToolApi.LedgerSelectOptions>(
    '/mes/quality/measure-tool/ledger/select-options',
  );
}

export function createMeasureToolLedger(data: QmsMeasureToolApi.Ledger) {
  return requestClient.post('/mes/quality/measure-tool/ledger/create', data);
}

export function updateMeasureToolLedger(data: QmsMeasureToolApi.Ledger) {
  return requestClient.put('/mes/quality/measure-tool/ledger/update', data);
}

export function updateMeasureToolLedgerStatus(
  data: QmsMeasureToolApi.LedgerStatusUpdate,
) {
  return requestClient.put(
    '/mes/quality/measure-tool/ledger/update-status',
    data,
  );
}

export function getMeasureToolLedgerStatusRecordPage(params: PageParam) {
  return requestClient.get<PageResult<QmsMeasureToolApi.StatusRecord>>(
    '/mes/quality/measure-tool/ledger/status-record-page',
    { params },
  );
}

export function deleteMeasureToolLedger(id: number) {
  return requestClient.delete(
    `/mes/quality/measure-tool/ledger/delete?id=${id}`,
  );
}

export function exportMeasureToolLedger(params: any) {
  return requestClient.download(
    '/mes/quality/measure-tool/ledger/export-excel',
    { params },
  );
}

export function importMeasureToolLedger(file: File) {
  return requestClient.upload<number>(
    '/mes/quality/measure-tool/ledger/import-excel',
    { file },
  );
}

export function maintainMeasureToolCalibration(data: {
  calibrationDate: string;
  calibrationMethod?: string;
  calibrationOrg?: string;
  calibrationReport?: string;
  calibrationResult: QmsMeasureToolApi.CalibrationResult;
  calibrationType?: 'EXTERNAL' | 'INTERNAL';
  calibrator?: string;
  certificateNo?: string;
  ledgerId: number;
  remark?: string;
}) {
  return requestClient.put(
    '/mes/quality/measure-tool/ledger/maintain-calibration',
    data,
  );
}

export function maintainMeasureToolMsa(data: {
  analyst?: string;
  ledgerId: number;
  msaDate: string;
  msaReport?: string;
  msaResult: 'QUALIFIED' | 'UNQUALIFIED';
  remark?: string;
}) {
  return requestClient.put(
    '/mes/quality/measure-tool/ledger/maintain-msa',
    data,
  );
}

export function getMeasureToolMsaRecordPage(
  params: PageParam & { ledgerId?: number },
) {
  return requestClient.get<PageResult<QmsMeasureToolApi.MsaRecord>>(
    '/mes/quality/measure-tool/msa-record/page',
    { params },
  );
}

export function getMeasureToolApplyPage(params: PageParam) {
  return requestClient.get<PageResult<QmsMeasureToolApi.Apply>>(
    '/mes/quality/measure-tool/apply/page',
    { params },
  );
}

export function getMeasureToolApply(id: number) {
  return requestClient.get<QmsMeasureToolApi.Apply>(
    `/mes/quality/measure-tool/apply/get?id=${id}`,
  );
}

export function createMeasureToolApply(data: QmsMeasureToolApi.Apply) {
  return requestClient.post('/mes/quality/measure-tool/apply/create', data);
}

export function updateMeasureToolApply(data: QmsMeasureToolApi.Apply) {
  return requestClient.put('/mes/quality/measure-tool/apply/update', data);
}

export function deleteMeasureToolApply(id: number) {
  return requestClient.delete(
    `/mes/quality/measure-tool/apply/delete?id=${id}`,
  );
}

export function submitMeasureToolApply(data: QmsMeasureToolApi.ApplyAction) {
  return requestClient.put('/mes/quality/measure-tool/apply/submit', data);
}

export function approveMeasureToolApply(data: QmsMeasureToolApi.ApplyAction) {
  return requestClient.put<number>(
    '/mes/quality/measure-tool/apply/approve',
    data,
  );
}

export function rejectMeasureToolApply(data: QmsMeasureToolApi.ApplyAction) {
  return requestClient.put('/mes/quality/measure-tool/apply/reject', data);
}

export function exportMeasureToolApply(params: any) {
  return requestClient.download(
    '/mes/quality/measure-tool/apply/export-excel',
    { params },
  );
}

export function getMeasureToolCalibrationTaskPage(params: PageParam) {
  return requestClient.get<PageResult<QmsMeasureToolApi.CalibrationTask>>(
    '/mes/quality/measure-tool/calibration-task/page',
    { params },
  );
}

export function getMeasureToolCalibrationTaskCandidates(params: {
  categoryId?: number;
  includeNonMonthDue?: boolean;
  month?: string;
}) {
  return requestClient.get<QmsMeasureToolApi.CalibrationTaskCandidate[]>(
    '/mes/quality/measure-tool/calibration-task/candidates',
    { params },
  );
}

export function getMeasureToolCalibrationDueHint(params: {
  categoryId?: number;
  includeNonMonthDue?: boolean;
  month?: string;
}) {
  return requestClient.get<QmsMeasureToolApi.CalibrationDueHint>(
    '/mes/quality/measure-tool/calibration-task/due-hint',
    { params },
  );
}

export function generateMeasureToolCalibrationTasks(data: {
  ledgerIds?: number[];
  month?: string;
  warningDays?: number;
}) {
  return requestClient.post<number>(
    '/mes/quality/measure-tool/calibration-task/generate',
    data,
  );
}

export function cancelMeasureToolCalibrationTask(data: {
  id: number;
  reason?: string;
}) {
  return requestClient.put(
    '/mes/quality/measure-tool/calibration-task/cancel',
    data,
  );
}

export function batchConfirmMeasureToolCalibrationTasks(data: {
  ids: number[];
}) {
  return requestClient.put<number>(
    '/mes/quality/measure-tool/calibration-task/batch-confirm',
    data,
  );
}

export function exportMeasureToolCalibrationTask(params: any) {
  return requestClient.download(
    '/mes/quality/measure-tool/calibration-task/export-excel',
    { params },
  );
}

export function getMeasureToolCalibrationRecordPage(
  params: PageParam & { ledgerId?: number },
) {
  return requestClient.get<PageResult<QmsMeasureToolApi.CalibrationRecord>>(
    '/mes/quality/measure-tool/calibration-record/page',
    { params },
  );
}

export function getMeasureToolCalibrationRecord(id: number) {
  return requestClient.get<QmsMeasureToolApi.CalibrationRecord>(
    `/mes/quality/measure-tool/calibration-record/get?id=${id}`,
  );
}

export function createMeasureToolCalibrationRecord(
  data: QmsMeasureToolApi.CalibrationRecord,
) {
  return requestClient.post(
    '/mes/quality/measure-tool/calibration-record/create',
    data,
  );
}

export function updateMeasureToolCalibrationRecord(
  data: QmsMeasureToolApi.CalibrationRecord,
) {
  return requestClient.put(
    '/mes/quality/measure-tool/calibration-record/update',
    data,
  );
}

export function deleteMeasureToolCalibrationRecord(id: number) {
  return requestClient.delete(
    `/mes/quality/measure-tool/calibration-record/delete?id=${id}`,
  );
}

export function getMeasureToolCalibrationMonthlySummary(params: any) {
  return requestClient.get<QmsMeasureToolApi.MonthlySummary[]>(
    '/mes/quality/measure-tool/calibration-record/monthly-summary',
    { params },
  );
}

export function exportMeasureToolCalibrationRecord(params: any) {
  return requestClient.download(
    '/mes/quality/measure-tool/calibration-record/export-excel',
    { params },
  );
}
