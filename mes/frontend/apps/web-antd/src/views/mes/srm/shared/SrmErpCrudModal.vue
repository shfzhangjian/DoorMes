<script lang="ts" setup>
import type { SrmCrudField, SrmCrudRecord } from './crud';

import type { PickerOption } from '#/components/picker';

import { computed, onDeactivated, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  AutoComplete,
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  message,
  Radio,
  Select,
  Spin,
} from 'ant-design-vue';

import {
  materialPickerConfig,
  PickerModal,
  productBomPickerConfig,
} from '#/components/picker';

import { SrmSilentCancelError } from './crud';
import SrmAttachmentPanel from './SrmAttachmentPanel.vue';
import SrmReferenceSelectModal from './SrmReferenceSelectModal.vue';

import './srm-erp-detail.css';

type SaveHandler = (payload: {
  mode: 'create' | 'edit';
  record: SrmCrudRecord;
}) =>
  | number
  | Promise<number | SrmCrudRecord | string | void>
  | SrmCrudRecord
  | string
  | void;

type CategoryOption = {
  disabled?: boolean;
  label: string;
  value: string;
};

const emit = defineEmits<{
  success: [
    payload: {
      mode: 'create' | 'edit';
      record: SrmCrudRecord;
    },
  ];
}>();

const SRM_DETAIL_MODAL_Z_INDEX = 4100;
const SRM_REFERENCE_MODAL_Z_INDEX = 5600;
const SRM_DROPDOWN_Z_INDEX = 5700;

const formRef = ref();
const detailOpen = ref(false);
const detailPopupHostRef = ref<HTMLElement>();
const mode = ref<'create' | 'detail' | 'edit'>('detail');
const entityName = ref('业务单据');
const moduleName = ref('供应商管理');
const codeField = ref('id');
const titleField = ref('');
const fields = ref<SrmCrudField[]>([]);
const formData = ref<SrmCrudRecord>({});
const saveHandler = ref<SaveHandler>();
const valueLabels = ref<Record<string, string>>({});
const saving = ref(false);
const activeReferenceField = ref<SrmCrudField>();
const attachmentBizType = ref('');
const attachmentCategoryOptions = ref<
  ((record: SrmCrudRecord) => CategoryOption[]) | CategoryOption[]
>([]);
const attachmentDefaultCategory = ref<
  ((record: SrmCrudRecord) => string) | string
>('');
const attachmentPanelRef = ref<InstanceType<typeof SrmAttachmentPanel>>();
const materialPickerOpen = ref(false);
const productBomPickerOpen = ref(false);

const isReadonly = computed(() => mode.value === 'detail');
const canEditRecord = computed(() => formData.value.canEdit !== false);
const visibleFields = computed(() =>
  fields.value.filter(
    (field) => !field.visibleWhen || field.visibleWhen(formData.value),
  ),
);
const activeAttachmentCategoryOptions = computed(() => {
  if (typeof attachmentCategoryOptions.value === 'function') {
    return attachmentCategoryOptions.value(formData.value);
  }
  return attachmentCategoryOptions.value;
});
const activeAttachmentDefaultCategory = computed(() => {
  if (typeof attachmentDefaultCategory.value === 'function') {
    return attachmentDefaultCategory.value(formData.value);
  }
  return attachmentDefaultCategory.value;
});
const modalTitle = computed(() => {
  const actionName = getModeActionName('查看');
  return `${actionName}${entityName.value}`;
});
const titleText = computed(() => {
  return entityName.value || '业务单据';
});
const recordTitleText = computed(() => {
  const titleValue = getFieldValue(titleField.value);
  const codeValue = getFieldValue(codeField.value);
  if (
    !titleValue ||
    titleValue === codeValue ||
    titleValue === titleText.value
  ) {
    return '';
  }
  return `名称 ${titleValue}`;
});
const subtitleItems = computed(() =>
  [
    getFieldValue(codeField.value)
      ? `单号 ${getFieldValue(codeField.value)}`
      : '',
    recordTitleText.value,
    moduleName.value,
    getModeActionName('明细'),
  ].filter(Boolean),
);
const materialPickerInitialFilters = computed(() => {
  const field = activeReferenceField.value;
  if (field?.reference?.type !== 'material') {
    return {};
  }
  const currentValue = getFieldValue(field.field);
  const displayValue = getFieldValue(field.reference.displayField);
  const materialCode =
    getFieldValue('materialCode') ||
    getFieldValue('productMaterialCode') ||
    (/code/i.test(field.field) ? currentValue || displayValue : '');
  const materialName =
    getFieldValue('materialName') ||
    getFieldValue('productMaterialName') ||
    (/name/i.test(field.field) ? currentValue || displayValue : '');
  const specModel =
    getFieldValue('materialModel') ||
    getFieldValue('productModel') ||
    getFieldValue('productSpec') ||
    getFieldValue('spec');
  const modelCode = getFieldValue('productModelCode');

  return {
    materialCode: materialCode || undefined,
    materialName: materialCode ? undefined : materialName || undefined,
    modelCode: modelCode || undefined,
    specModel: specModel || undefined,
  };
});
const materialPickerTitle = computed(
  () =>
    activeReferenceField.value?.reference?.title || materialPickerConfig.title,
);
const productBomPickerInitialFilters = computed(() => {
  const field = activeReferenceField.value;
  if (field?.reference?.type !== 'productBom') {
    return {};
  }
  const currentValue = getFieldValue(field.field);
  const snapshotPrefix = field.field;
  const productMaterialKeyword =
    getFieldValue(`${snapshotPrefix}MaterialCode`) ||
    getFieldValue(`${snapshotPrefix}MaterialName`) ||
    currentValue;
  const productModelCode =
    getFieldValue(`${snapshotPrefix}ModelCode`) ||
    getFieldValue('productModelCode') ||
    getFieldValue('modelCode');
  const productSpec =
    getFieldValue(`${snapshotPrefix}Spec`) ||
    getFieldValue('productSpec') ||
    getFieldValue('spec');

  return {
    productMaterialKeyword: productMaterialKeyword || undefined,
    productModelCode: productModelCode || undefined,
    productSpec: productSpec || undefined,
  };
});
const productBomPickerTitle = computed(
  () =>
    activeReferenceField.value?.reference?.title ||
    productBomPickerConfig.title,
);

