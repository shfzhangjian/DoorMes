import type { CatalogInput } from '#/api/doormes/catalog';
export function newCatalogInput(category:'finish'|'glass'='finish'):CatalogInput {
  return {category,code:'',name:'',specification:'',note:'',materialFamily:category==='glass'?'glass':'metal',baseColor:category==='glass'?'#c8e7f0':'#374151',
    metalness:category==='glass'?0:0.7,roughness:category==='glass'?0.1:0.35,opacity:category==='glass'?0.35:1,
    thicknessMm:category==='glass'?24:null,compatibleProfileSystemIds:category==='glass'?['AL70']:[]};
}
export function validateCatalogInput(input:CatalogInput):string {
  if(!/^[A-Za-z0-9][A-Za-z0-9._-]{0,59}$/.test(input.code))return '型号需为1–60位英文、数字、点、下划线或短横线。';
  if(!input.name.trim() || input.name.length>160 || !input.specification.trim() || input.specification.length>500)return '请填写有效名称和规格。';
  if(input.note.length>1000 || !/^#[0-9a-f]{6}$/i.test(input.baseColor))return '备注过长或颜色无效。';
  if([input.metalness,input.roughness,input.opacity].some(value=>!Number.isFinite(value)||value<0||value>1))return '渲染数值需在0–1之间。';
  if(input.category==='glass' && (!Number.isFinite(input.thicknessMm)||!input.thicknessMm||input.thicknessMm>200||input.thicknessMm<=0||!input.compatibleProfileSystemIds.length||input.compatibleProfileSystemIds.some(id=>!id.trim()||id.length>60)))return '玻璃厚度需大于0且不超过200mm，并设置适用型材系统。';
  if(input.category!=='finish' && input.category!=='glass')return '分类无效。';
  if(input.materialFamily!==(input.category==='glass'?'glass':'metal'))return '材质分类不匹配。';
  if(input.textureSetId&&(!/^MES-TEXTURE-[A-Za-z0-9._-]{1,88}$/.test(input.textureSetId)||!/^sha256:[a-f0-9]{64}$/.test(input.textureContentHash??'')))return '请选择有效本地纹理资源。';
  if(!input.textureSetId&&input.textureContentHash)return '纹理哈希需要对应资源编号。';
  if([input.textureRepeatX,input.textureRepeatY].some(value=>value!=null&&(!Number.isFinite(value)||value<.001||value>1000)))return '纹理重复需为0.001–1000，支持小数。';
  return '';
}
