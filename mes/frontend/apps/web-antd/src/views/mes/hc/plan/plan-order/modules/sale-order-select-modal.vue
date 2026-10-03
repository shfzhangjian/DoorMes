<script lang="ts" setup>
import type { MesSaleOrderApi } from '#/api/mes/sale-order';

import { useVbenModal } from '@vben/common-ui';
import { Button } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getSaleOrderPage } from '#/api/mes/sale-order';

const emit = defineEmits<{
  select: [row: MesSaleOrderApi.MesSaleOrder];
}>();

function calcRemainQty(row: MesSaleOrderApi.MesSaleOrder) {
  return Number(row.quantity || 0) - Number(row.plannedQty || 0);
}

function pickOrder(row: MesSaleOrderApi.MesSaleOrder) {
  emit('select', row);
  modalApi.close();
}

const [Modal, modalApi] = useVbenModal({
  title: '销售订单检索与挂接',
  class: 'w-[1180px]',
  draggable: true,
  onConfirm: () => {
    const selected =
      gridApi.grid?.getRadioRecord() || gridApi.grid?.getCurrentRecord<MesSaleOrderApi.MesSaleOrder>();
    if (!selected) {
      return;
    }
    emit('select', selected);
    modalApi.close();
  },
});

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        fieldName: 'formulaKeyword',
        label: '物理配方',
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '留空查所有' },
      },
      {
        fieldName: 'productSpec',
        label: '尺寸规格',
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '留空查所有' },
      },
      {
        fieldName: 'keyword',
        label: '关键词',
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '单号/客户/料号...' },
      },
    ],
    wrapperClass: 'grid-cols-3',
    actionWrapperClass: 'col-span-3 text-right',
  },
  gridOptions: {
    columns: [
      { type: 'radio', width: 54 },
      { field: 'orderNo', title: '销售单号', minWidth: 160 },
      { field: 'customerName', title: '客户', minWidth: 140 },
      { field: 'productCode', title: '参考料号', minWidth: 120 },
      { field: 'formula', title: '配方', width: 100, align: 'center', slots: { default: 'formula' } },
      { field: 'productSpec', title: '尺寸', width: 100, align: 'center' },
      { field: 'remainQty', title: '需求量', width: 100, align: 'right', slots: { default: 'remainQty' } },
      { field: 'actions', title: '操作', width: 90, align: 'center', slots: { default: 'actions' } },
    ],
    height: 460,
    pagerConfig: { enabled: true },
    radioConfig: { highlight: true, trigger: 'row' },
    rowConfig: { isCurrent: true, isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getSaleOrderPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            orderNo: formValues.keyword,
            customerName: formValues.keyword,
          });
          return {
            ...result,
            list: (result.list || []).filter((item) => {
              if (
                formValues.productSpec &&
                !String(item.productSpec || '').includes(String(formValues.productSpec))
              ) {
                return false;
              }
              if (
                formValues.keyword &&
                ![
                  item.orderNo,
                  item.customerName,
                  item.productCode,
                  item.productName,
                ].some((field) => String(field || '').includes(String(formValues.keyword)))
              ) {
                return false;
              }
              if (formValues.formulaKeyword) {
                return false;
              }
              return true;
            }),
          };
        },
      },
    },
  },
});
</script>

<template>
  <Modal>
    <div class="flex h-full flex-col p-2">
      <Grid
        @cell-dblclick="({ row }) => {
          pickOrder(row);
        }"
      >
        <template #formula>
          <span>-</span>
        </template>
        <template #remainQty="{ row }">
          <span class="font-bold text-amber-600">{{ calcRemainQty(row) }}</span>
        </template>
        <template #actions="{ row }">
          <Button size="small" type="link" @click="pickOrder(row)">挂接</Button>
        </template>
      </Grid>
    </div>
  </Modal>
</template>
