<script lang="ts" setup>
import type { MesFaultRepairApi } from '#/api/mes/resource/device/fault-repair';

import { nextTick, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, InputNumber } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getRepairPartListById } from '#/api/mes/resource/device/fault-repair';

import { usePartGridColumns } from '../data';

defineOptions({ name: 'FaultRepairPartListGrid' });

const props = defineProps<{ disabled?: boolean; orderId?: number | string }>();

const [BaseGrid, gridApi] = useVbenVxeGrid<MesFaultRepairApi.RepairPart>({
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
    const { row } = await gridApi.grid.insertAt(
      {
        id: Date.now().toString(), // 保证ID绝对唯一
        sort: gridApi.grid.getData().length + 1,
        quantity: 1,
        unit: '件',
      },
      -1,
    );
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
};

const handleDelete = async (row: any) => {
  if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row);
};

defineExpose({
  getData: () => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData() as MesFaultRepairApi.RepairPart[];
    const removes =
      gridApi.grid.getRemoveRecords() as MesFaultRepairApi.RepairPart[];
    const inserts =
      gridApi.grid.getInsertRecords() as MesFaultRepairApi.RepairPart[];
    return [
      ...data.filter((row) => !removes.some((r) => r.id === row.id)),
      ...inserts.map((row) => ({ ...row, id: undefined })),
    ];
  },
});

watch(
  () => props.orderId,
  async (val) => {
    if (!val) {
      gridApi.setGridOptions({ data: [] });
      return;
    }
    await nextTick();
    gridApi.setGridOptions({ data: await getRepairPartListById(val) });
  },
  { immediate: true },
);
</script>

<template>
  <div class="flex h-full flex-col rounded-lg bg-white">
    <div
      class="flex items-center justify-between border-b border-blue-100 px-4 py-2"
    >
      <div class="flex items-center gap-2">
        <IconifyIcon icon="lucide:wrench" class="text-blue-500" />
        <span class="text-sm font-bold text-slate-600">实际消耗备件申报</span>
      </div>
      <Button
        v-if="!disabled"
        size="small"
        type="primary"
        ghost
        @click="handleAdd"
      >
        <template #icon>
          <IconifyIcon icon="lucide:plus" />
        </template>
        领用耗材
      </Button>
    </div>
    <div class="p-2">
      <BaseGrid>
        <template #sort="{ row }">
          <span class="text-slate-500">{{ row.sort }}</span>
        </template>
        <template #partName="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.partName"
            placeholder="如：接触器"
          /><span v-else>{{ row.partName }}</span>
        </template>
        <template #partCode="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.partCode"
            placeholder="物料编码"
          /><span v-else class="font-mono text-slate-400">{{
            row.partCode
          }}</span>
        </template>
        <template #quantity="{ row }">
          <InputNumber
            v-if="!disabled"
            v-model:value="row.quantity"
            class="w-full"
            :min="1"
          /><span v-else class="font-bold">{{ row.quantity }}</span>
        </template>
        <template #unit="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.unit"
            placeholder="个"
          /><span v-else>{{ row.unit }}</span>
        </template>
        <template #remark="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.remark"
            placeholder="备注"
          /><span v-else>{{ row.remark || '-' }}</span>
        </template>
        <template #actions="{ row }">
          <Button
            v-if="!disabled"
            size="small"
            type="link"
            danger
            @click="handleDelete(row)"
          >
            撤销
          </Button>
          <span v-else>-</span>
        </template>
      </BaseGrid>
    </div>
  </div>
</template>
