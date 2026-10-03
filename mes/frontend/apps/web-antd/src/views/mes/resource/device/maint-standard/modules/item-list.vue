<script lang="ts" setup>
import type { MesMaintStandardApi } from '#/api/mes/resource/device/maint-standard';

import { h, nextTick, watch } from 'vue';

import { Plus } from '@vben/icons';

import { Button, Input, InputNumber } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getStandardItemListById } from '#/api/mes/resource/device/maint-standard';

import { useItemGridColumns } from '../data';

defineOptions({ name: 'MaintStandardItemList' });

const props = defineProps<{ disabled?: boolean; standardId?: number }>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useItemGridColumns(),
    data: [],
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 350, // 核心定高，内置滚动防止弹窗撑破
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true }, // 开启行光标支持
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

const handleAdd = async () => {
  if (!props.disabled && gridApi.grid) {
    const defaultData: MesMaintStandardApi.StandardItem = {
      id: Date.now().toString(),
      sort: gridApi.grid.getData().length + 1,
      itemGroup: '',
      itemName: '',
      method: '目视',
      requirement: '',
      frequency: '一个月',
      tool: '-',
    };
    const { row } = await gridApi.grid.insertAt(defaultData, -1);

    // 光标跟随并滚动至底部
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
};

const handleDelete = async (row: MesMaintStandardApi.StandardItem) => {
  if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row);
};

defineExpose({
  getData: (): MesMaintStandardApi.StandardItem[] => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData() as MesMaintStandardApi.StandardItem[];
    const removeRecords =
      gridApi.grid.getRemoveRecords() as MesMaintStandardApi.StandardItem[];
    const insertRecords =
      gridApi.grid.getInsertRecords() as MesMaintStandardApi.StandardItem[];
    return [
      ...data.filter((row) => !removeRecords.some((r) => r.id === row.id)),
      ...insertRecords.map((row) => ({ ...row, id: undefined })),
    ];
  },
});

// 安全拦截机制
watch(
  () => props.standardId,
  async (val) => {
    if (!val) {
      gridApi.setGridOptions({ data: [] });
      return;
    }
    await nextTick();
    const data = await getStandardItemListById(val);
    gridApi.setGridOptions({ data });
  },
  { immediate: true },
);
</script>

<template>
  <div class="qms-exception-subtable">
    <div
      class="qms-exception-subtable-title qms-exception-subtable-title--compact"
    >
      <span>检查与清扫要求明细</span>
      <Button
        v-if="!disabled"
        size="small"
        type="primary"
        @click="handleAdd"
        :icon="h(Plus)"
      >
        增加检查项
      </Button>
    </div>

    <div>
      <BaseGrid>
        <template #sort="{ row }">
          <InputNumber
            v-if="!disabled"
            v-model:value="row.sort"
            class="w-full"
            :min="1"
            size="small"
          />
          <span v-else class="text-slate-500">{{ row.sort }}</span>
        </template>
        <template #itemName="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.itemName"
            placeholder="如：控制柜、显示仪表"
          />
          <span v-else>{{ row.itemName }}</span>
        </template>
        <template #itemGroup="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.itemGroup"
            placeholder="如：电气系统"
          />
          <span v-else>{{ row.itemGroup || '-' }}</span>
        </template>
        <template #method="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.method"
            placeholder="如：目视、擦拭"
          />
          <span v-else>{{ row.method }}</span>
        </template>
        <template #requirement="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.requirement"
            placeholder="如：无灰尘遮挡"
          />
          <span v-else>{{ row.requirement }}</span>
        </template>
        <template #frequency="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.frequency"
            placeholder="如：一个月/每季度"
          />
          <span v-else>{{ row.frequency || '-' }}</span>
        </template>
        <template #tool="{ row }">
          <Input
            v-if="!disabled"
            v-model:value="row.tool"
            placeholder="如：无尘布"
          />
          <span v-else>{{ row.tool }}</span>
        </template>

        <template #actions="{ row }">
          <Button
            v-if="!disabled"
            size="small"
            type="link"
            danger
            @click="handleDelete(row)"
          >
            移除
          </Button>
          <span v-else>-</span>
        </template>
      </BaseGrid>
    </div>
  </div>
</template>
