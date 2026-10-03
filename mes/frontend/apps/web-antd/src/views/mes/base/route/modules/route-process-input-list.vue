<script lang="ts" setup>
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { Button } from 'ant-design-vue';
import { Trash2 } from '@vben/icons';
import { watch, nextTick } from 'vue';

const props = defineProps<{ processRow: any }>();

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    height: '100%',
    columns: [
      { type: 'seq', width: 40, align: 'center', fixed: 'left' },
      { field: 'materialCode', title: '物料编码', width: 120, editRender: { name: 'VxeInput' } },
      { field: 'materialName', title: '物料名称', minWidth: 150, editRender: { name: 'VxeInput' } },
      {
        field: 'standardQty', // V3.1 字段: standardQty
        title: '标准用量',
        width: 100,
        editRender: { name: 'VxeInputNumber', props: { min: 0, precision: 4 } }
      },
      {
        field: 'lossRate',    // V3.1 字段: lossRate
        title: '损耗率(%)',
        width: 100,
        editRender: { name: 'VxeInputNumber', props: { min: 0, max: 100, precision: 2 } }
      },
      { field: 'unit', title: '单位', width: 80, editRender: { name: 'VxeInput' } },
      { title: '操作', width: 60, slots: { default: 'actions' }, align: 'center', fixed: 'right' },
    ],
    editConfig: { trigger: 'click', mode: 'row', showStatus: true },
    toolbarConfig: { enabled: false },
    data: [],
  },
});

function syncData() {
  if (props.processRow && gridApi.grid) {
    props.processRow.inputs = gridApi.grid.getTableData().fullData;
  }
}

watch(() => props.processRow, async (newRow, oldRow) => {
  if (oldRow && oldRow !== newRow && gridApi.grid) {
    oldRow.inputs = gridApi.grid.getTableData().fullData;
  }

  await nextTick();
  if (!gridApi.grid) return;
  if (newRow) {
    if (!newRow.inputs) newRow.inputs = [];
    gridApi.grid.reloadData(newRow.inputs);
  } else {
    gridApi.grid.reloadData([]);
  }
}, { immediate: true });

function handleAdd() {
  if (!props.processRow) return;
  gridApi.grid?.insertAt({ standardQty: 1, lossRate: 0 }, -1);
}

function handleRemove(row: any) {
  gridApi.grid?.remove(row);
}

defineExpose({ handleAdd,syncData });
</script>

<template>
  <div class="h-full flex flex-col">
    <Grid>
      <template #actions="{ row }">
        <Button type="link" danger size="small" @click="handleRemove(row)">
          <Trash2 class="w-4 h-4" />
        </Button>
      </template>
    </Grid>
  </div>
</template>
