<script lang="ts" setup>
import type { MesHandoverApi } from '#/api/mes/execution/handover';

import { ref } from 'vue';
import { Button, Input, Select, Tag } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

defineOptions({ name: 'HandoverItemList' });

const props = defineProps<{ disabled?: boolean }>();
const tableData = ref<MesHandoverApi.HandoverItem[]>([]);

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
      { field: 'checkItem', title: '检查项目', minWidth: 150, slots: { default: 'checkItem' } },
      { field: 'checkResult', title: '检查结果', width: 140, align: 'center', slots: { default: 'checkResult' } },
      { field: 'remark', title: '异常说明 / 备注', minWidth: 200, slots: { default: 'remark' } },
      { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'action' } }
    ],
  },
});

function loadData(data: MesHandoverApi.HandoverItem[]) {
  tableData.value = data;
  gridApi.setGridOptions({ data: tableData.value });
}

function getData() {
  return tableData.value;
}

function handleAddRow() {
  const newRow: MesHandoverApi.HandoverItem = {
    id: Date.now(),
    checkItem: '常规项目检查',
    checkResult: 'OK',
    remark: '',
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
      <span class="font-bold text-slate-700">交接班检查清单</span>
      <Button v-if="!disabled" type="primary" size="small" @click="handleAddRow">
        <IconifyIcon icon="lucide:plus" class="mr-1" /> 添加检查项
      </Button>
    </div>

    <BaseGrid>
      <template #checkItem="{ row }">
        <Input v-if="!disabled" v-model:value="row.checkItem" size="small" placeholder="如: 设备运行状态、现场卫生" />
        <span v-else>{{ row.checkItem }}</span>
      </template>

      <template #checkResult="{ row }">
        <Select
          v-if="!disabled"
          v-model:value="row.checkResult"
          size="small"
          class="w-full"
          :options="[
            { label: '正常 (OK)', value: 'OK' },
            { label: '异常 (NG)', value: 'NG' }
          ]"
        />
        <Tag v-else :color="row.checkResult === 'OK' ? 'success' : 'error'">
          {{ row.checkResult === 'OK' ? '正常 (OK)' : '异常 (NG)' }}
        </Tag>
      </template>

      <template #remark="{ row }">
        <Input v-if="!disabled" v-model:value="row.remark" size="small" placeholder="若异常请务必填写说明" />
        <span v-else>{{ row.remark || '-' }}</span>
      </template>

      <template #action="{ row }">
        <Button v-if="!disabled" type="text" danger size="small" @click="handleDeleteRow(row)">
          <IconifyIcon icon="lucide:trash-2" />
        </Button>
      </template>
    </BaseGrid>
  </div>
</template>
