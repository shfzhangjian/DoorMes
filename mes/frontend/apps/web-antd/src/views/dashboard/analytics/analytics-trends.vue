<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import { onMounted, ref } from 'vue';

import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

onMounted(() => {
  renderEcharts({
    grid: {
      bottom: 20,
      containLabel: true,
      left: '1%',
      right: '1%',
      top: '5%',
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b0}<br />{a0}: {c0}%<br />{a1}: {c1}%',
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00'],
    },
    yAxis: {
      type: 'value',
      min: 90,
      max: 100,
      name: '良率 (%)',
    },
    series: [
      {
        name: '计划良率',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#5ab1ef' },
        areaStyle: { opacity: 0.3 },
        data: [98, 98, 98, 98, 98, 98, 98, 98, 98, 98],
      },
      {
        name: '实际良率',
        type: 'line',
        smooth: true,
        itemStyle: { color: '#019680' },
        areaStyle: { opacity: 0.3 },
        data: [96.5, 97.2, 98.5, 99.1, 98.0, 95.5, 97.8, 98.2, 99.0, 98.5],
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
