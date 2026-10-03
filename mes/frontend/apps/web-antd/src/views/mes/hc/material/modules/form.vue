<script lang="ts" setup>
import type { MesHcMaterialApi } from '#/api/mes/hc/material';
import type { MesHcModelRuleApi } from '#/api/mes/hc/modelrule';
import type { MesHcProductModelApi } from '#/api/mes/hc/productmodel';

import { computed, nextTick, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { Button, Input, Select, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  createMaterial,
  generateMaterialCode,
  getMaterialDetail,
  previewMaterialBinding,
  updateMaterial,
} from '#/api/mes/hc/material';
import { getModelRuleDetail } from '#/api/mes/hc/modelrule';
import { getModelRuleSelectOptions } from '#/api/mes/hc/modelrule';
import { getProductModelDetail } from '#/api/mes/hc/productmodel';
import { getRecipePage } from '#/api/mes/hc/recipe';
import { getRouteDetail, getRouteSelectOptions } from '#/api/mes/hc/route';
import { getUnitSelectOptions } from '#/api/mes/base/unit';

import {
  PickerInline,
  PickerModal,
  productModelPickerConfig,
  recipePickerConfig,
  routePickerConfig,
} from '#/components/picker';
import type { PickerOption } from '#/components/picker';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);

type ExtAttrOption = { label: string; value: string };
type ExtAttrRow = MesHcMaterialApi.MaterialExtAttr & {
  _rowKey: string;
  _ruleDriven?: boolean;
  _required?: boolean;
  _valueOptions?: ExtAttrOption[];
};
// PickerOption 已从 '#/components/picker' 导入，无需本地重复定义
type RefOption = PickerOption;
type SegmentRow = {
  itemCode?: string;
  itemName?: string;
  segmentValue?: string;
  segmentText?: string;
  sort?: number;
  dictId?: number;
};
type RouteOperationRow = {
  seqNo?: number;
  operationCode?: string;
  operationName?: string;
  workCenterCode?: string;
  workCenterName?: string;
  reportRequired?: boolean;
  qcRequired?: boolean;
  batchSplitMode?: string;
  remark?: string;
};

const formData = ref<MesHcMaterialApi.Material>();
const materialExtAttrs = ref<ExtAttrRow[]>([]);
const extTableRef = ref<any>();
const invalidExtAttrRowKeys = ref<string[]>([]);
const currentExtAttrRowKey = ref('');
const defaultRouteCode = ref('');
const defaultRouteName = ref('');
const defaultRecipeCode = ref('');
const defaultRecipeName = ref('');
const routeModalOpen = ref(false);
const recipeModalOpen = ref(false);
const productModelModalOpen = ref(false);
const baseExpanded = ref(true);
const baselineExpanded = ref(true);
const bindingExpanded = ref(true);
const extExpanded = ref(true);
const previewLoading = ref(false);
const materialCodeGenerating = ref(false);
const bindingPreview = ref<MesHcMaterialApi.MaterialBindingPreview>();
const currentModelRuleItems = ref<MesHcModelRuleApi.ModelRuleItem[]>([]);
const currentModelRuleDicts = ref<MesHcModelRuleApi.ModelRuleDict[]>([]);
const modelRuleOptions = ref<Array<{ label: string; value: number }>>([]);
const productModelValue = ref('');
const modelCodeRuleValue = ref<number | undefined>();
const modelCodeDisplay = ref('');
const routeOperationRows = ref<RouteOperationRow[]>([]);
const unitOptions = ref<Array<{ value: number; label: string; code?: string; name?: string }>>([]);

const getTitle = computed(() => (formData.value?.id ? '编辑物料主数据' : '新增物料主数据'));

const parsedModelSegments = computed<SegmentRow[]>(() => {
  const source = bindingPreview.value?.modelSegmentsJson || formData.value?.modelSegmentsJson;
  if (!source) return [];
  try {
    const parsed = JSON.parse(source);
    return Array.isArray(parsed)
      ? [...parsed].sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0))
      : [];
  } catch {
    return [];
  }
});

const currentModelRuleLabel = computed(() => {
  const option = modelRuleOptions.value.find((item) => item.value === modelCodeRuleValue.value);
  return option?.label || '';
});

function extractProductModelSegments(detail?: MesHcProductModelApi.ProductModel | null) {
  if (detail?.modelSegments?.length) {
    return [...detail.modelSegments].sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0));
  }
  if (!detail?.segmentSnapshotJson) return [];
  try {
    const parsed = JSON.parse(detail.segmentSnapshotJson);
    return Array.isArray(parsed)
      ? [...parsed].sort((a, b) => Number(a?.sort || 0) - Number(b?.sort || 0))
      : [];
  } catch {
    return [];
  }
}

