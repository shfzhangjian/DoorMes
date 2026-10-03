import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSampleEvaluationProjectApi {
  export type ProjectStatus = 'DISABLED' | 'ENABLED';

  export interface UserConfig {
    canAssign?: boolean;
    canInitiate?: boolean;
    deptCode?: string;
    deptName?: string;
    id?: number;
    projectId?: number;
    remark?: string;
    sortNo?: number;
    userId?: number;
    userName?: string;
  }

  export interface Project {
    createTime?: string;
    id?: number;
    projectCode: string;
    projectName: string;
    remark?: string;
    status?: ProjectStatus;
    updateTime?: string;
    users?: UserConfig[];
    version?: number;
  }
}

const BASE_URL = '/mes/srm/sample-evaluation-project';

export function getProjectPage(params: PageParam & Record<string, unknown>) {
  return requestClient.get<PageResult<SrmSampleEvaluationProjectApi.Project>>(
    `${BASE_URL}/page`,
    { params },
  );
}

export function getEnabledProjectList() {
  return requestClient.get<SrmSampleEvaluationProjectApi.Project[]>(
    `${BASE_URL}/enabled-list`,
  );
}

export function getProjectDetail(id: number) {
  return requestClient.get<SrmSampleEvaluationProjectApi.Project>(
    `${BASE_URL}/get`,
    { params: { id } },
  );
}

export function createProject(data: SrmSampleEvaluationProjectApi.Project) {
  return requestClient.post<number>(`${BASE_URL}/create`, data);
}

export function updateProject(data: SrmSampleEvaluationProjectApi.Project) {
  return requestClient.put<boolean>(`${BASE_URL}/update`, data);
}

export function deleteProject(id: number) {
  return requestClient.delete<boolean>(`${BASE_URL}/delete`, {
    params: { id },
  });
}
