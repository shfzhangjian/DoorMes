<script lang="ts" setup>
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { Tag, Switch, Button } from 'ant-design-vue';
import { Trash2 } from '@vben/icons';
import { watch, nextTick } from 'vue';

const props = defineProps<{ processRow: any }>();

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    height: '100%',
    columns: [
      { type: 'seq', width: 50, align: 'center', fixed: 'left' },
      { field: 'stationCode', title: '设备/机台编码', width: 140 },
      { field: 'stationName', title: '执行机台名称', minWidth: 180 },
      { field: 'defaultStatus', title: '排产优选', width: 100, align: 'center', slots: { default: 'defaultStatus' } },
      { field: 'remark', title: '备注说明', minWidth: 150, editRender: { name: 'VxeInput' } },
      { title: '操作', width: 80, align: 'center', slots: { default: 'actions' }, fixed: 'right' },
    ],
    editConfig: { trigger: 'click', mode: 'row', showStatus: true },
    toolbarConfig: { enabled: false },
    data: []
  }
});

// 监听工序行变化（开启 deep 以捕获外部的 push 动作）
watch(() => props.processRow, async (row) => {
  await nextTick();
  if (!gridApi.grid) return;

  if (row) {
    if (!row.stations) row.stations = [];
    gridApi.grid.reloadData(row.stations);
  } else {
    gridApi.grid.reloadData([]);
  }
}, { immediate: true, deep: true });

// 设为优选逻辑
function handleDefaultChange(currentRow: any, checked: boolean) {
  currentRow.defaultStatus = checked;
  if (checked && props.processRow && props.processRow.stations) {
    // 排他逻辑：只能有一个优选
    props.processRow.stations.forEach((row: any) => {
      if (row !== currentRow) {
        row.defaultStatus = false;
      }
    });
  }
  gridApi.grid?.reloadData(props.processRow.stations);
}

// 移除机台逻辑
function handleRemoveStation(row: any) {
  if (props.processRow && props.processRow.stations) {
    const index = props.processRow.stations.indexOf(row);
    if (index > -1) {
      props.processRow.stations.splice(index, 1);
      gridApi.grid?.reloadData(props.processRow.stations);
    }
  }
}
</script>

<template>
  <div class="h-full w-full flex flex-col relative bg-white">
    <div class="bg-purple-50/50 px-4 py-2 border-b border-purple-100 flex items-center justify-between shrink-0">
      <span class="text-xs text-purple-600 font-medium flex items-center gap-2">
        <span class="w-1.5 h-3.5 bg-purple-500 rounded-full inline-block"></span>
        下列为该工艺步骤在车间允许派发的物理机台矩阵
      </span>
    </div>

    <div class="flex-1 overflow-hidden p-2">
      <Grid>
        <template #defaultStatus="{ row }">
          <Switch :checked="row.defaultStatus" @change="(val: boolean) => handleDefaultChange(row, val)" />
        </template>
        <template #actions="{ row }">
          <Button type="link" danger size="small" @click="handleRemoveStation(row)">
            <Trash2 class="h-4 w-4" />
          </Button>
        </template>
      </Grid>
    </div>
  </div>
</template>