function isFormulaAttrRow(item?: { attrCode?: string; attrName?: string } | null) {
  // 只针对 attrCode='formula' 的行作为配方属性，避免"重复配方标记"被误判
  return String(item?.attrCode || '') === 'formula';
}

function buildRuleValueOptions(
  item: MesHcModelRuleApi.ModelRuleItem,
  dicts: MesHcModelRuleApi.ModelRuleDict[],
): ExtAttrOption[] {
  return dicts
    .filter((dict) => dict.itemCode === item.itemCode)
    .map((dict) => ({
      value: String(dict.dictCode || dict.dictValue || ''),
      label: [dict.dictCode, dict.dictValue].filter(Boolean).join(' / ') || '-',
    }))
    .filter((option) => option.value);
}

function mergeRuleDrivenExtAttrs(ruleItems: MesHcModelRuleApi.ModelRuleItem[]) {
  const existingValueMap = new Map(
    materialExtAttrs.value
      .filter((item) => item.attrCode)
      .map((item) => [String(item.attrCode), item]),
  );
  const ruleRows = ruleItems
    .filter((item) => item.itemCode && item.parseType !== 'skip' && item.parseType !== 'fixed')
    .map((item, index) => {
      const existing = existingValueMap.get(String(item.itemCode));
      return normalizeExtAttrRow(
        {
          ...(existing || {}),
          attrCode: String(item.itemCode || ''),
          attrName: item.itemName || item.itemCode || '',
          valueType: 'TEXT',
          sort: index + 1,
          attrValue: existing?.attrValue || '',
          _ruleDriven: true,
          _required: Boolean(item.requiredFlag),
          _valueOptions:
            item.dataSourceType === 'dict'
              ? buildRuleValueOptions(item, currentModelRuleDicts.value)
              : undefined,
        },
        index + 1,
      );
    });
  const extraRows = materialExtAttrs.value
    .filter((item) => !item._ruleDriven && !ruleRows.some((row) => row.attrCode === item.attrCode))
    .map((item, index) => normalizeExtAttrRow(item, ruleRows.length + index + 1));
  materialExtAttrs.value = [...ruleRows, ...extraRows].map((item, index) =>
    normalizeExtAttrRow({ ...item, sort: index + 1 }, index + 1),
  );
  currentExtAttrRowKey.value = materialExtAttrs.value[0]?._rowKey || '';
  recalculateExtTable();
}

async function loadModelRuleMeta(ruleId?: number | string | null) {
  const numericRuleId = Number(ruleId || 0);
  if (!numericRuleId) {
    currentModelRuleItems.value = [];
    currentModelRuleDicts.value = [];
    materialExtAttrs.value = materialExtAttrs.value.map((item, index) =>
      normalizeExtAttrRow(
        { ...item, sort: index + 1, _ruleDriven: false, _required: false, _valueOptions: undefined },
        index + 1,
      ),
    );
    return;
  }
  const detail = await getModelRuleDetail(numericRuleId);
  currentModelRuleItems.value = detail.modelRuleItems || [];
  currentModelRuleDicts.value = detail.modelRuleDicts || [];
  mergeRuleDrivenExtAttrs(currentModelRuleItems.value);
}

