<script setup lang="ts">
import { Page } from '@vben/common-ui';

import CoaReportWorkbench from '../components/CoaReportWorkbench.vue';

defineOptions({ name: 'MesQualityCoaReview' });
</script>

<template>
  <Page auto-content-height>
    <CoaReportWorkbench review-mode />
  </Page>
</template>
