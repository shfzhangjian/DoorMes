<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcLotInstanceApi } from '#/api/mes/hc/lotinstance';

import { Page, useVbenModal } from '@vben/common-ui';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getLotInstancePage } from '#/api/mes/hc/lotinstance';

import LotInstanceDetailModal from './modules/detail-modal.vue';

const productCategoryOptions = [
  { label: '白垫', value: 'WHITE_PAD' },
  { label: '黑垫', value: 'BLACK_PAD' },
];
const instanceStatusOptions = [
  { label: '已预占', value: 'RESERVED' },
  { label: '已生成', value: 'GENERATED' },
  { label: '已生成（历史）', value: 'ACTIVE' },
  { label: '已关闭', value: 'CLOSED' },
  { label: '已取消', value: 'CANCELLED' },
];
const batchLevelOptions = [
  { label: '主批', value: 'ROOT' },
  { label: '工序批', value: 'OPERATION' },
  { label: '片号', value: 'PIECE' },
];
const generateSourceOptions = [
  { label: '计划预览', value: 'PLAN_PREVIEW' },
  { label: '配方开工', value: 'FORMULA_REPORT' },
  { label: '工序报工', value: 'OPERATION_REPORT' },
];

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: LotInstanceDetailModal,
  destroyOnClose: true,
});

function showDetail(row: MesHcLotInstanceApi.LotInstance) {
  detailModalApi.setData(row).open();
}

function labelOf(options: Array<{ label: string; value: string }>, value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

const formSchema: VbenFormSchema[] = [
  { fieldName: 'lotNo', label: '生产批号', component: 'Input', componentProps: { placeholder: '请输入批号' } },
  { fieldName: 'planNo', label: '生产计划号', component: 'Input', componentProps: { placeholder: '请输入计划号' } },
  { fieldName: 'materialKeyword', label: '物料', component: 'Input', componentProps: { placeholder: '编码或名称' } },
  { fieldName: 'productCategoryCode', label: '垫型', component: 'Select', componentProps: { options: productCategoryOptions, allowClear: true } },
  { fieldName: 'modelCode', label: '产品型号', component: 'Input', componentProps: { placeholder: '请输入产品型号' } },
  { fieldName: 'instanceStatus', label: '生成状态', component: 'Select', componentProps: { options: instanceStatusOptions, allowClear: true } },
];

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: formSchema },
  gridOptions: {
    columns: [
      { field: 'productionBatchNo', title: '生产批号', minWidth: 160, formatter: ({ row }) => row.productionBatchNo || row.lotNo || '-' },
      { field: 'productCategoryCode', title: '垫型', width: 100, formatter: ({ cellValue }) => labelOf(productCategoryOptions, cellValue) },
      { field: 'ruleName', title: '规则名称', minWidth: 180, formatter: ({ row }) => row.ruleName || row.ruleCode || '-' },
      { field: 'ruleVersion', title: '规则版本', width: 92, formatter: ({ cellValue }) => (cellValue ? `V${cellValue}` : '-') },
      { field: 'planNo', title: '生产计划号', minWidth: 150 },
      { field: 'materialName', title: '物料', minWidth: 180, formatter: ({ row }) => [row.materialCode, row.materialName].filter(Boolean).join(' / ') || '-' },
      { field: 'modelCode', title: '产品型号', minWidth: 130 },
      { field: 'batchLevel', title: '批次层级', width: 108, formatter: ({ cellValue }) => labelOf(batchLevelOptions, cellValue) },
      { field: 'parentProductionBatchNo', title: '上游批号', minWidth: 150, formatter: ({ row }) => row.parentProductionBatchNo || row.parentLotNo || '-' },
      { field: 'lineName', title: '生产线', minWidth: 130, formatter: ({ row }) => [row.lineCode, row.lineName].filter(Boolean).join(' / ') || '-' },
      { field: 'instanceStatus', title: '生成状态', width: 108, formatter: ({ cellValue }) => labelOf(instanceStatusOptions, cellValue) },
      { field: 'generateSource', title: '生成来源', minWidth: 130, formatter: ({ cellValue }) => labelOf(generateSourceOptions, cellValue) },
      { field: 'generatedTime', title: '生成时间', minWidth: 170 },
      { field: 'operatorName', title: '操作人', minWidth: 110 },
      { title: '操作', width: 90, fixed: 'right', slots: { default: 'actions' } },
    ],
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => await getLotInstancePage({
          pageNo: page.currentPage,
          pageSize: page.pageSize,
          ...formValues,
        }),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesHcLotInstanceApi.LotInstance>,
});
</script>

<template>
  <Page auto-content-height>
    <DetailModal />
    <Grid table-title="批次实例台账">
      <template #actions="{ row }">
        <TableAction :actions="[{ label: '详情', type: 'link', onClick: showDetail.bind(null, row) }]" />
      </template>
    </Grid>
  </Page>
</template>