function normalizeExtAttrRow(
  item?: Partial<MesHcMaterialApi.MaterialExtAttr>,
  sort = materialExtAttrs.value.length + 1,
): ExtAttrRow {
  return {
    id: item?.id || 0,
    attrCode: item?.attrCode || '',
    attrName: item?.attrName || '',
    attrValue: item?.attrValue || '',
    valueType: item?.valueType || 'TEXT',
    sort: item?.sort || sort,
    _ruleDriven: (item as ExtAttrRow | undefined)?._ruleDriven,
    _required: (item as ExtAttrRow | undefined)?._required,
    _valueOptions: (item as ExtAttrRow | undefined)?._valueOptions,
    _rowKey:
      (item as any)?._rowKey ||
      `ext-attr-${Date.now()}-${sort}-${Math.random().toString(36).slice(2, 8)}`,
  };
}
function isEmptyExtAttr(item: Partial<MesHcMaterialApi.MaterialExtAttr>) {
  return ![item.attrCode, item.attrName, item.attrValue].some(
    (value) => value !== undefined && value !== null && String(value).trim() !== '',
  );
}
function focusExtAttrRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const holder = document.querySelector(
      `.hc-material-modal [data-ext-focus-key="${rowKey}"]`,
    ) as HTMLElement | null;
    if (!holder) return;
    holder.scrollIntoView({ block: 'center', behavior: 'smooth' });
    const target = holder.querySelector('input') as HTMLElement | null;
    target?.focus?.();
  });
}
function scrollToExtAttrTableRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const row = materialExtAttrs.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    extTableRef.value?.scrollToRow?.(row);
  });
}
function validateExtAttrs(items: ExtAttrRow[]) {
  const emptyIndex = items.findIndex((item) => isEmptyExtAttr(item));
  if (emptyIndex >= 0) {
    invalidExtAttrRowKeys.value = [items[emptyIndex]!._rowKey];
    currentExtAttrRowKey.value = items[emptyIndex]!._rowKey;
    extExpanded.value = true;
    message.warning(`型号规则属性第 ${emptyIndex + 1} 行为空`);
    scrollToExtAttrTableRow(items[emptyIndex]!._rowKey);
    focusExtAttrRow(items[emptyIndex]!._rowKey);
    return false;
  }
  const invalidIndex = items.findIndex(
    (item) => !item.attrCode?.trim() || !item.attrName?.trim(),
  );
  if (invalidIndex >= 0) {
    invalidExtAttrRowKeys.value = [items[invalidIndex]!._rowKey];
    currentExtAttrRowKey.value = items[invalidIndex]!._rowKey;
    extExpanded.value = true;
    message.warning(`型号规则属性第 ${invalidIndex + 1} 行需填写属性名称`);
    scrollToExtAttrTableRow(items[invalidIndex]!._rowKey);
    focusExtAttrRow(items[invalidIndex]!._rowKey);
    return false;
  }
  invalidExtAttrRowKeys.value = [];
  currentExtAttrRowKey.value = '';
  return true;
}
function recalculateExtTable() {
  nextTick(() => extTableRef.value?.recalculate?.(true));
}
async function fetchRouteOptions() {
  return (await getRouteSelectOptions()).map((item) => ({
    value: item.value,
    label: item.label,
    code: item.code,
    status: item.status,
  }));
}
async function fetchRecipeOptions() {
  const { list = [] } = await getRecipePage({
    pageNo: 1,
    pageSize: 200,
  });
  return list.map((item) => ({
    value: item.id,
    label: item.modelCode || item.recipeName || '',
    code: item.recipeCode,
    name: item.recipeName,
    recipeType: item.recipeType,
    modelCode: item.modelCode,
    remark: item.remark,
    status: item.status,
  }));
}
async function loadModelRuleOptions() {
  modelRuleOptions.value = (await getModelRuleSelectOptions()).map((item) => ({
    label: item.label,
    value: Number(item.value),
  }));
}
async function loadUnitOptions() {
  if (unitOptions.value.length > 0) return;
  unitOptions.value = (await getUnitSelectOptions()).map((item) => ({
    value: Number(item.value),
    label: item.label,
    code: item.code,
    name: item.name,
  }));
}
function clearProductModelBinding() {
  productModelValue.value = '';
  formApi.setFieldValue?.('productModelId', undefined);
  formApi.setFieldValue?.('productModelName', '');
  formApi.setFieldValue?.('modelCodeRuleId', undefined);
  formApi.setFieldValue?.('modelCode', '');
  formApi.setFieldValue?.('modelSegmentsJson', undefined);
  formApi.setFieldValue?.('defaultRecipeId', undefined);
  formApi.setFieldValue?.('defaultRecipeCode', '');
  formApi.setFieldValue?.('defaultRecipeName', '');
  modelCodeRuleValue.value = undefined;
  modelCodeDisplay.value = '';
  materialExtAttrs.value = [];
  bindingPreview.value = undefined;
}

async function applyProductModelDetail(productModelId: number) {
  const detail = await getProductModelDetail(productModelId);
  const modelSegments = extractProductModelSegments(detail);
  const modelSegmentsJson =
    detail.segmentSnapshotJson ||
    (modelSegments.length > 0 ? JSON.stringify(modelSegments) : undefined);
  productModelValue.value = detail.modelCode || '';
  formApi.setFieldValue?.('productModelId', productModelId);
  formApi.setFieldValue?.('productModelName', detail.modelName || '');
  formApi.setFieldValue?.('modelCodeRuleId', detail.modelRuleId);
  formApi.setFieldValue?.('modelCode', detail.modelCode || '');
  formApi.setFieldValue?.('modelSegmentsJson', modelSegmentsJson);
  formApi.setFieldValue?.('defaultRecipeId', detail.recipeId);
  formApi.setFieldValue?.('defaultRecipeCode', detail.recipeCode || '');
  formApi.setFieldValue?.('defaultRecipeName', detail.recipeName || '');
  modelCodeRuleValue.value = detail.modelRuleId;
  defaultRecipeCode.value = detail.recipeCode || '';
  defaultRecipeName.value = detail.recipeName || '';
  modelCodeDisplay.value = detail.modelCode || '';
  materialExtAttrs.value = modelSegments.map((item, index) =>
    normalizeExtAttrRow(
      {
        attrCode: item.itemCode || '',
        attrName: item.itemName || '',
        attrValue: item.segmentText || item.segmentValue || '',
        sort: item.sort || index + 1,
      },
      index + 1,
    ),
  );
  bindingPreview.value = {
    ...(bindingPreview.value || {}),
    modelCode: detail.modelCode,
    modelSegmentsJson,
  } as MesHcMaterialApi.MaterialBindingPreview;
}

