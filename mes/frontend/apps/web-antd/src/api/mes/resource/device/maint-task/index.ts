import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesMaintTaskApi {
  export interface Task {
    id?: number;
    orderId?: number;
    recordNo?: string;
    taskNo?: string;
    categoryId?: number;
    categoryName?: string;
    deviceCode?: string;
    deviceName?: string;
    standardName?: string;
    frequency?: string;
    maintType?: string;
    operator?: string;
    status?: number;
    dueDate?: string;
    planTime?: string;
    actualTime?: string;
    remark?: string;
    items?: TaskItem[];
  }

  export interface Order {
    id?: number;
    taskNo?: string;
    planPeriod?: string;
    deviceId?: number;
    deviceCode?: string;
    deviceName?: string;
    categoryId?: number;
    categoryName?: string;
    standardId?: number;
    standardName?: string;
    frequency?: string;
    maintType?: string;
    taskDesc?: string;
    executor?: string;
    planDate?: string;
    dueDate?: string;
    planTime?: string;
    actualDate?: string;
    executeRemark?: string;
    confirmer?: string;
    confirmTime?: string;
    status?: 'ABNORMAL' | 'DONE' | 'TODO' | 'WAIT_CONFIRM' | string;
    rawStatus?: string;
    remark?: string;
  }

  export interface MaintCandidate {
    deviceId?: number;
    deviceCode?: string;
    deviceName?: string;
    categoryId?: number;
    categoryName?: string;
    standardId?: number;
    standardCode?: string;
    standardName?: string;
    frequency?: string;
    maintType?: string;
    lastPlanDate?: string;
    lastActualDate?: string;
    taskDueDate?: string;
    dueInCurrentMonth?: boolean;
    overdue?: boolean;
    warningStatus?: string;
  }

  export interface CurrentMonthAppend {
    executor?: string;
    candidates: Array<{
      deviceId: number;
      standardId: number;
      taskDueDate?: string;
    }>;
  }

  export interface TaskItem {
    id?: number | string;
    taskId?: number;
    itemName?: string;
    method?: string;
    requirement?: string;
    result?: string;
    remark?: string;
    sort?: number;
  }

  export interface MonthlySummary {
    month?: string;
    categoryId?: number;
    categoryName?: string;
    maintType?: string;
    taskCount?: number;
    recordCount?: number;
    normalCount?: number;
    abnormalCount?: number;
    overdueCompletedCount?: number;
    completionRate?: number;
  }
}

function resultToLegacyStatus(status?: string) {
  if (status === 'DONE') return 20;
  if (status === 'ABNORMAL') return 30;
  return 10;
}

function legacyStatusToResult(status?: number) {
  if (status === 20) return 'DONE';
  if (status === 30) return 'ABNORMAL';
  return undefined;
}

function mapOrder(row: any): MesMaintTaskApi.Order {
  const done = ['ABNORMAL', 'CANCELLED', 'DONE'].includes(row.status);
  const waitConfirm = row.status === 'WAIT_CONFIRM';
  let status = 'TODO';
  if (waitConfirm) {
    status = 'WAIT_CONFIRM';
  } else if (row.status === 'ABNORMAL') {
    status = 'ABNORMAL';
  } else if (done) {
    status = 'DONE';
  }
  return {
    ...row,
    actualDate: row.actualDate || '',
    rawStatus: row.status,
    status,
  };
}

function mapRecord(row: any): MesMaintTaskApi.Task {
  return {
    id: row.id,
    orderId: row.orderId,
    recordNo: row.recordNo,
    taskNo: row.taskNo,
    categoryId: row.categoryId,
    categoryName: row.categoryName,
    deviceCode: row.deviceCode,
    deviceName: row.deviceName,
    frequency: row.frequency,
    maintType: row.maintType,
    operator: row.executor,
    dueDate: row.dueDate,
    planTime: row.planTime,
    actualTime: row.actualTime,
    standardName: row.standardName,
    status: resultToLegacyStatus(row.resultStatus),
    remark: row.remark,
  };
}

export async function getTaskPage(params: PageParam & Record<string, any>) {
  const queryParams = {
    ...params,
    resultStatus: legacyStatusToResult((params as any).status),
    status: undefined,
  };
  const res = await requestClient.get<PageResult<any>>(
    '/mes/resource/device/maint-record/page',
    { params: queryParams },
  );
  return {
    list: (res.list || []).map((row) => mapRecord(row)),
    total: res.total,
  } as PageResult<MesMaintTaskApi.Task>;
}

export async function getMaintOrderPage(
  params: PageParam & { tabType?: string },
) {
  const res = await requestClient.get<PageResult<any>>(
    '/mes/resource/device/maint-order/page',
    { params },
  );
  return {
    list: (res.list || []).map((row) => mapOrder(row)),
    total: res.total,
  } as PageResult<MesMaintTaskApi.Order>;
}

export function getMaintCurrentMonthCandidates(params: any) {
  return requestClient.get<MesMaintTaskApi.MaintCandidate[]>(
    '/mes/resource/device/maint-order/candidate/list',
    { params },
  );
}

export function appendCurrentMonthMaintOrders(
  data: MesMaintTaskApi.CurrentMonthAppend,
) {
  return requestClient.post<number>(
    '/mes/resource/device/maint-order/append-current-month',
    data,
  );
}

export function confirmMaintOrders(ids: number[]) {
  return requestClient.post<number>(
    '/mes/resource/device/maint-order/confirm-list',
    { ids },
  );
}

export async function getTask(id: number) {
  const record = await requestClient.get<any>(
    `/mes/resource/device/maint-record/get?id=${id}`,
  );
  const order = await requestClient.get<any>(
    `/mes/resource/device/maint-order/get?id=${record.orderId}`,
  );
  return {
    ...mapRecord(record),
    id: record.orderId,
    items: order.items || [],
  } as MesMaintTaskApi.Task;
}

export function deleteTask(id: number) {
  return requestClient.delete(
    `/mes/resource/device/maint-order/delete?id=${id}`,
  );
}

export function deleteTaskList(_ids: number[]) {
  return Promise.resolve(true);
}

export function exportTask(params: any) {
  return requestClient.download(
    '/mes/resource/device/maint-record/export-excel',
    {
      params: {
        ...params,
        resultStatus: legacyStatusToResult(params?.status),
        status: undefined,
      },
    },
  );
}

export function getMaintRecordMonthlySummary(params: any) {
  return requestClient.get<MesMaintTaskApi.MonthlySummary[]>(
    '/mes/resource/device/maint-record/monthly-summary',
    { params },
  );
}

export function getTaskItemListByTaskId(taskId: number) {
  return requestClient.get<MesMaintTaskApi.TaskItem[]>(
    '/mes/resource/device/maint-order/item/list',
    { params: { taskId } },
  );
}
