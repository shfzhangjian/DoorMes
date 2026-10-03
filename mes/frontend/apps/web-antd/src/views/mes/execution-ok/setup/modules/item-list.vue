<script lang="ts" setup>
import type { MesSetupRuleApi } from '#/api/mes/execution/setup';

import { ref } from 'vue';
import { Button, Input, InputNumber, Switch } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

defineOptions({ name: 'SetupRuleItemList' });

const props = defineProps<{ disabled?: boolean }>();
const tableData = ref<MesSetupRuleApi.RuleItem[]>([]);

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 350,
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
    columns: [
      { type: 'seq', width: 50, align: 'center' },
      { field: 'materialCode', title: '物料编码', minWidth: 150, slots: { default: 'materialCode' } },
      { field: 'materialName', title: '物料名称', minWidth: 180, slots: { default: 'materialName' } },
      { field: 'requiredQty', title: '需求数量', width: 120, slots: { default: 'requiredQty' } },
      { field: 'allowSubstitute', title: '允许替代料', width: 100, align: 'center', slots: { default: 'allowSubstitute' } },
      { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'action' } }
    ],
  },
});

function loadData(data: MesSetupRuleApi.RuleItem[]) {
  tableData.value = data;
  gridApi.setGridOptions({ data: tableData.value });
}

function getData() {
  return tableData.value;
}

function handleAddRow() {
  const newRow: MesSetupRuleApi.RuleItem = {
    id: Date.now(),
    materialCode: '',
    materialName: '',
    requiredQty: 1,
    allowSubstitute: false,
  };
  tableData.value.push(newRow);
  gridApi.setGridOptions({ data: tableData.value });
}

function handleDeleteRow(row: any) {
  tableData.value = tableData.value.filter(item => item.id !== row.id);
  gridApi.setGridOptions({ data: tableData.value });
}

defineExpose({ loadData, getData });
</script>

<template>
  <div>
    <div class="px-4 py-2 border-b border-slate-200 flex justify-between items-center bg-slate-50">
      <span class="font-bold text-slate-700">投料 BOM 明细</span>
      <Button v-if="!disabled" type="primary" size="small" @click="handleAddRow">
        <IconifyIcon icon="lucide:plus" class="mr-1" /> 添加物料
      </Button>
    </div>

    <BaseGrid>
      <template #materialCode="{ row }">
        <Input v-if="!disabled" v-model:value="row.materialCode" size="small" placeholder="请输入编码" />
        <span v-else>{{ row.materialCode }}</span>
      </template>

      <template #materialName="{ row }">
        <Input v-if="!disabled" v-model:value="row.materialName" size="small" placeholder="请输入名称" />
        <span v-else>{{ row.materialName }}</span>
      </template>

      <template #requiredQty="{ row }">
        <InputNumber v-if="!disabled" v-model:value="row.requiredQty" :min="0.01" size="small" class="w-full" />
        <span v-else>{{ row.requiredQty }}</span>
      </template>

      <template #allowSubstitute="{ row }">
        <Switch v-if="!disabled" v-model:checked="row.allowSubstitute" size="small" />
        <span v-else>{{ row.allowSubstitute ? '是' : '否' }}</span>
      </template>

      <template #action="{ row }">
        <Button v-if="!disabled" type="text" danger size="small" @click="handleDeleteRow(row)">
          <IconifyIcon icon="lucide:trash-2" />
        </Button>
      </template>
    </BaseGrid>
  </div>
</template>
