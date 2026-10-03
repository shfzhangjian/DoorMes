<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesSupplierApi } from '#/api/mes/supplier';

import { h } from 'vue';

import { Page } from '@vben/common-ui';

import { Modal } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getSupplier, getSupplierPage } from '#/api/mes/supplier';

defineOptions({ name: 'MesQualityIqcSupplierRoster' });

const supplierStatusOptions = [
  { label: '考察中', value: 'PENDING' },
  { label: '合格', value: 'QUALIFIED' },
  { label: '冻结', value: 'FROZEN' },
  { label: '淘汰', value: 'ELIMINATED' },
  { label: '退出', value: 'EXITED' },
  { label: '不合格', value: 'UNQUALIFIED' },
];

const supplierStatusLabelMap = supplierStatusOptions.reduce<Record<string, string>>(
  (map, item) => {
    map[item.value] = item.label;
    return map;
  },
  {},
);

const searchFormFields = [
  'supplierInfo',
  'materialCode',
  'applicableProduct',
  'model',
  'providedProduct',
  'status',
];

const detailFields: Array<{
  field: keyof MesSupplierApi.Supplier;
  label: string;
  render?: (value: any) => string;
}> = [
  { field: 'supplierCode', label: '供应商编码' },
  { field: 'supplierName', label: '供应商名称' },
  { field: 'materialCode', label: '物料代码' },
  { field: 'applicableProduct', label: '适用产品' },
  { field: 'model', label: '型号' },
  { field: 'providedProduct', label: '提供/协作产品' },
  { field: 'status', label: '供应商状态', render: getSupplierStatusLabel },
];

function getSupplierStatusLabel(value?: string) {
  return value ? supplierStatusLabelMap[value] || value : '-';
}

function buildSearchFormSchema(): VbenFormSchema[] {
  return [
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入供应商编码或名称' },
      fieldName: 'supplierInfo',
      label: '供应商信息',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入物料代码' },
      fieldName: 'materialCode',
      label: '物料代码',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入适用产品' },
      fieldName: 'applicableProduct',
      label: '适用产品',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入型号' },
      fieldName: 'model',
      label: '型号',
    },
    {
      component: 'Input',
      componentProps: { allowClear: true, placeholder: '请输入提供/协作产品' },
      fieldName: 'providedProduct',
      label: '提供/协作产品',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        options: supplierStatusOptions,
        placeholder: '请选择供应商状态',
      },
      fieldName: 'status',
      label: '供应商状态',
    },
  ];
}

function buildGridColumns(): VxeTableGridOptions<MesSupplierApi.Supplier>['columns'] {
  return [
    {
      field: 'supplierCode',
      fixed: 'left',
      minWidth: 140,
      title: '供应商编码',
    },
    {
      field: 'supplierName',
      fixed: 'left',
      minWidth: 180,
      title: '供应商名称',
    },
    { field: 'materialCode', minWidth: 140, title: '物料代码' },
    { field: 'applicableProduct', minWidth: 180, title: '适用产品' },
    { field: 'model', minWidth: 140, title: '型号' },
    { field: 'providedProduct', minWidth: 180, title: '提供/协作产品' },
    {
      align: 'center',
      field: 'status',
      formatter: ({ cellValue }) => getSupplierStatusLabel(cellValue),
      minWidth: 120,
      title: '供应商状态',
    },
    {
      fixed: 'right',
      slots: { default: 'actions' },
      title: '操作',
      width: 100,
    },
  ];
}

function getCollapsedKeepCount() {
  if (window.innerWidth < 768) return 1;
  if (window.innerWidth < 1024) return 2;
  return 3;
}

function handleSearchCollapsedChange(collapsed: boolean) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  gridApi.formApi.updateSchema(
    searchFormFields.map((fieldName) => ({
      fieldName,
      hide: collapsed && !keepFields.has(fieldName),
    })),
  );
}

async function handleView(row: MesSupplierApi.Supplier) {
  const record = row.id ? await getSupplier(row.id) : row;
  Modal.info({
    content: h(
      'div',
      { class: 'grid grid-cols-2 gap-x-4 gap-y-3 pt-2 text-sm' },
      detailFields.map((item) =>
        h('div', { class: 'flex min-w-0 border-b border-gray-100 pb-2' }, [
          h('span', { class: 'mr-3 shrink-0 text-gray-500' }, `${item.label}：`),
          h(
            'span',
            { class: 'min-w-0 break-all text-gray-900' },
            item.render
              ? item.render(record[item.field])
              : String(record[item.field] ?? '-'),
          ),
        ]),
      ),
    ),
    icon: null,
    title: '供应商名录详情',
    width: 760,
  });
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildSearchFormSchema(),
  },
  gridOptions: {
    columns: buildGridColumns(),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          return await getSupplierPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          });
        },
      },
    },
    rowConfig: {
      keyField: 'id',
      isHover: true,
    },
    toolbarConfig: {
      refresh: true,
      search: true,
    },
  } as VxeTableGridOptions<MesSupplierApi.Supplier>,
});
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="供应商名录">
      <template #actions="{ row }">
        <TableAction
          :actions="[
            {
              label: '查看',
              type: 'link',
              icon: ACTION_ICON.VIEW,
              onClick: handleView.bind(null, row),
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>
