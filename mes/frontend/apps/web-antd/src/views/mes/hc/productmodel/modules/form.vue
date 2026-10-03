<script lang="ts" setup>
import type { MesHcModelRuleApi } from '#/api/mes/hc/modelrule';
import type { MesHcProductModelApi } from '#/api/mes/hc/productmodel';
import type { MesHcRecipeApi } from '#/api/mes/hc/recipe';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, InputNumber, message, Select, Switch, Tooltip } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import { getModelRuleDetail } from '#/api/mes/hc/modelrule';
import { getRecipePage } from '#/api/mes/hc/recipe';
import {
  createProductModel,
  generateProductModelCode,
  getProductModelDetail,
  updateProductModel,
} from '#/api/mes/hc/productmodel';
import { materialPickerConfig, PickerInline, PickerModal, routePickerConfig } from '#/components/picker';
import type { PickerOption } from '#/components/picker';

import { materialTypeOptions, optionalSizeOptions, prodTypeOptions, sizeOptions, useFormSchema } from '../data';

type SegmentRow = MesHcProductModelApi.ProductModelSegment & { _rowKey: string };
type MaterialRow = MesHcProductModelApi.ProductModelMaterial & { _rowKey: string };
type RefOption = PickerOption;

const emit = defineEmits(['success']);

const rowSeed = ref(1);
const formData = ref<MesHcProductModelApi.ProductModel>();
const segmentRows = ref<SegmentRow[]>([]);
const materialRows = ref<MaterialRow[]>([]);
const ruleDicts = ref<MesHcModelRuleApi.ModelRuleDict[]>([]);
const recipeOptions = ref<Array<MesHcRecipeApi.Recipe & { label: string; value: number }>>([]);
const currentValues = ref<Partial<MesHcProductModelApi.ProductModel>>({});
const suppressFormSync = ref(false);
const segmentTableRef = ref<any>();
const materialTableRef = ref<any>();
const routeModalOpen = ref(false);
const defaultRouteName = ref('');
const materialModalOpen = ref(false);
const activeMaterialRowKey = ref('');

const title = computed(() => (formData.value?.id ? '编辑产品型号字典' : '新增产品型号字典'));
const readonlyLocked = computed(() => !!formData.value?.referencedFlag);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
  handleValuesChange: (_values, changedFields) => {
    if (suppressFormSync.value) return;
    currentValues.value = {
      ...currentValues.value,
      ...(_values as MesHcProductModelApi.ProductModel),
    };
    if (changedFields.includes('modelRuleId')) {
      const ruleId = _values.modelRuleId as number | undefined;
      handleRuleChange(ruleId);
    }
    if (changedFields.includes('modelLevel') && _values.modelLevel === 'FAMILY') {
      currentValues.value.parentModelId = undefined;
      formApi.setFieldValue?.('parentModelId', undefined);
    }
    if (changedFields.includes('prodType')) {
      syncProdTypeField(_values.prodType as string | undefined);
    }
    if (changedFields.includes('categoryCode')) {
      syncCategoryField(_values.categoryCode as string | undefined);
    }
    if (changedFields.includes('sizeSpec')) {
      syncSizeField(_values.sizeSpec as string | undefined);
    }
    if (changedFields.includes('recipeId')) {
      syncRecipeField(Number(_values.recipeId || 0) || undefined);
    }
    if (changedFields.includes('defaultRouteId')) {
      syncRouteSelection({
        id: Number(_values.defaultRouteId || 0) || undefined,
        code: String(_values.defaultRouteCode || ''),
        name: String(_values.defaultRouteName || ''),
      });
    }
  },
});

const polishTypeOptions = [
  { label: '通用白垫', value: 'W' },
  { label: '通用黑垫', value: 'B' },
];

function categoryCodeToSegmentValue(categoryCode?: string) {
  if (categoryCode === 'WHITE_PAD') return 'W';
  if (categoryCode === 'BLACK_PAD') return 'B';
  return categoryCode || '';
}

function segmentValueToCategoryCode(value?: string) {
  if (value === 'W') return 'WHITE_PAD';
  if (value === 'B') return 'BLACK_PAD';
  return value || '';
}

function categoryLabelBySegmentValue(value?: string) {
  return polishTypeOptions.find((item) => item.value === value)?.label || '';
}