function handleProductModelInput(value: string) {
  productModelValue.value = value;
  if (!value.trim()) {
    clearProductModelBinding();
  } else {
    formApi.setFieldValue?.('productModelId', undefined);
    formApi.setFieldValue?.('productModelName', '');
    formApi.setFieldValue?.('modelCodeRuleId', undefined);
    formApi.setFieldValue?.('modelCode', '');
    formApi.setFieldValue?.('modelSegmentsJson', undefined);
    formApi.setFieldValue?.('defaultRecipeId', undefined);
    formApi.setFieldValue?.('defaultRecipeCode', '');
    formApi.setFieldValue?.('defaultRecipeName', '');
    modelCodeRuleValue.value = undefined;
    modelCodeDisplay.value = '';
    materialExtAttrs.value = [];
    bindingPreview.value = undefined;
  }
}

async function handlePickProductModel(option: RefOption) {
  productModelModalOpen.value = false;
  await applyProductModelDetail(Number(option.id));
}
async function handleModelRuleChange(value?: number | string) {
  const numericValue = value ? Number(value) : undefined;
  modelCodeRuleValue.value = Number.isNaN(Number(numericValue)) ? undefined : numericValue;
  modelCodeDisplay.value = '';
  defaultRecipeCode.value = '';
  defaultRecipeName.value = '';
  formApi.setFieldValue?.('defaultRecipeId', undefined);
  formApi.setFieldValue?.('modelCodeRuleId', modelCodeRuleValue.value);
  formApi.setFieldValue?.('modelCode', '');
  await loadModelRuleMeta(value);
  void handleRefreshBindingPreview();
}
function handleDefaultRouteInput(field: 'code' | 'name', value: string) {
  if (field === 'code') {
    defaultRouteCode.value = value;
    defaultRouteName.value = '';
  } else {
    defaultRouteName.value = value;
    defaultRouteCode.value = '';
  }
  formApi.setFieldValue?.('defaultRouteId', undefined);
  void syncRouteOperations();
  void handleRefreshBindingPreview();
}
function handleDefaultRecipeInput(field: 'code' | 'name', value: string) {
  if (field === 'code') {
    defaultRecipeCode.value = value;
    defaultRecipeName.value = '';
  } else {
    defaultRecipeName.value = value;
    defaultRecipeCode.value = '';
  }
  formApi.setFieldValue?.('defaultRecipeId', undefined);
  void handleRefreshBindingPreview();
}
function handlePickDefaultRoute(option: RefOption) {
  defaultRouteCode.value = String(option.code || '').trim();
  defaultRouteName.value = String(option.name || option.label || '').trim();
  formApi.setFieldValue?.('defaultRouteId', Number(option.id));
  routeModalOpen.value = false;
  void syncRouteOperations(Number(option.id));
  void handleRefreshBindingPreview();
}
function handlePickDefaultRecipe(option: RefOption) {
  defaultRecipeCode.value = String(option.code || '').trim();
  defaultRecipeName.value = String(option.extra?.modelCode || option.label || '').trim();
  formApi.setFieldValue?.('defaultRecipeId', Number(option.id));
  recipeModalOpen.value = false;
  void handleRefreshBindingPreview();
}
function syncDefaultReferenceDisplays(data?: MesHcMaterialApi.Material) {
  defaultRouteCode.value = String(data?.defaultRouteCode || '').trim();
  defaultRouteName.value = String(data?.defaultRouteName || '').trim();
  defaultRecipeCode.value = String(data?.defaultRecipeCode || '').trim();
  defaultRecipeName.value = String(data?.defaultRecipeName || '').trim();
}
async function syncRouteOperations(routeId?: number) {
  if (!routeId) {
    routeOperationRows.value = [];
    return;
  }
  const detail = await getRouteDetail(routeId);
  routeOperationRows.value = (detail.routeOperations || []).map((item) => ({
    seqNo: item.seqNo,
    operationCode: item.operationCode,
    operationName: item.operationName,
    workCenterCode: item.workCenterCode,
    workCenterName: item.workCenterName,
    reportRequired: item.reportRequired,
    qcRequired: item.qcRequired,
    batchSplitMode: item.batchSplitMode,
    remark: item.remark,
  }));
}
function displayText(value?: string | number | null) {
  return value === null || value === undefined || value === '' ? '-' : String(value);
}
function formatBatchSplitMode(value?: string | null) {
  const labelMap: Record<string, string> = {
    none: '不拆分',
    segment: '按段拆分',
    piece: '按片拆分',
    box: '按箱拆分',
  };
  return labelMap[String(value || '').trim()] || displayText(value);
}
function buildDraftExtAttrs(strict = false) {
  const rawExtAttrs = materialExtAttrs.value.map((item, index) => normalizeExtAttrRow(item, index + 1));
  return rawExtAttrs
    .filter((item) => strict ? !isEmptyExtAttr(item) : Boolean(item.attrCode?.trim() || item.attrName?.trim() || item.attrValue?.trim()))
    .map((item, index) => ({
      id: item.id || 0,
      materialId: item.materialId,
      materialCode: item.materialCode,
      sort: item.sort || index + 1,
      attrCode: item.attrCode?.trim(),
      attrName: item.attrName?.trim(),
      attrValue: isFormulaAttrRow(item) ? defaultRecipeName.value.trim() : item.attrValue?.trim(),
      valueType: item.valueType,
    }));
}

