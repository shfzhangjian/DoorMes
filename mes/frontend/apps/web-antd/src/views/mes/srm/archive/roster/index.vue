<script lang="ts" setup>
import type { SrmCrudField, SrmCrudRecord } from '../../shared/crud';

import { computed, onMounted, ref } from 'vue';

import { getDictOptions } from '@vben/hooks';

import {
  createSupplier,
  deleteSupplier,
  getSupplier,
  updateSupplier,
} from '#/api/mes/supplier';
import { getSupplierCandidatePage } from '#/api/mes/srm/supplier-candidate';
import { getSupplierScopeSimpleList } from '#/api/mes/srm/supplier-scope';

import SrmSupplierRelationTabs from '../shared/SrmSupplierRelationTabs.vue';
import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { materialReference, productBomReference } from '../../shared/referenceFields';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'SrmSupplierRoster' });

const gridColumns = useGridColumns();
const scopeOptions = ref<Array<{ label: string; value: number }>>([]);
const gridFormSchema = computed(() => useGridFormSchema(scopeOptions.value));

const companyNatureOptions = [
  { label: '生产厂家(原厂)', value: 'MANUFACTURER' },
  { label: '贸易/代理商', value: 'AGENT' },
];
const levelOptions = [
  { label: 'A级', value: 'A' },
  { label: 'B级', value: 'B' },
  { label: 'C级', value: 'C' },
  { label: 'D级', value: 'D' },
];
const materialGradeOptions = [
  { label: 'A级', value: 'A' },
  { label: 'B级', value: 'B' },
  { label: 'C级', value: 'C' },
  { label: 'D级', value: 'D' },
];
const statusOptions = [
  { label: '考察中', value: 'PENDING' },
  { label: '合格', value: 'QUALIFIED' },
  { label: '冻结', value: 'FROZEN' },
  { label: '淘汰', value: 'ELIMINATED' },
  { label: '退出', value: 'EXITED' },
  { label: '不合格', value: 'UNQUALIFIED' },
];
const usingDepartmentOptions = computed(() =>
  getDictOptions('mes_srm_apply_department').map((option) => ({
    label: String(option.label ?? option.value),
    value: String(option.label ?? option.value),
  })),
);

const detailFields = computed<SrmCrudField[]>(() => [
  { field: 'supplierCode', label: '供应商代码' },
  { field: 'supplierName', label: '供应商名称' },
  {
    field: 'usingDepartment',
    label: '使用部门',
    component: 'Select',
    joinSeparator: '、',
    mode: 'multiple',
    options: usingDepartmentOptions.value,
  },
  {
    field: 'status',
    label: '资源状态',
    component: 'Select',
    options: statusOptions,
  },
  { field: 'contactPhone', label: '联系电话' },
  { field: 'level', label: '评定级别', component: 'Select', options: levelOptions },
  {
    field: 'materialGrade',
    label: '物料等级',
    component: 'Select',
    options: materialGradeOptions,
  },
  {
    field: 'companyNature',
    label: '企业性质',
    component: 'Select',
    options: companyNatureOptions,
  },
  { field: 'contactPerson', label: '联系人' },
  {
    field: 'scopeId',
    label: '名录范围',
    component: 'Select',
    options: scopeOptions.value,
    span: 3,
  },
  { field: 'mainProducts', label: '主营产品', component: 'Textarea', span: 3 },
  { field: 'providedProduct', label: '供应/协作内容', component: 'Textarea', span: 3 },
  {
    field: 'materialCode',
    label: '物料代码',
    component: 'ReferencePicker',
    reference: materialReference(),
  },
  {
    field: 'model',
    label: '型号',
    component: 'ReferencePicker',
    reference: materialReference(),
  },
  {
    field: 'applicableProduct',
    label: '适用产品',
    component: 'ReferencePicker',
    reference: productBomReference({
      targetField: 'applicableProduct',
    }),
  },
  { field: 'legalPerson', label: '法定代表人' },
  { field: 'registeredCapital', label: '注册资本', component: 'InputNumber' },
  { field: 'establishDate', label: '成立日期', component: 'DatePicker' },
  { field: 'importDate', label: '导入日期', component: 'DatePicker' },
  { field: 'paymentTerms', label: '结算条件' },
  { field: 'deliveryMethod', label: '交货方式' },
  { field: 'email', label: '电子邮箱' },
  { field: 'originPlace', label: '产地' },
  { field: 'address', label: '供应商地址', component: 'Textarea', span: 3 },
  { field: 'originalFactoryInfo', label: '原厂信息', component: 'Textarea', span: 3 },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
]);

const valueLabels = {
  A: 'A级',
  B: 'B级',
  C: 'C级',
  D: 'D级',
  AGENT: '贸易/代理商',
  MANUFACTURER: '生产厂家(原厂)',
  ELIMINATED: '淘汰',
  EXITED: '退出',
  FROZEN: '冻结',
  PENDING: '考察中',
  QUALIFIED: '合格',
  UNQUALIFIED: '不合格',
};

function createDefaults(): SrmCrudRecord {
  return {
    importDate: todayText(),
    status: 'QUALIFIED',
  };
}

function loadPage(params: SrmCrudRecord) {
  const queryParams = { ...params };
  delete queryParams.status;
  return getSupplierCandidatePage({
    ...queryParams,
    sourceType: 'REGISTERED',
    status: 'QUALIFIED',
  } as any).then((result) => ({
    ...result,
    list: (result.list || []).map((item) => ({
      ...item,
      id: item.supplierId || item.id,
      sourceType: 'REGISTERED',
      status: 'QUALIFIED',
    })),
  }));
}

async function getDetail(id: number | string, row: SrmCrudRecord) {
  const supplierId = Number(row.supplierId || id);
  const supplier = await getSupplier(supplierId);
  return {
    ...row,
    ...supplier,
    supplierId,
    sourceType: 'REGISTERED',
  } as SrmCrudRecord;
}

function createRecord(record: SrmCrudRecord) {
  return createSupplier({
    ...record,
    materialCategory: record.materialCategory || record.materialGrade,
    status: record.status || 'QUALIFIED',
  } as any);
}

function updateRecord(record: SrmCrudRecord) {
  return updateSupplier({
    ...record,
    materialCategory: record.materialCategory || record.materialGrade,
  } as any);
}

function deleteRecord(id: number | string, row: SrmCrudRecord) {
  return deleteSupplier(Number(row.supplierId || id));
}

function todayText() {
  const date = new Date();
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}

onMounted(async () => {
  const scopes = await getSupplierScopeSimpleList();
  scopeOptions.value = (scopes || [])
    .filter((scope) => scope.id)
    .map((scope) => ({
      label: `${scope.scopeName || '-'}（${scope.scopeCode || '-'}）`,
      value: Number(scope.id),
    }));
});
</script>

<template>
  <SrmCrudPrototypePage
    attachment-biz-type="SUPPLIER"
    code-field="supplierCode"
    :columns="gridColumns"
    :create-defaults="createDefaults"
    :create-record="createRecord"
    create-text="新增供应商档案"
    :delete-record="deleteRecord"
    entity-name="供应商档案"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    :get-detail="getDetail"
    :load-page="loadPage"
    module-name="合格供应商名录"
    title-field="supplierName"
    :update-record="updateRecord"
    :value-labels="valueLabels"
  >
    <template #detail-extra="{ record }">
      <SrmSupplierRelationTabs :record="record" />
    </template>
  </SrmCrudPrototypePage>
</template>
