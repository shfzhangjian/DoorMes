<script lang="ts" setup>
import type { MesMaintTaskApi } from '#/api/mes/resource/device/maint-task';

import { nextTick, watch } from 'vue';

import { Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTaskItemListByTaskId } from '#/api/mes/resource/device/maint-task';

import { useItemGridColumns } from '../data';

defineOptions({ name: 'MaintTaskItemListGrid' });

const props = defineProps<{ taskId?: number }>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useItemGridColumns(),
    data: [],
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 300,
    rowConfig: { keyField: 'id', isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

// 安全加载数据，切断由于挂载时机导致的 loadData undefined 报错
watch(
  () => props.taskId,
  async (val) => {
    if (!val) {
      gridApi.setGridOptions({ data: [] });
      return;
    }
    await nextTick();
    const data = await getTaskItemListByTaskId(val);
    gridApi.setGridOptions({ data });
  },
  { immediate: true },
);
</script>

<template>
  <div class="qms-exception-subtable">
    <div class="qms-exception-subtable-title qms-exception-subtable-title--compact">
      <span>实际打卡项明细</span>
    </div>

    <div>
      <BaseGrid>
        <template #result="{ row }">
          <Tag v-if="row.result === 'NORMAL'" color="success">正常</Tag>
          <Tag v-else-if="row.result === 'ABNORMAL'" color="error">异常</Tag>
          <Tag v-else color="default">待检</Tag>
        </template>
      </BaseGrid>
    </div>
  </div>
</template>
