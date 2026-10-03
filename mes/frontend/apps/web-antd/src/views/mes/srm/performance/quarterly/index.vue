<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud } from '../../shared/documentCrud';
import { supplierReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const valueLabels = {
  APPROVING: '评定中',
  DRAFT: '草稿',
  FINISHED: '已归档',
  REJECTED: '已退回',
};
const crud = createSrmDocumentCrud({
  bizType: 'SRM_PERFORMANCE_QUARTERLY',
  codeField: 'reportNo',
  defaultStatus: 'DRAFT',
  docNoPrefix: 'SRM-QTR',
  titleField: 'supplierName',
});

const detailFields: SrmCrudField[] = [
  { field: 'reportNo', label: '报告编号' },
  {
    field: 'supplierName',
    label: '供应商名称',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  { field: 'evalYear', label: '年份', component: 'InputNumber' },
  {
    field: 'evalQuarter',
    label: '季度',
    component: 'Select',
    options: [
      { label: 'Q1', value: 1 },
      { label: 'Q2', value: 2 },
      { label: 'Q3', value: 3 },
      { label: 'Q4', value: 4 },
    ],
  },
  { field: 'totalScore', label: '季度综合得分', component: 'InputNumber' },
  { field: 'evalGrade', label: '综合评级' },
  {
    field: 'status',
    label: '评定状态',
    component: 'Select',
    options: [
      { label: '草稿', value: 'DRAFT' },
      { label: '评定中', value: 'APPROVING' },
      { label: '已退回', value: 'REJECTED' },
      { label: '已归档', value: 'FINISHED' },
    ],
  },
  { field: 'summary', label: '综合评定说明', component: 'Textarea', span: 3 },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增季度评定"
    entity-name="季度绩效评定"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="季度综合绩效评定"
    :value-labels="valueLabels"
  />
</template>
