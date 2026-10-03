<script lang="ts" setup>
import type { SrmCrudField, SrmCrudRecord } from '../../shared/crud';

import type { SrmSupplierCandidateApi } from '#/api/mes/srm/supplier-candidate';

import { Modal as AntModal, message } from 'ant-design-vue';

import { checkSupplierName } from '#/api/mes/srm/supplier-candidate';
import {
  createSurvey,
  deleteSurvey,
  getSurvey,
  getSurveyPage,
  updateSurvey,
} from '#/api/mes/srm/survey';

import { SrmSilentCancelError } from '../../shared/crud';
import { supplierReference } from '../../shared/referenceFields';
import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { useGridColumns, useGridFormSchema } from './data';

defineOptions({ name: 'SrmSupplierSurvey' });

const gridColumns = useGridColumns();
const gridFormSchema = useGridFormSchema();

const yesNoOptions = [
  { label: '是', value: true },
  { label: '否', value: false },
];
const candidateSupplierReference = supplierReference({
  supplierSource: 'candidate',
  title: '选择供应商',
});
const surveyAttachmentCategoryOptions = [
  {
    label: '供应商基本情况调查表(贸易/代理型）',
    value: 'SURVEY_TRADE_AGENT_FORM',
  },
  {
    label: '新供应商基本情况调查表',
    value: 'SURVEY_NEW_SUPPLIER_FORM',
  },
];

const detailFields: SrmCrudField[] = [
  { field: 'surveyNo', label: '调查表编号', readonly: true },
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
    component: 'RadioGroup',
    field: 'unregisteredSupplier',
    label: '未入库供应商标记',
    options: yesNoOptions,
  },
  { component: 'DatePicker', field: 'surveyDate', label: '调查日期' },
  {
    component: 'DateTimePicker',
    field: 'applyTime',
    label: '调查表登记时间',
    readonly: true,
  },
  { field: 'applicantName', label: '登记人', readonly: true },
  {
    component: 'DateTimePicker',
    field: 'updateTime',
    label: '更新时间',
    readonly: true,
  },
  {
    component: 'Textarea',
    field: 'conclusion',
    label: '调查结论说明',
    rows: 5,
    span: 3,
  },
];

const valueLabels = {
  false: '否',
  PENDING: '未入库',
  REGISTERED: '已入库',
  true: '是',
};

function createDefaults(): SrmCrudRecord {
  return {
    applyTime: nowText(),
    status: 'DRAFT',
    surveyDate: todayText(),
    unregisteredSupplier: false,
  };
}

function loadPage(params: SrmCrudRecord) {
  return getSurveyPage(params);
}

function getDetail(id: number | string) {
  return getSurvey(Number(id)) as Promise<SrmCrudRecord>;
}

async function createRecord(record: SrmCrudRecord) {
  return createSurvey((await prepareSupplier(record)) as any);
}

async function updateRecord(record: SrmCrudRecord) {
  return updateSurvey((await prepareSupplier(record)) as any);
}

function deleteRecord(id: number | string) {
  return deleteSurvey(Number(id));
}

async function prepareSupplier(record: SrmCrudRecord) {
  const prepared = {
    ...record,
    confirmInitializePending: false,
    supplierName: String(record.supplierName || '').trim(),
  };
  if (!prepared.supplierName) {
    cancelSave('请填写或选择供应商名称');
  }
  if (!prepared.unregisteredSupplier) {
    if (!prepared.supplierId) {
      cancelSave('已入库供应商必须从供应商台账弹窗中选择');
    }
    return prepared;
  }
  if (prepared.supplierId) {
    return prepared;
  }

  const checkResult = await checkSupplierName(prepared.supplierName);
  const registeredMatches = checkResult.registeredMatches || [];
  const pendingMatches = checkResult.pendingMatches || [];
  if (registeredMatches.length > 1) {
    cancelSave(
      '供应商资源池存在多条同名已入库记录，请从供应商选择弹窗中核对代码后选择',
    );
  }
  if (registeredMatches.length === 1) {
    const useExisting = await confirmUseExisting(
      `供应商资源池已有“${prepared.supplierName}”（代码：${registeredMatches[0]?.supplierCode || '-'}），是否使用已有供应商？`,
    );
    if (!useExisting) {
      throw new SrmSilentCancelError();
    }
    return applySupplierCandidate(prepared, registeredMatches[0]!);
  }
  if (pendingMatches.length > 1) {
    cancelSave(
      '供应商资源池存在多条同名考察中记录，请从供应商选择弹窗中核对临时代码后选择',
    );
  }
  if (pendingMatches.length === 1) {
    const useExisting = await confirmUseExisting(
      `供应商资源池已有考察中供应商“${prepared.supplierName}”（临时代码：${pendingMatches[0]?.supplierCode || '-'}），是否使用已有编号？`,
    );
    if (!useExisting) {
      throw new SrmSilentCancelError();
    }
    return applySupplierCandidate(prepared, pendingMatches[0]!);
  }

  const initialize = await confirmUseExisting(
    `供应商资源池中未找到“${prepared.supplierName}”。确认基础信息无误并初始化为考察中供应商吗？`,
    '确认并生成临时编码',
  );
  if (!initialize) {
    throw new SrmSilentCancelError();
  }
  prepared.confirmInitializePending = true;
  prepared.supplierCode = undefined;
  prepared.supplierId = undefined;
  prepared.supplierSourceType = 'REGISTERED';
  return prepared;
}

function applySupplierCandidate(
  record: SrmCrudRecord,
  candidate: SrmSupplierCandidateApi.Candidate,
) {
  return {
    ...record,
    confirmInitializePending: false,
    supplierCode: candidate.supplierCode,
    supplierId: candidate.supplierId,
    supplierName: candidate.supplierName,
    supplierSourceType: 'REGISTERED',
    unregisteredSupplier: Boolean(candidate.unregisteredSupplier),
  };
}

function confirmUseExisting(content: string, okText = '使用已有记录') {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      cancelText: '返回核对',
      content,
      okText,
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
      title: '供应商名称防重确认',
    });
  });
}

function cancelSave(text: string): never {
  message.warning(text);
  throw new SrmSilentCancelError(text);
}

function todayText() {
  const date = new Date();
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}

function nowText() {
  const date = new Date();
  return `${todayText()} ${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}
</script>

<template>
  <SrmCrudPrototypePage
    attachment-biz-type="SURVEY"
    :attachment-category-options="surveyAttachmentCategoryOptions"
    code-field="surveyNo"
    :columns="gridColumns"
    :create-defaults="createDefaults"
    :create-record="createRecord"
    create-text="新增基本情况调查"
    :delete-record="deleteRecord"
    entity-name="基本情况调查表"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    :get-detail="getDetail"
    :load-page="loadPage"
    module-name="基本情况调查"
    title-field="supplierName"
    :update-record="updateRecord"
    :value-labels="valueLabels"
  />
</template>
