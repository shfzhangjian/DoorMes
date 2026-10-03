<script lang="ts" setup>
import type { MesTrackHistoryApi } from '#/api/mes/report/track-history';

import { ref } from 'vue';
import { Tag } from 'ant-design-vue';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

defineOptions({ name: 'TrackBarcodeItemList' });

const tableData = ref<MesTrackHistoryApi.BarcodeItem[]>([]);

const [BaseGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    showOverflow: true,
    keepSource: true,
    height: 350,
    rowConfig: { keyField: 'id', isHover: true },
    pagerConfig: { enabled: false },
    toolbarConfig: { enabled: false },
    columns: [
      { type: 'seq', width: 60, align: 'center' },
      { field: 'serialNumber', title: '产品唯一序列号 (SN)', minWidth: 200 },
      { field: 'status', title: '品质判定', width: 120, align: 'center', slots: { default: 'status' } },
      { field: 'generateTime', title: '条码生成时间', width: 180, align: 'center' },
    ],
  },
});

function loadData(data: MesTrackHistoryApi.BarcodeItem[]) {
  tableData.value = data;
  gridApi.setGridOptions({ data: tableData.value });
}

defineExpose({ loadData });
</script>

<template>
  <div>
    <div class="px-4 py-2 border-b border-slate-200 bg-slate-50 flex items-center gap-2">
      <span class="font-bold text-slate-700">产出条码追溯明细</span>
      <Tag color="purple">一物一码</Tag>
    </div>

    <BaseGrid>
      <template #status="{ row }">
        <Tag :color="row.status === 'GOOD' ? 'success' : 'error'">
          {{ row.status === 'GOOD' ? '良品 (GOOD)' : '报废 (SCRAP)' }}
        </Tag>
      </template>
    </BaseGrid>
  </div>
</template>
