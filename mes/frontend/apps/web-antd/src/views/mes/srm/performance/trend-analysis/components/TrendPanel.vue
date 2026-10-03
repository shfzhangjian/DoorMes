<script lang="ts" setup>
import type { SrmQuarterlyPerformanceApi } from '#/api/mes/srm/assessment/quarterly-performance';

import { Empty } from 'ant-design-vue';

import TrendChartCard from './TrendChartCard.vue';

defineProps<{
  emptyDescription?: string;
  panels: SrmQuarterlyPerformanceApi.TrendPanel[];
}>();
</script>

<template>
  <div class="srm-trend-panel">
    <template v-if="panels.length > 0">
      <TrendChartCard
        v-for="panel in panels"
        :key="panel.indicatorCode"
        :panel="panel"
      />
    </template>
    <div v-else class="srm-trend-panel__empty">
      <Empty :description="emptyDescription || '请选择左侧指标查看趋势'" />
    </div>
  </div>
</template>

<style scoped>
.srm-trend-panel {
  min-width: 0;
}

.srm-trend-panel__empty {
  display: flex;
  min-height: 360px;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #fff;
}
</style>
