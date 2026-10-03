<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud } from '../../shared/documentCrud';
import {
  performancePlanReference,
  supplierReference,
} from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const valueLabels = { 0: '待评价', 1: '评价中', 2: '已评价' };
const crud = createSrmDocumentCrud({
  bizType: 'SRM_PERFORMANCE_MONTHLY',
  codeField: 'recordNo',
  defaultStatus: 0,
  docNoPrefix: 'SRM-MON',
  titleField: 'supplierName',
});

const detailFields: SrmCrudField[] = [
  { field: 'recordNo', label: '评分单号' },
  {
    field: 'supplierName',
    label: '被考评供应商',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  {
    field: 'planTitle',
    label: '所属计划名称',
    component: 'ReferencePicker',
    reference: performancePlanReference(),
  },
  {
    field: 'status',
    label: '阅卷进度',
    component: 'Select',
    options: [
      { label: '待评价', value: 0 },
      { label: '评价中', value: 1 },
      { label: '已评价', value: 2 },
    ],
  },
  { field: 'totalScore', label: '最终总分', component: 'InputNumber' },
  { field: 'evalGrade', label: '评级' },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增月度评价"
    entity-name="月度评价记录"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="供应商月度评定"
    :value-labels="valueLabels"
  />
</template>
