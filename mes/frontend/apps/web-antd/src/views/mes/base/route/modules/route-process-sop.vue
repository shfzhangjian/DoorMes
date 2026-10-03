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
      { type: 'seq', width: 40 },
      { field: 'docCode', title: '文件编号', width: 120, editRender: { name: 'VxeInput' } },
      // 实际项目中这里应该是 Upload 组件，此处简化为输入链接
      { field: 'docUrl', title: '文件链接', minWidth: 150, editRender: { name: 'VxeInput' } },
      { field: 'version', title: '版本', width: 80, editRender: { name: 'VxeInput' } },
      {
        field: 'critical',
        title: '强制阅读',
        width: 80,
        cellRender: { name: 'VxeSwitch' }
      },
      { title: '操作', width: 60, slots: { default: 'actions' }, fixed: 'right' }
    ],
    editConfig: { trigger: 'click', mode: 'row', showStatus: true },
    toolbarConfig: { enabled: false },
    data: []
  }
});

function syncData() {
  if (props.processRow && gridApi.grid) {
    props.processRow.sops = gridApi.grid.getTableData().fullData;
  }
}

watch(() => props.processRow, async (newRow, oldRow) => {
  if (oldRow && oldRow !== newRow && gridApi.grid) {
    oldRow.sops = gridApi.grid.getTableData().fullData;
  }

  await nextTick();
  if (!gridApi.grid) return;
  if (newRow) {
    if (!newRow.sops) newRow.sops = [];
    gridApi.grid.reloadData(newRow.sops);
  } else {
    gridApi.grid.reloadData([]);
  }
}, { immediate: true });

function handleAdd() {
  if (!props.processRow) return;
  gridApi.grid?.insertAt({ critical: false, version: 'V1.0' }, -1);
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
