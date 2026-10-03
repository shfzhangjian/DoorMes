<script lang="ts" setup>
import { ref, h, watch, nextTick } from 'vue';
import { Button, Input, Switch } from 'ant-design-vue';
import { Plus } from "@vben/icons";
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { useProductGridColumns } from '../data';
import { getMoldProductListById } from '#/api/mes/resource/mold/ledger';

defineOptions({ name: 'MoldLedgerProductListGrid' });
const props = defineProps<{ moldId?: string | number; disabled?: boolean }>();

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: useProductGridColumns(),
    data: [], border: true, showOverflow: true, keepSource: true,
    height: 300, rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false }, toolbarConfig: { enabled: false },
  },
});

const handleAdd = async () => {
  if (!props.disabled && gridApi.grid) {
    const { row } = await gridApi.grid.insertAt({
      id: Date.now().toString(),
      sort: gridApi.grid.getData().length + 1,
      isDefault: false
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

watch(() => props.moldId, async (val) => {
  if (!val) { gridApi.setGridOptions({ data: [] }); return; }
  await nextTick();
  gridApi.setGridOptions({ data: await getMoldProductListById(val) });
}, { immediate: true });
</script>

<template>
  <div class="flex flex-col h-full bg-slate-50">
    <div class="flex items-center justify-between px-4 py-2 border-b border-indigo-100 bg-white">
      <div class="flex items-center gap-2">
        <IconifyIcon icon="lucide:box" class="text-indigo-500" />
        <span class="font-bold text-slate-600 text-sm">模具适用产品(BOM)绑定清单</span>
      </div>
      <Button v-if="!disabled" size="small" type="primary" ghost @click="handleAdd" :icon="h(Plus)">绑定产品</Button>
    </div>
    <div class="p-2 bg-white">
      <BaseGrid>
        <template #sort="{ row }"><span class="text-slate-500">{{ row.sort }}</span></template>
        <template #productCode="{ row }"><Input v-if="!disabled" v-model:value="row.productCode" placeholder="输入产品编号" /><span v-else>{{ row.productCode }}</span></template>
        <template #productName="{ row }"><Input v-if="!disabled" v-model:value="row.productName" placeholder="如：外壳A面" /><span v-else class="font-bold text-slate-700">{{ row.productName }}</span></template>
        <template #spec="{ row }"><Input v-if="!disabled" v-model:value="row.spec" placeholder="材料或规格" /><span v-else>{{ row.spec }}</span></template>
        <template #isDefault="{ row }"><Switch v-if="!disabled" v-model:checked="row.isDefault" size="small" /><Tag v-else :color="row.isDefault ? 'success' : 'default'">{{ row.isDefault ? '是' : '否' }}</Tag></template>
        <template #remark="{ row }"><Input v-if="!disabled" v-model:value="row.remark" placeholder="工艺备注" /><span v-else>{{ row.remark || '-' }}</span></template>
        <template #actions="{ row }"><Button v-if="!disabled" size="small" type="link" danger @click="handleDelete(row)">解绑</Button><span v-else>-</span></template>
      </BaseGrid>
    </div>
  </div>
</template>
