<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';

import { onMounted, ref } from 'vue';

import { EchartsUI, useEcharts } from '@vben/plugins/echarts';

const chartRef = ref<EchartsUIType>();
const { renderEcharts } = useEcharts(chartRef);

onMounted(() => {
  renderEcharts({
    tooltip: {},
    legend: {
      data: ['造型车间', '浇注车间'],
      bottom: 0,
    },
    radar: {
      radius: '60%',
      center: ['50%', '45%'],
      indicator: [
        { name: '产量达成率', max: 100 },
        { name: '设备OEE', max: 100 },
        { name: '良品率', max: 100 },
        { name: '人员出勤', max: 100 },
        { name: '安全合规', max: 100 },
        { name: '物料消耗', max: 100 },
      ],
    },
    series: [
      {
        name: '车间能力对比',
        type: 'radar',
        data: [
          {
            value: [95, 80, 98, 90, 100, 85],
            name: '造型车间',
            itemStyle: { color: '#b6a2de' },
            areaStyle: { opacity: 0.3 },
          },
          {
            value: [88, 85, 92, 95, 98, 90],
            name: '浇注车间',
            itemStyle: { color: '#5ab1ef' },
            areaStyle: { opacity: 0.3 },
          },
        ],
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
