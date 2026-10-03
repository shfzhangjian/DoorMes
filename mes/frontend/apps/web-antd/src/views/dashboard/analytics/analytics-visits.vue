<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import { onMounted, ref } from 'vue';

import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

onMounted(() => {
  renderEcharts({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
    },
    grid: {
      left: '1%',
      right: '1%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: ['熔炼A', '熔炼B', '造型一线', '造型二线', '浇注A', '浇注B', '落砂', '清理', '热处理', '机加', '包装'],
      axisLabel: { interval: 0, rotate: 30 },
    },
    yAxis: {
      type: 'value',
      name: '产量 (件)',
    },
    series: [
      {
        name: '今日产出',
        type: 'bar',
        barWidth: '60%',
        itemStyle: { color: '#4f69fd', borderRadius: [4, 4, 0, 0] },
        data: [1200, 1150, 2200, 2100, 2150, 2080, 2000, 1800, 1600, 1500, 1200],
      },
    ],
  });
});
</script>

<template>
  <div class="h-[320px] w-full">
    <EchartsUI ref="chartRef" />
  </div>
</template>
