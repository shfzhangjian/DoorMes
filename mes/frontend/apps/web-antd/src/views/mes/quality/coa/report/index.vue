<script setup lang="ts">
import { Page } from '@vben/common-ui';

import CoaReportWorkbench from '../components/CoaReportWorkbench.vue';

defineOptions({ name: 'MesQualityCoaReport' });
</script>

<template>
  <Page auto-content-height>
    <CoaReportWorkbench />
  </Page>
</template>
