<script lang="ts" setup>
import { ref, h, watch, nextTick } from 'vue';
import { Button, Input, InputNumber } from 'ant-design-vue';
import { Plus } from "@vben/icons";
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { usePartGridColumns } from '../data';
import { getTaskPartListById } from '#/api/mes/resource/device/maint-task-exec';

defineOptions({ name: 'MesMaintTaskExecPartList' });

const props = defineProps<{ taskId?: string | number; disabled?: boolean }>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: usePartGridColumns(),
    data: [],
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 250,
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

const handleAdd = async () => {
  if (!props.disabled && gridApi.grid) {
    const { row } = await gridApi.grid.insertAt({
      id: Date.now().toString(),
      sort: gridApi.grid.getData().length + 1,
      quantity: 1,
      unit: '个'
    }, -1);
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
};

const handleDelete = async (row: any) => { if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row); };

defineExpose({
  getData: () => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData();
    const removes = gridApi.grid.getRemoveRecords();
    const inserts = gridApi.grid.getInsertRecords();
    return data.filter((row: any) => !removes.some((r: any) => r.id === row.id)).concat(inserts.map((row: any) => ({ ...row, id: undefined })));
  },
});

watch(() => props.taskId, async (val) => {
  if (!val) { gridApi.setGridOptions({ data: [] }); return; }
  await nextTick();
  gridApi.setGridOptions({ data: await getTaskPartListById(val) });
}, { immediate: true });
</script>

<template>
  <div class="qms-exception-subtable">
    <div class="qms-exception-subtable-title qms-exception-subtable-title--compact">
      <span>本次保养消耗备品备件</span>
      <Button v-if="!disabled" size="small" type="primary" ghost @click="handleAdd" :icon="h(Plus)">添加备件</Button>
    </div>
    <div>
      <BaseGrid>
        <template #sort="{ row }"><span class="text-slate-500">{{ row.sort }}</span></template>
        <template #partName="{ row }"><Input v-if="!disabled" v-model:value="row.partName" placeholder="如：润滑油" /><span v-else>{{ row.partName }}</span></template>
        <template #partCode="{ row }"><Input v-if="!disabled" v-model:value="row.partCode" placeholder="物料编码" /><span v-else>{{ row.partCode }}</span></template>
        <template #quantity="{ row }"><InputNumber v-if="!disabled" v-model:value="row.quantity" class="w-full" :min="1" /><span v-else>{{ row.quantity }}</span></template>
        <template #unit="{ row }"><Input v-if="!disabled" v-model:value="row.unit" placeholder="L/个" /><span v-else>{{ row.unit }}</span></template>
        <template #remark="{ row }"><Input v-if="!disabled" v-model:value="row.remark" placeholder="使用说明" /><span v-else>{{ row.remark || '-' }}</span></template>
        <template #actions="{ row }"><Button v-if="!disabled" size="small" type="link" danger @click="handleDelete(row)">移除</Button><span v-else>-</span></template>
      </BaseGrid>
    </div>
  </div>
</template>