async function buildDraftMaterial(strict = false) {
  const data = (await formApi.getValues()) as MesHcMaterialApi.Material;
  const extAttrs = buildDraftExtAttrs(strict);
  fillMaterialUnitSnapshot(data, 'base');
  fillMaterialUnitSnapshot(data, 'stock');
  fillMaterialUnitSnapshot(data, 'produce');
  data.defaultRouteCode = defaultRouteCode.value.trim() || undefined;
  data.defaultRouteName = defaultRouteName.value.trim() || undefined;
  data.defaultRecipeCode = defaultRecipeCode.value.trim() || undefined;
  data.defaultRecipeName = defaultRecipeName.value.trim() || undefined;
  data.materialExtAttrs = extAttrs || [];
  return data;
}

function fillMaterialUnitSnapshot(data: MesHcMaterialApi.Material, kind: 'base' | 'stock' | 'produce') {
  const idKey = `${kind}UnitId` as keyof MesHcMaterialApi.Material;
  const codeKey = `${kind}UnitCode` as keyof MesHcMaterialApi.Material;
  const nameKey = `${kind}UnitName` as keyof MesHcMaterialApi.Material;
  const oldKey = (kind === 'base' ? 'baseUom' : kind === 'stock' ? 'stockUom' : 'produceUom') as keyof MesHcMaterialApi.Material;
  const option = unitOptions.value.find((item) => Number(item.value) === Number(data[idKey]));
  (data as any)[codeKey] = option?.code || '';
  (data as any)[nameKey] = option?.name || '';
  (data as any)[oldKey] = option?.code || '';
}

function normalizeMaterialUnitFormData(data?: MesHcMaterialApi.Material) {
  if (!data) return data;
  normalizeOneUnitFormData(data, 'base', data.baseUom);
  normalizeOneUnitFormData(data, 'stock', data.stockUom);
  normalizeOneUnitFormData(data, 'produce', data.produceUom);
  return data;
}

function normalizeOneUnitFormData(data: MesHcMaterialApi.Material, kind: 'base' | 'stock' | 'produce', legacyCode?: string) {
  const idKey = `${kind}UnitId` as keyof MesHcMaterialApi.Material;
  const codeKey = `${kind}UnitCode` as keyof MesHcMaterialApi.Material;
  const nameKey = `${kind}UnitName` as keyof MesHcMaterialApi.Material;
  const currentId = data[idKey];
  if (currentId) return;
  const code = String((data[codeKey] as any) || legacyCode || '').trim();
  if (!code) return;
  const option = unitOptions.value.find((item) => item.code === code);
  if (!option) return;
  (data as any)[idKey] = option.value;
  (data as any)[codeKey] = option.code;
  (data as any)[nameKey] = option.name;
}

function canPreviewCurrentRule() {
  if (productModelValue.value) {
    return true;
  }
  const requiredCodes = new Set(
    currentModelRuleItems.value
      .filter((item) => Boolean(item.requiredFlag) && item.parseType !== 'skip' && item.parseType !== 'fixed')
      .map((item) => String(item.itemCode || ''))
      .filter(Boolean),
  );
  if (requiredCodes.size === 0) {
    return true;
  }
  return materialExtAttrs.value
    .filter((item) => requiredCodes.has(String(item.attrCode || '')))
    .every((item) =>
      isFormulaAttrRow(item)
        ? Boolean(defaultRecipeName.value.trim())
        : Boolean(String(item.attrValue || '').trim()),
    );
}

