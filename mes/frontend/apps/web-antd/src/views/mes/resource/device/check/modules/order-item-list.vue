<script lang="ts" setup>
import { ref, h, watch, nextTick } from 'vue';
import { Button, Input, InputNumber } from 'ant-design-vue';
import { Plus } from "@vben/icons";
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { useOrderItemColumns } from '../data';
import { getOrderItemListById } from '#/api/mes/resource/device/check';

// 【防递归命名】
defineOptions({ name: 'MaintOrderItemListGrid' });

const props = defineProps<{ orderId?: number; disabled?: boolean }>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useOrderItemColumns(),
    data: [],
    border: true,
    showOverflow: true,
    keepSource: true,
    // 【外观法则】: 核心定高防撑破！严禁 autoResize
    height: 350,
    // 【身份法则】: 必须使用 id 作为 keyField
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
  },
});

const handleAdd = async () => {
  if (!props.disabled && gridApi.grid) {
    // 【身份法则】: 插入时必须附带绝对唯一的 ID (Date.now() 或 UUID)
    const { row } = await gridApi.grid.insertAt({
      id: Date.now().toString(),
      sort: gridApi.grid.getData().length + 1,
      quantity: 1,
      unit: '个'
    }, -1);

    // 焦点跟随
    await gridApi.grid.scrollToRow(row);
    await gridApi.grid.setCurrentRow(row);
  }
};

const handleDelete = async (row: any) => { if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row); };

// 【提取法则】: 安全提取不污染 VNode
defineExpose({
  getData: () => {
    if (!gridApi.grid) return [];
    const data = gridApi.grid.getData();
    const removes = gridApi.grid.getRemoveRecords();
    const inserts = gridApi.grid.getInsertRecords();
    return data.filter((row: any) => !removes.some((r: any) => r.id === row.id)).concat(inserts.map((row: any) => ({ ...row, id: undefined })));
  },
});

// 【清洗法则】: 弹窗每次变动触发重置洗盘，避免幽灵数据
watch(() => props.orderId, async (val) => {
  if (!val) {
    gridApi.setGridOptions({ data: [] });
    return;
  }
  await nextTick();
  gridApi.setGridOptions({ data: await getOrderItemListById(val) });
}, { immediate: true });
</script>

<template>
  <div class="flex flex-col h-full bg-slate-50">
    <div class="flex items-center justify-between px-3 py-2 border-b border-slate-200 bg-white">
      <div class="flex items-center gap-2">
        <div class="w-1 h-3.5 bg-blue-600 rounded-sm"></div>
        <span class="font-bold text-slate-700 text-sm">实际消耗备件申报</span>
      </div>
      <Button v-if="!disabled" size="small" type="primary" @click="handleAdd" :icon="h(Plus)">添加耗材</Button>
    </div>

    <div class="p-2 bg-white">
      <BaseGrid>
        <template #sort="{ row }"><span class="text-slate-500">{{ row.sort }}</span></template>
        <template #partSlot="{ row }"><Input v-if="!disabled" v-model:value="row.partName" placeholder="填入备件名称" /><span v-else>{{ row.partName }}</span></template>
        <template #spec="{ row }"><Input v-if="!disabled" v-model:value="row.spec" placeholder="型号规格" /><span v-else>{{ row.spec }}</span></template>
        <template #quantity="{ row }"><InputNumber v-if="!disabled" v-model:value="row.quantity" class="w-full" :min="1" /><span v-else>{{ row.quantity }}</span></template>
        <template #unit="{ row }"><Input v-if="!disabled" v-model:value="row.unit" placeholder="个/L" /><span v-else>{{ row.unit }}</span></template>
        <template #remark="{ row }"><Input v-if="!disabled" v-model:value="row.remark" placeholder="使用说明" /><span v-else>{{ row.remark || '-' }}</span></template>
        <template #actions="{ row }"><Button v-if="!disabled" size="small" type="link" danger @click="handleDelete(row)">移除</Button><span v-else>-</span></template>
      </BaseGrid>
    </div>
  </div>
</template>
