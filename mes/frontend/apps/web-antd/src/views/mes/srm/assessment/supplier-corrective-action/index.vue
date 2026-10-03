<script lang="ts" setup>
import type { SrmCrudField, SrmCrudRecord } from '../../shared/crud';

import {
  createScar,
  deleteScar,
  getScar,
  getScarPage,
  updateScar,
} from '#/api/mes/srm/scar';

import { materialReference, supplierReference } from '../../shared/referenceFields';
import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'SrmAssessmentSupplierCorrectiveAction' });

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();

const candidateSupplierReference = supplierReference({
  supplierSource: 'roster',
  title: '选择供应商',
});
const materialReferenceConfig = materialReference('选择物料');
const scarAttachmentCategoryOptions = [
  { label: '异常证据材料', value: 'SCAR_ISSUE' },
  { label: '供方回复材料', value: 'SCAR_REPLY' },
  { label: '其他附件', value: 'OTHER' },
];

const detailFields: SrmCrudField[] = [
  { field: 'scarNo', label: '台账编号', readonly: true },
  { component: 'DatePicker', field: 'issueDate', label: '异常发生日期' },
  {
    component: 'ReferencePicker',
    field: 'supplierCode',
    label: '供应商代码',
    reference: candidateSupplierReference,
  },
  {
    allowManualInput: true,
    component: 'ReferencePicker',
    field: 'supplierName',
    label: '供应商名称',
    manualClearFields: ['supplierCode'],
    reference: candidateSupplierReference,
  },
  {
    component: 'ReferencePicker',
    field: 'materialCode',
    label: '物料代码',
    reference: materialReferenceConfig,
  },
  { field: 'materialName', label: '物料名称', readonly: true },
  { field: 'materialModel', label: '物料型号', readonly: true },
  { field: 'batchNo', label: '物料批次' },
  { component: 'InputNumber', field: 'quantity', label: '数量' },
  {
    component: 'Textarea',
    field: 'issueDesc',
    label: '异常描述',
    rows: 4,
    span: 3,
  },
  { component: 'DatePicker', field: 'replyDate', label: '异常回复日期' },
  {
    component: 'Textarea',
    field: 'replyDesc',
    label: '异常回复说明',
    rows: 4,
    span: 3,
  },
  { field: 'applicantName', label: '登记人', readonly: true },
  {
    component: 'DateTimePicker',
    field: 'applyTime',
    label: '登记时间',
    readonly: true,
  },
];

const valueLabels = {
  CLOSED: '已整改关闭',
  WAIT_SUPPLIER: '待供方回复',
};

function createDefaults(): SrmCrudRecord {
  return {
    issueDate: todayText(),
  };
}

function loadPage(params: SrmCrudRecord) {
  return getScarPage(params);
}

function getDetail(id: number | string) {
  return getScar(Number(id)) as Promise<SrmCrudRecord>;
}

function createRecord(record: SrmCrudRecord) {
  return createScar(record as any);
}

function updateRecord(record: SrmCrudRecord) {
  return updateScar(record as any);
}

function deleteRecord(id: number | string) {
  return deleteScar(Number(id));
}

function todayText() {
  const date = new Date();
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}
</script>

<template>
  <SrmCrudPrototypePage
    attachment-biz-type="SRM_SCAR"
    :attachment-category-options="scarAttachmentCategoryOptions"
    code-field="scarNo"
    :columns="gridColumns"
    :create-defaults="createDefaults"
    :create-record="createRecord"
    create-text="新增供方异常"
    :delete-record="deleteRecord"
    entity-name="供方异常与整改台账"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    :get-detail="getDetail"
    :load-page="loadPage"
    module-name="供方异常与整改"
    title-field="supplierName"
    :update-record="updateRecord"
    :value-labels="valueLabels"
  />
</template>