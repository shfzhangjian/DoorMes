import type { RequestResponse } from '@vben/request';
import { requestClient } from '#/api/request';
import type { AssetDescriptor,AssetFetch } from '../../../../../vendor/doormes-engine/dist/engine.mjs';
export type { AssetDescriptor };
const endpoint='/doormes/visual-assets';
export function listAssets(params:{kind?:string;keyword?:string;pageNo:number;pageSize:number}){return requestClient.get<{list:AssetDescriptor[];total:number}>(endpoint+'/page',{params});}
export async function uploadAsset(assetId:string,mediaType:string,bytes:ArrayBuffer,expectedContentHash:string){
  const grant=await requestClient.post<{grantId:string}>(endpoint+'/authorize',{assetId,mediaType,byteLength:bytes.byteLength,expectedContentHash});
  return requestClient.put<{asset:AssetDescriptor}>(endpoint+'/uploads/'+encodeURIComponent(grant.grantId),bytes,{headers:{'Content-Type':mediaType},timeout:30_000});
}
/** Reuses MES authentication/refresh/tenant interceptors; never calls an unbound Window.fetch. */
export const assetTransport:AssetFetch=async(path,init={})=>{
  if(!/^\/(authorize|uploads\/[0-9a-f-]{36}|get\?|content\?)/.test(path))throw Error('资源路径无效。');
  if(path.startsWith('/content?')){
    const result=await requestClient.get<RequestResponse<Blob>>(endpoint+path,{responseReturn:'raw',responseType:'blob',timeout:30_000});
    return new Response(result.data,{status:result.status,headers:{'Content-Type':String(result.headers['content-type']||'application/octet-stream')}});
  }
  const data=await requestClient.request<unknown>(endpoint+path,{method:init.method||'GET',headers:Object.fromEntries(new Headers(init.headers).entries()),data:init.body,timeout:30_000});
  return new Response(JSON.stringify({code:0,data}),{headers:{'Content-Type':'application/json'}});
};
export async function downloadAsset(asset:AssetDescriptor){
  const response=await assetTransport('/content?assetId='+encodeURIComponent(asset.assetId)+'&contentHash='+encodeURIComponent(asset.contentHash));
  if(!response.ok||response.headers.get('content-type')?.includes('application/json'))throw Error('资源文件无法读取。');
  const bytes=await response.arrayBuffer();const engine=await import('../../../../../vendor/doormes-engine/dist/engine.mjs');
  if(bytes.byteLength!==asset.byteLength||await engine.calculateVisualAssetContentHash(bytes)!==asset.contentHash)throw Error('资源文件哈希不符。');
  const extension={'image/png':'png','image/jpeg':'jpg','model/gltf-binary':'glb','model/gltf+json':'gltf'}[asset.mediaType]||'bin';
  const url=URL.createObjectURL(new Blob([bytes],{type:asset.mediaType}));const link=document.createElement('a');link.href=url;link.download=asset.assetId+'.'+extension;link.click();URL.revokeObjectURL(url);
}
