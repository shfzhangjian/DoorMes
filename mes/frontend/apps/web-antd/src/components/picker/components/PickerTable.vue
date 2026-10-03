<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { computed, watch } from 'vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';

import type { PickerEntityConfig, PickerOption } from '../types';

const props = withDefaults(
  defineProps<{
    config: PickerEntityConfig;
    filters: Record<string, any>;
    tableTitle?: string;
  }>(),
  { tableTitle: '' },
);

const emit = defineEmits<{ pick: [PickerOption] }>();

function handlePick(row: any) {
  emit('pick', props.config.buildOption(row));
}

const vxeColumns = computed(() => [
  ...props.config.columns.map((col) => ({
    field: col.field,
    title: col.title,
    ...(col.width ? { width: col.width } : { minWidth: col.minWidth ?? 120 }),
    align: col.align ?? 'left',
    fixed: col.fixed,
    showOverflow: 'tooltip',
    formatter: col.formatter
      ? ({ cellValue, row }: any) => col.formatter!(cellValue, row)
      : undefined,
  })),
  {
    title: '操作',
    width: 90,
    fixed: 'right' as const,
    align: 'center' as const,
    slots: { default: 'actions' },
  },
]);

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: vxeColumns.value as any,
    height: 'auto',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true },
    pagerConfig: {
      enabled: true,
      pageSize: 10,
      pageSizes: [10, 20, 50],
      layouts: ['PrevPage', 'JumpNumber', 'NextPage', 'FullJump', 'Sizes', 'Total'],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          return await props.config.fetchPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            filters: { ...props.filters },
          });
        },
      },
    },
  } as VxeTableGridOptions,
  gridEvents: {
    cellDblclick: ({ row }) => handlePick(row),
  },
});

watch(
  () => vxeColumns.value,
  (cols) => gridApi.setGridOptions({ columns: cols as any }),
  { immediate: true },
);

function reload() {
  gridApi.query();
}

defineExpose({ reload });
</script>

<template>
  <div class="hc-picker-table">
    <Grid :table-title="tableTitle">
      <template #actions="{ row }">
        <a class="vben-link" @click="handlePick(row)">选择</a>
      </template>
    </Grid>
  </div>
</template>

<style scoped>
.hc-picker-table {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.hc-picker-table :deep(.vben-grid) {
  height: 100%;
}
</style>
