import { requestClient } from '#/api/request';
import type { CatalogChoice } from '../../../../../vendor/doormes-engine/dist/engine.mjs';
export interface CatalogInput {
  category:'finish'|'glass';code:string;name:string;specification:string;note:string;
  materialFamily:'metal'|'glass';baseColor:string;metalness:number;roughness:number;opacity:number;
  thicknessMm:number|null;compatibleProfileSystemIds:string[];
  textureSetId?:string|null;textureContentHash?:string|null;textureRepeatX?:number|null;textureRepeatY?:number|null;
}
export interface CatalogSnapshot extends CatalogChoice {
  schemaVersion:'doormes-material-catalog.v1';tenantId:number;status:'DRAFT'|'PUBLISHED';
  changedBy:number;updatedAt:string;changeNote:string;item:CatalogInput;
}
export interface CatalogVersion {revision:number;status:'DRAFT'|'PUBLISHED';changeNote:string;changedBy:number;updatedAt:string;}
const endpoint='/doormes/catalog';
export function listCatalog(params:{category?:string;keyword?:string;published?:boolean;pageNo:number;pageSize:number}){return requestClient.get<{list:CatalogSnapshot[];total:number}>(`${endpoint}/page`,{params});}
export function getCatalog(id:string,revision?:number){return requestClient.get<CatalogSnapshot>(`${endpoint}/get`,{params:{id,revision}});}
export function catalogVersions(id:string){return requestClient.get<CatalogVersion[]>(`${endpoint}/versions`,{params:{id}});}
export function saveCatalog(item:CatalogInput,expectedRevision:number,changeNote:string,id?:string){const data={item,expectedRevision,changeNote};return id?requestClient.put<CatalogSnapshot>(`${endpoint}/revise`,data,{params:{id}}):requestClient.post<CatalogSnapshot>(`${endpoint}/create`,data);}
export function publishCatalog(id:string,expectedRevision:number,note:string){return requestClient.post<CatalogSnapshot>(`${endpoint}/publish`,{expectedRevision,note},{params:{id}});}
