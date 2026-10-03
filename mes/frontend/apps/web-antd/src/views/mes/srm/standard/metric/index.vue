<script lang="ts" setup>
import type { SrmCrudField } from '../../shared/crud';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { createSrmDocumentCrud } from '../../shared/documentCrud';
import { useGridColumns, useGridFormSchema } from './data';

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();
const crud = createSrmDocumentCrud({
  bizType: 'SRM_STANDARD_METRIC',
  codeField: 'code',
  defaultStatus: 0,
  docNoPrefix: 'KPI',
  titleField: 'name',
});

const detailFields: SrmCrudField[] = [
  { field: 'code', label: '指标编码' },
  { field: 'name', label: '指标题干/名称' },
  {
    field: 'category',
    label: '维度',
    component: 'Select',
    options: [
      { label: '质量', value: '质量' },
      { label: '交付', value: '交付' },
      { label: '成本', value: '成本' },
      { label: '服务', value: '服务' },
      { label: '技术', value: '技术' },
      { label: '体系', value: '体系' },
      { label: 'EHS', value: 'EHS' },
      { label: 'IT', value: 'IT' },
      { label: '管理', value: '管理' },
    ],
  },
  {
    field: 'type',
    label: '考核模式',
    component: 'Select',
    options: [
      { label: '定量(系统抓取)', value: 1 },
      { label: '定性(人工阅卷)', value: 2 },
      { label: '红线(一票否决)', value: 3 },
    ],
  },
  {
    field: 'status',
    label: '状态',
    component: 'Select',
    options: [
      { label: '启用', value: 0 },
      { label: '停用', value: 2 },
    ],
  },
  { field: 'dataSource', label: '客观数据来源/证据要求', component: 'Textarea', span: 3 },
  { field: 'scoringMethod', label: '评分规则与计算逻辑', component: 'Textarea', rows: 4, span: 3 },
];
</script>

<template>
  <SrmCrudPrototypePage
    v-bind="crud"
    create-text="新增考核指标"
    entity-name="考核指标"
    :columns="gridColumns"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    module-name="供应商绩效考评指标"
  />
</template>
