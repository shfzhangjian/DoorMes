<script lang="ts" setup>
import type { MesBatchingTaskApi } from '#/api/mes/execution/batching-task';

import { ref, h, watch, nextTick } from 'vue';
import { Button, Input, InputNumber, Tag } from 'ant-design-vue';
import { Plus } from "@vben/icons";

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { useItemGridColumns } from '../data';
import { getTaskItemListByTaskId } from '#/api/mes/execution/batching-task';

defineOptions({ name: 'BatchingTaskItemListGrid' });

const props = defineProps<{ taskId?: number; disabled?: boolean }>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useItemGridColumns(),
    data: [],
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 350, // 严格定高，内部滚动，防止 Modal 高度抽搐
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true }, // 支持光标
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

const handleAdd = async () => {
  if (!props.disabled && gridApi.grid) {
    const defaultData: MesBatchingTaskApi.TaskItem = {
      id: Date.now().toString(),
      sort: gridApi.grid.getData().length + 1,
      actualQty: 0,
      standardQty: 0,
      result: 'PENDING',
      unit: 'kg'
    };
    // 尾部插入
    const { row } = await gridApi.grid.insertAt(defaultData, -1);

    // 【光标跟随】：自动滚动并高亮选中新增行
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
};

const handleDelete = async (row: MesBatchingTaskApi.TaskItem) => {
  if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row);
};

defineExpose({
  getData: (): MesBatchingTaskApi.TaskItem[] => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData() as MesBatchingTaskApi.TaskItem[];
    const removeRecords = gridApi.grid.getRemoveRecords() as MesBatchingTaskApi.TaskItem[];
    const insertRecords = gridApi.grid.getInsertRecords() as MesBatchingTaskApi.TaskItem[];
    return data.filter(row => !removeRecords.some(r => r.id === row.id)).concat(insertRecords.map(row => ({ ...row, id: undefined })));
  },
});

// 安全加载数据，切断由于挂载时机导致的报错
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
  { immediate: true }
);

function checkResult(row: MesBatchingTaskApi.TaskItem) {
  // 简易逻辑模拟：如果有实际重量就算 PASS
  if ((row.actualQty || 0) > 0) row.result = 'PASS';
  else row.result = 'PENDING';
}
</script>

<template>
  <div class="flex flex-col h-full bg-slate-50">
    <div class="flex items-center justify-between px-3 py-2 border-b border-slate-200 bg-white">
      <div class="flex items-center gap-2">
        <div class="w-1 h-3.5 bg-blue-600 rounded-sm"></div>
        <span class="font-bold text-slate-700 text-sm">实际投料执行明细</span>
      </div>
      <Button v-if="!disabled" size="small" type="primary" @click="handleAdd" :icon="h(Plus)">
        补录扫码记录
      </Button>
    </div>

    <div class="p-2 bg-white">
      <BaseGrid>
        <template #materialSlot="{ row }">
          <Input v-if="!disabled" v-model:value="row.materialName" placeholder="输入物料名" />
          <span v-else>{{ row.materialName }}</span>
        </template>
        <template #sort="{ row }"><span class="text-slate-400">{{ row.sort }}</span></template>

        <template #barcode="{ row }">
          <Input v-if="!disabled" v-model:value="row.barcode" placeholder="扫描或输入条码" />
          <span v-else>{{ row.barcode || '-' }}</span>
        </template>
        <template #actualQty="{ row }">
          <InputNumber v-if="!disabled" v-model:value="row.actualQty" class="w-full" :min="0" @change="checkResult(row)" />
          <span v-else class="font-bold text-blue-600">{{ row.actualQty }}</span>
        </template>

        <template #result="{ row }">
          <Tag v-if="row.result === 'PASS'" color="success">核对通过</Tag>
          <Tag v-else-if="row.result === 'FAIL'" color="error">重量超差</Tag>
          <Tag v-else color="default">待称量</Tag>
        </template>

        <template #actions="{ row }">
          <Button v-if="!disabled" size="small" type="link" danger @click="handleDelete(row)">移除</Button>
          <span v-else>-</span>
        </template>
      </BaseGrid>
    </div>
  </div>
</template>
