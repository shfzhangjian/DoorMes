<script lang="ts" setup>
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDictOptions } from '@vben/hooks';
import { Button, message, Select, Input, Modal } from 'ant-design-vue';
import { watch, nextTick, ref } from 'vue';
import { Trash2 } from '@vben/icons'; // 仅保留行内删除图标

const props = defineProps<{
  processRow: any;
  allProcesses?: any[];
}>();

// 定义表格列
const gridColumns: VxeGridProps['columns'] = [
  { type: 'seq', width: 40, align: 'center' },
  {
    field: 'paramName',
    title: '参数名称',
    minWidth: 120,
    editRender: { name: 'VxeInput' },
    slots: { edit: 'paramName_edit', default: 'paramName_default' }
  },
  {
    field: 'paramCode',
    title: '参数编码',
    width: 100,
    editRender: { name: 'VxeInput' },
    slots: { edit: 'paramCode_edit' }
  },
  {
    field: 'paramType',
    title: '类型',
    width: 90,
    editRender: { name: 'VxeSelect' },
    slots: { edit: 'paramType_edit', default: 'paramType_default' }
  },
  {
    field: 'standardValue',
    title: '标准值',
    minWidth: 100,
    editRender: { name: 'VxeInput' },
    slots: { edit: 'standardValue_edit' }
  },
  {
    field: 'unit',
    title: '单位',
    width: 80,
    editRender: { name: 'VxeSelect' },
    slots: { edit: 'unit_edit', default: 'unit_default' }
  },
  {
    field: 'critical',
    title: '关键',
    width: 70,
    align: 'center',
    cellRender: { name: 'VxeSwitch', props: { disabled: false } }
  },
  { title: '操作', width: 60, slots: { default: 'actions' }, align: 'center', fixed: 'right' },
];

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    border: true,
    height: '100%',
    keepSource: true,
    editConfig: { trigger: 'click', mode: 'row', showStatus: true },
    toolbarConfig: { enabled: false },
    data: [],
    pagerConfig: { enabled: false },
  },
});

function syncData() {
  if (props.processRow && gridApi.grid) {
    props.processRow.params = gridApi.grid.getTableData().fullData;
  }
}

watch(
  () => props.processRow,
  async (newRow, oldRow) => {
    // 🌟 核心修复：在渲染新工序前，先把当前网格的数据“反向回写”到老工序内存中！
    if (oldRow && oldRow !== newRow && gridApi.grid) {
      oldRow.params = gridApi.grid.getTableData().fullData;
    }

    await nextTick();
    if (!gridApi.grid) return;

    if (newRow) {
      if (!newRow.params) newRow.params = [];
      gridApi.grid.reloadData(newRow.params);
    } else {
      gridApi.grid.reloadData([]);
    }
  },
  { immediate: true },
);

function handleAdd() {
  if (!props.processRow) return;
  const record = {
    paramName: '',
    paramCode: '',
    paramType: 'numeric',
    standardValue: '',
    critical: false,
  };
  gridApi.grid?.insertAt(record, -1);
}

function handleRemove(row: any) {
  gridApi.grid?.remove(row);
}

// --- 复制参数功能 ---
const copyModalVisible = ref(false);
const sourceProcessId = ref<any>(undefined);
const availableSourceProcesses = ref<any[]>([]);

function handleOpenCopy() {
  if (!props.allProcesses || props.allProcesses.length === 0) {
    message.warning('没有其他工序可供复制');
    return;
  }
  availableSourceProcesses.value = props.allProcesses.filter(p =>
    ((p.id && p.id !== props.processRow.id) || (p._tempId && p._tempId !== props.processRow._tempId)) &&
    p.params && p.params.length > 0
  );

  if (availableSourceProcesses.value.length === 0) {
    message.warning('其他工序没有配置参数，无法复制');
    return;
  }
  sourceProcessId.value = undefined;
  copyModalVisible.value = true;
}

function handleCopyConfirm() {
  if (!sourceProcessId.value) {
    message.warn('请选择源工序');
    return;
  }
  const sourceProcess = availableSourceProcesses.value.find(p =>
    (p.id && p.id === sourceProcessId.value) || (p._tempId && p._tempId === sourceProcessId.value)
  );

  if (sourceProcess && sourceProcess.params) {
    const newParams = sourceProcess.params.map((p: any) => ({
      ...p,
      id: undefined,
    }));
    gridApi.grid?.insertAt(newParams, -1);
    message.success(`成功复制 ${newParams.length} 个参数`);
  }
  copyModalVisible.value = false;
}

// [关键] 暴露方法给父组件
defineExpose({
  handleAdd,
  handleOpenCopy,
  syncData
});
</script>

<template>
  <div class="flex h-full flex-col bg-white overflow-hidden">
    <!-- 头部已移除 -->

    <!-- 表格区域 -->
    <div class="flex-1 overflow-hidden p-2 relative h-full">
      <div v-if="!processRow" class="absolute inset-0 flex items-center justify-center bg-gray-50/50 text-gray-400 z-10">
        <div class="text-center">
          <div class="text-3xl mb-2">👈</div>
          <div>请先在左侧选择工序</div>
        </div>
      </div>

      <Grid class="h-full">
        <template #paramName_edit="{ row }">
          <Input v-model:value="row.paramName" class="w-full" />
        </template>
        <template #paramName_default="{ row }">{{ row.paramName }}</template>

        <template #paramCode_edit="{ row }">
          <Input v-model:value="row.paramCode" class="w-full" />
        </template>

        <template #standardValue_edit="{ row }">
          <Input v-model:value="row.standardValue" class="w-full" />
        </template>

        <template #paramType_edit="{ row }">
          <Select v-model:value="row.paramType" class="w-full" :options="getDictOptions('mes_param_type', 'string')" />
        </template>
        <template #paramType_default="{ row }">
          {{ getDictOptions('mes_param_type', 'string').find((o:any)=>o.value==row.paramType)?.label || row.paramType }}
        </template>

        <template #unit_edit="{ row }">
          <Select v-model:value="row.unit" class="w-full" :options="getDictOptions('mes_unit', 'string')" />
        </template>
        <template #unit_default="{ row }">{{ row.unit }}</template>

        <template #actions="{ row }">
          <Button type="link" danger size="small" @click="handleRemove(row)">
            <Trash2 class="w-4 h-4" />
          </Button>
        </template>
      </Grid>
    </div>

    <Modal v-model:open="copyModalVisible" title="复制参数" @ok="handleCopyConfirm">
      <div class="py-4">
        <div class="mb-2 text-gray-500">选择要从哪个工序复制参数：</div>
        <Select v-model:value="sourceProcessId" class="w-full" placeholder="请选择源工序">
          <Select.Option
            v-for="p in availableSourceProcesses"
            :key="p.id || p._tempId"
            :value="p.id || p._tempId"
          >
            {{ p.processName }} (包含 {{ p.params.length }} 个参数)
          </Select.Option>
        </Select>
      </div>
    </Modal>
  </div>
</template>
