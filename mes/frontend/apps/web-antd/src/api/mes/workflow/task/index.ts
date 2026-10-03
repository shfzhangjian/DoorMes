import type { PageParam, PageResult } from '@vben/request';

import type { BpmTaskApi } from '#/api/bpm/task';

import { requestClient } from '#/api/request';

/** 查询 MES 工作台待办任务分页 */
export async function getMesWorkflowTaskTodoPage(params: PageParam) {
  return requestClient.get<PageResult<BpmTaskApi.Task>>(
    '/mes/workflow/task/todo-page',
    { params },
  );
}

/** 查询 MES 工作台已办任务分页 */
export async function getMesWorkflowTaskDonePage(params: PageParam) {
  return requestClient.get<PageResult<BpmTaskApi.Task>>(
    '/mes/workflow/task/done-page',
    { params },
  );
}
