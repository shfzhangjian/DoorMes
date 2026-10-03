<script lang="ts" setup>
import type { MesMaintTaskExecApi } from '#/api/mes/resource/device/maint-task-exec';

import { nextTick, watch } from 'vue';

import { Checkbox, Input } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTaskItemListById } from '#/api/mes/resource/device/maint-task-exec';

import { useExecItemGridColumns } from '../data';

defineOptions({ name: 'MesMaintTaskExecStandardItemList' });

const props = defineProps<{ disabled?: boolean; taskId?: string | number }>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: useExecItemGridColumns(),
    data: [],
    height: 300,
    keepSource: true,
    pagerConfig: { enabled: false },
    rowConfig: { isCurrent: true, isHover: true, keyField: 'id' },
    showOverflow: true,
    toolbarConfig: { enabled: false },
  },
});

function setNormal(row: MesMaintTaskExecApi.TaskItem, checked: boolean) {
  row.result = checked ? 'NORMAL' : 'ABNORMAL';
}

function handleNormalChange(row: MesMaintTaskExecApi.TaskItem, event: any) {
  setNormal(row, Boolean(event?.target?.checked));
}

function normalizeRows(rows: MesMaintTaskExecApi.TaskItem[]) {
  return rows.map((row, index) => ({
    ...row,
    result: props.disabled ? row.result : row.result === 'NORMAL' ? 'NORMAL' : row.result || 'PENDING',
    sort: row.sort || index + 1,
  }));
}

function getRows() {
  return (gridApi.grid?.getData() || []) as MesMaintTaskExecApi.TaskItem[];
}

defineExpose({
  getData: () => getRows().map((row) => ({
    ...row,
    result: row.result === 'NORMAL' ? 'NORMAL' : 'ABNORMAL',
  })),
  hasAbnormal: () => getRows().some((row) => row.result !== 'NORMAL'),
});

watch(
  () => props.taskId,
  async (val) => {
    if (!val) {
      gridApi.setGridOptions({ data: [] });
      return;
    }
    await nextTick();
    gridApi.setGridOptions({ data: normalizeRows(await getTaskItemListById(val)) });
  },
  { immediate: true },
);
</script>

<template>
  <div class="qms-exception-subtable">
    <div class="qms-exception-subtable-title qms-exception-subtable-title--compact">
      <span>保养标准执行明细</span>
    </div>
    <div>
      <BaseGrid>
        <template #normal="{ row }">
          <Checkbox
            :checked="row.result === 'NORMAL'"
            :disabled="disabled"
            @change="handleNormalChange(row, $event)"
          />
        </template>
        <template #remark="{ row }">
          <Input v-if="!disabled" v-model:value="row.remark" placeholder="填写检查说明或异常备注" />
          <span v-else>{{ row.remark || '-' }}</span>
        </template>
      </BaseGrid>
    </div>
  </div>
</template>
