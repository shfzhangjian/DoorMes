import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesMaintTaskExecApi {
  export interface Task {
    id?: number | string;
    taskNo?: string;
    planPeriod?: string;
    deviceId?: number;
    deviceCode?: string;
    deviceName?: string;
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
    photos?: string[];
    status?: 'ABNORMAL' | 'DONE' | 'TODO' | 'WAIT_CONFIRM';
    rawStatus?: string;
    tabType?: string;
    remark?: string;
    version?: number;
    items?: TaskItem[];
    parts?: TaskPart[];
  }

  export interface TaskItem {
    id?: number | string;
    taskId?: number | string;
    standardItemId?: number | string;
    itemName?: string;
    method?: string;
    requirement?: string;
    result?: 'ABNORMAL' | 'NA' | 'NORMAL' | 'PENDING' | string;
    remark?: string;
    sort?: number;
  }

  export interface TaskPart {
    id?: number | string;
    taskId?: number | string;
    partCode?: string;
    partName?: string;
    quantity?: number;
    unit?: string;
    remark?: string;
    sort?: number;
  }
}

function mapOrder(row: any): MesMaintTaskExecApi.Task {
  const done = ['ABNORMAL', 'CANCELLED', 'DONE'].includes(row.status);
  const waitConfirm = row.status === 'WAIT_CONFIRM';
  let status = 'TODO';
  if (waitConfirm) {
    status = 'WAIT_CONFIRM';
  } else if (done) {
    status = 'DONE';
  }
  return {
    ...row,
    actualDate: row.actualDate || '',
    parts: row.parts || [],
    photos: row.photos ? String(row.photos).split(',').filter(Boolean) : [],
    rawStatus: row.status,
    status,
  };
}

function unmapExecutePayload(data: MesMaintTaskExecApi.Task) {
  return {
    ...data,
    actualDate: data.actualDate,
    items: data.items,
    parts: data.parts,
    photos: Array.isArray(data.photos) ? data.photos.join(',') : data.photos,
    status:
      data.rawStatus === 'ABNORMAL' || data.status === 'ABNORMAL'
        ? 'ABNORMAL'
        : 'DONE',
  };
}

export async function getTaskPage(params: PageParam & { tabType?: string }) {
  const res = await requestClient.get<PageResult<any>>(
    '/mes/resource/device/maint-order/page',
    { params },
  );
  return {
    list: (res.list || []).map((row) => mapOrder(row)),
    total: res.total,
  } as PageResult<MesMaintTaskExecApi.Task>;
}

export async function getTask(id: number | string) {
  const res = await requestClient.get<any>(
    `/mes/resource/device/maint-order/get?id=${id}`,
  );
  return mapOrder(res);
}

export function createTask(data: MesMaintTaskExecApi.Task) {
  return requestClient.post('/mes/resource/device/maint-order/create', data);
}

export function updateTask(data: MesMaintTaskExecApi.Task) {
  return requestClient.put('/mes/resource/device/maint-order/update', data);
}

export function executeTask(data: MesMaintTaskExecApi.Task) {
  return requestClient.post(
    '/mes/resource/device/maint-order/execute',
    unmapExecutePayload(data),
  );
}

export function exportTask(params: any) {
  return requestClient.download(
    '/mes/resource/device/maint-order/export-excel',
    {
      params,
    },
  );
}

export function getTaskPartListById(taskId: number | string) {
  return requestClient.get<MesMaintTaskExecApi.TaskPart[]>(
    '/mes/resource/device/maint-order/part/list',
    { params: { taskId } },
  );
}

export function getTaskItemListById(taskId: number | string) {
  return requestClient.get<MesMaintTaskExecApi.TaskItem[]>(
    '/mes/resource/device/maint-order/item/list',
    { params: { taskId } },
  );
}
