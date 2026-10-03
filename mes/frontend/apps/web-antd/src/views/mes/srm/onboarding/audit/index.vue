<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud } from '../../shared/documentCrud';
import { materialReference, supplierReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const crud = createSrmDocumentCrud({
  bizType: 'SRM_SELECTION_AUDIT',
  codeField: 'selectionNo',
  defaultStatus: 'DRAFT',
  docNoPrefix: 'SRM-SEL',
  titleField: 'supplierName',
});

const detailFields: SrmCrudField[] = [
  { field: 'selectionNo', label: '定标单号' },
  {
    field: 'supplierName',
    label: '供应商名称',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  {
    field: 'materialName',
    label: '采购项目/物料',
    component: 'ReferencePicker',
    reference: materialReference(),
  },
  { field: 'finalScore', label: '定标综合得分', component: 'InputNumber' },
  {
    field: 'status',
    label: '单据状态',
    component: 'Select',
    options: [
      { label: '草稿', value: 'DRAFT' },
      { label: '审批中', value: 'APPROVING' },
      { label: '已退回', value: 'REJECTED' },
      { label: '已通过', value: 'PASSED' },
    ],
  },
  { field: 'applicant', label: '发起人' },
  { field: 'applyDept', label: '发起部门' },
  { field: 'applyTime', label: '发起时间', component: 'DateTimePicker' },
  { field: 'finalSummary', label: '定标结论说明', component: 'Textarea', span: 3 },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增定标选择书"
    entity-name="定标选择书"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="供应商选择与定标"
  />
</template>
