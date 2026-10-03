<script lang="ts" setup>
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { Button } from 'ant-design-vue';
import { Trash2, Plus } from '@vben/icons';
import { watch, nextTick } from 'vue';

const props = defineProps<{ processRow: any }>();

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    height: '100%',
    columns: [
      { type: 'seq', width: 40, align: 'center' },
      { field: 'postCode', title: '岗位编码', width: 120, editRender: { name: 'VxeInput' } },
      { field: 'postName', title: '岗位名称', minWidth: 120, editRender: { name: 'VxeInput' } },
      {
        field: 'skillLevel',
        title: '技能等级',
        width: 100,
        editRender: {
          name: 'VxeSelect',
          options: [
            { label: 'L1', value: 'L1' },
            { label: 'L2', value: 'L2' },
            { label: 'L3', value: 'L3' },
            { label: 'L4', value: 'L4' },
            { label: 'L5', value: 'L5' }
          ]
        }
      },
      { field: 'stdManHour', title: '标准工时', width: 100, editRender: { name: 'VxeInputNumber', props: { min: 0 } } },
      { field: 'minPerson', title: '最少人数', width: 80, editRender: { name: 'VxeInputNumber', props: { min: 1 } } },
      { title: '操作', width: 60, slots: { default: 'actions' }, fixed: 'right' }
    ],
    editConfig: { trigger: 'click', mode: 'row', showStatus: true },
    toolbarConfig: { enabled: false },
    data: []
  }
});

function syncData() {
  if (props.processRow && gridApi.grid) {
    props.processRow.posts = gridApi.grid.getTableData().fullData;
  }
}

watch(() => props.processRow, async (newRow, oldRow) => {
  if (oldRow && oldRow !== newRow && gridApi.grid) {
    oldRow.posts = gridApi.grid.getTableData().fullData;
  }

  await nextTick();
  if (!gridApi.grid) return;
  if (newRow) {
    if (!newRow.posts) newRow.posts = [];
    gridApi.grid.reloadData(newRow.posts);
  } else {
    gridApi.grid.reloadData([]);
  }
}, { immediate: true });

function handleAdd() {
  if (!props.processRow) return;
  gridApi.grid?.insertAt({ skillLevel: 'L1', minPerson: 1 }, -1);
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
