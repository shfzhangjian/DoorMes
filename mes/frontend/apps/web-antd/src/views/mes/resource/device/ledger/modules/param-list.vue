<script lang="ts" setup>
import type { MesDeviceLedgerApi } from '#/api/mes/resource/device/ledger';

import { h, nextTick, watch } from 'vue';

import { Plus, Trash2 } from '@vben/icons';

import { Button, Input, Tooltip } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDeviceParamListById } from '#/api/mes/resource/device/ledger';

import { useParamGridColumns } from '../data';

defineOptions({ name: 'DeviceLedgerParamList' });

const props = defineProps<{ deviceId?: number; disabled?: boolean }>();

const [BaseGrid, gridApi] = useVbenVxeGrid<MesDeviceLedgerApi.DeviceParam>({
  gridOptions: {
    columns: useParamGridColumns(),
    data: [],
    border: true,
    showOverflow: true,
    keepSource: true,
    // [规范] 固定高度，防止多个 Panel 导致弹窗过长
    height: 250,
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

const handleAdd = async () => {
  if (!props.disabled && gridApi.grid) {
    const defaultData: MesDeviceLedgerApi.DeviceParam = {
      id: Date.now(),
      sort: gridApi.grid.getData().length + 1,
      paramName: '',
      paramValue: '',
      unit: '',
    };
    const { row } = await gridApi.grid.insertAt(defaultData, -1);
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
};

const handleDelete = async (row: MesDeviceLedgerApi.DeviceParam) => {
  if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row);
};

defineExpose({
  getData: (): MesDeviceLedgerApi.DeviceParam[] => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData() as MesDeviceLedgerApi.DeviceParam[];
    const removeRecords =
      gridApi.grid.getRemoveRecords() as MesDeviceLedgerApi.DeviceParam[];
    const insertRecords =
      gridApi.grid.getInsertRecords() as MesDeviceLedgerApi.DeviceParam[];
    return [
      ...data.filter((row) => !removeRecords.some((r) => r.id === row.id)),
      ...insertRecords.map((row) => ({ ...row, id: undefined })),
    ];
  },
});

watch(
  () => props.deviceId,
  async (val) => {
    if (!val) {
      gridApi.setGridOptions({ data: [] });
      return;
    }
    await nextTick();
    const data = await getDeviceParamListById(val);
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
        <span class="text-sm font-bold text-slate-700">技术与规格参数</span>
      </div>
      <Button
        v-if="!disabled"
        size="small"
        type="primary"
        @click="handleAdd"
        :icon="h(Plus)"
      >
        添加参数
      </Button>
    </div>

    <div class="bg-white p-2">
      <BaseGrid>
        <template #sort="{ row }">
          <span class="text-slate-500">{{ row.sort }}</span>
        </template>
        <template #paramName="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.paramName"
            placeholder="如：额定功率"
          />
          <span v-else class="font-bold text-slate-700">{{
            row.paramName
          }}</span>
        </template>
        <template #paramValue="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.paramValue"
            placeholder="如：15"
          />
          <span v-else>{{ row.paramValue }}</span>
        </template>
        <template #unit="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.unit"
            placeholder="如：kW"
          />
          <span v-else>{{ row.unit }}</span>
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
                aria-label="移除参数"
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
