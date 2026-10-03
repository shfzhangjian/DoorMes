<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud } from '../../shared/documentCrud';
import { materialReference, supplierReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const valueLabels = {
  PASSED: '评估合格',
  REJECTED: '样品不合格',
  TESTING: '测试评估中',
};
const crud = createSrmDocumentCrud({
  bizType: 'SRM_SAMPLE_EVALUATE',
  codeField: 'sampleNo',
  defaultStatus: 'TESTING',
  docNoPrefix: 'SMP',
  titleField: 'supplierName',
});

const detailFields: SrmCrudField[] = [
  { field: 'sampleNo', label: '送样评估单号' },
  {
    field: 'supplierName',
    label: '供应商名称',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  {
    field: 'materialName',
    label: '样品品名及规格',
    component: 'ReferencePicker',
    reference: materialReference(),
  },
  { field: 'sampleQty', label: '送样数量' },
  {
    field: 'verifyType',
    label: '验证类别',
    component: 'Select',
    options: [
      { label: '新品开发', value: '新品开发' },
      { label: '产品改进', value: '产品改进' },
      { label: '增加供应商', value: '增加供应商' },
    ],
  },
  { field: 'finalScore', label: '综合得分', component: 'InputNumber' },
  {
    field: 'status',
    label: '综合结论',
    component: 'Select',
    options: [
      { label: '测试评估中', value: 'TESTING' },
      { label: '评估合格', value: 'PASSED' },
      { label: '样品不合格', value: 'REJECTED' },
    ],
  },
  { field: 'reviewOpinion', label: '评审意见', component: 'Textarea', span: 3 },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增送样评价"
    entity-name="送样评价记录"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="送样评价"
    :value-labels="valueLabels"
  />
</template>
