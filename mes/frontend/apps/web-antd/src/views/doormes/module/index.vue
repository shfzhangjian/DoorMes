<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { IconifyIcon } from '@vben/icons';
import { FACTORY_MODULES } from '../factory-navigation';

defineOptions({ name: 'DoorMesModule' });
const route = useRoute();
const module = computed(() => FACTORY_MODULES.find((item) => route.path === `/factory/${item.key}`));
</script>

<template>
  <main v-if="module" class="factory-module">
    <header><IconifyIcon :icon="module.icon" /><h1>{{ module.title }}</h1><span>业务接入中</span></header>
    <p class="factory-module__purpose">{{ module.purpose }}</p>
    <section><h2>当前接入任务</h2><p>{{ module.next }}</p></section>
    <section><h2>本模块业务范围</h2><ul><li v-for="action in module.actions" :key="action">{{ action }}</li></ul></section>
    <aside>当前页仅明确业务入口和接入范围，不生成模拟订单、图纸、BOM 或审核结果。后续功能通过真实 API、数据和权限逐项开放。</aside>
  </main>
</template>

<style scoped>
.factory-module { margin: 24px; padding: 28px; border: 1px solid #dce4ec; border-radius: 6px; background: white; color: #1e293b; }
header { display: flex; align-items: center; gap: 14px; } header > svg { width: 28px; height: 28px; color: #28638c; }
h1 { margin: 0; font-size: 24px; font-weight: 600; } header span { margin-left: auto; padding: 5px 10px; background: #eef3f7; color: #567084; border-radius: 4px; font-size: 13px; }
.factory-module__purpose { margin: 22px 0; line-height: 1.8; color: #526779; }
section { padding: 22px 0; border-top: 1px solid #e2e8f0; } h2 { font-size: 16px; font-weight: 600; margin-bottom: 10px; }
ul { padding-left: 22px; line-height: 2; color: #526779; } aside { padding: 16px; background: #f5f8fb; font-size: 13px; line-height: 1.8; color: #64748b; }
</style>
