<!-- 源码完整路径: src/views/dashboard/analytics/analysis-overview.vue -->
<script lang="ts" setup>
import type { AnalysisOverviewItem } from './typing';

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  VbenIcon,
} from '@vben-core/shadcn-ui';

defineOptions({
  name: 'AnalysisOverview',
});

// MES 核心指标模拟数据
const items: AnalysisOverviewItem[] = [
  {
    icon: 'icon-park-outline:factory-building', // 工厂图标
    title: '今日总产量 (件)',
    total: 3450,
    value: 120, // 较昨日增长量
    valueTitle: '较昨日',
  },
  {
    icon: 'icon-park-outline:chart-line', // 趋势图标
    title: '综合良率 (FY)',
    total: '98.5%',
    value: -0.2, // 较昨日下降
    valueTitle: '较昨日',
  },
  {
    icon: 'icon-park-outline:cpu', // 设备图标
    title: '设备综合效率 (OEE)',
    total: '87.2%',
    value: 1.5, // 较昨日提升
    valueTitle: '较昨日',
  },
  {
    icon: 'icon-park-outline:check-one', // 待办图标
    title: '待判定批次 (QA)',
    total: 12,
    value: 3, // 新增待办
    valueTitle: '新增',
  },
];
</script>

<template>
  <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
    <template v-for="item in items" :key="item.title">
      <Card :title="item.title">
        <CardHeader>
          <CardTitle class="text-md text-foreground/80 font-medium">
            {{ item.title }}
          </CardTitle>
          <VbenIcon :icon="item.icon" class="text-foreground/80 size-4" />
        </CardHeader>

        <CardContent>
          <div class="text-2xl font-bold">{{ item.total }}</div>
          <p class="text-foreground/80 text-xs">
            {{ item.valueTitle }}
            <span
              v-if="item.value > 0"
              class="text-green-600 dark:text-green-400"
            >
              +{{ item.value }}%
            </span>
            <span v-else class="text-red-600 dark:text-red-400">
              {{ item.value }}%
            </span>
          </p>
        </CardContent>
      </Card>
    </template>
  </div>
</template>