const nextRowKey = (prefix: string) => `${prefix}-${Date.now()}-${++rowSeed.value}`;
const normalizeSegment = (row?: Partial<MesHcProductModelApi.ProductModelSegment>): SegmentRow => ({
  ...(row || {}),
  _rowKey: (row as any)?._rowKey || nextRowKey('segment'),
  sort: row?.sort || segmentRows.value.length + 1,
});
const normalizeMaterial = (row?: Partial<MesHcProductModelApi.ProductModelMaterial>): MaterialRow => ({
  ...(row || {}),
  _rowKey: (row as any)?._rowKey || nextRowKey('material'),
  isDefault: row?.isDefault ?? false,
});

function getDictOptions(row: SegmentRow) {
  return ruleDicts.value
    .filter((item) => item.itemCode === row.itemCode && item.enabled !== false)
    .map((item) => ({
      label: item.dictValue || item.dictCode || '',
      value: item.dictCode || item.dictValue || '',
      dictId: item.id,
      text: item.dictValue,
    }));
}

function normalizeBindingKey(value?: string) {
  return String(value || '')
    .toLowerCase()
    .replace(/[\s_\-()（）]/g, '');
}

function getSegmentBindingKey(row: SegmentRow) {
  const source = `${row.itemCode || ''}|${row.itemName || ''}`;
  const normalized = normalizeBindingKey(source);
  if (normalized.includes('生产类型') || normalized.includes('prodtype') || normalized.includes('producttype')) {
    return 'prodType' as const;
  }
  if (normalized.includes('抛光类型') || normalized.includes('物料类型') || normalized.includes('materialtype') || normalized.includes('category')) {
    return 'categoryCode' as const;
  }
  if (normalized.includes('尺寸规格') || normalized.includes('sizespec') || normalized.includes('size')) {
    return 'sizeSpec' as const;
  }
  if (normalized.includes('配方') || normalized.includes('recipe') || normalized.includes('formula')) {
    return 'recipeId' as const;
  }
  return undefined;
}

function getRecipeSelectOptions() {
  return recipeOptions.value.map((item) => ({
    label: item.recipeCode || item.recipeName || item.label,
    value: item.id,
  }));
}

function getSizeName(value?: string) {
  return value ? sizeOptions.find((item) => item.value === value)?.label || '' : '';
}

function getBoundValueOptions(row: SegmentRow) {
  const bindingKey = getSegmentBindingKey(row);
  if (bindingKey === 'prodType') return prodTypeOptions;
  if (bindingKey === 'categoryCode') return polishTypeOptions;
  if (bindingKey === 'sizeSpec') return optionalSizeOptions;
  if (bindingKey === 'recipeId') return getRecipeSelectOptions();
  return getDictOptions(row);
}

function getSegmentControlValue(row: SegmentRow) {
  const bindingKey = getSegmentBindingKey(row);
  if (bindingKey === 'recipeId') {
    return currentValues.value.recipeId;
  }
  return row.segmentValue;
}

function syncSelectText(row: SegmentRow, value?: string) {
  const option = getDictOptions(row).find((item) => item.value === value);
  row.segmentText = option?.text || value || '';
  row.dictId = option?.dictId;
}

function syncBoundFieldsFromSegments() {
  const nextValues: Partial<MesHcProductModelApi.ProductModel> = {};
  segmentRows.value.forEach((row) => {
    const bindingKey = getSegmentBindingKey(row);
    if (!bindingKey) return;
    if (bindingKey === 'prodType') {
      nextValues.prodType = row.segmentValue || undefined;
      nextValues.prodTypeName = prodTypeOptions.find((item) => item.value === row.segmentValue)?.label || '';
      row.segmentText = nextValues.prodTypeName;
      return;
    }
    if (bindingKey === 'categoryCode') {
      const categoryCode = segmentValueToCategoryCode(row.segmentValue);
      nextValues.categoryCode = categoryCode || undefined;
      nextValues.categoryName = materialTypeOptions.find((item) => item.value === categoryCode)?.label || '';
      row.segmentText = categoryLabelBySegmentValue(row.segmentValue);
      return;
    }
    if (bindingKey === 'sizeSpec') {
      nextValues.sizeSpec = row.segmentValue || undefined;
      nextValues.sizeName = getSizeName(row.segmentValue);
      row.segmentText = nextValues.sizeName;
      return;
    }
    if (bindingKey === 'recipeId') {
      const option = recipeOptions.value.find(
        (item) => item.modelCode === row.segmentValue || item.recipeCode === row.segmentValue || String(item.id) === String(row.segmentValue),
      );
      nextValues.recipeId = option?.id;
      nextValues.recipeCode = option?.recipeCode || '';
      nextValues.recipeName = option?.recipeName || '';
      row.segmentValue = option?.modelCode || option?.recipeCode || row.segmentValue || '';
      row.segmentText = option?.recipeCode || option?.recipeName || '';
    }
  });
  currentValues.value = {
    ...currentValues.value,
    ...nextValues,
  };
  suppressFormSync.value = true;
  void formApi.setValues(nextValues).finally(() => {
    suppressFormSync.value = false;
  });
}