async function handleRefreshBindingPreview() {
  previewLoading.value = true;
  try {
    const draft = await buildDraftMaterial(false);
    if (!draft) {
      bindingPreview.value = undefined;
      return;
    }
    const hasPreviewSource = Boolean(
      draft.id ||
      draft.productModelId ||
      draft.modelCodeRuleId ||
      draft.defaultRecipeId ||
      draft.defaultRouteId ||
      draft.materialExtAttrs?.length,
    );
    if (!hasPreviewSource) {
      bindingPreview.value = undefined;
      routeOperationRows.value = [];
      return;
    }
    if (!canPreviewCurrentRule()) {
      bindingPreview.value = {
        ...bindingPreview.value,
        materialId: draft.id,
        materialCode: draft.materialCode,
        materialName: draft.materialName,
        modelCodeRuleId: draft.modelCodeRuleId,
        modelCode: '',
        modelSegmentsJson: undefined,
        suggestedRecipeId: undefined,
        suggestedRecipeCode: undefined,
        suggestedRecipeName: undefined,
        suggestedRouteId: undefined,
        suggestedRouteCode: undefined,
        suggestedRouteName: undefined,
        consumeGroupCodes: [],
        bomCandidates: [],
      };
      return;
    }
    bindingPreview.value = await previewMaterialBinding(draft);
    modelCodeDisplay.value = String(bindingPreview.value?.modelCode || '');
  } catch {
    bindingPreview.value = undefined;
    modelCodeDisplay.value = '';
  } finally {
    previewLoading.value = false;
  }
}

