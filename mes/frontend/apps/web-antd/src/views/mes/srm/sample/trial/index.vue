<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud, todayText } from '../../shared/documentCrud';
import { materialReference, supplierReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const crud = createSrmDocumentCrud({
  bizType: 'SRM_TRIAL_TRACK',
  codeField: 'trialNo',
  createDefaults: () => ({
    status: 'DRAFT',
    trialDate: todayText(),
  }),
  docNoPrefix: 'TRIAL',
  titleField: 'supplierName',
});

const detailFields: SrmCrudField[] = [
  { field: 'trialNo', label: '试产追踪单号' },
  {
    field: 'supplierName',
    label: '供应商名称',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  {
    field: 'materialName',
    label: '试产物料',
    component: 'ReferencePicker',
    reference: materialReference(),
  },
  { field: 'trialQty', label: '试产批量' },
  { field: 'lineYield', label: '综合良率', component: 'InputNumber' },
  { field: 'applyDept', label: '发起部门' },
  { field: 'applicant', label: '发起人' },
  { field: 'trialDate', label: '试产日期', component: 'DatePicker' },
  {
    field: 'status',
    label: '单据状态',
    component: 'Select',
    options: [
      { label: '草稿', value: 'DRAFT' },
      { label: '试产评估中', value: 'APPROVING' },
      { label: '试产失败', value: 'REJECTED' },
      { label: '试产合格', value: 'PASSED' },
    ],
  },
  { field: 'trialSummary', label: '试产总结', component: 'Textarea', span: 3 },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增试产记录"
    entity-name="试产验证记录"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="样品试产验证跟踪"
  />
</template>
