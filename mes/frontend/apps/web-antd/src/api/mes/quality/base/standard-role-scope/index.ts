import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQualityStandardRoleScopeApi {
  export type ScopeType =
    | 'FINISHED'
    | 'INCOMING'
    | 'PACKAGING'
    | 'PROCESS'
    | 'PROCESS_GLUE_BOARD';

  export interface ScopeRow {
    id?: number;
    roleId?: number;
    scopeType?: ScopeType;
    standardId: number;
    standardApplyType?: 'FAI' | 'FQC' | 'GLUE_BOARD_FAI' | 'IPQC' | 'IQC' | 'OQC';
    standardNo?: string;
    standardName?: string;
    glueBoardModel?: string;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    productModelCode?: string;
    productModelName?: string;
    prodTypeName?: string;
    processCode?: string;
    processName?: string;
    version?: string;
    status?: number;
    auditStatus?: number;
    createTime?: string;
  }

  export interface PageReq extends PageParam {
    roleId: number;
    scopeType: ScopeType;
    modelKeyword?: string;
    standardKeyword?: string;
    standardName?: string;
    standardNo?: string;
    materialCode?: string;
    processKeyword?: string;
  }

  export interface AddReq {
    roleId: number;
    scopeType: ScopeType;
    standardIds: number[];
  }
}

const baseUrl = '/mes/quality/base/standard-role-scope';

export function getRoleScopePage(params: MesQualityStandardRoleScopeApi.PageReq) {
  return requestClient.get<
    PageResult<MesQualityStandardRoleScopeApi.ScopeRow>
  >(`${baseUrl}/page`, { params });
}

export function getRoleScopeCandidatePage(
  params: MesQualityStandardRoleScopeApi.PageReq,
) {
  return requestClient.get<
    PageResult<MesQualityStandardRoleScopeApi.ScopeRow>
  >(`${baseUrl}/candidate-page`, { params });
}

export function getRoleScopeCount(roleId: number) {
  return requestClient.get<Record<string, number>>(`${baseUrl}/count`, {
    params: { roleId },
  });
}

export function addRoleScopes(data: MesQualityStandardRoleScopeApi.AddReq) {
  return requestClient.post<boolean>(`${baseUrl}/add`, data);
}

export function removeRoleScopes(ids: number[]) {
  return requestClient.delete<boolean>(`${baseUrl}/remove?ids=${ids.join(',')}`);
}
