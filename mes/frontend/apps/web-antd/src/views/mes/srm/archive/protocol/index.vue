<script lang="ts" setup>
import type { SrmCrudField, SrmCrudRecord } from '../../shared/crud';

import { computed, ref } from 'vue';

import { useDictStore } from '@vben/stores';

import { message } from 'ant-design-vue';

import {
  createSupplierFile,
  deleteSupplierFile,
  getSupplierFile,
  getSupplierFilePage,
  updateSupplierFile,
} from '#/api/mes/srm/supplier-file';

import { SrmSilentCancelError } from '../../shared/crud';
import { supplierReference } from '../../shared/referenceFields';
import SrmCrudPrototypePage from '../../shared/SrmCrudPrototypePage.vue';
import { useGridColumns, useGridFormSchema } from './data';
import {
  buildComplianceValueLabels,
  COMPLIANCE_ATTACHMENT_CATEGORY_DICT,
  COMPLIANCE_FILE_NAME_DICT,
  COMPLIANCE_FILE_TYPE_DICT,
  COMPLIANCE_WARNING_DAYS,
  fallbackFileNameOptions,
  fallbackFileTypeOptions,
  FILE_TYPE_ENVIRONMENT_CERT,
  FILE_TYPE_SYSTEM_CERT,
  fileStatusOptions,
  getEffectDateFieldLabel,
  getExpiryDateFieldLabel,
  getFileNameFieldLabel,
  getFileNameLabel,
  getFileNameOptions,
  getFileTypeLabel,
  isAutoExpiryRecord,
  isComplianceAgreementRecord,
  isEnvironmentCertRecord,
  isReportCodeVisible,
  prepareComplianceRecordForSave,
  refreshComplianceRecord,
  standardCompliantOptions,
} from './template';

defineOptions({ name: 'SrmProtocolLedger' });

const CATEGORY_ALL_KEY = 'ALL';
const CATEGORY_TYPE_PREFIX = 'TYPE:';
const CATEGORY_NAME_PREFIX = 'NAME:';
const OLD_PRODUCT_STANDARD_NAME = '《产品化学物质控制标准》';
const PRODUCT_STANDARD_RECEIPT_NAME = '《产品化学物质控制标准》文件接收确认表';
const OLD_ENVIRONMENT_TYPE_LABEL = '环保证书';
const ENVIRONMENT_TYPE_LABEL = '环保资料';

const dictStore = useDictStore();
const selectedCategoryKey = ref(CATEGORY_ALL_KEY);

const fileTypeOptions = normalizeFileTypeOptions(
  resolveDictionaryOptions(COMPLIANCE_FILE_TYPE_DICT, fallbackFileTypeOptions),
);
const fileNameOptions = normalizeFileNameOptions(
  resolveDictionaryOptions(COMPLIANCE_FILE_NAME_DICT, fallbackFileNameOptions),
);
const configuredAttachmentCategoryOptions = normalizeAttachmentCategoryOptions(
  getDictionaryOptions(COMPLIANCE_ATTACHMENT_CATEGORY_DICT),
);
const gridFormSchema = useGridFormSchema();
const candidateSupplierReference = supplierReference({
  supplierSource: 'candidate',
  title: '选择供应商',
});

const selectedCategoryFilter = computed(() =>
  parseCategoryKey(selectedCategoryKey.value),
);
const selectedFileType = computed(
  () => selectedCategoryFilter.value.fileType || '',
);
const gridColumns = computed(() => useGridColumns(selectedFileType.value));
const categoryDefaultExpandedKeys = [CATEGORY_ALL_KEY];
const categoryPathText = computed(() =>
  buildCategoryPathText(selectedCategoryFilter.value),
);
const selectedCategoryKeys = computed(() => [selectedCategoryKey.value]);
const categoryTreeData = computed(() => [
  {
    key: CATEGORY_ALL_KEY,
    title: '全部合规资料',
    children: fileTypeOptions.map((fileType) => ({
      key: buildTypeCategoryKey(fileType.value),
      title: fileType.label,
      children: getFileNameOptions(fileType.value, fileNameOptions).map(
        (fileName) => ({
          key: buildNameCategoryKey(fileType.value, fileName.value),
          title: fileName.label,
        }),
      ),
    })),
  },
]);
const valueLabels = computed(() =>
  buildComplianceValueLabels(
    fileTypeOptions,
    fileNameOptions,
    getComplianceAttachmentCategoryOptions({}),
  ),
);