const [ReferenceSelectModal, referenceSelectModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

function openWithData(data: Record<string, any>) {
  mode.value = data.mode || 'detail';
  entityName.value = data.entityName || '业务单据';
  moduleName.value = data.moduleName || '供应商管理';
  codeField.value = data.codeField || 'id';
  attachmentBizType.value =
    data.attachmentBizType || data.record?.bizType || '';
  attachmentCategoryOptions.value = data.attachmentCategoryOptions || [];
  attachmentDefaultCategory.value = data.attachmentDefaultCategory || '';
  titleField.value = data.titleField || codeField.value;
  fields.value = data.fields || [];
  formData.value = normalizeRecordForFields(
    cloneRecord(data.record || {}),
    fields.value,
  );
  saveHandler.value = data.saveRecord;
  valueLabels.value = data.valueLabels || {};
  detailOpen.value = true;
}

function close() {
  detailOpen.value = false;
  materialPickerOpen.value = false;
  productBomPickerOpen.value = false;
  activeReferenceField.value = undefined;
  saving.value = false;
}

function getDetailPopupContainer(triggerNode?: HTMLElement) {
  return (
    detailPopupHostRef.value || triggerNode?.parentElement || document.body
  );
}

onDeactivated(close);

defineExpose({
  close,
  openWithData,
});

function cloneRecord(record: SrmCrudRecord) {
  return structuredClone(record || {}) as SrmCrudRecord;
}

function normalizeRecordForFields(
  record: SrmCrudRecord,
  fieldList: SrmCrudField[],
) {
  fieldList.forEach((field) => {
    if (field.component !== 'Select' || field.mode !== 'multiple') {
      return;
    }
    const value = record[field.field];
    if (Array.isArray(value)) {
      return;
    }
    record[field.field] = splitMultiValue(value);
  });
  return record;
}

function resolveSavedBizId(result: unknown, record: SrmCrudRecord) {
  return (
    resolveReturnedId(result) ||
    resolveReturnedId(record) ||
    resolveReturnedId(formData.value)
  );
}

function resolveReturnedId(result: unknown): number | string | undefined {
  if (typeof result === 'number' || typeof result === 'string') {
    return result;
  }
  if (!result || typeof result !== 'object') {
    return undefined;
  }
  const data = result as SrmCrudRecord;
  return (
    data.id ||
    data.bizId ||
    resolveReturnedId(data.data) ||
    resolveReturnedId(data.result)
  );
}

function getFieldValue(field?: string) {
  if (!field) {
    return '';
  }
  const value = formData.value[field];
  if (value === undefined || value === null || value === '') {
    return '';
  }
  return String(value);
}

function displayValue(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (Array.isArray(value)) {
    return value.length > 0
      ? value.map((item) => displayValue(item)).join('、')
      : '-';
  }
  if (typeof value === 'object') {
    return JSON.stringify(value);
  }
  return String(value);
}

function displayFieldValue(field: SrmCrudField) {
  if (isMaskedField(field.field)) {
    return '*';
  }
  const value = formData.value[field.field];
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  const option = getFieldOptions(field).find(
    (item) => String(item.value) === String(value),
  );
  if (option) {
    return option.label;
  }
  return valueLabels.value[String(value)] || displayValue(value);
}

function getFieldLabel(field: SrmCrudField) {
  return field.labelWhen?.(formData.value) || field.label;
}

function getFieldOptions(field: SrmCrudField) {
  return field.getOptions
    ? field.getOptions(formData.value)
    : field.options || [];
}

function isFieldReadonly(field: SrmCrudField) {
  return Boolean(
    isReadonly.value || field.readonly || field.readonlyWhen?.(formData.value),
  );
}

function handleFieldChange(field: SrmCrudField, value: any) {
  field.onChange?.(value, formData.value);
}

function filterAutoCompleteOption(
  inputValue: string,
  option?: { label?: unknown; value?: unknown },
) {
  const keyword = String(inputValue || '')
    .trim()
    .toLowerCase();
  if (!keyword) {
    return true;
  }
  return [option?.label, option?.value].some((value) =>
    String(value ?? '')
      .toLowerCase()
      .includes(keyword),
  );
}

function normalizeExternalUrl(value: unknown) {
  const text = String(value ?? '').trim();
  if (!text) {
    return '';
  }
  const candidate = /^https?:\/\//i.test(text) ? text : `http://${text}`;
  try {
    const url = new URL(candidate);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch {
    return '';
  }
}

function getExternalUrl(field: SrmCrudField) {
  return normalizeExternalUrl(formData.value[field.field]);
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return;
    } catch {
      // 非安全上下文下回退到传统复制方式。
    }
  }
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.append(textarea);
  textarea.select();
  const copied = document.execCommand('copy');
  textarea.remove();
  if (!copied) {
    throw new Error('复制失败');
  }
}

