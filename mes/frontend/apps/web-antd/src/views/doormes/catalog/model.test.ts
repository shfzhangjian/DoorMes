import { describe,it,expect } from 'vitest';
import { newCatalogInput,validateCatalogInput } from './model';
describe('real MES catalog input',()=>{
  const valid=()=>({...newCatalogInput(),code:'FIN-001',name:'深灰喷涂',specification:'RAL7016'});
  it('defaults are detached and correctly categorized',()=>{const a=newCatalogInput('glass'),b=newCatalogInput('glass');a.compatibleProfileSystemIds.push('OTHER');expect(b.compatibleProfileSystemIds).toEqual(['AL70']);expect(a.materialFamily).toBe('glass');});
  it('validates required business model/name/specification',()=>{expect(validateCatalogInput(valid())).toBe('');expect(validateCatalogInput({...valid(),code:'名称不能作为编号'})).not.toBe('');expect(validateCatalogInput({...valid(),name:''})).not.toBe('');});
  it('rejects unsafe color and nonfinite rendering values',()=>{expect(validateCatalogInput({...valid(),baseColor:'url(fake)'})).not.toBe('');expect(validateCatalogInput({...valid(),opacity:NaN})).not.toBe('');expect(validateCatalogInput({...valid(),roughness:1.1})).not.toBe('');});
  it('glass needs physical thickness and compatibility',()=>{const glass={...newCatalogInput('glass'),code:'GL-24',name:'玻璃',specification:'6+12+6'};expect(validateCatalogInput(glass)).toBe('');expect(validateCatalogInput({...glass,thicknessMm:0})).not.toBe('');expect(validateCatalogInput({...glass,compatibleProfileSystemIds:[]})).not.toBe('');});
  it('retains pinned local texture and fractional repeats',()=>{expect(validateCatalogInput({...valid(),textureSetId:'MES-TEXTURE-CHECKER',textureContentHash:'sha256:'+'a'.repeat(64),textureRepeatX:.5,textureRepeatY:.75})).toBe('');});
  it('rejects invalid texture identity/hash and unsafe repeat counts',()=>{expect(validateCatalogInput({...valid(),textureSetId:'OTHER',textureContentHash:'sha256:'+'a'.repeat(64)})).not.toBe('');expect(validateCatalogInput({...valid(),textureSetId:'MES-TEXTURE-CHECKER'})).not.toBe('');expect(validateCatalogInput({...valid(),textureContentHash:'sha256:'+'a'.repeat(64)})).not.toBe('');for(const value of [NaN,0,1001])expect(validateCatalogInput({...valid(),textureRepeatX:value})).not.toBe('');});
});
