import { requestClient } from '#/api/request';
import type { RequirementDocument, RequirementInput, RequirementSummary, RequirementVersion } from './requirement-contract';
export * from './requirement-contract';

const endpoint = '/doormes/requirements';
export function listRequirements(params: {keyword?: string;status?: string;pageNo: number;pageSize: number}) {
  return requestClient.get<{list: RequirementSummary[];total: number}>(`${endpoint}/page`, { params });
}
export function getRequirement(id: string, revision?: number) {
  return requestClient.get<RequirementDocument>(`${endpoint}/get`, { params: { id, revision } });
}
export function getRequirementVersions(id: string) {
  return requestClient.get<RequirementVersion[]>(`${endpoint}/versions`, { params: { id } });
}
export function saveRequirement(demand: RequirementInput, expectedRevision: number, changeNote: string, id?: string) {
  const data = { demand, expectedRevision, changeNote };
  return id
    ? requestClient.put<RequirementDocument>(`${endpoint}/update`, data, { params: { id } })
    : requestClient.post<RequirementDocument>(`${endpoint}/create`, data);
}
export function actOnRequirement(id: string, action: 'claim' | 'submit', expectedRevision: number, note: string) {
  return requestClient.post<RequirementDocument>(`${endpoint}/${action}`, { expectedRevision, note }, { params: { id } });
}
