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
        name: '设备状态',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: { show: false, position: 'center' },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold',
          },
        },
        data: [
          { value: 45, name: '运行中', itemStyle: { color: '#52c41a' } },
          { value: 8, name: '待机空闲', itemStyle: { color: '#faad14' } },
          { value: 5, name: '故障维修', itemStyle: { color: '#ff4d4f' } },
          { value: 2, name: '停机保养', itemStyle: { color: '#d9d9d9' } },
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
