<script lang="ts" setup>
import type { MesHandoverHistoryApi } from '#/api/mes/report/handover-history';

import { ref } from 'vue';
import { Tag } from 'ant-design-vue';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

defineOptions({ name: 'HandoverHistoryItemList' });

const props = defineProps<{ disabled?: boolean }>();
const tableData = ref<MesHandoverHistoryApi.Item[]>([]);

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 300,
    rowConfig: { keyField: 'id', isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
    columns: [
      { type: 'seq', width: 50, align: 'center' },
      { field: 'checkItem', title: '设备/环境点检项目', minWidth: 250 },
      { field: 'checkResult', title: '核查结果', width: 120, align: 'center', slots: { default: 'checkResult' } },
      { field: 'remark', title: '异常说明', minWidth: 200 },
    ],
  },
});

function loadData(data: MesHandoverHistoryApi.Item[]) {
  tableData.value = data;
  gridApi.setGridOptions({ data: tableData.value });
}

defineExpose({ loadData });
</script>

<template>
  <div>
    <div class="px-4 py-2 border-b border-slate-200 bg-slate-50">
      <span class="font-bold text-slate-700">现场核查清单 (SOP要求)</span>
    </div>

    <BaseGrid>
      <template #checkResult="{ row }">
        <Tag :color="row.checkResult === 'OK' ? 'success' : 'error'">
          {{ row.checkResult === 'OK' ? '合格 (OK)' : '异常 (NG)' }}
        </Tag>
      </template>
    </BaseGrid>
  </div>
</template>
