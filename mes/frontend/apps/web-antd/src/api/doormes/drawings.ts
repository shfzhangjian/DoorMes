import { requestClient } from '#/api/request';
import type { EngineDocument } from '../../../../../vendor/doormes-engine/dist/engine.mjs';
export type { EngineDocument };
export type DrawingSource='INDEPENDENT'|'REQUIREMENT';
export interface DrawingMetadata {number:string;name:string;note:string;source:DrawingSource;}
export type DrawingMetadataInput=Pick<DrawingMetadata,'number'|'name'|'note'>;
export interface DrawingCreateInput {clientRequestId?:string;number?:string;name:string;note?:string;mark:string;quantity:number;widthMm:number;heightMm:number;}
export interface DrawingSummary {id:string;revision:number;status:'DRAFT';number:string;name:string;note:string;source:DrawingSource;requirementId:string|null;requirementRevision:number;lineId:string|null;createdBy:number;updatedAt:string;editable:boolean;}
export interface DrawingSnapshot {
  schemaVersion:'doormes-drawing-snapshot.v1';id:string;tenantId:number;requirementId:string|null;
  requirementRevision:number;lineId:string|null;revision:number;status:'DRAFT';createdBy:number;
  changedBy:number;createdAt:string;updatedAt:string;changeNote:string;document:EngineDocument;
  metadata?:DrawingMetadata;editable?:boolean;
}
export interface DrawingVersion { revision:number;changeNote:string;changedBy:number;updatedAt:string; }
const endpoint='/doormes/drawings';
export function listDrawings(params:{keyword?:string;source?:DrawingSource;mine?:boolean;pageNo:number;pageSize:number}) {return requestClient.get<{list:DrawingSummary[];total:number}>(`${endpoint}/page`,{params});}
export function createDrawing(input:DrawingCreateInput) {return requestClient.post<DrawingSnapshot>(`${endpoint}/create`,input);}
export function findDrawing(requirementId:string,lineId:string) {return requestClient.get<DrawingSnapshot|null>(`${endpoint}/find`,{params:{requirementId,lineId}});}
export function openDrawing(requirementId:string,lineId:string,expectedRequirementRevision:number) {return requestClient.post<DrawingSnapshot>(`${endpoint}/open`,{requirementId,lineId,expectedRequirementRevision});}
export function getDrawing(id:string,revision?:number) {return requestClient.get<DrawingSnapshot>(`${endpoint}/get`,{params:{id,revision}});}
export function getDrawingVersions(id:string) {return requestClient.get<DrawingVersion[]>(`${endpoint}/versions`,{params:{id}});}
export function saveDrawing(id:string,expectedRevision:number,changeNote:string,document:EngineDocument,metadata?:DrawingMetadataInput) {return requestClient.put<DrawingSnapshot>(`${endpoint}/save`,{expectedRevision,changeNote,document,...(metadata?{metadata}:{})},{params:{id}});}
