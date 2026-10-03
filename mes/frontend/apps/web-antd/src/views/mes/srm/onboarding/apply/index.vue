<script lang="ts" setup>
import type { SrmCrudField, SrmCrudRecord } from '../../shared/crud';

import {
  createOnboardingApply,
  deleteOnboardingApply,
  getOnboardingApply,
  getOnboardingApplyPage,
  updateOnboardingApply,
} from '#/api/mes/srm/onboarding-apply';

import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { materialReference, supplierReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'SrmOnboardingApply' });

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();

const applyReasonOptions = [
  { label: '产能不满足', value: '产能不满足' },
  { label: '品质需提升', value: '品质需提升' },
  { label: '降本需求', value: '降本需求' },
  { label: '开发二供三供', value: '开发二供三供' },
  { label: '客户指定', value: '客户指定' },
];
const companyNatureOptions = [
  { label: '生产厂家(原厂)', value: 'MANUFACTURER' },
  { label: '贸易/代理商', value: 'AGENT' },
];
const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '审批中', value: 'APPROVING' },
  { label: '已通过', value: 'PASSED' },
  { label: '已退回', value: 'REJECTED' },
];

const detailFields: SrmCrudField[] = [
  { field: 'applyNo', label: '申请流水号' },
  {
    field: 'supplierCode',
    label: '供应商代码',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  {
    field: 'supplierName',
    label: '推荐意向供应商',
    component: 'ReferencePicker',
    reference: supplierReference(),
  },
  {
    field: 'materialName',
    label: '寻源物料/品类',
    component: 'ReferencePicker',
    reference: materialReference(),
  },
  {
    field: 'applyReason',
    label: '导入原因',
    component: 'Select',
    options: applyReasonOptions,
  },
  {
    field: 'natureRequirement',
    label: '企业性质要求',
    component: 'Select',
    options: companyNatureOptions,
  },
  { field: 'specReq', label: '规格/技术要求', component: 'Textarea', span: 3 },
  { field: 'certRequirement', label: '资质要求', component: 'Textarea', span: 3 },
  { field: 'status', label: '单据状态', component: 'Select', options: statusOptions },
  { field: 'applicantName', label: '发起人' },
  { field: 'applyDept', label: '发起部门' },
  { field: 'applyTime', label: '发起时间', component: 'DateTimePicker' },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];

const valueLabels = {
  AGENT: '贸易/代理商',
  APPROVING: '审批中',
  DRAFT: '草稿',
  MANUFACTURER: '生产厂家(原厂)',
  PASSED: '已通过',
  REJECTED: '已退回',
};

function createDefaults(): SrmCrudRecord {
  return {
    applyNo: buildNo('SRM-IN'),
    applyTime: nowText(),
    status: 'DRAFT',
  };
}

function loadPage(params: SrmCrudRecord) {
  return getOnboardingApplyPage(params);
}

function getDetail(id: number | string) {
  return getOnboardingApply(Number(id)) as Promise<SrmCrudRecord>;
}

function createRecord(record: SrmCrudRecord) {
  return createOnboardingApply(record as any);
}

function updateRecord(record: SrmCrudRecord) {
  return updateOnboardingApply(record as any);
}

function deleteRecord(id: number | string) {
  return deleteOnboardingApply(Number(id));
}

function buildNo(prefix: string) {
  const date = new Date();
  return `${prefix}-${date.getFullYear()}${pad2(date.getMonth() + 1)}${pad2(date.getDate())}${pad2(date.getHours())}${pad2(date.getMinutes())}${pad2(date.getSeconds())}`;
}

function nowText() {
  const date = new Date();
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}
</script>

<template>
  <SrmCrudPrototypePage
    attachment-biz-type="ONBOARDING_APPLY"
    code-field="applyNo"
    :columns="gridColumns"
    :create-defaults="createDefaults"
    :create-record="createRecord"
    create-text="新增导入申请"
    :delete-record="deleteRecord"
    entity-name="供应商导入申请"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    :get-detail="getDetail"
    :load-page="loadPage"
    module-name="供应商导入申请"
    title-field="supplierName"
    :update-record="updateRecord"
    :value-labels="valueLabels"
  />
</template>
