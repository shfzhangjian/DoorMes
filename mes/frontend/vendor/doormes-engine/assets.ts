import { calculateVisualAssetContentHash, type ManagedGltfAssetGateway, type StoredVisualAssetBlob } from '@doormes/visual-asset-storage';

export { calculateVisualAssetContentHash, inspectManagedGltfAsset } from '@doormes/visual-asset-storage';
export { portableSha256 } from './packages/visual-asset-storage/src/sha256';
export { listManagedComponentAssetRequirements,listManagedTextureAssetRequirements } from '@doormes/ui-components';
export interface AssetDescriptor { assetId:string;kind:'component-model'|'texture-bundle';mediaType:string;contentHash:string;byteLength:number;storedAtIso:string; }
export type AssetFetch = (path:string,init?:RequestInit)=>Promise<Response>;

/** MES response envelope, real authenticated transport supplied by the Vue app. */
export function createMesAssetGateway(transport:AssetFetch):ManagedGltfAssetGateway {
  async function data<T>(response:Response):Promise<T>{
    const result=await response.json() as {code:number;data:T;msg?:string};
    if(!response.ok||result.code!==0)throw new Error(typeof result.msg==='string'?result.msg.slice(0,180):'本地资源服务请求失败。');
    return result.data;
  }
  return {
    async upload(input){
      const grant=await data<{grantId:string}>(await transport('/authorize',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({assetId:input.assetId,expectedContentHash:input.expectedContentHash,mediaType:input.mediaType,byteLength:input.bytes.byteLength})}));
      const evidence=await data<Awaited<ReturnType<ManagedGltfAssetGateway['upload']>>>(await transport('/uploads/'+encodeURIComponent(grant.grantId),{method:'PUT',headers:{'Content-Type':input.mediaType},body:input.bytes}));
      if(evidence.asset.assetId!==input.assetId||evidence.asset.contentHash!==input.expectedContentHash||evidence.inspection.contentHash!==input.expectedContentHash||evidence.asset.byteLength!==input.bytes.byteLength)throw Error('资源保存结果与上传文件不一致。');
      return evidence;
    },
    async describe(assetId){
      const response=await transport('/get?assetId='+encodeURIComponent(assetId));
      const envelope=await response.json() as {code:number;data:AssetDescriptor;msg?:string};
      if(response.status===404||envelope.code===404)return undefined;
      if(!response.ok||envelope.code!==0)throw Error('本地资源不存在、无权限或已损坏。');
      if(envelope.data.assetId!==assetId)throw Error('资源身份不一致。');
      return envelope.data;
    },
    async read(assetId,expectedContentHash):Promise<StoredVisualAssetBlob|undefined>{
      const descriptor=await this.describe(assetId);if(!descriptor||descriptor.contentHash!==expectedContentHash)return undefined;
      const response=await transport('/content?assetId='+encodeURIComponent(assetId)+'&contentHash='+encodeURIComponent(expectedContentHash));
      if(!response.ok||response.headers.get('content-type')?.includes('application/json')){await data(response);throw Error('资源二进制读取失败。');}
      const bytes=await response.arrayBuffer();
      if(bytes.byteLength!==descriptor.byteLength||await calculateVisualAssetContentHash(bytes)!==expectedContentHash)throw Error('资源文件哈希不一致。');
      return {descriptor,bytes};
    },
  };
}