function syncProdTypeField(value?: string) {
  currentValues.value.prodType = value;
  currentValues.value.prodTypeName = prodTypeOptions.find((item) => item.value === value)?.label || '';
  suppressFormSync.value = true;
  void formApi.setValues({
    prodType: value,
    prodTypeName: currentValues.value.prodTypeName,
  }).finally(() => {
    suppressFormSync.value = false;
  });
}

function syncCategoryField(value?: string) {
  currentValues.value.categoryCode = value;
  currentValues.value.categoryName = materialTypeOptions.find((item) => item.value === value)?.label || '';
  suppressFormSync.value = true;
  void formApi.setValues({
    categoryCode: value,
    categoryName: currentValues.value.categoryName,
  }).finally(() => {
    suppressFormSync.value = false;
  });
}

function syncSizeField(value?: string) {
  currentValues.value.sizeSpec = value;
  currentValues.value.sizeName = getSizeName(value);
  suppressFormSync.value = true;
  void formApi.setValues({
    sizeSpec: value,
    sizeName: currentValues.value.sizeName,
  }).finally(() => {
    suppressFormSync.value = false;
  });
}

function syncRecipeField(recipeId?: number) {
  const option = recipeOptions.value.find((item) => Number(item.id) === Number(recipeId));
  currentValues.value.recipeId = recipeId;
  currentValues.value.recipeCode = option?.recipeCode || '';
  currentValues.value.recipeName = option?.recipeName || '';
  suppressFormSync.value = true;
  void formApi.setValues({
    recipeId,
    recipeCode: currentValues.value.recipeCode,
    recipeName: currentValues.value.recipeName,
  }).finally(() => {
    suppressFormSync.value = false;
  });
}

function syncRouteSelection(option?: { id?: number | string; code?: string; name?: string }) {
  const routeId = option?.id ? Number(option.id) : undefined;
  currentValues.value.defaultRouteId = routeId;
  currentValues.value.defaultRouteCode = option?.code || '';
  currentValues.value.defaultRouteName = option?.name || '';
  defaultRouteName.value = currentValues.value.defaultRouteName || '';
  suppressFormSync.value = true;
  void formApi.setValues({
    defaultRouteId: routeId,
    defaultRouteCode: currentValues.value.defaultRouteCode,
    defaultRouteName: currentValues.value.defaultRouteName,
  }).finally(() => {
    suppressFormSync.value = false;
  });
}

async function loadSelectOptions() {
  const { list = [] } = await getRecipePage({ pageNo: 1, pageSize: 200 });
  recipeOptions.value = list.map((item) => ({
    ...item,
    value: item.id,
    label: item.recipeCode || item.recipeName || '',
  }));
}

function refreshFormSchema(disabled: boolean) {
  const referenceEditableFields = new Set(['retentionPeriodValue', 'retentionPeriodUnit']);
  const drivenFields = new Set(
    segmentRows.value
      .map((row) => getSegmentBindingKey(row))
      .filter((item): item is string => Boolean(item)),
  );
  formApi.updateSchema(useFormSchema().map((item) => {
    const fieldName = item.fieldName || '';
    const fieldDisabled = (disabled && !referenceEditableFields.has(fieldName)) || drivenFields.has(fieldName);
    if (item.fieldName === 'recipeId') {
      return {
        ...item,
        componentProps: {
          ...(item.componentProps || {}),
          disabled: fieldDisabled,
          options: getRecipeSelectOptions(),
        },
      };
    }
    return {
      ...item,
      componentProps: {
        ...(item.componentProps || {}),
        disabled: fieldDisabled,
      },
    };
  }));
}

