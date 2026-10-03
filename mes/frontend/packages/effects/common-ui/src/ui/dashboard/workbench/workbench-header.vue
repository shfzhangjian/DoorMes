<script lang="ts" setup>
import { VbenAvatar } from '@vben-core/shadcn-ui';

interface Props {
  avatar?: string;
}

defineOptions({
  name: 'WorkbenchHeader',
});

withDefaults(defineProps<Props>(), {
  avatar: '',
});
</script>
<template>
  <div class="card-box p-4 py-6 lg:flex">
    <VbenAvatar :src="avatar" class="size-20" />
    <div
      v-if="$slots.title || $slots.description"
      class="flex flex-col justify-center md:ml-6 md:mt-0"
    >
      <h1 v-if="$slots.title" class="text-md font-semibold md:text-xl">
        <slot name="title"></slot>
      </h1>
      <span v-if="$slots.description" class="text-foreground/80 mt-1">
        <slot name="description"></slot>
      </span>
    </div>

    <!-- MES 关键指标展示区域 -->
    <div class="mt-4 flex flex-1 justify-end md:mt-0">
      <div class="flex flex-col justify-center text-right">
        <span class="text-foreground/80"> 综合良率 </span>
        <!-- 绿色代表健康，模拟数据 98.5% -->
        <span class="text-2xl font-bold text-green-600">98.5%</span>
      </div>

      <div class="mx-12 flex flex-col justify-center text-right md:mx-16">
        <span class="text-foreground/80"> 今日产量 (件) </span>
        <span class="text-2xl font-bold">3,450</span>
      </div>

      <div class="mr-4 flex flex-col justify-center text-right md:mr-10">
        <span class="text-foreground/80"> 设备 OEE </span>
        <!-- 稍微低一点的数值用不同颜色 -->
        <span class="text-2xl font-bold text-blue-600">87.2%</span>
      </div>
    </div>
  </div>
</template>