async function handleCopyExternalLink(field: SrmCrudField) {
  const url = getExternalUrl(field);
  if (!url) {
    message.warning('请先填写有效的 OA 审批链接');
    return;
  }
  try {
    await copyText(url);
    message.success('OA 审批链接已复制');
  } catch {
    message.error('复制失败，请手工复制链接');
  }
}

function handleOpenExternalLink(field: SrmCrudField) {
  const url = getExternalUrl(field);
  if (!url) {
    message.warning('请先填写有效的 OA 审批链接');
    return;
  }
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.rel = 'noopener noreferrer';
  anchor.target = '_blank';
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
}

function normalizeFieldValue(field: SrmCrudField) {
  const value = formData.value[field.field];
  if (field.component === 'Select' && field.mode === 'multiple') {
    return Array.isArray(value)
      ? value.join(field.joinSeparator || '、')
      : value;
  }
  if (field.component !== 'JsonTextarea') {
    return value;
  }
  if (typeof value !== 'string') {
    return value;
  }
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

async function handleSave() {
  if (isReadonly.value) {
    return;
  }
  saving.value = true;
  try {
    const record = { ...formData.value };
    visibleFields.value.forEach((field) => {
      record[field.field] = normalizeFieldValue(field);
    });
    const payload = {
      mode: mode.value === 'create' ? 'create' : 'edit',
      record,
    } as const;
    let saveResult: number | SrmCrudRecord | string | void;
    if (saveHandler.value) {
      saveResult = await saveHandler.value(payload);
    } else {
      emit('success', payload);
    }
    const savedBizId = resolveSavedBizId(saveResult, record);
    const currentAttachmentBizType =
      attachmentBizType.value || String(record.bizType || '');
    if (savedBizId && currentAttachmentBizType) {
      await attachmentPanelRef.value?.syncAttachments({
        bizId: savedBizId,
        bizType: currentAttachmentBizType,
      });
      formData.value.id = savedBizId;
    }
    message.success('保存成功');
    close();
  } catch (error) {
    if (!(error instanceof SrmSilentCancelError)) {
      message.error(resolveSaveErrorMessage(error));
    }
  } finally {
    saving.value = false;
  }
}

function resolveSaveErrorMessage(error: unknown) {
  const responseData =
    (error as any)?.response?.data || (error as any)?.data || {};
  return (
    responseData.error ||
    responseData.message ||
    responseData.msg ||
    (error as Error)?.message ||
    '保存失败，请检查必填项或后台错误日志'
  );
}

function switchToEdit() {
  if (!canEditRecord.value) {
    message.warning('当前账号无权编辑该记录');
    return;
  }
  mode.value = 'edit';
}

function isMaskedField(field: string) {
  return (
    Array.isArray(formData.value.maskedFields) &&
    formData.value.maskedFields.includes(field)
  );
}

function getReferenceDisplayValue(field: SrmCrudField) {
  const displayField = field.reference?.displayField || field.field;
  return getFieldValue(displayField) || getFieldValue(field.field);
}

function openReferenceSelector(field: SrmCrudField) {
  if (isFieldReadonly(field) || !field.reference) {
    return;
  }
  activeReferenceField.value = field;
  if (field.reference.type === 'material') {
    materialPickerOpen.value = true;
    return;
  }
  if (field.reference.type === 'productBom') {
    productBomPickerOpen.value = true;
    return;
  }
  referenceSelectModalApi
    .setData({
      keyword: getReferenceDisplayValue(field),
      modalZIndex: SRM_REFERENCE_MODAL_Z_INDEX,
      referenceType: field.reference.type,
      supplierSource: field.reference.supplierSource,
      title: field.reference.title,
    })
    .open();
}

function handleReferenceInputClick(field: SrmCrudField) {
  if (isFieldReadonly(field)) {
    return;
  }
  if (!field.allowManualInput) {
    openReferenceSelector(field);
  }
}

function handleReferenceManualInput(field: SrmCrudField) {
  if (!field.allowManualInput || !field.reference?.mappings) {
    return;
  }
  const visibleFieldNames = new Set(fields.value.map((item) => item.field));
  const manualClearFields = new Set(field.manualClearFields || []);
  field.reference.mappings.forEach((mapping) => {
    if (
      mapping.target !== field.field &&
      (!visibleFieldNames.has(mapping.target) ||
        manualClearFields.has(mapping.target))
    ) {
      delete formData.value[mapping.target];
    }
  });
}

function handleReferenceSelect(row: SrmCrudRecord) {
  const field = activeReferenceField.value;
  if (!field?.reference) {
    return;
  }

  const mappings =
    field.reference.mappings && field.reference.mappings.length > 0
      ? field.reference.mappings
      : [
          {
            source: field.reference.displayField || field.field,
            target: field.field,
          },
        ];

  mappings.forEach((mapping) => {
    formData.value[mapping.target] = getNestedValue(row, mapping.source);
  });
  if (field.reference.type === 'supplier') {
    formData.value.supplierId = row.supplierId;
    formData.value.supplierSourceType = 'REGISTERED';
    formData.value.unregisteredSupplier =
      field.reference.supplierSource === 'roster'
        ? false
        : Boolean(row.unregisteredSupplier);
  }
  activeReferenceField.value = undefined;
}

function handleMaterialPickerClose() {
  materialPickerOpen.value = false;
  if (activeReferenceField.value?.reference?.type === 'material') {
    activeReferenceField.value = undefined;
  }
}

function handleMaterialPick(option: PickerOption) {
  handleReferenceSelect((option.raw || {}) as SrmCrudRecord);
  materialPickerOpen.value = false;
}

function handleProductBomPickerClose() {
  productBomPickerOpen.value = false;
  if (activeReferenceField.value?.reference?.type === 'productBom') {
    activeReferenceField.value = undefined;
  }
}

function handleProductBomPick(option: PickerOption) {
  handleReferenceSelect((option.raw || {}) as SrmCrudRecord);
  productBomPickerOpen.value = false;
}

function getNestedValue(row: SrmCrudRecord, path: string) {
  let value: any = row;
  for (const key of path.split('.')) {
    value = value?.[key];
  }
  return value;
}

function getModeActionName(detailName: string) {
  if (mode.value === 'create') {
    return '新增';
  }
  if (mode.value === 'edit') {
    return '编辑';
  }
  return detailName;
}

function splitMultiValue(value: unknown) {
  if (Array.isArray(value)) {
    return value;
  }
  return String(value ?? '')
    .split(/[、,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean);
}
</script>

<template>
  <AntModal
    v-model:open="detailOpen"
    :body-style="{ height: '100dvh', overflow: 'hidden', padding: 0 }"
    :closable="false"
    :destroy-on-close="true"
    :footer="null"
    :keyboard="false"
    :mask-closable="false"
    :style="{ paddingBottom: 0, top: 0 }"
    width="100vw"
    wrap-class-name="qms-product-event-detail-modal srm-erp-crud-modal srm-independent-detail-modal"
    :z-index="SRM_DETAIL_MODAL_Z_INDEX"
    @cancel="close"
  >
    <div ref="detailPopupHostRef" class="srm-independent-detail-host">
      <Spin :spinning="saving" class="detail-spin">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="close">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>

            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">
                {{ titleText }}
              </div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in subtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
              </div>
            </div>

            <div class="qms-ncr-toolbar__actions">
              <Button
                v-if="isReadonly && canEditRecord"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="switchToEdit"
              >
                <IconifyIcon icon="lucide:edit-3" />
                编辑
              </Button>
              <Button
                v-else
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="handleSave"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button class="qms-ncr-toolbar-action" @click="close">
                关闭
              </Button>
            </div>
          </div>

          <div class="detail-content">
            <div class="qms-exception-workbench">
              <div class="qms-exception-form">
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>{{ modalTitle }}</strong>
                    </div>
                  </div>

                  <Form ref="formRef" :model="formData" layout="vertical">
                    <div class="erp-form-grid">
                      <template
                        v-for="field in visibleFields"
                        :key="field.field"
                      >
                        <div
                          class="erp-form-item"
                          :class="{
                            'erp-form-item--full': field.span === 3,
                            'erp-form-item--wide': field.span === 2,
                          }"
                        >
                          <label
                            class="erp-form-label"
                            :class="{
                              'erp-form-label--tall': field.span === 3,
                            }"
                          >
                            {{ getFieldLabel(field) }}
                          </label>
                          <div class="erp-form-value">
                            <div
                              v-if="
                                isFieldReadonly(field) &&
                                field.component === 'UrlInput'
                              "
                              class="flex min-w-0 items-center gap-2"
                            >
                              <a
                                v-if="getExternalUrl(field)"
                                class="min-w-0 flex-1 break-all text-primary hover:underline"
                                :href="getExternalUrl(field)"
                                rel="noopener noreferrer"
                                target="_blank"
                              >
                                {{ displayFieldValue(field) }}
                              </a>
                              <div v-else class="qms-exception-readonly-value">
                                -
                              </div>
                              <Button
                                v-if="getExternalUrl(field)"
                                size="small"
                                type="link"
                                @click="handleCopyExternalLink(field)"
                              >
                                <IconifyIcon icon="lucide:copy" />
                                复制链接
                              </Button>
                            </div>

                            <span
                              v-else-if="isFieldReadonly(field)"
                              class="qms-exception-readonly-value"
                              :class="{
                                'qms-exception-readonly-value--multiline':
                                  Number(field.span || 1) > 1,
                              }"
                            >
                              {{ displayFieldValue(field) }}
                            </span>

                            <Input.TextArea
                              v-else-if="
                                field.component === 'Textarea' ||
                                field.component === 'JsonTextarea'
                              "
                              v-model:value="formData[field.field]"
                              :rows="field.rows || 3"
                              :placeholder="`请输入${getFieldLabel(field)}`"
                            />

                            <Select
                              v-else-if="field.component === 'Select'"
                              v-model:value="formData[field.field]"
                              allow-clear
                              :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                              :get-popup-container="getDetailPopupContainer"
                              :mode="field.mode"
                              :options="getFieldOptions(field)"
                              :placeholder="`请选择${getFieldLabel(field)}`"
                              show-search
                              @change="
                                (value) => handleFieldChange(field, value)
                              "
                            />

                            <Radio.Group
                              v-else-if="field.component === 'RadioGroup'"
                              v-model:value="formData[field.field]"
                              :options="getFieldOptions(field)"
                              @change="
                                (event) =>
                                  handleFieldChange(field, event.target.value)
                              "
                            />

                            <AutoComplete
                              v-else-if="field.component === 'AutoComplete'"
                              v-model:value="formData[field.field]"
                              allow-clear
                              :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                              :filter-option="filterAutoCompleteOption"
                              :get-popup-container="getDetailPopupContainer"
                              :options="getFieldOptions(field)"
                              :placeholder="`请选择或输入${getFieldLabel(field)}`"
                              @change="
                                (value) => handleFieldChange(field, value)
                              "
                            />

                            <DatePicker
                              v-else-if="field.component === 'DateTimePicker'"
                              v-model:value="formData[field.field]"
                              format="YYYY-MM-DD HH:mm:ss"
                              :get-popup-container="getDetailPopupContainer"
                              :popup-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                              show-time
                              value-format="YYYY-MM-DD HH:mm:ss"
                              @change="
                                (value) => handleFieldChange(field, value)
                              "
                            />

                            <DatePicker
                              v-else-if="field.component === 'DatePicker'"
                              v-model:value="formData[field.field]"
                              format="YYYY-MM-DD"
                              :get-popup-container="getDetailPopupContainer"
                              :popup-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                              value-format="YYYY-MM-DD"
                              @change="
                                (value) => handleFieldChange(field, value)
                              "
                            />

                            <InputNumber
                              v-else-if="field.component === 'InputNumber'"
                              v-model:value="formData[field.field]"
                              class="w-full"
                              :placeholder="`请输入${getFieldLabel(field)}`"
                              @change="
                                (value) => handleFieldChange(field, value)
                              "
                            />

                            <div
                              v-else-if="field.component === 'ReferencePicker'"
                              class="qms-exception-linked-ncr srm-reference-picker"
                            >
                              <Input
                                v-model:value="formData[field.field]"
                                :placeholder="
                                  field.allowManualInput
                                    ? `可输入${getFieldLabel(field)}或点击右侧选择`
                                    : `请选择${getFieldLabel(field)}`
                                "
                                :readonly="
                                  !field.allowManualInput ||
                                  isFieldReadonly(field)
                                "
                                @click="handleReferenceInputClick(field)"
                                @update:value="
                                  handleReferenceManualInput(field)
                                "
                              >
                                <template #suffix>
                                  <Button
                                    class="qms-exception-inline-icon-btn"
                                    size="small"
                                    title="选择引用数据"
                                    type="text"
                                    @click.stop="openReferenceSelector(field)"
                                  >
                                    <IconifyIcon icon="lucide:search" />
                                  </Button>
                                </template>
                              </Input>
                            </div>

                            <div
                              v-else-if="field.component === 'UrlInput'"
                              class="srm-url-input"
                            >
                              <Input
                                v-model:value="formData[field.field]"
                                class="srm-url-input__field"
                                placeholder="请输入 OA 审批链接（http/https）"
                              />
                              <Button
                                :disabled="!getExternalUrl(field)"
                                @click="handleCopyExternalLink(field)"
                              >
                                <IconifyIcon icon="lucide:copy" />
                                复制
                              </Button>
                              <Button
                                :disabled="!getExternalUrl(field)"
                                @click="handleOpenExternalLink(field)"
                              >
                                <IconifyIcon icon="lucide:external-link" />
                                打开
                              </Button>
                            </div>

                            <Input
                              v-else
                              v-model:value="formData[field.field]"
                              :placeholder="`请输入${getFieldLabel(field)}`"
                              @change="
                                (event) =>
                                  handleFieldChange(field, event.target.value)
                              "
                            />
                          </div>
                        </div>
                      </template>
                    </div>
                  </Form>
                </section>
                <slot
                  name="extra"
                  :mode="mode"
                  :readonly="mode === 'detail'"
                  :record="formData"
                ></slot>
                <SrmAttachmentPanel
                  ref="attachmentPanelRef"
                  :biz-id="formData.id"
                  :biz-type="attachmentBizType"
                  :category-options="activeAttachmentCategoryOptions"
                  :default-category="activeAttachmentDefaultCategory"
                  :modal-z-index="SRM_REFERENCE_MODAL_Z_INDEX"
                  :mode="mode"
                />
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </div>
  </AntModal>
  <PickerModal
    :config="materialPickerConfig"
    :initial-filters="materialPickerInitialFilters"
    :open="materialPickerOpen"
    :title="materialPickerTitle"
    :z-index="SRM_REFERENCE_MODAL_Z_INDEX"
    @close="handleMaterialPickerClose"
    @pick="handleMaterialPick"
  />
  <PickerModal
    :config="productBomPickerConfig"
    :initial-filters="productBomPickerInitialFilters"
    :open="productBomPickerOpen"
    :title="productBomPickerTitle"
    :z-index="SRM_REFERENCE_MODAL_Z_INDEX"
    @close="handleProductBomPickerClose"
    @pick="handleProductBomPick"
  />
  <ReferenceSelectModal @select="handleReferenceSelect" />
</template>