async function handleRuleChange(ruleId?: number) {
  if (!ruleId) {
    segmentRows.value = [];
    ruleDicts.value = [];
    refreshFormSchema(readonlyLocked.value);
    return;
  }
  const detail = await getModelRuleDetail(ruleId);
  await formApi.setValues({
    modelRuleCode: detail.ruleCode,
    modelRuleName: detail.ruleName,
  });
  ruleDicts.value = detail.modelRuleDicts || [];
  segmentRows.value = (detail.modelRuleItems || []).map((item) =>
    normalizeSegment({
      ruleItemId: item.id,
      itemCode: item.itemCode,
      itemName: item.itemName,
      segmentValue: item.fixedValue || '',
      segmentText: item.fixedValue || '',
      sort: item.sort,
    }),
  );
  syncBoundFieldsFromSegments();
  refreshFormSchema(readonlyLocked.value);
  nextTick(() => segmentTableRef.value?.recalculate?.(true));
}

function handleDefaultRouteInput(value: string) {
  defaultRouteName.value = value;
  syncRouteSelection({ name: '', code: '' });
}

function handlePickDefaultRoute(option: RefOption) {
  routeModalOpen.value = false;
  syncRouteSelection({
    id: option.id,
    code: option.code,
    name: option.name || option.label,
  });
}

function syncFixedLabels(values: MesHcProductModelApi.ProductModel) {
  values.prodTypeName = prodTypeOptions.find((item) => item.value === values.prodType)?.label;
  values.categoryName = materialTypeOptions.find((item) => item.value === values.categoryCode)?.label;
  values.sizeName = getSizeName(values.sizeSpec);
}

async function handleGenerateCode() {
  const values = (await formApi.getValues()) as MesHcProductModelApi.ProductModel;
  if (!values.modelRuleId) {
    message.warning('请先选择型号规则');
    return;
  }
  const segmentValues: Record<string, string> = {};
  segmentRows.value.forEach((row) => {
    if (row.itemCode) segmentValues[row.itemCode] = row.segmentValue || '';
  });
  const resp = await generateProductModelCode({
    modelRuleId: values.modelRuleId,
    segmentValues,
  });
  await formApi.setValues({ modelCode: resp.generatedCode });
}

async function handleBoundSegmentChange(row: SegmentRow, value: string | number | undefined) {
  const bindingKey = getSegmentBindingKey(row);
  if (!bindingKey) {
    row.segmentValue = value == null ? '' : String(value);
    syncSelectText(row, value == null ? undefined : String(value));
    return;
  }
  if (bindingKey === 'prodType') {
    row.segmentValue = String(value || '');
    syncBoundFieldsFromSegments();
    return;
  }
  if (bindingKey === 'categoryCode') {
    row.segmentValue = String(value || '');
    syncBoundFieldsFromSegments();
    return;
  }
  if (bindingKey === 'sizeSpec') {
    row.segmentValue = String(value || '');
    syncBoundFieldsFromSegments();
    return;
  }
  if (bindingKey === 'recipeId') {
    const option = recipeOptions.value.find((item) => Number(item.id) === Number(value));
    row.segmentValue = option?.modelCode || option?.recipeCode || '';
    row.segmentText = option?.recipeCode || option?.recipeName || '';
    syncBoundFieldsFromSegments();
  }
}

function addMaterialRow() {
  materialRows.value.push(normalizeMaterial());
  nextTick(() => materialTableRef.value?.recalculate?.(true));
}

function removeMaterialRow(index: number) {
  materialRows.value.splice(index, 1);
}

function handleMaterialCodeInput(row: MaterialRow, value: string) {
  row.materialCode = value;
  if (!value.trim()) {
    row.materialId = undefined;
    row.materialName = '';
  }
}

function openMaterialPicker(row: MaterialRow) {
  activeMaterialRowKey.value = row._rowKey;
  materialModalOpen.value = true;
}

function handlePickMaterial(option: RefOption) {
  const target = materialRows.value.find((item) => item._rowKey === activeMaterialRowKey.value);
  materialModalOpen.value = false;
  if (!target) return;
  target.materialId = Number(option.id) || undefined;
  target.materialCode = option.code || '';
  target.materialName = option.name || '';
}

