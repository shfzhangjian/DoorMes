<script lang="ts" setup>
import { Page } from '@vben/common-ui';

import ComingSoon from '#/views/_core/fallback/coming-soon.vue';
</script>

<template>
  <Page auto-content-height title="盘点作业">
    <div class="rounded-2xl bg-background p-4">
      <ComingSoon />
      <div class="mt-4 text-center text-sm text-muted-foreground">
        盘点作业页面的菜单与路由已完成映射，后续继续补业务实现。
      </div>
    </div>
  </Page>
</template>
