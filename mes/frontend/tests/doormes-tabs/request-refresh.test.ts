import { describe, expect, it, vi } from 'vitest';
import { RequestClient } from '../../packages/effects/request/src/request-client/request-client';
import { authenticateResponseInterceptor, defaultResponseInterceptor } from '../../packages/effects/request/src/request-client/preset-interceptors';

vi.mock('@vben/locales',()=>({$t:(key:string)=>key}));
vi.mock('@vben/utils',async()=>({
  ...(await import('../../packages/@core/base/shared/src/utils/merge')),
  ...(await import('../../packages/@core/base/shared/src/utils/util')),
  isFunction:(value:unknown)=>typeof value==='function',isString:(value:unknown)=>typeof value==='string',isUndefined:(value:unknown)=>value===undefined,
}));

// S-REFRESH-v1: Real request client + Axios interceptor chain, only the HTTP adapter is controlled.
describe('drawing response contract across authentication refresh',()=>{
  it('401 -> refresh -> retry returns the drawing snapshot, not an Axios/envelope wrapper',async()=>{
    let token='expired';const seen:any[]=[];
    const snapshot={id:'drawing-controlled-1',revision:3,document:{windows:[]}};
    const client=new RequestClient({responseReturn:'data',adapter:async(config)=>{
      seen.push({url:config.url,params:config.params,responseReturn:config.responseReturn,authorization:config.headers.Authorization});
      return {status:200,statusText:'OK',headers:{},config,data:token==='expired'?{code:401,msg:'账号未登录'}:{code:0,data:snapshot}};
    }});
    client.addRequestInterceptor({fulfilled:config=>{config.headers.Authorization=`Bearer ${token}`;return config;}});
    client.addResponseInterceptor(defaultResponseInterceptor({codeField:'code',dataField:'data',successCode:0}));
    const refresh=vi.fn(async()=>{token='fresh';return token;});
    client.addResponseInterceptor(authenticateResponseInterceptor({client,doRefreshToken:refresh,doReAuthenticate:vi.fn(),enableRefreshToken:true,formatToken:value=>`Bearer ${value}`}));
    const result=await client.get('/doormes/drawings/get',{params:{id:snapshot.id,revision:3}});
    expect(refresh).toHaveBeenCalledTimes(1);
    expect(seen).toEqual([{url:'/doormes/drawings/get',params:{id:snapshot.id,revision:3},responseReturn:'data',authorization:'Bearer expired'},
      {url:'/doormes/drawings/get',params:{id:snapshot.id,revision:3},responseReturn:'data',authorization:'Bearer fresh'}]);
    expect(result).toEqual(snapshot);
  });

  it('concurrent expired requests share one refresh and each receives its own unwrapped result',async()=>{
    let token='expired',completeRefresh:undefined|(()=>void);
    const seen:Array<{id:string;revision:number;authorization:unknown}>=[];
    const client=new RequestClient({responseReturn:'data',adapter:async(config)=>{
      seen.push({...config.params,authorization:config.headers.Authorization});
      return {status:200,statusText:'OK',headers:{},config,data:token==='expired'?{code:401,msg:'账号未登录'}:{code:0,data:{...config.params,document:{windows:[]}}}};
    }});
    client.addRequestInterceptor({fulfilled:config=>{config.headers.Authorization=`Bearer ${token}`;return config;}});
    client.addResponseInterceptor(defaultResponseInterceptor({codeField:'code',dataField:'data',successCode:0}));
    const refresh=vi.fn(()=>new Promise<string>(resolve=>{completeRefresh=()=>{token='fresh';resolve(token);};}));
    const reAuthenticate=vi.fn();
    client.addResponseInterceptor(authenticateResponseInterceptor({client,doRefreshToken:refresh,doReAuthenticate:reAuthenticate,enableRefreshToken:true,formatToken:value=>`Bearer ${value}`}));
    const first=client.get('/doormes/drawings/get',{params:{id:'drawing-one',revision:1}});
    const second=client.get('/doormes/drawings/get',{params:{id:'drawing-two',revision:3}});
    await vi.waitFor(()=>expect(client.refreshTokenQueue).toHaveLength(1));
    completeRefresh!();
    expect(await Promise.all([first,second])).toEqual([{id:'drawing-one',revision:1,document:{windows:[]}},
      {id:'drawing-two',revision:3,document:{windows:[]}}]);
    expect(refresh).toHaveBeenCalledTimes(1);expect(reAuthenticate).not.toHaveBeenCalled();
    expect(seen.filter(item=>item.authorization==='Bearer fresh')).toEqual(expect.arrayContaining([
      {id:'drawing-one',revision:1,authorization:'Bearer fresh'},
      {id:'drawing-two',revision:3,authorization:'Bearer fresh'},
    ]));
    expect(client.refreshTokenQueue).toEqual([]);expect(client.isRefreshing).toBe(false);
  });

  it('normal data/body/raw response modes remain explicit and a missing optional drawing stays null',async()=>{
    const client=new RequestClient({responseReturn:'data',adapter:async config=>({status:200,statusText:'OK',headers:{},config,data:{code:0,data:null}})});
    client.addRequestInterceptor({fulfilled:async config=>config});
    client.addResponseInterceptor(defaultResponseInterceptor({codeField:'code',dataField:'data',successCode:0}));
    expect(await client.get('/find')).toBeNull();
    expect(await client.get('/find',{responseReturn:'body'})).toEqual({code:0,data:null});
    expect(await client.get('/find',{responseReturn:'raw'})).toEqual(expect.objectContaining({status:200,data:{code:0,data:null}}));
  });
});
