import { requestClient } from '#/api/request';
import type { DrawingSnapshot } from './drawings';

export interface BomLine {
  objectId: string; displayCode: string; name: string; category: string; modelCode: string;
  quantity: number; unit: string; orderQuantity?: number; totalQuantity?: number;
  dimensions: unknown; remark: string; productionReady: false;
  [key: string]: unknown;
}
export interface DesignBom { lines: BomLine[]; diagnostics: unknown[]; productionReady: false; }
export interface DrawingReference { id: string; revision: number; number: string; name: string; }
export type OrderStatus = 'DRAFT'|'SUBMITTED'|'IN_DESIGN'|'BOM_READY';
export interface OrderChange {
  id: string; status: 'PENDING'|'APPROVED'|'REJECTED'|'APPLIED'; reason: string;
  from: DrawingReference; to: DrawingReference;
  diff: {added: BomLine[]; removed: BomLine[]; modified: {before: BomLine; after: BomLine}[]};
  requestedBy: number; requestedAt: string; reviewNote?: string;
}
export interface ProductionOrder {
  id: string; revision: number; number: string; customer: string; project: string;
  type: 'STANDARD'|'CUSTOM'; quantity: number; note: string; status: OrderStatus;
  requirement?: {id: string; revision: number; lineId: string}|null;
  drawing?: DrawingReference|null; bom?: DesignBom|null; changes: OrderChange[];
  productionReady: false; createdBy: number; assignedTo?: number|null; updatedAt: string;
}
export interface OrderVersion {revision: number; action: string; note: string; changedBy: number; updatedAt: string;}
export type ProductionOrderSummary=Omit<ProductionOrder,'bom'|'changes'> & {changeStatus?:OrderChange['status']|null;changeCount:number};
export interface CustomDemand {
  mark: string; widthMm: number; heightMm: number; material: string; glass: string;
  hardware: string; finish: string; dueDate: string; note: string;
}
export interface OrderInput {
  number: string; customer: string; project: string; type: 'STANDARD'|'CUSTOM'; quantity: number;
  note: string; drawingId?: string; drawingRevision?: number; custom?: CustomDemand;
}
const endpoint='/doormes/production-orders';
export const listProductionOrders=(params:{keyword?:string;type?:string;status?:string;pageNo:number;pageSize:number})=>requestClient.get<{list:ProductionOrderSummary[];total:number}>(`${endpoint}/page`,{params});
export const getProductionOrder=(id:string,revision?:number)=>requestClient.get<ProductionOrder>(`${endpoint}/get`,{params:{id,revision}});
export const getOrderVersions=(id:string)=>requestClient.get<OrderVersion[]>(`${endpoint}/versions`,{params:{id}});
export const createProductionOrder=(input:OrderInput)=>requestClient.post<ProductionOrder>(`${endpoint}/create`,input);
export const updateProductionOrder=(id:string,expectedRevision:number,input:OrderInput)=>requestClient.put<ProductionOrder>(`${endpoint}/update`,{expectedRevision,...input},{params:{id}});
export const actOnOrder=(id:string,action:'submit'|'claim',expectedRevision:number,note:string)=>requestClient.post<ProductionOrder>(`${endpoint}/${action}`,{expectedRevision,note},{params:{id}});
export const openOrderDrawing=(id:string,expectedRevision:number,note:string)=>requestClient.post<{order:ProductionOrder;drawing:DrawingSnapshot}>(`${endpoint}/open-drawing`,{expectedRevision,note},{params:{id}});
export const bindOrderDrawing=(id:string,expectedRevision:number,drawingId:string,drawingRevision:number,note:string)=>requestClient.post<ProductionOrder>(`${endpoint}/bind-drawing`,{expectedRevision,drawingId,drawingRevision,note},{params:{id}});
export const requestOrderChange=(id:string,expectedRevision:number,drawingId:string,drawingRevision:number,reason:string)=>requestClient.post<ProductionOrder>(`${endpoint}/changes/request`,{expectedRevision,drawingId,drawingRevision,reason},{params:{id}});
export const reviewOrderChange=(id:string,expectedRevision:number,changeId:string,approved:boolean,note:string)=>requestClient.post<ProductionOrder>(`${endpoint}/changes/review`,{expectedRevision,changeId,approved,note},{params:{id}});
export const applyOrderChange=(id:string,expectedRevision:number,changeId:string,note:string)=>requestClient.post<ProductionOrder>(`${endpoint}/changes/apply`,{expectedRevision,changeId,note},{params:{id}});
