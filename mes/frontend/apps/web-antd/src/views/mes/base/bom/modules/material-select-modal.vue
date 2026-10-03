<script lang="ts" setup>
import type { baseMesMaterialApi } from '#/api/mes/base/material';

import { useVbenModal } from '@vben/common-ui';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getMesMaterialPage } from '#/api/mes/base/material';

const emit = defineEmits<{
  select: [baseMesMaterialApi.MesMaterial];
}>();

const [Modal, modalApi] = useVbenModal({
  title: '选择产品物料',
  class: 'w-[920px]',
  draggable: true,
  onConfirm: () => {
    const selected = gridApi.grid?.getRadioRecord() || gridApi.grid?.getCurrentRecord();
    if (selected) {
      emit('select', selected as baseMesMaterialApi.MesMaterial);
      modalApi.close();
    }
  },
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        fieldName: 'code',
        label: '物料编码',
        component: 'Input',
        componentProps: { allowClear: true },
      },
      {
        fieldName: 'name',
        label: '物料名称',
        component: 'Input',
        componentProps: { allowClear: true },
      },
      {
        fieldName: 'spec',
        label: '规格型号',
        component: 'Input',
        componentProps: { allowClear: true },
      },
    ],
    wrapperClass: 'grid-cols-3',
    actionWrapperClass: 'col-span-3 text-right',
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 60 },
      { field: 'code', title: '物料编码', minWidth: 140 },
      { field: 'name', title: '物料名称', minWidth: 180 },
      { field: 'spec', title: '规格型号', minWidth: 140 },
      { field: 'unit', title: '单位', width: 90, align: 'center' },
      {
        field: 'trackingMode',
        title: '追溯模式',
        width: 120,
        formatter: ({ cellValue }) =>
          ({ BATCH: '批次管控', SN: '单件管控', NONE: '不追溯' })[
            String(cellValue)
          ] || cellValue || '-',
      },
      { field: 'batchRuleName', title: '预置规则', minWidth: 150 },
    ],
    height: 430,
    pagerConfig: { enabled: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getMesMaterialPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            status: 0,
            ...formValues,
          });
        },
      },
    },
    radioConfig: { highlight: true, trigger: 'row' },
    rowConfig: { isCurrent: true, isHover: true, keyField: 'id' },
    toolbarConfig: { search: true, refresh: true },
  },
});

function handleSelect(row: baseMesMaterialApi.MesMaterial) {
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
      <Grid @cell-dblclick="({ row }) => handleSelect(row)" />
    </div>
  </Modal>
</template>
