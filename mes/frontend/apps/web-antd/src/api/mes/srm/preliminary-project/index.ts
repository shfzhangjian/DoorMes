import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmPreliminaryProjectApi {
  export type ProjectStatus = 'DISABLED' | 'ENABLED';

  export interface ScorerConfigItem {
    id?: number;
    projectId?: number;
    templateId?: number;
    templateVersionId?: number;
    templateItemId?: number;
    groupCodeSnapshot?: string;
    groupNameSnapshot?: string;
    groupSort?: number;
    indicatorCodeSnapshot?: string;
    indicatorNameSnapshot?: string;
    indicatorSort?: number;
    defaultDeptNames?: string;
    scorerCandidateUserIds?: string;
    scorerCandidateUserNames?: string;
    scorerUserId?: number;
    scorerUserName?: string;
  }

  export interface Project {
    id?: number;
    projectCode: string;
    projectName: string;
    currentTemplateId?: number;
    currentTemplateVersionId?: number;
    templateCodeSnapshot?: string;
    templateNameSnapshot?: string;
    templateVersionNoSnapshot?: string;
    status?: ProjectStatus;
    remark?: string;
    version?: number;
    scorerItems?: ScorerConfigItem[];
    createTime?: string;
    updateTime?: string;
  }

  export interface ScorerConfigSaveReq {
    projectId: number;
    templateVersionId: number;
    items: Array<{
      scorerCandidateUserIds?: number[];
      scorerUserId?: number;
      templateItemId: number;
    }>;
  }
}

export function getProjectPage(params: PageParam & Record<string, unknown>) {
  return requestClient.get<PageResult<SrmPreliminaryProjectApi.Project>>(
    '/mes/srm/preliminary-project/page',
    { params },
  );
}

export function getEnabledProjectList() {
  return requestClient.get<SrmPreliminaryProjectApi.Project[]>(
    '/mes/srm/preliminary-project/enabled-list',
  );
}

export function getProjectDetail(id: number) {
  return requestClient.get<SrmPreliminaryProjectApi.Project>(
    '/mes/srm/preliminary-project/get',
    { params: { id } },
  );
}

export function createProject(data: SrmPreliminaryProjectApi.Project) {
  return requestClient.post<number>(
    '/mes/srm/preliminary-project/create',
    data,
  );
}

export function updateProject(data: SrmPreliminaryProjectApi.Project) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-project/update',
    data,
  );
}

export function deleteProject(id: number) {
  return requestClient.delete<boolean>('/mes/srm/preliminary-project/delete', {
    params: { id },
  });
}

export function getProjectScorerConfig(
  projectId: number,
  templateVersionId: number,
) {
  return requestClient.get<SrmPreliminaryProjectApi.Project>(
    '/mes/srm/preliminary-project/scorer-config',
    { params: { projectId, templateVersionId } },
  );
}

export function saveProjectScorerConfig(
  data: SrmPreliminaryProjectApi.ScorerConfigSaveReq,
) {
  return requestClient.put<boolean>(
    '/mes/srm/preliminary-project/scorer-config',
    data,
  );
}