const detailFields: SrmCrudField[] = [
  {
    field: 'supplierName',
    label: '供应商名称',
    component: 'ReferencePicker',
    reference: candidateSupplierReference,
  },
  {
    field: 'supplierCode',
    label: '供应商代码',
    component: 'ReferencePicker',
    reference: candidateSupplierReference,
  },
  {
    field: 'providedProduct',
    label: '供应产品',
    component: 'Input',
  },
  {
    field: 'fileType',
    label: '档案类型',
    component: 'Select',
    onChange: handleFileTypeChange,
    options: fileTypeOptions,
  },
  {
    field: 'fileName',
    label: '档案名称',
    component: 'Select',
    getOptions: (record) =>
      getFileNameOptions(record.fileType, fileNameOptions),
    labelWhen: getFileNameFieldLabel,
    onChange: handleComplianceFieldChange,
    span: 2,
  },
  {
    field: 'productModel',
    label: '产品型号',
    component: 'Input',
    visibleWhen: isEnvironmentCertRecord,
  },
  {
    field: 'inspectionAgency',
    label: '检测机构',
    component: 'Input',
    visibleWhen: isEnvironmentCertRecord,
  },
  {
    field: 'reportCode',
    label: '报告编码',
    component: 'Input',
    visibleWhen: isReportCodeVisible,
  },
  {
    field: 'effectDate',
    label: '生效/生成日期',
    component: 'DatePicker',
    labelWhen: getEffectDateFieldLabel,
    onChange: handleComplianceFieldChange,
  },
  {
    field: 'expiryDate',
    label: '失效/过期日期',
    component: 'DatePicker',
    labelWhen: getExpiryDateFieldLabel,
    onChange: handleComplianceFieldChange,
    readonlyWhen: isAutoExpiryRecord,
  },
  {
    field: 'standardCompliant',
    label: '是否符合标准',
    component: 'RadioGroup',
    options: standardCompliantOptions,
    visibleWhen: isEnvironmentCertRecord,
  },
  {
    field: 'expiryRule',
    label: '有效期规则',
    component: 'Select',
    options: [
      { label: '手工维护', value: 'MANUAL' },
      { label: '自动12个月-1天', value: 'AUTO_12M_MINUS_1D' },
      { label: '自动60个月-1天', value: 'AUTO_60M_MINUS_1D' },
    ],
    readonly: true,
  },
  {
    field: 'validityMonths',
    label: '有效期(月)',
    readonly: true,
    visibleWhen: isComplianceAgreementRecord,
  },
  {
    field: 'fileStatus',
    label: '当前状态',
    component: 'Select',
    options: fileStatusOptions,
    readonly: true,
  },
  { field: 'daysLeft', label: '剩余效期(天)', readonly: true },
  { field: 'remark', label: '备注', component: 'Textarea', span: 3 },
];

function getDictionaryOptions(dictType: string) {
  return dictStore.getDictOptions(dictType).map((option) => ({
    disabled: (option as any).disabled,
    label: String(option.label ?? option.value),
    parentType: String(option.cssClass || ''),
    value: String(option.value),
  }));
}

function resolveDictionaryOptions(
  dictType: string,
  fallbackOptions: Array<{ label: string; parentType?: string; value: any }>,
) {
  const dictOptions = getDictionaryOptions(dictType);
  return dictOptions.length > 0 ? dictOptions : fallbackOptions;
}

function normalizeFileTypeOptions(
  options: Array<{ label: string; parentType?: string; value: any }>,
) {
  return options.map((option) => ({
    ...option,
    label:
      option.value === FILE_TYPE_ENVIRONMENT_CERT
        ? ENVIRONMENT_TYPE_LABEL
        : option.label,
    value: String(option.value),
  }));
}

