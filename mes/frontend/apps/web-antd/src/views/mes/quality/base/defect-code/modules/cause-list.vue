<script lang="ts" setup>
import type { MesDefectCodeApi } from '#/api/mes/quality/base/defect-code';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { Button, Input, InputNumber, Select } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';

import { useVbenVxeGrid } from '#/adapter/vxe-table';

defineOptions({ name: 'DefectCauseList' });

const props = defineProps<{ disabled?: boolean }>();

const reasonTypeOptions = [
  { label: '人员原因', value: 'MAN' },
  { label: '设备原因', value: 'MACHINE' },
  { label: '物料原因', value: 'MATERIAL' },
  { label: '方法原因', value: 'METHOD' },
  { label: '环境原因', value: 'ENVIRONMENT' },
];

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 260,
    rowConfig: { keyField: 'id', isCurrent: true, isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
    columns: [
      { type: 'seq', width: 50, align: 'center' },
      { field: 'reasonCode', title: '原因代码', width: 130, slots: { default: 'reasonCode' } },
      { field: 'reasonName', title: '发生原因', minWidth: 150, slots: { default: 'reasonName' } },
      { field: 'reasonType', title: '原因分类', width: 130, slots: { default: 'reasonType' } },
      { field: 'reasonDesc', title: '原因说明', minWidth: 220, slots: { default: 'reasonDesc' } },
      { field: 'sort', title: '排序', width: 90, slots: { default: 'sort' } },
      { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
      { title: '操作', width: 80, align: 'center', slots: { default: 'action' }, visible: !props.disabled },
    ],
    data: [],
  } as VxeTableGridOptions<MesDefectCodeApi.DefectCause>,
});

function loadData(data?: MesDefectCodeApi.DefectCause[]) {
  gridApi.setGridOptions({ data: data || [] });
}

function getData(): MesDefectCodeApi.DefectCause[] {
  if (!gridApi.grid) return [];
  const { fullData } = gridApi.grid.getTableData();
  return fullData.map((item: any) => ({
    ...item,
    id: typeof item.id === 'string' || item.id > 10000000000 ? undefined : item.id,
  }));
}

async function handleAddRow() {
  if (props.disabled || !gridApi.grid) return;
  const { fullData } = gridApi.grid.getTableData();
  const defaultData: MesDefectCodeApi.DefectCause = {
    id: Date.now(),
    reasonName: '',
    reasonType: 'MATERIAL',
    sort: (fullData.length + 1) * 10,
    status: 1,
  };
  const { row } = await gridApi.grid.insertAt(defaultData, -1);
  await gridApi.grid.scrollToRow(row);
  await gridApi.grid.setCurrentRow(row);
}

async function handleRemoveRow(row: any) {
  if (!props.disabled && gridApi.grid) await gridApi.grid.remove(row);
}

defineExpose({ loadData, getData });
</script>

<template>
  <div class="flex flex-col rounded border border-slate-200 bg-white">
    <div class="flex items-center justify-between border-b border-slate-200 px-3 py-2">
      <div class="flex items-center gap-2">
        <div class="h-3.5 w-1 rounded-sm bg-amber-500"></div>
        <span class="text-sm font-bold text-slate-700">发生原因明细</span>
        <span class="text-xs text-slate-400">按 4M1E 维护，一个缺陷项可多原因</span>
      </div>
      <Button v-if="!disabled" type="primary" size="small" @click="handleAddRow">
        <IconifyIcon icon="lucide:plus" class="mr-1" /> 添加原因
      </Button>
    </div>

    <div class="p-2">
      <BaseGrid>
        <template #reasonCode="{ row }">
          <Input v-if="!disabled" v-model:value="row.reasonCode" placeholder="可选" size="small" />
          <span v-else>{{ row.reasonCode || '-' }}</span>
        </template>

        <template #reasonName="{ row }">
          <Input v-if="!disabled" v-model:value="row.reasonName" placeholder="必填" size="small" />
          <span v-else>{{ row.reasonName }}</span>
        </template>

        <template #reasonType="{ row }">
          <Select v-if="!disabled" v-model:value="row.reasonType" size="small" class="w-full" :options="reasonTypeOptions" allow-clear />
          <span v-else>{{ reasonTypeOptions.find(item => item.value === row.reasonType)?.label || '-' }}</span>
        </template>

        <template #reasonDesc="{ row }">
          <Input.TextArea v-if="!disabled" v-model:value="row.reasonDesc" :rows="1" placeholder="描述发生机制或典型场景" size="small" />
          <span v-else>{{ row.reasonDesc || '-' }}</span>
        </template>

        <template #sort="{ row }">
          <InputNumber v-if="!disabled" v-model:value="row.sort" :min="0" size="small" class="w-full" />
          <span v-else>{{ row.sort }}</span>
        </template>

        <template #status="{ row }">
          <Select v-if="!disabled" v-model:value="row.status" size="small" class="w-full" :options="statusOptions" />
          <span v-else>{{ row.status === 0 ? '停用' : '启用' }}</span>
        </template>

        <template #action="{ row }">
          <Button v-if="!disabled" type="link" danger size="small" @click="handleRemoveRow(row)">移除</Button>
          <span v-else>-</span>
        </template>
      </BaseGrid>
    </div>
  </div>
</template>
