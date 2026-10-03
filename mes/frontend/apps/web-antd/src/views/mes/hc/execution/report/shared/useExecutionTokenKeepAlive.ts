import { onBeforeUnmount, onMounted } from 'vue';

import { useAccessStore } from '@vben/stores';

import { refreshTokenApi } from '#/api/core';

const DEFAULT_KEEP_ALIVE_INTERVAL_MS = 20 * 60 * 1000;
const MIN_REFRESH_GAP_MS = 5 * 60 * 1000;

let refreshPromise: Promise<void> | null = null;
let lastRefreshAt = 0;
let unauthorizedAlertShown = false;

function resolveTokenPayload(response: any) {
  return response?.data?.data ?? response?.data ?? response ?? {};
}

function resolveErrorPayload(error: any) {
  return error?.response?.data ?? error?.data ?? error ?? {};
}

function getErrorMessage(error: any) {
  const payload = resolveErrorPayload(error);
  return String(
    payload?.msg
      || payload?.message
      || payload?.error
      || error?.message
      || '',
  );
}

function isUnauthorizedError(error: any) {
  const payload = resolveErrorPayload(error);
  const message = getErrorMessage(error);
  return error?.response?.status === 401
    || error?.status === 401
    || payload?.code === 401
    || message.includes('401')
    || message.includes('未登录')
    || message.includes('登录状态')
    || message.includes('令牌已过期')
    || message.includes('刷新令牌已过期');
}

function alertUnauthorizedOnce(error: any) {
  if (unauthorizedAlertShown) {
    return;
  }
  unauthorizedAlertShown = true;
  const message = getErrorMessage(error);
  window.alert(message || '登录状态已失效，请重新登录后再继续报工。');
}

async function refreshExecutionToken(force = false) {
  const now = Date.now();
  if (!force && now - lastRefreshAt < MIN_REFRESH_GAP_MS) {
    return;
  }
  if (refreshPromise) {
    return refreshPromise;
  }

  const accessStore = useAccessStore();
  const refreshToken = accessStore.refreshToken;
  if (!refreshToken) {
    return;
  }

  refreshPromise = (async () => {
    const response = await refreshTokenApi(refreshToken);
    const payload = resolveTokenPayload(response);
    if (payload?.accessToken) {
      accessStore.setAccessToken(payload.accessToken);
      lastRefreshAt = Date.now();
      unauthorizedAlertShown = false;
    }
    if (payload?.refreshToken) {
      accessStore.setRefreshToken(payload.refreshToken);
    }
  })()
    .catch((error) => {
      if (isUnauthorizedError(error)) {
        alertUnauthorizedOnce(error);
        return;
      }
      console.warn('[MES execution token keep-alive] refresh failed:', error);
    })
    .finally(() => {
      refreshPromise = null;
    });

  return refreshPromise;
}

export function useExecutionTokenKeepAlive(intervalMs = DEFAULT_KEEP_ALIVE_INTERVAL_MS) {
  let timer: ReturnType<typeof window.setInterval> | null = null;

  const refreshWhenActive = () => {
    if (document.visibilityState !== 'hidden') {
      void refreshExecutionToken();
    }
  };

  const refreshOnResume = () => {
    void refreshExecutionToken();
  };

  onMounted(() => {
    void refreshExecutionToken(true);
    timer = window.setInterval(refreshWhenActive, intervalMs);
    document.addEventListener('visibilitychange', refreshWhenActive);
    window.addEventListener('focus', refreshOnResume);
    window.addEventListener('online', refreshOnResume);
  });

  onBeforeUnmount(() => {
    if (timer) {
      window.clearInterval(timer);
      timer = null;
    }
    document.removeEventListener('visibilitychange', refreshWhenActive);
    window.removeEventListener('focus', refreshOnResume);
    window.removeEventListener('online', refreshOnResume);
  });
}

export function refreshExecutionTokenBeforeAction() {
  return refreshExecutionToken(true);
}