function normalizeFileNameOptions(
  options: Array<{ label: string; parentType?: string; value: any }>,
) {
  return options.map((option) => {
    const value = String(option.value);
    if (value !== OLD_PRODUCT_STANDARD_NAME) {
      return { ...option, value };
    }
    return {
      ...option,
      label: PRODUCT_STANDARD_RECEIPT_NAME,
      value: PRODUCT_STANDARD_RECEIPT_NAME,
    };
  });
}

function normalizeAttachmentCategoryOptions(
  options: Array<{ label: string; parentType?: string; value: any }>,
) {
  return options.map((option) => {
    const value = String(option.value).replace(
      OLD_PRODUCT_STANDARD_NAME,
      PRODUCT_STANDARD_RECEIPT_NAME,
    );
    const label = String(option.label)
      .replace(OLD_PRODUCT_STANDARD_NAME, PRODUCT_STANDARD_RECEIPT_NAME)
      .replace(OLD_ENVIRONMENT_TYPE_LABEL, ENVIRONMENT_TYPE_LABEL);
    return {
      ...option,
      label,
      value,
    };
  });
}

function handleFileTypeChange(_value: unknown, record: SrmCrudRecord) {
  resetInvalidFileName(record);
  refreshComplianceRecord(record);
}

function handleComplianceFieldChange(_value: unknown, record: SrmCrudRecord) {
  refreshComplianceRecord(record);
}

function resetInvalidFileName(record: SrmCrudRecord) {
  const currentFileName = String(record.fileName || '');
  if (!currentFileName) {
    return;
  }
  const matched = getFileNameOptions(record.fileType, fileNameOptions).some(
    (option) => String(option.value) === currentFileName,
  );
  if (!matched) {
    record.fileName = undefined;
  }
}

function handleCategorySelect(keys: Array<number | string>) {
  selectedCategoryKey.value = String(keys[0] || CATEGORY_ALL_KEY);
}

function parseCategoryKey(key: string) {
  if (key.startsWith(CATEGORY_TYPE_PREFIX)) {
    return {
      fileType: key.slice(CATEGORY_TYPE_PREFIX.length),
    };
  }
  if (key.startsWith(CATEGORY_NAME_PREFIX)) {
    const payload = key.slice(CATEGORY_NAME_PREFIX.length);
    const [fileType = '', ...fileNameParts] = payload.split(':');
    return {
      fileName: fileNameParts.join(':'),
      fileType,
    };
  }
  return {};
}

function buildTypeCategoryKey(fileType: unknown) {
  return `${CATEGORY_TYPE_PREFIX}${fileType}`;
}

function buildNameCategoryKey(fileType: unknown, fileName: unknown) {
  return `${CATEGORY_NAME_PREFIX}${fileType}:${fileName}`;
}

function buildCategoryPathText(categoryFilter: {
  fileName?: string;
  fileType?: string;
}) {
  const fileType = String(categoryFilter.fileType || '');
  const fileName = String(categoryFilter.fileName || '');
  if (!fileType) {
    return '全部合规资料';
  }
  const fileTypeLabel = getFileTypeLabel(fileTypeOptions, fileType);
  if (!fileName) {
    return `全部合规资料 / ${fileTypeLabel}`;
  }
  return `全部合规资料 / ${fileTypeLabel} / ${getFileNameLabel(fileNameOptions, fileName)}`;
}

function getComplianceAttachmentCategoryOptions(record: SrmCrudRecord) {
  const selectedCategory = buildAttachmentCategoryValue(record);
  const allOptions =
    configuredAttachmentCategoryOptions.length > 0
      ? configuredAttachmentCategoryOptions
      : buildAttachmentCategoryOptionsFromFileNames();
  if (!selectedCategory) {
    return allOptions;
  }
  const existing = allOptions.find(
    (option) => option.value === selectedCategory,
  );
  return [
    existing || {
      label: buildAttachmentCategoryLabel(record.fileType, record.fileName),
      value: selectedCategory,
    },
  ];
}

function getComplianceAttachmentDefaultCategory(record: SrmCrudRecord) {
  return (
    buildAttachmentCategoryValue(record) ||
    getComplianceAttachmentCategoryOptions(record)[0]?.value ||
    ''
  );
}

