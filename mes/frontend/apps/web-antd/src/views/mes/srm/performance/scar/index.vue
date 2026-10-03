<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud, todayText } from '../../shared/documentCrud';
import { supplierReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const valueLabels = {
  CLOSED: '已关闭',
  QA_REVIEW: '质量审核',
  VERIFYING: '验证中',
  WAIT_SUPPLIER: '待供应商回复',
};
const crud = createSrmDocumentCrud({
  bizType: 'SRM_SCAR',
  codeField: 'scarNo',
  createDefaults: () => ({
    dueDate: todayText(),
    status: 'WAIT_SUPPLIER',
  }),
  docNoPrefix: 'SCAR',
  titleField: 'supplierName',
});

const detailFields: SrmCrudField[] = [
  { field: 'scarNo', label: 'SCAR 整改单号' },
  {
    field: 'supplierName',
    label: '供应商名称',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  {
    field: 'source',
    label: '异常来源',
    component: 'Select',
    options: [
      { label: '绩效C/D级', value: '绩效C/D级' },
      { label: 'IQC拒收', value: 'IQC拒收' },
      { label: '产线不良', value: '产线不良' },
      { label: '重大客诉', value: '重大客诉' },
    ],
  },
  {
    field: 'level',
    label: '严重等级',
    component: 'Select',
    options: [
      { label: '轻微', value: 'MINOR' },
      { label: '严重', value: 'MAJOR' },
      { label: '致命', value: 'CRITICAL' },
    ],
  },
  { field: 'applyDept', label: '发起部门' },
  {
    field: 'status',
    label: '单据状态',
    component: 'Select',
    options: [
      { label: '供应商回复中', value: 'WAIT_SUPPLIER' },
      { label: '质量审核', value: 'QA_REVIEW' },
      { label: '验证中', value: 'VERIFYING' },
      { label: '已关闭', value: 'CLOSED' },
    ],
  },
  { field: 'dueDate', label: '要求回复期限', component: 'DatePicker' },
  { field: 'issueDesc', label: '异常问题描述', component: 'Textarea', span: 3 },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增整改记录"
    entity-name="供应商整改记录"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="供方异常与整改"
    :value-labels="valueLabels"
  />
</template>
