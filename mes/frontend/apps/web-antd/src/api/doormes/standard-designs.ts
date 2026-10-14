import { requestClient } from '#/api/request';
import type { DrawingSnapshot } from './drawings';

export interface StandardDesignEntry {
  drawingId:string;revision:number;status:'SUBMITTED'|'PUBLISHED'|'REJECTED';active:boolean;
  number:string;name:string;note:string;reviewNote:string|null;disableReason:string|null;
  submittedBy:number;reviewedBy:number|null;submittedAt:string;reviewedAt:string|null;
}
const endpoint='/doormes/standard-designs';
export function listStandardDesigns(params:{publishedOnly:boolean;keyword:string;pageNo:number;pageSize:number}) {
  return requestClient.get<{list:StandardDesignEntry[];total:number}>(`${endpoint}/page`,{params});
}
export function submitStandardDesign(drawingId:string,revision:number) {
  return requestClient.post<StandardDesignEntry>(`${endpoint}/submit`,{drawingId,revision});
}
export function reviewStandardDesign(drawingId:string,revision:number,approved:boolean,note:string) {
  return requestClient.post<StandardDesignEntry>(`${endpoint}/review`,{revision,approved,note},{params:{drawingId}});
}
export function disableStandardDesign(drawingId:string,revision:number,reason:string) {
  return requestClient.post<StandardDesignEntry>(`${endpoint}/disable`,{revision,reason},{params:{drawingId}});
}
export function reuseStandardDesign(drawingId:string,revision:number) {
  return requestClient.post<DrawingSnapshot>(`${endpoint}/reuse`,{drawingId,revision});
}
