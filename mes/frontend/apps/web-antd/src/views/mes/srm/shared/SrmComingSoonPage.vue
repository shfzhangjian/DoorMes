<script lang="ts" setup>
import { Page } from '@vben/common-ui';

import ComingSoon from '#/views/_core/fallback/coming-soon.vue';

withDefaults(
  defineProps<{
    description?: string;
    title: string;
  }>(),
  {
    description: '菜单与组件路径已初始化，业务功能建设中。',
  },
);
</script>

<template>
  <Page auto-content-height :title="title">
    <div class="rounded-2xl bg-background p-4">
      <ComingSoon />
      <div class="mt-4 text-center text-sm text-muted-foreground">
        {{ description }}
      </div>
    </div>
  </Page>
</template>