async function resetState() {
  formData.value = undefined;
  currentValues.value = { modelLevel: 'MODEL', status: 'ENABLE', sourceType: 'RULE_GENERATED' };
  segmentRows.value = [];
  materialRows.value = [];
  ruleDicts.value = [];
  defaultRouteName.value = '';
  activeMaterialRowKey.value = '';
  materialModalOpen.value = false;
  await formApi.resetForm();
  refreshFormSchema(false);
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  fullscreen: true,
  fullscreenButton: false,
  class: 'hc-product-model-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    modalApi.lock();
    try {
      const values = (await formApi.getValues()) as MesHcProductModelApi.ProductModel;
      if (values.modelLevel === 'FAMILY') values.parentModelId = undefined;
      syncFixedLabels(values);
      values.modelSegments = segmentRows.value.map(({ _rowKey, ...rest }) => rest);
      values.modelMaterials = materialRows.value
        .filter((row) => row.materialId || row.materialCode)
        .map(({ _rowKey, ...rest }) => rest);
      if (values.id) {
        await updateProductModel(values);
        message.success('更新成功');
      } else {
        await createProductModel(values);
        message.success('创建成功');
      }
      emit('success');
      modalApi.close();
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) return;
    await loadSelectOptions();
    const data = modalApi.getData<MesHcProductModelApi.ProductModel>();
    if (!data?.id) {
      await resetState();
      await formApi.setValues({ modelLevel: 'MODEL', status: 'ENABLE', sourceType: 'RULE_GENERATED' });
      currentValues.value = { modelLevel: 'MODEL', status: 'ENABLE', sourceType: 'RULE_GENERATED' };
      defaultRouteName.value = '';
      refreshFormSchema(false);
      return;
    }
    modalApi.lock();
    try {
      const detail = await getProductModelDetail(data.id);
      formData.value = detail;
      currentValues.value = { ...detail };
      await formApi.setValues(detail);
      defaultRouteName.value = detail.defaultRouteName || '';
      segmentRows.value = (detail.modelSegments || []).map((item) => normalizeSegment(item));
      materialRows.value = (detail.modelMaterials || []).map((item) => normalizeMaterial(item));
      if (detail.modelRuleId) {
        const ruleDetail = await getModelRuleDetail(detail.modelRuleId);
        ruleDicts.value = ruleDetail.modelRuleDicts || [];
      }
      syncBoundFieldsFromSegments();
      refreshFormSchema(Boolean(detail.referencedFlag));
      nextTick(() => {
        segmentTableRef.value?.recalculate?.(true);
        materialTableRef.value?.recalculate?.(true);
      });
    } finally {
      modalApi.unlock();
    }
  },
  async onClosed() {
    await resetState();
  },
});
</script>

