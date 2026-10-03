import { describe,expect,it,vi } from 'vitest';
import { createRequestId,newDrawingInput,safeDrawingFileName,snapshotMetadata,validateDrawingInput,validateDrawingMetadata } from './model';
import type { DrawingSnapshot } from '#/api/doormes/drawings';

describe('independent drawing creation and metadata',()=>{
  const valid=()=>({...newDrawingInput(),name:'测试门窗设计'});
  it('creates detached forms and LAN-compatible request UUIDs without randomUUID',()=>{
    const first=valid(),second=valid();first.mark='C9';
    expect(second.mark).toBe('C1');expect(first.clientRequestId).not.toBe(second.clientRequestId);
    expect(first.clientRequestId).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    expect(second).toMatchObject({widthMm:1200,heightMm:1500,quantity:1,number:''});
  });
  it('fails closed if cryptographic random bytes are unavailable',()=>{
    vi.stubGlobal('crypto',undefined);try{expect(()=>createRequestId()).toThrow();}finally{vi.unstubAllGlobals();}
  });
  it('requires a valid name but permits generated drawing numbers',()=>{
    expect(validateDrawingInput(valid())).toBe('');expect(validateDrawingInput({...valid(),name:'  '})).not.toBe('');
    expect(validateDrawingMetadata({number:'研发-001/A',name:'转角窗',note:''})).toBe('');
    for(const number of ['bad number','bad\nnumber','<tag>', 'a'.repeat(61)])expect(validateDrawingMetadata({number,name:'设计',note:''})).not.toBe('');
  });
  it('matches the engine ASCII first-window mark contract',()=>{
    expect(validateDrawingInput({...valid(),mark:'C1-A_1.2'})).toBe('');
    for(const mark of ['中文','C1/A','C 1','-C1','', 'C'.repeat(41)])expect(validateDrawingInput({...valid(),mark})).not.toBe('');
  });
  it('rejects invalid quantity and nonfinite geometry',()=>{
    for(const quantity of [0,1.5,1001,NaN])expect(validateDrawingInput({...valid(),quantity})).not.toBe('');
    for(const value of [0,NaN,Infinity,50001]){
      expect(validateDrawingInput({...valid(),widthMm:value})).not.toBe('');expect(validateDrawingInput({...valid(),heightMm:value})).not.toBe('');
    }
  });
  it('keeps metadata separate from geometry and suppresses internal IDs in names',()=>{
    const snapshot={metadata:{number:'工厂/设计-001',name:'转角窗',note:'备注',source:'INDEPENDENT'},document:{windows:[{mark:'C1'}]}} as unknown as DrawingSnapshot;
    expect(snapshotMetadata(snapshot)).toEqual({number:'工厂/设计-001',name:'转角窗',note:'备注'});
    expect(safeDrawingFileName(snapshot.metadata!.number,2,true)).toBe('工厂_设计-001-R2-未保存.json');
    delete snapshot.metadata;expect(snapshotMetadata(snapshot)).toEqual({number:'C1',name:'门窗设计',note:''});
  });
});
