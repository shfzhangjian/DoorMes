import type { Router } from 'vue-router';
import { reactive } from 'vue';

export const routeLoadFailure = reactive({ path: '', retrying: false });

/** Keep authentication intact when the server is temporarily unavailable. */
export function installRouteLoadRecovery(router: Router) {
  router.onError((_error, to) => {
    routeLoadFailure.path = to.fullPath;
    routeLoadFailure.retrying = false;
  });
  router.afterEach((_to, _from, failure) => {
    if (!failure) routeLoadFailure.path = '';
  });
}

export async function retryRouteLoad(router: Router) {
  if (!routeLoadFailure.path || routeLoadFailure.retrying) return;
  routeLoadFailure.retrying = true;
  try {
    await router.replace(routeLoadFailure.path);
  } catch {
    // onError keeps the recovery view visible until navigation succeeds.
  } finally {
    routeLoadFailure.retrying = false;
  }
}
