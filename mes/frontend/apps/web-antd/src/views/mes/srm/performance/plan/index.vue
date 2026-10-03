<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud, todayText } from '../../shared/documentCrud';
import { templateReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const valueLabels = { 0: '草稿', 1: '进行中', 2: '已完结' };
const crud = createSrmDocumentCrud({
  bizType: 'SRM_PERFORMANCE_PLAN',
  codeField: 'planNo',
  createDefaults: () => ({
    deadline: todayText(),
    status: 0,
  }),
  docNoPrefix: 'SRM-PLAN',
  titleField: 'title',
});

const detailFields: SrmCrudField[] = [
  { field: 'planNo', label: '计划单号' },
  { field: 'title', label: '计划标题' },
  {
    field: 'templateName',
    label: '考核采用模板',
    component: 'ReferencePicker',
    reference: templateReference(),
  },
  {
    field: 'periodType',
    label: '考核周期',
    component: 'Select',
    options: [
      { label: '季度考核', value: 'QUARTER' },
      { label: '年度考核', value: 'YEAR' },
      { label: '专项稽核', value: 'AUDIT' },
    ],
  },
  { field: 'evalYear', label: '考核年份', component: 'InputNumber' },
  {
    field: 'evalQuarter',
    label: '考核季度',
    component: 'Select',
    options: [
      { label: 'Q1', value: 1 },
      { label: 'Q2', value: 2 },
      { label: 'Q3', value: 3 },
      { label: 'Q4', value: 4 },
    ],
  },
  { field: 'deadline', label: '打分截止日期', component: 'DatePicker' },
  {
    field: 'status',
    label: '计划状态',
    component: 'Select',
    options: [
      { label: '草稿', value: 0 },
      { label: '进行中', value: 1 },
      { label: '已完结', value: 2 },
    ],
  },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增评审计划"
    entity-name="评审计划"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="供方评审计划"
    :value-labels="valueLabels"
  />
</template>
