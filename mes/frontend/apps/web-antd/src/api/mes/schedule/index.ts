import { defHttp } from '/@/utils/http/axios';

enum Api {
  ListByPlan = '/mes/schedule/list-by-plan',
  UpdateDispatch = '/mes/schedule/dispatch',
}

// 获取排程数据 (支持按 planId 或 workOrderId 查询)
export const getScheduleList = (params: { planId?: number; workOrderId?: number }) =>
  defHttp.get({ url: Api.ListByPlan, params });

// 更新排程 (拖拽后调用)
export const updateDispatch = (data: { id: number; stationId: number; startTime: string; endTime: string }) =>
  defHttp.put({ url: Api.UpdateDispatch, data });