async function handleGenerateMaterialCode() {
  materialCodeGenerating.value = true;
  try {
    const data = await buildDraftMaterial(true);
    if (!data) return;
    const resp = await generateMaterialCode(data);
    const code = String(resp?.materialCode || '').trim();
    formApi.setFieldValue?.('materialCode', code);
    await handleRefreshBindingPreview();
    message.success(code ? `已生成物料编码：${code}` : '未生成有效的物料编码');
  } finally {
    materialCodeGenerating.value = false;
  }
}

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 120 },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[1320px] hc-material-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      baseExpanded.value = true;
      return;
    }
    modalApi.lock();
    try {
      const data = await buildDraftMaterial(true);
      if (!data) return;
      await (formData.value?.id ? updateMaterial(data) : createMaterial(data));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      materialExtAttrs.value = [];
      currentModelRuleItems.value = [];
      currentModelRuleDicts.value = [];
      productModelValue.value = '';
      modelCodeRuleValue.value = undefined;
      modelCodeDisplay.value = '';
      invalidExtAttrRowKeys.value = [];
      currentExtAttrRowKey.value = '';
      defaultRouteCode.value = '';
      defaultRouteName.value = '';
      defaultRecipeCode.value = '';
      defaultRecipeName.value = '';
      bindingPreview.value = undefined;
      routeOperationRows.value = [];
      previewLoading.value = false;
      await formApi.resetForm();
      if (modelRuleOptions.value.length === 0) {
        await loadModelRuleOptions();
      }
      await loadUnitOptions();
      return;
    }
    const data = modalApi.getData<MesHcMaterialApi.Material>();
    if (!data?.id) {
      formData.value = undefined;
      materialExtAttrs.value = [];
      currentModelRuleItems.value = [];
      currentModelRuleDicts.value = [];
      productModelValue.value = '';
      modelCodeRuleValue.value = undefined;
      modelCodeDisplay.value = '';
      invalidExtAttrRowKeys.value = [];
      currentExtAttrRowKey.value = '';
      defaultRouteCode.value = '';
      defaultRouteName.value = '';
      defaultRecipeCode.value = '';
      defaultRecipeName.value = '';
      bindingPreview.value = undefined;
      routeOperationRows.value = [];
      previewLoading.value = false;
      await formApi.resetForm();
      if (modelRuleOptions.value.length === 0) {
        await loadModelRuleOptions();
      }
      await loadUnitOptions();
      return;
    }
    modalApi.lock();
    try {
      if (modelRuleOptions.value.length === 0) {
        await loadModelRuleOptions();
      }
      await loadUnitOptions();
      formData.value = normalizeMaterialUnitFormData(await getMaterialDetail(data.id));
      await formApi.setValues(formData.value);
      productModelValue.value = formData.value.modelCode || '';
      modelCodeRuleValue.value = formData.value.modelCodeRuleId;
      modelCodeDisplay.value = formData.value.modelCode || '';
      syncDefaultReferenceDisplays(formData.value);
      await syncRouteOperations(formData.value.defaultRouteId);
      materialExtAttrs.value =
        formData.value.materialExtAttrs?.map((item, index) => normalizeExtAttrRow(item, index + 1)) || [];
      currentExtAttrRowKey.value = materialExtAttrs.value[0]?._rowKey || '';
      if (materialExtAttrs.value.length > 0) recalculateExtTable();
      await handleRefreshBindingPreview();
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="hc-master-modal">
      <div class="hc-master-modal__scroll">
        <div class="hc-master-panel">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">基本信息</span>
            <div class="hc-master-panel__header-spacer" />
            <Button size="small" :loading="materialCodeGenerating" @click="handleGenerateMaterialCode">
              生成物料编码
            </Button>
            <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="baseExpanded = !baseExpanded">
              <IconifyIcon :icon="baseExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="baseExpanded" class="hc-master-panel__body">
            <div class="hc-master-panel__base-box">
              <Form />
            </div>
          </div>
        </div>

        <div class="hc-master-panel">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">产品型号信息</span>
            <div class="hc-master-panel__header-spacer" />
            <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="extExpanded = !extExpanded">
              <IconifyIcon :icon="extExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="extExpanded" class="hc-master-panel__body">
            <div class="hc-master-panel__base-box">
              <div class="hc-info-grid">
                <div class="hc-info-row">
                  <div class="hc-info-row__label">型号编码</div>
                  <div class="hc-info-row__control">
                    <PickerInline
                      :model-value="productModelValue"
                      :config="productModelPickerConfig"
                      placeholder="请选择型号编码"
                      @update:model-value="handleProductModelInput"
                      @search="() => { productModelModalOpen = true; }"
                      @pick="handlePickProductModel"
                    />
                  </div>
                </div>
                <div class="hc-info-row">
                  <div class="hc-info-row__label">型号编码规则</div>
                  <div class="hc-info-row__control">
                    <Input :value="currentModelRuleLabel || '-'" readonly />
                  </div>
                </div>
                <div class="hc-info-row hc-info-row--full">
                  <div class="hc-info-row__label">型号规则段值</div>
                  <div class="hc-info-row__control">
                    <div class="hc-readonly-box hc-segment-box">
                      <span
                        v-for="segment in parsedModelSegments"
                        :key="`${segment.itemCode}-${segment.sort}`"
                        class="hc-segment-tag"
                      >
                        {{ displayText(segment.itemName || segment.itemCode) }}：{{ displayText(segment.segmentValue) }}
                      </span>
                      <span v-if="parsedModelSegments.length === 0" class="hc-empty-text">暂无规则段值</span>
                    </div>
                  </div>
                </div>
                <div class="hc-info-row hc-info-row--full">
                  <div class="hc-info-row__label">规则属性</div>
                  <div class="hc-info-row__control">
                    <div class="material-ext-table-wrap">
                      <VxeTable
                        ref="extTableRef"
                        :data="materialExtAttrs"
                        :row-class-name="({ row }) => invalidExtAttrRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentExtAttrRowKey === row._rowKey ? 'is-current-row' : ''"
                        auto-resize
                        border
                        stripe
                        :round="false"
                        size="small"
                        height="360"
                        show-overflow
                        row-id="_rowKey"
                        @cell-click="({ row }) => (currentExtAttrRowKey = row._rowKey)"
                      >
                        <VxeColumn field="attrName" title="属性名称" width="220">
                          <template #default="{ row }">
                            <div class="hc-rule-attr-name">
                              {{ row.attrName }}
                            </div>
                          </template>
                        </VxeColumn>
                        <VxeColumn field="attrValue" title="属性值" min-width="320">
                          <template #default="{ row }">
                            <Input :value="row.attrValue || '-'" readonly />
                          </template>
                        </VxeColumn>
                      </VxeTable>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="hc-master-panel">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">生产工艺信息</span>
            <div class="hc-master-panel__header-spacer" />
            <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="baselineExpanded = !baselineExpanded">
              <IconifyIcon :icon="baselineExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="baselineExpanded" class="hc-master-panel__body">
            <div class="hc-master-panel__base-box">
              <div class="hc-info-grid">
                <div class="hc-info-row">
                  <div class="hc-info-row__label">工艺路线编码</div>
                  <div class="hc-info-row__control">
                    <PickerInline
                      :model-value="defaultRouteCode"
                      :config="routePickerConfig"
                      placeholder="请选择工艺路线编码"
                      @update:model-value="(value) => handleDefaultRouteInput('code', value)"
                      @search="() => { routeModalOpen = true; }"
                      @pick="handlePickDefaultRoute"
                    />
                  </div>
                </div>
                <div class="hc-info-row">
                  <div class="hc-info-row__label">工艺路线名称</div>
                  <div class="hc-info-row__control">
                    <PickerInline
                      :model-value="defaultRouteName"
                      :config="routePickerConfig"
                      placeholder="请选择工艺路线名称"
                      @update:model-value="(value) => handleDefaultRouteInput('name', value)"
                      @search="() => { routeModalOpen = true; }"
                      @pick="handlePickDefaultRoute"
                    />
                  </div>
                </div>
                <div class="hc-info-row hc-info-row--full hc-info-row--table">
                  <div class="hc-info-row__label">工艺路线工序</div>
                  <div class="hc-info-row__control">
                    <div class="hc-table-box">
                      <VxeTable :data="routeOperationRows" border stripe size="small" height="320" show-overflow>
                        <VxeColumn field="seqNo" title="顺序" width="80" align="center" />
                        <VxeColumn field="operationCode" title="工序编码" min-width="120" />
                        <VxeColumn field="operationName" title="工序名称" min-width="160" />
                        <VxeColumn field="workCenterCode" title="工作中心编码" min-width="140" />
                        <VxeColumn field="workCenterName" title="工作中心名称" min-width="160" />
                        <VxeColumn field="reportRequired" title="报工" width="80" align="center">
                          <template #default="{ row }">{{ row.reportRequired ? '是' : '否' }}</template>
                        </VxeColumn>
                        <VxeColumn field="qcRequired" title="质检" width="80" align="center">
                          <template #default="{ row }">{{ row.qcRequired ? '是' : '否' }}</template>
                        </VxeColumn>
                        <VxeColumn field="batchSplitMode" title="批次拆分方式" min-width="120">
                          <template #default="{ row }">{{ formatBatchSplitMode(row.batchSplitMode) }}</template>
                        </VxeColumn>
                        <VxeColumn field="remark" title="备注" min-width="160" />
                      </VxeTable>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <PickerModal
      :config="routePickerConfig"
      :open="routeModalOpen"
      title="选择工艺路线"
      @close="routeModalOpen = false"
      @pick="handlePickDefaultRoute"
    />
    <PickerModal
      :config="productModelPickerConfig"
      :open="productModelModalOpen"
      title="选择型号编码"
      @close="productModelModalOpen = false"
      @pick="handlePickProductModel"
    />
    <PickerModal
      :config="recipePickerConfig"
      :open="recipeModalOpen"
      title="选择配方"
      @close="recipeModalOpen = false"
      @pick="handlePickDefaultRecipe"
    />
  </Modal>
</template>

<style>
.hc-material-modal .ant-modal-body {
  padding-top: 16px;
}
</style>

<style scoped>
.hc-master-modal {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.hc-master-modal__scroll {
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}

.hc-master-panel {
  border: none;
  background: transparent;
  overflow: visible;
  min-width: 0;
  padding: 0;
}

.hc-master-panel__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px 0;
}

.hc-master-panel__title-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 104px;
  height: 24px;
  padding: 0 14px;
  border-radius: 3px;
  background: #8b8b8b;
  color: #fff;
  font-size: 13px;
  line-height: 24px;
}

