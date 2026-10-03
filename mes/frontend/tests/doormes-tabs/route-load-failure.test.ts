import { createMemoryHistory, createRouter } from 'vue-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { installRouteLoadRecovery, retryRouteLoad, routeLoadFailure } from '../../apps/web-antd/src/router/load-failure';

afterEach(()=>{routeLoadFailure.path='';routeLoadFailure.retrying=false;});
describe('initial route recovery after backend permission failure',()=>{
  it('shows a recoverable failure and retries the original design address after the server recovers',async()=>{
    const router=createRouter({history:createMemoryHistory(),routes:[{path:'/factory/design',component:{render:()=>null}}]});
    let available=false;
    const permission=vi.fn(()=>{if(!available)throw new Error('controlled server unavailable');return true;});
    router.beforeEach(permission);installRouteLoadRecovery(router);
    await expect(router.push('/factory/design')).rejects.toThrow('controlled server unavailable');
    expect(routeLoadFailure.path).toBe('/factory/design');expect(router.currentRoute.value.path).toBe('/');
    await retryRouteLoad(router);expect(routeLoadFailure.path).toBe('/factory/design');expect(routeLoadFailure.retrying).toBe(false);
    available=true;await retryRouteLoad(router);
    expect(router.currentRoute.value.path).toBe('/factory/design');expect(routeLoadFailure.path).toBe('');
    expect(permission).toHaveBeenCalledTimes(3);
  });
  it('does not start duplicate retries while a permission request is pending',async()=>{
    routeLoadFailure.path='/factory/design';let finish:()=>void=()=>{};
    const replace=vi.fn(()=>new Promise<void>(resolve=>{finish=resolve;}));
    const router={replace} as unknown as Parameters<typeof retryRouteLoad>[0];
    const first=retryRouteLoad(router);await retryRouteLoad(router);expect(replace).toHaveBeenCalledTimes(1);
    finish();await first;expect(routeLoadFailure.retrying).toBe(false);
  });
});
