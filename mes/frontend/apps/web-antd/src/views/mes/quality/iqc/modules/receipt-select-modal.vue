<script lang="ts" setup>
import { useVbenModal } from '@vben/common-ui';
import { useVbenVxeGrid, type VxeTableGridOptions } from '#/adapter/vxe-table';
import { message } from 'ant-design-vue';
import { getPendingReceipts } from '#/api/mes/quality/iqc';

defineOptions({ name: 'ReceiptSelectModal' });
const emit = defineEmits(['select']);

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: [
      { type: 'radio', width: 50, align: 'center' },
      { field: 'receiptNo', title: '收料单号', width: 140 },
      { field: 'supplierName', title: '供应商', minWidth: 160 },
      { field: 'materialCode', title: '物料编码', width: 120 },
      { field: 'materialName', title: '品名', minWidth: 150 },
      { field: 'batchNo', title: '到货批次', width: 120 },
      { field: 'receiveQty', title: '数量', width: 80, align: 'right' },
    ],
    height: 400, rowConfig: { isHover: true, isCurrent: true }, pagerConfig: { enabled: false },
    proxyConfig: { ajax: { query: async () => ({ list: await getPendingReceipts() }) } },
  } as VxeTableGridOptions<any>,
});

const [Modal, modalApi] = useVbenModal({
  title: '📦 选择待检验的收料单', class: 'w-[900px]',
  onConfirm: () => {
    const row = gridApi.grid?.getRadioRecord();
    if (!row) return message.warning('请选中一条报检记录！');
    emit('select', row);
    modalApi.close();
  },
});
</script>
<template><Modal><div class="p-2 h-[450px] flex flex-col"><Grid /></div></Modal></template>
