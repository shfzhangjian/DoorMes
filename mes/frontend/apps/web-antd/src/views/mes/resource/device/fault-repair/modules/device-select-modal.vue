<script lang="ts" setup>
import { useVbenModal } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getDevicePage } from '#/api/mes/resource/device/ledger';

const emit = defineEmits(['select']);

const [Modal, modalApi] = useVbenModal({
  title: '从台账中提取故障设备',
  class: 'w-[900px]',
  draggable: true,
  onConfirm: () => {
    const selected = gridApi.grid?.getRadioRecord();
    if (!selected) {
      const currentRow = gridApi.grid?.getCurrentRecord();
      if (currentRow) emit('select', currentRow);
      modalApi.close();
      return;
    }
    emit('select', selected);
    modalApi.close();
  },
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      { fieldName: 'deviceCode', label: '设备编号', component: 'Input' },
      { fieldName: 'deviceName', label: '设备名称', component: 'Input' },
    ],
    wrapperClass: 'grid-cols-2',
    actionWrapperClass: 'col-span-2 text-right',
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 60 },
      { field: 'deviceCode', title: '设备编号', minWidth: 120 },
      { field: 'deviceName', title: '设备名称', minWidth: 160 },
      { field: 'location', title: '所在位置', minWidth: 150 },
    ],
    height: 500,
    pagerConfig: { enabled: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }: any, formValues: Record<string, any>) =>
          await getDevicePage({
            ...formValues,
            pageNo: page.currentPage,
            pageSize: page.pageSize,
          }),
      },
    },
    radioConfig: { highlight: true, trigger: 'row' },
    rowConfig: { isCurrent: true, isHover: true, keyField: 'id' },
    toolbarConfig: { search: true, refresh: true },
  },
});

function handleDeviceDblclick({ row }: any) {
  emit('select', row);
  modalApi.close();
}

defineExpose({
  open: () => modalApi.open(),
  close: () => modalApi.close(),
});
</script>

<template>
  <Modal>
    <div class="flex h-full flex-col p-2">
      <Grid @cell-dblclick="handleDeviceDblclick" />
    </div>
  </Modal>
</template>
