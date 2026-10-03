<!-- 完整路径: src/views/mes/base/route/modules/process-select-modal.vue -->
<script lang="ts" setup>
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getProcessList } from '#/api/mes/base/route/index';
import { useVbenModal } from '@vben/common-ui';

const emit = defineEmits(['select']);

const [Modal, modalApi] = useVbenModal({
  title: '选择标准工序',
  class: 'w-[800px]',
  draggable: true,
  onConfirm: () => {
    const selected = gridApi.grid?.getCheckboxRecords();
    if (selected && selected.length > 0) {
      emit('select', selected);
      modalApi.close();
    }
  },
});

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { type: 'checkbox', width: 50 },
      { field: 'code', title: '工序编码', width: 120 },
      { field: 'name', title: '工序名称', minWidth: 150 },
      { field: 'workshopName', title: '默认车间', width: 120 },
      { field: 'processType', title: '类型', width: 80 },
      { field: 'remark', title: '备注' },
    ],
    height: 450,
    pagerConfig: { enabled: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          return await getProcessList({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            status: 0, // 仅查询启用
          });
        },
      },
    },
    toolbarConfig: { search: true, refresh: true },
  },
});

defineExpose({
  open: modalApi.open,
});
</script>

<template>
  <Modal>
    <div class="h-full p-2">
      <Grid />
    </div>
  </Modal>
</template>
