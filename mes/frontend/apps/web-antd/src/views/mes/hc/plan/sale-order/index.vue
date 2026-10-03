<script lang="ts" setup>
import type { MesSaleOrderApi } from '#/api/mes/sale-order';

import { Page, useVbenModal } from '@vben/common-ui';
import { Progress, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getSaleOrderPage } from '#/api/mes/sale-order';

import FormModal from './modules/form.vue';
import { PLAN_FIELD_COPY, PLAN_PLACEHOLDER_COPY } from '../shared/field-copy';

const [SaleOrderForm, formApi] = useVbenModal({
  connectedComponent: FormModal,
  destroyOnClose: true,
});

function statusColor(status?: string) {
  return (
    {
      APPROVED: 'cyan',
      COMPLETED: 'green',
      DRAFT: 'default',
      PART_PLANNED: 'blue',
    } as Record<string, string>
  )[String(status || '')] || 'default';
}

function refresh() {
  gridApi.query();
}

function openCreate() {
  formApi.setData(null).open();
}

function openEdit(row: MesSaleOrderApi.MesSaleOrder) {
  formApi.setData({ id: row.id }).open();
}

function openDetail(row: MesSaleOrderApi.MesSaleOrder) {
  formApi.setData({ id: row.id, readonly: true }).open();
}

function rowActions(row: MesSaleOrderApi.MesSaleOrder) {
  return [
    { label: '查阅', type: 'link', icon: ACTION_ICON.VIEW, onClick: () => openDetail(row) },
    {
      label: '修改',
      type: 'link',
      icon: ACTION_ICON.EDIT,
      disabled: row.status !== 'DRAFT',
      onClick: () => openEdit(row),
    },
  ];
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    schema: [
      {
        fieldName: 'orderNo',
        label: '销售订单号',
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入销售订单号' },
      },
      {
        fieldName: 'erpNo',
        label: PLAN_FIELD_COPY.erpNo,
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入ERP编号' },
      },
      {
        fieldName: 'customerName',
        label: '客户名称',
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入客户名称' },
      },
      {
        fieldName: 'productCode',
        label: PLAN_FIELD_COPY.productionMaterial,
        component: 'Input',
        componentProps: { allowClear: true, placeholder: PLAN_PLACEHOLDER_COPY.productionMaterial },
      },
      {
        fieldName: 'status',
        label: '状态',
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: [
            { label: '草稿', value: 'DRAFT' },
            { label: '已审核', value: 'APPROVED' },
            { label: '部分计划', value: 'PART_PLANNED' },
            { label: '已完成', value: 'COMPLETED' },
          ],
          placeholder: '请选择状态',
        },
      },
    ],
    wrapperClass: 'grid-cols-5',
    actionWrapperClass: 'col-span-5 text-right',
  },
  gridOptions: {
    columns: [
      { type: 'checkbox', width: 48, fixed: 'left' },
      { field: 'orderNo', title: '销售订单号', minWidth: 170, fixed: 'left' },
      { field: 'erpNo', title: PLAN_FIELD_COPY.erpNo, minWidth: 140 },
      { field: 'customerName', title: '客户', minWidth: 140 },
      { field: 'materialCode', title: PLAN_FIELD_COPY.productionMaterial, minWidth: 160 },
      { field: 'materialName', title: PLAN_FIELD_COPY.materialName, minWidth: 220 },
      { field: 'modelCode', title: PLAN_FIELD_COPY.productModel, minWidth: 140 },
      { field: 'sizeName', title: PLAN_FIELD_COPY.sizeSpec, width: 100, align: 'center' },
      { field: 'quantity', title: '订单量', width: 120, align: 'right', slots: { default: 'quantity' } },
      { field: 'plannedQty', title: '计划进度', minWidth: 180, slots: { default: 'progress' } },
      { field: 'remainQty', title: '欠交量', width: 120, align: 'right', slots: { default: 'remainQty' } },
      { field: 'deliveryDate', title: '交货日期', width: 120, align: 'center' },
      { field: 'statusName', title: '状态', width: 110, align: 'center', slots: { default: 'status' } },
      { field: 'auditorName', title: '审核人', width: 110, align: 'center' },
      { field: 'auditTime', title: '审核时间', width: 170, align: 'center' },
      { title: '操作', width: 250, fixed: 'right', slots: { default: 'actions' } },
    ],
    height: 'auto',
    pagerConfig: { enabled: true },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getSaleOrderPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  },
});
</script>

<template>
  <Page auto-content-height>
    <SaleOrderForm @success="refresh" />
    <div class="sale-order-list-page">
      <Grid table-title="销售订单台账（生产计划需求源）">
        <template #toolbar-tools>
          <TableAction
            :actions="[
              { label: '新增草稿', type: 'primary', icon: ACTION_ICON.ADD, onClick: openCreate },
            ]"
          />
        </template>
        <template #quantity="{ row }">
          <span>{{ row.quantity }} {{ row.unitCode || row.unit || '' }}</span>
        </template>
        <template #progress="{ row }">
          <div class="flex w-full items-center gap-2">
            <Progress
              :percent="Number(((Number(row.plannedQty || 0) / Math.max(Number(row.quantity || 0), 1)) * 100).toFixed(1))"
              size="small"
              :status="Number(row.plannedQty || 0) >= Number(row.quantity || 0) ? 'success' : 'active'"
              class="flex-1"
            />
            <span class="text-xs text-muted-foreground">
              {{ row.plannedQty || 0 }}/{{ row.quantity || 0 }}
            </span>
          </div>
        </template>
        <template #remainQty="{ row }">
          <span class="font-semibold text-amber-600">{{ row.remainQty || 0 }} {{ row.unitCode || row.unit || '' }}</span>
        </template>
        <template #status="{ row }">
          <Tag :color="statusColor(row.status)">{{ row.statusName || row.status }}</Tag>
        </template>
        <template #actions="{ row }">
          <TableAction :actions="rowActions(row)" />
        </template>
      </Grid>
    </div>
  </Page>
</template>

<style scoped>
.sale-order-list-page {
  height: 100%;
  min-height: 0;
}

:deep(.vben-vxe-grid) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.vxe-grid) {
  height: 100%;
}

:deep(.vxe-grid--layout-wrapper) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

:deep(.vxe-grid--layout-body-wrapper) {
  flex: 1;
  min-height: 0;
}

:deep(.vxe-grid--pager-wrapper),
:deep(.vxe-pager) {
  flex-shrink: 0;
}
</style>
