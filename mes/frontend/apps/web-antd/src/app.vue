<script lang="ts" setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

import { useAntdDesignTokens } from '@vben/hooks';
import { preferences, usePreferences } from '@vben/preferences';

import { App, Button, ConfigProvider, theme } from 'ant-design-vue';

import { antdLocale } from '#/locales';
import { retryRouteLoad, routeLoadFailure } from '#/router/load-failure';

defineOptions({ name: 'App' });

const { isDark } = usePreferences();
const { tokens } = useAntdDesignTokens();
const router = useRouter();

const tokenTheme = computed(() => {
  const algorithm = isDark.value
    ? [theme.darkAlgorithm]
    : [theme.defaultAlgorithm];

  // antd 紧凑模式算法
  if (preferences.app.compact) {
    algorithm.push(theme.compactAlgorithm);
  }

  return {
    algorithm,
    token: tokens,
  };
});
</script>

<template>
  <ConfigProvider :locale="antdLocale" :theme="tokenTheme">
    <App>
      <section v-if="routeLoadFailure.path" class="route-load-failure" role="alert">
        <h1>页面暂时无法加载</h1>
        <p>无法读取账号权限或页面数据，请确认服务连接后重试。</p>
        <Button type="primary" :loading="routeLoadFailure.retrying" @click="retryRouteLoad(router)">重新加载</Button>
      </section>
      <RouterView v-else />
    </App>
  </ConfigProvider>
</template>

<style scoped>
.route-load-failure{min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:16px;padding:24px;color:#263849;background:#f3f6f9;text-align:center;}
.route-load-failure h1{margin:0;font-size:22px;font-weight:600;}.route-load-failure p{margin:0;color:#66788a;}
</style>