<template>
  <Modal :title="title">
    <div class="hc-product-model">
      <div v-if="readonlyLocked" class="hc-product-model__tip">
        当前产品型号已被物料或计划引用，型号编码、型号规则、规则段值、默认配方、默认尺寸、默认路线不允许修改；留样时长可继续维护。
      </div>
      <div class="hc-product-model__panel">
        <div class="hc-product-model__panel-title">基本信息</div>
        <Form />
        <div class="hc-product-model__route-row">
          <div class="hc-product-model__route-label">默认工艺路线</div>
          <div class="hc-product-model__route-control">
            <PickerInline
              :model-value="defaultRouteName"
              :config="routePickerConfig"
              :disabled="readonlyLocked"
              placeholder="请选择工艺路线名称"
              @update:model-value="handleDefaultRouteInput"
              @search="() => { routeModalOpen = true; }"
              @pick="handlePickDefaultRoute"
            />
          </div>
        </div>
      </div>

      <div class="hc-product-model__panel">
      <div class="hc-product-model__panel-header">
          <div class="hc-product-model__panel-title">规则属性</div>
          <Button type="primary" size="small" :disabled="readonlyLocked" @click="handleGenerateCode">生成型号编码</Button>
        </div>
        <div class="hc-product-model__table">
          <VxeTable ref="segmentTableRef" :data="segmentRows" auto-resize border stripe size="small" height="100%">
            <VxeColumn field="sort" title="顺序" width="80" align="center">
              <template #default="{ row }">
                <span class="hc-product-model__plain-text">{{ row.sort || '-' }}</span>
              </template>
            </VxeColumn>
            <VxeColumn field="itemName" title="属性名称" min-width="160">
              <template #default="{ row }">
                <span class="hc-product-model__plain-text">{{ row.itemName || '-' }}</span>
              </template>
            </VxeColumn>
            <VxeColumn field="segmentValue" title="属性值" min-width="180">
              <template #default="{ row }">
                <Select
                  v-if="getBoundValueOptions(row).length"
                  :value="getSegmentControlValue(row)"
                  :disabled="readonlyLocked"
                  :options="getBoundValueOptions(row)"
                  allow-clear
                  class="w-full"
                  placeholder="请选择"
                  @change="(value) => handleBoundSegmentChange(row, value as string | number | undefined)"
                />
                <Input v-else v-model:value="row.segmentValue" :disabled="readonlyLocked" placeholder="请输入" @change="() => syncSelectText(row, row.segmentValue)" />
              </template>
            </VxeColumn>
          </VxeTable>
        </div>
      </div>

      <div class="hc-product-model__panel">
        <div class="hc-product-model__panel-header">
          <div class="hc-product-model__panel-title">适用物料</div>
          <Button type="primary" size="small" :disabled="readonlyLocked" @click="addMaterialRow">新增物料</Button>
        </div>
        <div class="hc-product-model__table hc-product-model__table--material">
          <VxeTable ref="materialTableRef" :data="materialRows" auto-resize border stripe size="small" height="100%">
            <VxeColumn field="materialId" title="物料ID" width="120">
              <template #default="{ row }">
                <InputNumber v-model:value="row.materialId" :disabled="true" :min="1" :precision="0" class="w-full" />
              </template>
            </VxeColumn>
            <VxeColumn field="materialCode" title="物料编码" min-width="160">
              <template #default="{ row }">
                <PickerInline
                  :model-value="row.materialCode || ''"
                  :config="materialPickerConfig"
                  :disabled="readonlyLocked"
                  placeholder="请选择物料编码"
                  @update:model-value="(value) => handleMaterialCodeInput(row, value)"
                  @search="() => openMaterialPicker(row)"
                  @pick="handlePickMaterial"
                />
              </template>
            </VxeColumn>
            <VxeColumn field="materialName" title="物料名称" min-width="200">
              <template #default="{ row }">
                <Input :value="row.materialName || '-'" :disabled="true" />
              </template>
            </VxeColumn>
            <VxeColumn field="isDefault" title="默认" width="90" align="center">
              <template #default="{ row }">
                <Switch v-model:checked="row.isDefault" :disabled="readonlyLocked" checked-children="是" un-checked-children="否" />
              </template>
            </VxeColumn>
            <VxeColumn title="操作" width="90" align="center">
              <template #default="{ $rowIndex }">
                <Tooltip title="删除">
                  <Button danger type="link" class="hc-product-model__icon-btn" :disabled="readonlyLocked" @click="removeMaterialRow($rowIndex)">
                    <IconifyIcon icon="carbon:trash-can" />
                  </Button>
                </Tooltip>
              </template>
            </VxeColumn>
          </VxeTable>
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
      :config="materialPickerConfig"
      :open="materialModalOpen"
      title="选择物料"
      @close="materialModalOpen = false"
      @pick="handlePickMaterial"
    />
  </Modal>
</template>

<style>
.hc-product-model-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.hc-product-model {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding-right: 4px;
}
.hc-product-model__tip {
  border: 1px solid var(--ant-color-warning-border);
  border-radius: 6px;
  background: var(--ant-color-warning-bg);
  color: var(--ant-color-warning-text);
  padding: 8px 12px;
}
.hc-product-model__panel {
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.hc-product-model__route-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px;
  align-items: center;
  margin-top: 12px;
}

.hc-product-model__route-label {
  color: var(--ant-color-text);
  text-align: right;
}

.hc-product-model__route-control {
  width: 100%;
}
.hc-product-model__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.hc-product-model__panel-title {
  font-weight: 600;
  color: var(--ant-color-text);
}
.hc-product-model__table {
  height: 320px;
  min-height: 320px;
  overflow: hidden;
}
.hc-product-model__table--material {
  height: 240px;
  min-height: 240px;
}
.hc-product-model__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding-inline: 4px;
}

.hc-product-model__plain-text {
  color: var(--ant-color-text);
  line-height: 22px;
}
</style>