function buildAttachmentCategoryOptionsFromFileNames() {
  return fileNameOptions.map((option) => ({
    disabled: option.disabled,
    label: `${getFileTypeLabel(fileTypeOptions, option.parentType)}/${option.label}`,
    parentType: option.parentType,
    value: `${option.parentType}/${option.value}`,
  }));
}

function buildAttachmentCategoryValue(record: SrmCrudRecord) {
  const fileType = String(record.fileType || '');
  const fileName = String(record.fileName || '');
  return fileType && fileName ? `${fileType}/${fileName}` : '';
}

function buildAttachmentCategoryLabel(fileType?: unknown, fileName?: unknown) {
  const normalizedFileName = String(fileName || '');
  return `${getFileTypeLabel(fileTypeOptions, fileType)}/${getFileNameLabel(fileNameOptions, normalizedFileName)}`;
}

function createDefaults(): SrmCrudRecord {
  const categoryFilter = selectedCategoryFilter.value;
  return refreshComplianceRecord({
    fileName: categoryFilter.fileName,
    fileStatus: 'VALID',
    fileType: categoryFilter.fileType || FILE_TYPE_SYSTEM_CERT,
    warningDays: COMPLIANCE_WARNING_DAYS,
  });
}

function loadPage(params: SrmCrudRecord) {
  return getSupplierFilePage({
    ...params,
    ...selectedCategoryFilter.value,
  });
}

function getDetail(id: number | string) {
  return getSupplierFile(Number(id)) as Promise<SrmCrudRecord>;
}

function createRecord(record: SrmCrudRecord) {
  return createSupplierFile(buildRecordForSave(record) as any);
}

function updateRecord(record: SrmCrudRecord) {
  return updateSupplierFile(buildRecordForSave(record) as any);
}

function deleteRecord(id: number | string) {
  return deleteSupplierFile(Number(id));
}

function buildRecordForSave(record: SrmCrudRecord) {
  const normalized = prepareComplianceRecordForSave(record);
  validateRecordForSave(normalized);
  return normalized;
}

function validateRecordForSave(record: SrmCrudRecord) {
  const missingLabels: string[] = [];
  addMissingLabel(missingLabels, record.supplierName, '供应商名称');
  addMissingLabel(missingLabels, record.fileType, '档案类型');
  addMissingLabel(
    missingLabels,
    record.fileName,
    getFileNameFieldLabel(record),
  );
  if (isEnvironmentCertRecord(record)) {
    addMissingLabel(missingLabels, record.productModel, '产品型号');
    addMissingLabel(missingLabels, record.inspectionAgency, '检测机构');
    addMissingLabel(missingLabels, record.standardCompliant, '是否符合标准');
  }
  if (missingLabels.length === 0) {
    return;
  }
  message.warning(`请先填写${missingLabels.join('、')}`);
  throw new SrmSilentCancelError();
}

function addMissingLabel(
  missingLabels: string[],
  value: unknown,
  label: string,
) {
  if (String(value ?? '').trim()) {
    return;
  }
  missingLabels.push(label);
}
</script>

<template>
  <SrmCrudPrototypePage
    attachment-biz-type="SUPPLIER_FILE"
    :attachment-category-options="getComplianceAttachmentCategoryOptions"
    :attachment-default-category="getComplianceAttachmentDefaultCategory"
    :category-tree-data="categoryTreeData"
    :category-default-expanded-keys="categoryDefaultExpandedKeys"
    :category-path-text="categoryPathText"
    category-tree-title="档案类型"
    code-field="fileName"
    :columns="gridColumns"
    :create-defaults="createDefaults"
    :create-record="createRecord"
    create-text="新增合规资料"
    :delete-record="deleteRecord"
    entity-name="合规资料台账"
    :fields="detailFields"
    :form-schema="gridFormSchema"
    :get-detail="getDetail"
    :load-page="loadPage"
    module-name="合规资料台账"
    :selected-category-keys="selectedCategoryKeys"
    title-field="supplierName"
    :update-record="updateRecord"
    :value-labels="valueLabels"
    @category-select="handleCategorySelect"
  />
</template>
