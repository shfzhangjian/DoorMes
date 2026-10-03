<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, onUpdated, ref } from 'vue';

// Ant Table's scroll.y is only a maximum. Reserve the actual body height so a
// short/empty page keeps its horizontal scrollbar at the allocated area's foot.
const host = ref<HTMLElement | null>(null);
const bodyHeight = ref(280);
let observer: ResizeObserver | undefined;
let observedHeader: Element | null = null;

function measure() {
  const element = host.value;
  if (!element || element.clientHeight <= 0) return;
  const header = element.querySelector<HTMLElement>('.ant-table-header');
  if (header !== observedHeader) {
    if (observedHeader) observer?.unobserve(observedHeader);
    observedHeader = header;
    if (header) observer?.observe(header);
  }
  const container = element.querySelector<HTMLElement>('.ant-table-container');
  const border = container ? getComputedStyle(container) : null;
  const borderHeight = border ? (Number.parseFloat(border.borderTopWidth) || 0) + (Number.parseFloat(border.borderBottomWidth) || 0) : 0;
  bodyHeight.value = Math.max(0, Math.floor(element.clientHeight - (header?.getBoundingClientRect().height || 55) - borderHeight));
}

onMounted(async () => {
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(measure);
    if (host.value) observer.observe(host.value);
  }
  window.addEventListener('resize', measure);
  await nextTick();
  measure();
});
onUpdated(measure);
onBeforeUnmount(() => {
  observer?.disconnect();
  window.removeEventListener('resize', measure);
});
</script>

<template>
  <section ref="host" class="doormes-table-viewport" :style="{ '--doormes-table-body-height': `${bodyHeight}px` }">
    <slot :scroll-y="bodyHeight" />
  </section>
</template>

<style scoped>
.doormes-table-viewport { flex: 1; min-width: 0; min-height: 0; overflow: hidden; }
.doormes-table-viewport :deep(.ant-table-wrapper) { width: 100%; }
.doormes-table-viewport :deep(.ant-table-cell) { word-break: normal; }
.doormes-table-viewport :deep(.ant-table-body) {
  height: var(--doormes-table-body-height);
  /* Preserve real overflow; never force a bar when all columns/rows fit. */
  overflow: auto !important;
}
</style>