.hc-master-panel__header-spacer {
  flex: 1;
}

.hc-master-panel__toggle-btn {
  padding-inline: 6px;
}

.hc-master-panel__body {
  padding: 2px 12px 12px;
}

.hc-master-panel__base-box {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
}

.hc-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
}

.hc-info-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  align-items: center;
  column-gap: 12px;
  min-width: 0;
}

.hc-info-row--full {
  grid-column: 1 / -1;
}

.hc-info-row--table {
  align-items: start;
}

.hc-info-row__label {
  color: rgb(0 0 0 / 88%);
  font-size: 14px;
  line-height: 32px;
  text-align: right;
}

.hc-info-row__control {
  min-width: 0;
}

.hc-readonly-box {
  min-height: 32px;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fafafa;
}

.hc-segment-box {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hc-segment-tag {
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  height: 24px;
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  background: #fff;
  font-size: 12px;
  line-height: 22px;
}

.hc-empty-text {
  color: #8c8c8c;
  line-height: 24px;
}

.hc-rule-attr-name {
  line-height: 32px;
  color: rgb(0 0 0 / 88%);
}

.hc-rule-attr-required {
  margin-left: 4px;
  color: #ff4d4f;
}

.hc-table-box {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}

.material-ext-table-wrap {
  width: 100%;
}

.material-native-select {
  width: 100%;
  height: 32px;
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.material-ext-table-wrap :deep(.is-invalid-row > td) {
  background: #fff2f0 !important;
}

.material-ext-table-wrap :deep(.is-current-row > td) {
  background: #e6f4ff !important;
}
</style>
