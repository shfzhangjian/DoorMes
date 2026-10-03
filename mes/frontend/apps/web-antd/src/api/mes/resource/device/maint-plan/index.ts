import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesMaintPlanMatrixApi {
  export interface MonthCell {
    id: number | string;
    monthNo?: number;
    weekNo?: number;
    weekLabel?: string;
    planStartDate?: string;
    planEndDate?: string;
    planDate?: string;
    generatedOrderId?: number;
    generatedTaskNo?: string;
    actualDate?: string;
    executor?: string;
    executeRemark?: string;
    confirmer?: string;
    confirmTime?: string;
    itemGroup?: string;
    itemName: string;
    maintType?: string;
    frequency?: string;
    published?: boolean;
    status?: string;
    orderStatus?: string;
    executeStatus?: string;
    cellStatus?: 'DONE' | 'OVERDUE' | 'PENDING' | 'WAIT_CONFIRM' | string;
    executed?: boolean;
    overdue?: boolean;
  }

  export interface MatrixItem {
    id: number | string;
    standardItemId?: number;
    itemGroup?: string;
    itemName: string;
    method?: string;
    requirement?: string;
    frequency?: string;
    maintType?: string;
    sort?: number;
    months: Record<number, MonthCell>;
  }

  export interface MatrixRow {
    id: number | string;
    planYear?: number;
    deviceId?: number;
    deviceCode: string;
    deviceName: string;
    categoryName?: string;
    standardId?: number;
    standardCode?: string;
    standardName?: string;
    lastMaintDate?: string;
    items: MatrixItem[];
  }

  export interface PlanRecord {
    id: number | string;
    planYear?: number;
    planPeriod: string;
    monthNo?: number;
    weekNo?: number;
    planStartDate?: string;
    planEndDate?: string;
    planDate?: string;
    deviceId?: number;
    deviceCode: string;
    deviceName: string;
    categoryName?: string;
    standardName?: string;
    itemGroup?: string;
    itemName: string;
    method?: string;
    requirement?: string;
    frequency?: string;
    maintType: string;
    sourceRule?: string;
    published?: boolean;
    generatedOrderId?: number;
    generatedTaskNo?: string;
    executeStatus?: string;
    actualDate?: string;
    executor?: string;
    executeRemark?: string;
    confirmer?: string;
    confirmTime?: string;
    status?: string;
    remark?: string;
  }

  export interface ImportResp {
    successCount: number;
    failureCount: number;
    messages: string[];
  }

  export interface WeekSaveReq {
    planYear: number;
    monthNo: number;
    weekNo: number;
    deviceId?: number;
    deviceCode: string;
    deviceName: string;
    standardId?: number;
    standardCode?: string;
    standardName?: string;
    standardItemId?: number;
    itemGroup?: string;
    itemName: string;
    method?: string;
    requirement?: string;
    frequency?: string;
    maintType?: string;
    sourceRule?: string;
    remark?: string;
  }
}

const BASE = '/mes/resource/device/maint-plan';

export function getAnnualPlanMatrix(year: number, deviceId?: number) {
  return requestClient.get<{
    list: MesMaintPlanMatrixApi.MatrixRow[];
    total: number;
  }>(`${BASE}/matrix`, {
    params: { deviceId, year },
  });
}

export function savePlanWeek(data: MesMaintPlanMatrixApi.WeekSaveReq) {
  return requestClient.put<number>(`${BASE}/week`, data);
}

export function getPlanList(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<MesMaintPlanMatrixApi.PlanRecord>>(
    `${BASE}/page`,
    { params },
  );
}

export function publishPlan(ids: (number | string)[]) {
  return requestClient.post<number>(`${BASE}/generate-orders`, { ids });
}

export function executePlan(data: {
  executeRemark?: string;
  id: number | string;
}) {
  return requestClient.post<number>(`${BASE}/execute`, data);
}

export function confirmPlan(ids: (number | string)[]) {
  return requestClient.post<number>(`${BASE}/confirm`, { ids });
}

export function copyPlanYear(data: {
  overwrite?: boolean;
  sourceYear?: number;
  targetYear: number;
}) {
  return requestClient.post<number>(`${BASE}/copy-year`, data);
}

export function importPlanExcel(file: File, year?: number, overwrite = false) {
  return requestClient.upload<MesMaintPlanMatrixApi.ImportResp>(
    `${BASE}/import-excel`,
    {
      file,
      year,
      overwrite,
    },
  );
}

export function exportPlanExcel(params: Record<string, any>) {
  return requestClient.download(`${BASE}/export-excel`, { params });
}
