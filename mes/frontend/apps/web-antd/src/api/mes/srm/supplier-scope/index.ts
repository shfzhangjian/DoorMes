import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace SrmSupplierScopeApi {
  export type PermissionLevel = 'EDIT' | 'FULL' | 'MASKED';
  export type ScopeStatus = 'DISABLED' | 'ENABLED';

  export interface Member {
    id?: number;
    permissionLevel?: PermissionLevel;
    scopeId?: number;
    userId?: number;
    userName?: string;
  }

  export interface Scope {
    createTime?: string;
    id?: number;
    memberCount?: number;
    members?: Member[];
    remark?: string;
    scopeCode?: string;
    scopeName?: string;
    sort?: number;
    status?: ScopeStatus;
    version?: number;
  }

  export interface MaskField {
    fieldKey: string;
    fieldLabel?: string;
    id?: number;
    maskEnabled?: boolean;
    remark?: string;
    sort?: number;
    version?: number;
  }
}

export function getSupplierScopePage(params: PageParam & Record<string, any>) {
  return requestClient.get<PageResult<SrmSupplierScopeApi.Scope>>(
    '/mes/srm/supplier-scope/page',
    { params },
  );
}

export function getSupplierScope(id: number) {
  return requestClient.get<SrmSupplierScopeApi.Scope>(
    `/mes/srm/supplier-scope/get?id=${id}`,
  );
}

export function getSupplierScopeSimpleList() {
  return requestClient.get<SrmSupplierScopeApi.Scope[]>(
    '/mes/srm/supplier-scope/simple-list',
  );
}

export function createSupplierScope(data: SrmSupplierScopeApi.Scope) {
  return requestClient.post('/mes/srm/supplier-scope/create', data);
}

export function updateSupplierScope(data: SrmSupplierScopeApi.Scope) {
  return requestClient.put('/mes/srm/supplier-scope/update', data);
}

export function deleteSupplierScope(id: number) {
  return requestClient.delete(`/mes/srm/supplier-scope/delete?id=${id}`);
}

export function getSupplierMaskFields() {
  return requestClient.get<SrmSupplierScopeApi.MaskField[]>(
    '/mes/srm/supplier-scope/mask-field/list',
  );
}

export function updateSupplierMaskFields(
  items: Array<{ fieldKey: string; maskEnabled: boolean }>,
) {
  return requestClient.put('/mes/srm/supplier-scope/mask-field/update', {
    items,
  });
}
