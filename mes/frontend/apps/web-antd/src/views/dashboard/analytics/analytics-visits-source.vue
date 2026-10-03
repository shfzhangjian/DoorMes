<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import { onMounted, ref } from 'vue';

import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

onMounted(() => {
  renderEcharts({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%' },
    series: [
      {
        name: '缺陷类型',
        type: 'pie',
        radius: '70%',
        center: ['50%', '45%'],
        data: [
          { value: 1048, name: '气孔 (Porosity)', itemStyle: { color: '#5ab1ef' } },
          { value: 735, name: '砂眼 (Sand Hole)', itemStyle: { color: '#b6a2de' } },
          { value: 580, name: '缩孔 (Shrinkage)', itemStyle: { color: '#67e0e3' } },
          { value: 484, name: '冷隔 (Cold Shut)', itemStyle: { color: '#2ec7c9' } },
          { value: 300, name: '裂纹 (Crack)', itemStyle: { color: '#ffb980' } },
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  });
});
</script>

<template>
  <div class="h-[300px] w-full">
    <EchartsUI ref="chartRef" />
  </div>
</template>
