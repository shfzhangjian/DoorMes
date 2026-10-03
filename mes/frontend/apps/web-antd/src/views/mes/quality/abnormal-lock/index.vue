<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesQmsAbnormalLockApi } from '#/api/mes/quality/abnormal-lock';

import { Page } from '@vben/common-ui';

import { Tag } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getAbnormalLockPage } from '#/api/mes/quality/abnormal-lock';
import { getRangePickerDefaultProps } from '#/utils';

defineOptions({ name: 'MesQmsAbnormalLock' });

const sourceTypeOptions = [
  { label: '首检/FAI', value: 'FAI' },
  { label: '胶板检', value: 'GLUE_BOARD_FAI' },
  { label: '过程加检/IPQC', value: 'IPQC' },
  { label: '成品检/FQC', value: 'FQC' },
];

const scopeOptions = [
  { label: '原料批次', value: 'SOURCE_BATCH' },
  { label: '下游批次', value: 'DOWNSTREAM_BATCH' },
  { label: '胶板关联', value: 'GLUE_BOARD' },
];

const statusOptions = [
  { label: '锁定中', value: 'LOCKED' },
  { label: '已释放', value: 'RELEASED' },
  { label: '已取消', value: 'CANCELED' },
];

function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'inspectionOrderNo', label: '检验单号', component: 'Input', componentProps: { placeholder: '输入检验单号' } },
    { fieldName: 'affectedBatchNo', label: '受影响批次', component: 'Input', componentProps: { placeholder: '输入批次号' } },
    { fieldName: 'glueBoardBatchNo', label: '胶板批次', component: 'Input', componentProps: { placeholder: '输入胶板批次' } },
    { fieldName: 'lockSourceType', label: '锁定来源', component: 'Select', componentProps: { options: sourceTypeOptions, allowClear: true } },
    { fieldName: 'lockScope', label: '锁定范围', component: 'Select', componentProps: { options: scopeOptions, allowClear: true } },
    { fieldName: 'lockStatus', label: '锁定状态', component: 'Select', componentProps: { options: statusOptions, allowClear: true } },
    { fieldName: 'planNo', label: '工单/计划', component: 'Input', componentProps: { placeholder: '输入工单或计划号' } },
    { fieldName: 'operationName', label: '工序', component: 'Input', componentProps: { placeholder: '输入工序名称' } },
    { fieldName: 'lockTime', label: '锁定时间', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps() } },
  ];
}

const [Grid] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    schema: useGridFormSchema(),
  },
  gridOptions: {
    columns: [
      { type: 'seq', width: 50, align: 'center', fixed: 'left' },
      { field: 'inspectionOrderNo', title: '检验单号', width: 170, fixed: 'left' },
      { field: 'lockSourceType', title: '来源', width: 120, align: 'center', slots: { default: 'lockSourceType' } },
      { field: 'lockScope', title: '范围', width: 120, align: 'center', slots: { default: 'lockScope' } },
      { field: 'lockStatus', title: '状态', width: 100, align: 'center', slots: { default: 'lockStatus' } },
      { field: 'affectedBatchNo', title: '受影响批次', minWidth: 170 },
      { field: 'sourceBatchNo', title: '来源批次', minWidth: 150 },
      { field: 'parentProductionBatchNo', title: '父级批次', minWidth: 150 },
      { field: 'productionBatchNo', title: '生产批次', minWidth: 150 },
      { field: 'glueBoardBatchNo', title: '胶板批次', minWidth: 140 },
      { field: 'operationName', title: '工序', width: 110 },
      { field: 'planNo', title: '工单/计划', minWidth: 150 },
      { field: 'inspectionSubmitTime', title: '送检时间', width: 170 },
      { field: 'ngConfirmTime', title: '确认NG时间', width: 170 },
      { field: 'lockTime', title: '落锁时间', width: 170 },
      { field: 'lockReason', title: '锁定原因', minWidth: 240 },
      { field: 'affectedSourceTable', title: '来源表', minWidth: 180 },
    ],
    height: 'auto',
    keepSource: true,
    rowConfig: { isHover: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getAbnormalLockPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  } as VxeTableGridOptions<MesQmsAbnormalLockApi.AbnormalLockRecord>,
});

function optionText(options: Array<{ label: string; value: string }>, value?: string) {
  return options.find((item) => item.value === value)?.label || value || '-';
}

function statusColor(status?: string) {
  if (status === 'LOCKED') return 'error';
  if (status === 'RELEASED') return 'success';
  if (status === 'CANCELED') return 'default';
  return 'processing';
}
</script>

<template>
  <Page auto-content-height>
    <Grid>
      <template #lockSourceType="{ row }">
        <Tag color="blue">{{ optionText(sourceTypeOptions, row.lockSourceType) }}</Tag>
      </template>
      <template #lockScope="{ row }">
        <Tag color="purple">{{ optionText(scopeOptions, row.lockScope) }}</Tag>
      </template>
      <template #lockStatus="{ row }">
        <Tag :color="statusColor(row.lockStatus)">
          {{ optionText(statusOptions, row.lockStatus) }}
        </Tag>
      </template>
    </Grid>
  </Page>
</template>
