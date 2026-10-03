<script lang="ts" setup>
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';

import { h, nextTick, watch } from 'vue';

import { Plus, Trash2 } from '@vben/icons';

import { Button, Input, InputNumber, Tooltip } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDevicePartListById } from '#/api/mes/resource/device/ledger';

import { useItemGridColumns } from '../data';

defineOptions({ name: 'DeviceLedgerItemList' });

const props = defineProps<{ deviceId?: number; disabled?: boolean }>();

const [BaseGrid, gridApi] = useVbenVxeGrid<MesDeviceLedgerApi.DevicePart>({
  gridOptions: {
    columns: useItemGridColumns(),
    data: [],
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 300, // 高度写死，绝不让 Modal 高度撑满变形
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true }, // 支持行光标
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

const handleAdd = async () => {
  if (!props.disabled && gridApi.grid) {
    const defaultData: MesDeviceLedgerApi.DevicePart = {
      id: Date.now(),
      sort: gridApi.grid.getData().length + 1,
      partCode: '',
      partName: '',
      quantity: 1,
      replaceCycle: 360,
    };
    const { row } = await gridApi.grid.insertAt(defaultData, -1);

    // 光标自动跟踪到新增行
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
};

const handleDelete = async (row: MesDeviceLedgerApi.DevicePart) => {
  if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row);
};

defineExpose({
  getData: (): MesDeviceLedgerApi.DevicePart[] => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData() as MesDeviceLedgerApi.DevicePart[];
    const removeRecords =
      gridApi.grid.getRemoveRecords() as MesDeviceLedgerApi.DevicePart[];
    const insertRecords =
      gridApi.grid.getInsertRecords() as MesDeviceLedgerApi.DevicePart[];
    return [
      ...data.filter((row) => !removeRecords.some((r) => r.id === row.id)),
      ...insertRecords.map((row) => ({ ...row, id: undefined })),
    ];
  },
});

// 安全加载数据，切断无挂载异常
watch(
  () => props.deviceId,
  async (val) => {
    if (!val) {
      gridApi.setGridOptions({ data: [] });
      return;
    }
    await nextTick();
    const data = await getDevicePartListById(val);
    gridApi.setGridOptions({ data });
  },
  { immediate: true },
);
</script>

<template>
  <div class="flex h-full flex-col bg-slate-50">
    <div
      class="flex items-center justify-between border-b border-slate-200 bg-white px-3 py-2"
    >
      <div class="flex items-center gap-2">
        <div class="h-3.5 w-1 rounded-sm bg-blue-600"></div>
        <span class="text-sm font-bold text-slate-700">
          核心易损件 / 备件清单
        </span>
      </div>
      <Button
        v-if="!disabled"
        size="small"
        type="primary"
        @click="handleAdd"
        :icon="h(Plus)"
      >
        添加备件
      </Button>
    </div>

    <div class="bg-white p-2">
      <BaseGrid>
        <template #sort="{ row }">
          <span class="text-slate-500">{{ row.sort }}</span>
        </template>
        <template #partCode="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.partCode"
            placeholder="备件编码"
          />
          <span v-else>{{ row.partCode }}</span>
        </template>
        <template #partName="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.partName"
            placeholder="备件名称"
          />
          <span v-else class="font-bold text-slate-700">{{
            row.partName
          }}</span>
        </template>
        <template #spec="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.spec"
            placeholder="型号参数"
          />
          <span v-else>{{ row.spec }}</span>
        </template>
        <template #quantity="{ row }">
          <InputNumber
            v-if="!disabled"
            v-model:value="row.quantity"
            class="w-full"
            :min="1"
          />
          <span v-else>{{ row.quantity }}</span>
        </template>
        <template #replaceCycle="{ row }">
          <InputNumber
            v-if="!disabled"
            v-model:value="row.replaceCycle"
            class="w-full"
            :min="0"
          />
          <span v-else>{{ row.replaceCycle }}</span>
        </template>
        <template #remark="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.remark"
            placeholder="备注"
          />
          <span v-else>{{ row.remark || '-' }}</span>
        </template>

        <template #actions="{ row }">
          <div class="flex justify-center">
            <Tooltip v-if="!disabled" title="移除">
              <Button
                aria-label="移除备件"
                size="small"
                type="link"
                danger
                @click="handleDelete(row)"
              >
                <template #icon>
                  <Trash2 class="h-4 w-4" />
                </template>
              </Button>
            </Tooltip>
            <span v-else>-</span>
          </div>
        </template>
      </BaseGrid>
    </div>
  </div>
</template>
