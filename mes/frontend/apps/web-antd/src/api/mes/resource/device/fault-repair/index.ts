import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesFaultRepairApi {
  export interface Order {
    id?: number | string;
    orderNo?: string;
    exceptionNo?: string;
    deviceId?: number;
    deviceCode?: string;
    deviceName?: string;
    exceptionLevel?: 'CRITICAL' | 'MAJOR' | 'MINOR' | string;

    faultDesc?: string;
    reporterId?: number;
    reporter?: string;
    reportTime?: string;

    dispatcherId?: number;
    dispatcher?: string;
    dispatchTime?: string;
    responseResult?: 'HANDLED' | 'NEED_REPAIR' | string;
    responseRemark?: string;
    assigneeId?: number;
    assignee?: string;

    repairPlanTime?: string;
    repairPlanRemark?: string;
    faultReason?: string;
    repairAction?: string;
    repairTime?: string;
    repairStatus?: 'DONE' | 'LEFTOVER' | string;
    partChangeDesc?: string;

    confirmResult?: 'FIXED' | 'UNFIXED' | string;
    confirmRemark?: string;
    unfixReason?: string;
    confirmerId?: number;
    confirmer?: string;
    confirmTime?: string;
    archiverId?: number;
    archiver?: string;
    archiveTime?: string;
    archiveReason?: string;

    faultCategory?: string;
    faultSubCategory?: string;
    impactScope?: string;
    rootCause?: string;
    preventiveAction?: string;
    attachments?: string;

    status?:
      | 'CLOSED'
      | 'DISPATCHED'
      | 'PENDING_ARCHIVE'
      | 'PENDING_CONFIRM'
      | 'REPORTED'
      | string;
    processInstanceId?: string;
    currentNodeCode?: string;
    currentNodeName?: string;
    currentHandlerUserId?: number;
    currentHandlerUserName?: string;
    currentUserTaskTodo?: boolean;
    currentUserTaskTodoCount?: number;
    currentUserTaskTodoType?: string;
    currentUserTaskTodoLabel?: string;
    currentUserTaskDone?: boolean;
    currentUserTaskDoneCount?: number;
    currentUserTaskDoneType?: string;
    currentUserTaskDoneLabel?: string;
    canHandle?: boolean;
    listActionCode?: string;
    listActionName?: string;
    tabType?: 'initiated' | 'monitor' | 'todo';
    remark?: string;
    parts?: RepairPart[];
    flowLogs?: FlowLog[];
  }

  export interface RepairPart {
    id?: number | string;
    exceptionId?: number | string;
    partCode?: string;
    partName?: string;
    spec?: string;
    quantity?: number;
    unit?: string;
    remark?: string;
    sort?: number;
  }

  export interface ProcessReq {
    id: number | string;
    action: 'ARCHIVE' | 'CONFIRM' | 'REPAIR' | 'RESPOND';
    responseResult?: 'HANDLED' | 'NEED_REPAIR' | string;
    responseRemark?: string;
    assigneeId?: number;
    assignee?: string;
    repairPlanTime?: string;
    repairPlanRemark?: string;
    repairAction?: string;
    repairTime?: string;
    repairStatus?: 'DONE' | 'LEFTOVER' | string;
    partChangeDesc?: string;
    unfixReason?: string;
    confirmResult?: string;
    confirmRemark?: string;
    rootCause?: string;
    preventiveAction?: string;
    archiveReason?: string;
    parts?: RepairPart[];
  }

  export interface FlowLog {
    actionCode?: string;
    actionName?: string;
    fromStatus?: string;
    toStatus?: string;
    fromNodeCode?: string;
    fromNodeName?: string;
    toNodeCode?: string;
    toNodeName?: string;
    opinion?: string;
    handlerUserId?: number;
    handlerUserName?: string;
    handleTime?: string;
  }
}

const BASE = '/mes/resource/device/exception';

function normalizeOrder(
  row: MesFaultRepairApi.Order,
  tabType?: string,
): MesFaultRepairApi.Order {
  return {
    ...row,
    orderNo: row.orderNo || row.exceptionNo,
    exceptionNo: row.exceptionNo || row.orderNo,
    tabType:
      (tabType as MesFaultRepairApi.Order['tabType']) ||
      row.tabType ||
      'monitor',
  };
}

export async function getOrderPage(params: PageParam & { tabType?: string }) {
  const res = await requestClient.get<PageResult<MesFaultRepairApi.Order>>(
    `${BASE}/page`,
    { params },
  );
  return {
    ...res,
    list: (res.list || []).map((item) => normalizeOrder(item, params.tabType)),
  };
}

export async function getOrder(id: number | string) {
  return normalizeOrder(
    await requestClient.get<MesFaultRepairApi.Order>(`${BASE}/get`, {
      params: { id },
    }),
  );
}

export function createOrder(data: MesFaultRepairApi.Order) {
  return requestClient.post<number>(`${BASE}/create`, data);
}

export function updateOrder(data: MesFaultRepairApi.Order) {
  return requestClient.put<boolean>(`${BASE}/update`, {
    ...data,
    exceptionNo: data.exceptionNo || data.orderNo,
  });
}

export function processOrder(data: MesFaultRepairApi.ProcessReq) {
  return requestClient.post<boolean>(`${BASE}/process`, data);
}

export function deleteOrderList(ids: number[]) {
  return Promise.all(
    ids.map((id) =>
      requestClient.delete<boolean>(`${BASE}/delete`, { params: { id } }),
    ),
  );
}

export function exportOrder(params: any) {
  return requestClient.download(`${BASE}/export-excel`, { params });
}

export function getRepairPartListById(exceptionId: number | string) {
  return requestClient.get<MesFaultRepairApi.RepairPart[]>(
    `${BASE}/part/list`,
    {
      params: { exceptionId },
    },
  );
}
