<script lang="ts" setup>
import type { MesHcBomApi } from '#/api/mes/hc/bom';

import { nextTick, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Button, Input, InputNumber, Select, Tabs, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import { createBom, getBomDetail, updateBom } from '#/api/mes/hc/bom';

import { useFormSchema } from '../data';
import { PickerInline, PickerModal, materialPickerConfig, productModelPickerConfig, recipePickerConfig } from '#/components/picker';
import type { PickerOption } from '#/components/picker';

const emit = defineEmits(['success']);

type BomRow = MesHcBomApi.BomItem & { _rowKey: string };
type MaterialLite = { id: number; code?: string; name?: string; label?: string; baseUom?: string };

const formData = ref<MesHcBomApi.Bom>();
const activeKey = ref('base');
const bomItems = ref<BomRow[]>([]);
const rowSeed = ref(1);
const itemTableRef = ref<any>();
const invalidBomRowKeys = ref<string[]>([]);
const currentBomRowKey = ref('');
const pendingBomScrollRowKey = ref('');
const productMaterialText = ref('');
const productMaterialNameText = ref('');
const productMaterialSpecModelText = ref('');
const productMaterialPickerOpen = ref(false);
const productModelText = ref('');
const productModelNameText = ref('');
const productModelPickerOpen = ref(false);
const productSpecText = ref('');
const recipeText = ref('');
const recipeNameText = ref('');
const recipePickerOpen = ref(false);
const bomRecipePickerConfig = {
  ...recipePickerConfig,
  inlinePanelWidth: productModelPickerConfig.inlinePanelWidth,
  modalWidth: productModelPickerConfig.modalWidth,
};

const componentTypeOptions = [
  { label: '中间品', value: '中间品' },
  { label: '辅料', value: '辅料' },
  { label: '半成品', value: '半成品' },
  { label: '包材', value: '包材' },
  { label: '原料', value: '原料' },
];
const supplyModeOptions = [
  { label: '倒冲', value: '倒冲' },
  { label: '领料', value: '领料' },
  { label: '工序投料', value: '工序投料' },
];
const operationOptions = [
  { label: '磨皮', value: 'WC-GRIND', name: '磨皮' },
  { label: '粘胶1', value: 'WC-ADH1', name: '粘胶1' },
  { label: '粘胶2', value: 'WC-ADH2', name: '粘胶2' },
  { label: '分切', value: 'WC-SLIT', name: '分切' },
];
const getTitle = ref('新增BOM');

// 行级物料弹窗
const materialPickerOpen = ref(false);
const pickerRowKey = ref('');

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
});

function nextRowKey() {
  rowSeed.value += 1;
  return `bom-row-${Date.now()}-${rowSeed.value}`;
}

function normalizeRow(row?: Partial<MesHcBomApi.BomItem>): BomRow {
  return {
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey(),
    lineNo: row?.lineNo || bomItems.value.length + 1,
    baseQty: row?.baseQty ?? 1,
    lossRate: row?.lossRate ?? 0,
    componentMaterialCode: (row as any)?.componentMaterialCode || '',
    componentMaterialName: (row as any)?.componentMaterialName || '',
    uom: row?.uom || '',
    issueOperationName: row?.issueOperationName || operationOptions.find((item) => item.value === row?.issueOperationCode)?.name || '',
  } as BomRow;
}

function isEmptyBomItem(row: Partial<MesHcBomApi.BomItem>) {
  return ![
    row.componentMaterialId,
    (row as any).componentMaterialCode,
    (row as any).componentMaterialName,
    row.componentType,
    row.uom,
    row.supplyMode,
    row.issueOperationCode,
    row.issueOperationName,
    row.remark,
  ].some((value) => value !== undefined && value !== null && String(value).trim() !== '');
}

function recalculateItemTable() {
  nextTick(() => itemTableRef.value?.recalculate?.(true));
}

function scrollToBomTableRow(rowKey?: string) {
  if (!rowKey) return;
  const doScroll = () => {
    const row = bomItems.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    itemTableRef.value?.scrollToRow?.(row);
  };
  nextTick(() => {
    doScroll();
    setTimeout(doScroll, 60);
  });
}

function focusBomRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const holder = document.querySelector(`.hc-bom-modal [data-focus-key="${rowKey}"]`) as HTMLElement | null;
    if (!holder) return;
    holder.scrollIntoView({ block: 'center', behavior: 'smooth' });
    const target = holder.querySelector('input') as HTMLElement | null;
    target?.focus?.();
  });
}

function validateBomItems(items: BomRow[]) {
  const invalidIndex = items.findIndex((item) => {
    if (isEmptyBomItem(item)) return true;
    return !item.componentMaterialId || item.baseQty === undefined || item.baseQty === null || !item.componentType || !item.issueOperationCode;
  });
  invalidBomRowKeys.value = invalidIndex >= 0 ? [items[invalidIndex]?._rowKey || ''] : [];
  if (invalidIndex >= 0) {
    activeKey.value = 'items';
    const row = items[invalidIndex];
    currentBomRowKey.value = row?._rowKey || '';
    if (row && isEmptyBomItem(row)) {
      message.warning(`BOM明细第 ${invalidIndex + 1} 行为空，请填写后提交或删除空行`);
    } else {
      message.warning(`BOM明细第 ${invalidIndex + 1} 行请填写子件物料、物料类型、对应工序和基础用量`);
    }
    scrollToBomTableRow(row?._rowKey);
    focusBomRow(row?._rowKey);
    return false;
  }
  currentBomRowKey.value = '';
  return true;
}

function handleProductMaterialInput(value: string) {
  const nextValue = String(value || '');
  productMaterialText.value = nextValue;
  productMaterialNameText.value = '';
  productMaterialSpecModelText.value = '';
  void formApi.setValues({
    productMaterialId: undefined,
    productMaterialCode: nextValue,
    productMaterialName: '',
    bomName: '',
  });
}

function handleProductModelInput(value: string) {
  const nextValue = String(value || '');
  productModelText.value = nextValue;
  productModelNameText.value = '';
  void formApi.setValues({
    productModelId: undefined,
    productModelCode: nextValue,
    productModelName: '',
    bomType: resolveProductBomType(nextValue),
  });
}

async function handleProductSpecInput(value: string) {
  const nextValue = String(value || '');
  const values = (await formApi.getValues()) as MesHcBomApi.Bom;
  productSpecText.value = nextValue;
  await formApi.setValues({
    productSpec: nextValue,
    bomName: buildProductBomName(productMaterialSpecModelText.value, values.bomName),
  });
}

async function setProductMaterial(option: PickerOption) {
  const values = (await formApi.getValues()) as MesHcBomApi.Bom;
  const productModelCode = values.productModelCode || '';
  const productSpec = values.productSpec || option.extra?.specModel || '';
  const productMaterialName = option.name || '';
  const productMaterialSpecModel = option.extra?.specModel || '';
  productMaterialText.value = option.code || '';
  productMaterialNameText.value = productMaterialName;
  productMaterialSpecModelText.value = productMaterialSpecModel;
  productSpecText.value = productSpec;
  await formApi.setValues({
    productMaterialId: Number(option.id),
    productMaterialCode: option.code,
    productMaterialName,
    productModelCode,
    productSpec,
    bomCode: values.bomCode || (option.code ? `PBOM-${option.code}` : ''),
    bomName: buildProductBomName(productMaterialSpecModel, productMaterialName),
    bomType: resolveProductBomType(productModelCode),
  });
}

async function setProductModel(option: PickerOption) {
  const values = (await formApi.getValues()) as MesHcBomApi.Bom;
  const productModelCode = option.code || option.label || '';
  const productModelName = option.name || option.extra?.modelName || productModelCode;
  const productSpec = values.productSpec || option.extra?.sizeName || option.extra?.sizeSpec || '';
  productModelText.value = productModelCode;
  productModelNameText.value = productModelName;
  productSpecText.value = productSpec;
  await formApi.setValues({
    productModelId: Number(option.id),
    productModelCode,
    productModelName,
    productSpec,
    bomType: resolveProductBomType(productModelCode),
  });
}

async function handleProductMaterialPickerSelect(option: PickerOption) {
  await setProductMaterial(option);
  productMaterialPickerOpen.value = false;
}

async function handleProductModelPickerSelect(option: PickerOption) {
  await setProductModel(option);
  productModelPickerOpen.value = false;
}

function handleRecipeInput(value: string) {
  const nextValue = String(value || '');
  recipeText.value = nextValue;
  recipeNameText.value = '';
  void formApi.setValues({
    recipeId: undefined,
    recipeCode: nextValue,
    recipeName: '',
  });
}

async function setRecipe(option: PickerOption) {
  recipeText.value = option.code || '';
  recipeNameText.value = option.name || '';
  await formApi.setValues({
    recipeId: Number(option.id),
    recipeCode: option.code || '',
    recipeName: option.name || '',
  });
}

async function handleRecipePickerSelect(option: PickerOption) {
  await setRecipe(option);
  recipePickerOpen.value = false;
}

function handleOperationChange(row: BomRow, value?: string) {
  const option = operationOptions.find((item) => item.value === value);
  row.issueOperationName = option?.name || '';
}

function buildProductBomName(productMaterialSpecModel?: string, fallbackName?: string) {
  return String(productMaterialSpecModel || fallbackName || '').trim();
}

function resolveProductBomType(productModelCode?: string) {
  return String(productModelCode || '').trim().startsWith('W') ? '量产' : '研发样';
}

function setRowMaterial(row: BomRow, material?: MaterialLite) {
  row.componentMaterialId = material?.id as any;
  (row as any).componentMaterialCode = material?.code || '';
  (row as any).componentMaterialName = material?.name || material?.label || '';
  row.uom = material?.baseUom || '';
}

function clearRowMaterialByInput(row: BomRow, field: 'code' | 'name', value: string) {
  const nextValue = String(value || '');
  row.componentMaterialId = undefined as any;
  if (field === 'code') {
    (row as any).componentMaterialCode = nextValue;
    (row as any).componentMaterialName = '';
  } else {
    (row as any).componentMaterialName = nextValue;
    (row as any).componentMaterialCode = '';
  }
  row.uom = '';
}

function openRowMaterialPicker(row: BomRow) {
  pickerRowKey.value = row._rowKey;
  materialPickerOpen.value = true;
}

function handleMaterialPickerSelect(option: PickerOption) {
  const mapped: MaterialLite = {
    id: Number(option.id),
    code: option.code,
    name: option.name,
    label: option.name,
    baseUom: option.extra?.baseUom || '',
  };
  const target = bomItems.value.find((item) => item._rowKey === pickerRowKey.value);
  if (!target) return;
  setRowMaterial(target, mapped);
  materialPickerOpen.value = false;
}

function addBomItem() {
  const row = normalizeRow();
  bomItems.value.push(row);
  invalidBomRowKeys.value = [];
  currentBomRowKey.value = row._rowKey;
  activeKey.value = 'items';
  pendingBomScrollRowKey.value = row._rowKey;
  recalculateItemTable();
  scrollToBomTableRow(row._rowKey);
  focusBomRow(row._rowKey);
}

function removeBomItem(index: number) {
  const removed = bomItems.value[index];
  bomItems.value.splice(index, 1);
  invalidBomRowKeys.value = [];
  if (currentBomRowKey.value === removed?._rowKey) {
    currentBomRowKey.value = bomItems.value[Math.max(0, index - 1)]?._rowKey || bomItems.value[0]?._rowKey || '';
  }
  bomItems.value.forEach((item, itemIndex) => {
    item.lineNo = itemIndex + 1;
  });
  recalculateItemTable();
}

watch(activeKey, async (value) => {
  if (value !== 'items') return;
  await nextTick();
  recalculateItemTable();
  if (pendingBomScrollRowKey.value) {
    const rowKey = pendingBomScrollRowKey.value;
    pendingBomScrollRowKey.value = '';
    scrollToBomTableRow(rowKey);
    focusBomRow(rowKey);
  }
});

async function resetBomFormState() {
  activeKey.value = 'base';
  formData.value = undefined;
  bomItems.value = [];
  invalidBomRowKeys.value = [];
  currentBomRowKey.value = '';
  pendingBomScrollRowKey.value = '';
  productMaterialText.value = '';
  productMaterialNameText.value = '';
  productMaterialSpecModelText.value = '';
  productMaterialPickerOpen.value = false;
  productModelText.value = '';
  productModelNameText.value = '';
  productModelPickerOpen.value = false;
  productSpecText.value = '';
  recipeText.value = '';
  recipeNameText.value = '';
  recipePickerOpen.value = false;
  await formApi.resetForm();
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[1180px] hc-bom-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      activeKey.value = 'base';
      return;
    }

    modalApi.lock();
    const data = (await formApi.getValues()) as MesHcBomApi.Bom;
    if (!data.productMaterialId) {
      activeKey.value = 'base';
      modalApi.unlock();
      message.warning('请选择制成品物料');
      return;
    }
    if (!data.productModelId) {
      activeKey.value = 'base';
      modalApi.unlock();
      message.warning('请选择产品型号字典');
      return;
    }
    if (!data.productSpec) {
      activeKey.value = 'base';
      modalApi.unlock();
      message.warning('请输入尺寸规格');
      return;
    }
    try {
      const rawBomItems = bomItems.value.map((row, index) => ({ ...row, lineNo: row.lineNo || index + 1 }));
      if (!validateBomItems(rawBomItems)) return;
      const validBomItems = rawBomItems
        .filter((row) => !isEmptyBomItem(row))
        .map((row, index) => {
          const { _rowKey, ...rest } = row as any;
          return { ...rest, bomCode: data.bomCode, lineNo: index + 1 };
        });
      data.bomItems = validBomItems;
      await (formData.value?.id ? updateBom(data) : createBom(data));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;
    const data = modalApi.getData<MesHcBomApi.Bom>();
    if (!data?.id) {
      await resetBomFormState();
      getTitle.value = '新增BOM';
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getBomDetail(data.id);
      getTitle.value = '编辑BOM';
      await formApi.setValues(formData.value);
      productMaterialText.value = formData.value.productMaterialCode || '';
      productMaterialNameText.value = formData.value.productMaterialName || '';
      productMaterialSpecModelText.value = formData.value.bomName || '';
      productModelText.value = formData.value.productModelCode || '';
      productModelNameText.value = formData.value.productModelName || '';
      productSpecText.value = formData.value.productSpec || '';
      recipeText.value = formData.value.recipeCode || '';
      recipeNameText.value = formData.value.recipeName || '';
      bomItems.value = ((formData.value as any).bomItems || []).map((item: any) => normalizeRow(item));
      invalidBomRowKeys.value = [];
      currentBomRowKey.value = bomItems.value[0]?._rowKey || '';
      if (bomItems.value.length > 0) recalculateItemTable();
      activeKey.value = 'base';
    } finally {
      modalApi.unlock();
    }
  },
  async onClosed() {
    await resetBomFormState();
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="bom-form-modal">
      <PickerModal
        :config="materialPickerConfig"
        :open="materialPickerOpen"
        title="选择子件物料"
        @close="materialPickerOpen = false"
        @pick="handleMaterialPickerSelect"
      />
      <PickerModal
        :config="materialPickerConfig"
        :open="productMaterialPickerOpen"
        title="选择制成品物料"
        @close="productMaterialPickerOpen = false"
        @pick="handleProductMaterialPickerSelect"
      />
      <PickerModal
        :config="productModelPickerConfig"
        :open="productModelPickerOpen"
        title="选择产品型号"
        @close="productModelPickerOpen = false"
        @pick="handleProductModelPickerSelect"
      />
      <PickerModal
        :config="bomRecipePickerConfig"
        :open="recipePickerOpen"
        title="选择配方管理"
        @close="recipePickerOpen = false"
        @pick="handleRecipePickerSelect"
      />
      <Tabs v-model:active-key="activeKey" class="bom-form-modal__tabs">
        <Tabs.TabPane key="base" tab="基本信息">
          <div class="bom-form-modal__pane bom-form-modal__pane--base">
            <div class="bom-product-picker-row">
              <div class="bom-product-picker-label"><span>*</span>制成品物料</div>
              <div class="bom-product-picker-main">
                <PickerInline
                  :model-value="productMaterialText"
                  :config="materialPickerConfig"
                  placeholder="输入制成品料号或点击选择"
                  @update:model-value="handleProductMaterialInput"
                  @search="productMaterialPickerOpen = true"
                  @pick="setProductMaterial"
                />
                <Input :value="productMaterialNameText" readonly disabled class="bom-readonly-input" placeholder="选择后回显物料名称" />
              </div>
            </div>
            <div class="bom-product-picker-row">
              <div class="bom-product-picker-label"><span>*</span>产品型号</div>
              <div class="bom-product-picker-main">
                <PickerInline
                  :model-value="productModelText"
                  :config="productModelPickerConfig"
                  placeholder="输入型号编码或点击选择"
                  @update:model-value="handleProductModelInput"
                  @search="productModelPickerOpen = true"
                  @pick="setProductModel"
                />
                <Input :value="productModelNameText" readonly disabled class="bom-readonly-input" placeholder="选择后回显型号名称" />
              </div>
            </div>
            <div class="bom-product-picker-row">
              <div class="bom-product-picker-label">配方管理</div>
              <div class="bom-product-picker-main">
                <PickerInline
                  :model-value="recipeText"
                  :config="bomRecipePickerConfig"
                  placeholder="输入配方编码或点击选择"
                  @update:model-value="handleRecipeInput"
                  @search="recipePickerOpen = true"
                  @pick="setRecipe"
                />
                <Input :value="recipeNameText" readonly disabled class="bom-readonly-input" placeholder="选择后回显配方名称" />
              </div>
            </div>
            <div class="bom-product-picker-row">
              <div class="bom-product-picker-label"><span>*</span>尺寸规格</div>
              <div class="bom-product-picker-main bom-product-picker-main--single">
                <Input
                  :value="productSpecText"
                  placeholder="请输入尺寸规格，如 740"
                  @update:value="handleProductSpecInput"
                />
              </div>
            </div>
            <Form />
          </div>
        </Tabs.TabPane>
        <Tabs.TabPane key="items" tab="BOM明细" force-render>
          <div class="bom-form-modal__pane bom-form-modal__pane--items">
            <div class="bom-items-toolbar">
              <div class="text-base font-medium">BOM明细</div>
              <Button type="primary" @click="addBomItem">新增明细</Button>
            </div>
            <div class="bom-items-table-wrap">
              <VxeTable
                ref="itemTableRef"
                :data="bomItems"
                :row-class-name="({ row }) => invalidBomRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentBomRowKey === row._rowKey ? 'is-current-row' : ''"
                auto-resize
                border
                stripe
                :round="false"
                size="small"
                height="100%"
                show-overflow
                row-id="_rowKey"
                @cell-click="({ row }) => (currentBomRowKey = row._rowKey)"
              >
                <VxeColumn field="lineNo" title="行号" width="70" align="center">
                  <template #default="{ row }">
                    {{ row.lineNo }}
                  </template>
                </VxeColumn>
                <VxeColumn field="componentMaterialCode" title="*子件物料编码" min-width="200">
                  <template #default="{ row }">
                    <div :data-focus-key="row._rowKey">
                      <PickerInline
                        :model-value="(row as any).componentMaterialCode"
                        :config="materialPickerConfig"
                        placeholder="输入名称搜索或点击选择"
                        @update:model-value="(value) => clearRowMaterialByInput(row, 'code', value)"
                        @search="openRowMaterialPicker(row)"
                        @pick="(opt) => setRowMaterial(row, { id: Number(opt.id), code: opt.code, name: opt.name, label: opt.name, baseUom: opt.extra?.baseUom || '' })"
                      />
                    </div>
                  </template>
                </VxeColumn>
                <VxeColumn field="componentMaterialName" title="*子件物料名称" min-width="220">
                  <template #default="{ row }">
                    <PickerInline
                      :model-value="(row as any).componentMaterialName"
                      :config="materialPickerConfig"
                      placeholder="输入名称搜索或点击选择"
                      @update:model-value="(value) => clearRowMaterialByInput(row, 'name', value)"
                      @search="openRowMaterialPicker(row)"
                      @pick="(opt) => setRowMaterial(row, { id: Number(opt.id), code: opt.code, name: opt.name, label: opt.name, baseUom: opt.extra?.baseUom || '' })"
                    />
                  </template>
                </VxeColumn>
                <VxeColumn field="componentType" title="*物料类型" width="130">
                  <template #default="{ row }">
                    <Select v-model:value="row.componentType" :options="componentTypeOptions" class="w-full" allow-clear placeholder="请选择" />
                  </template>
                </VxeColumn>
                <VxeColumn field="issueOperationCode" title="*对应工序" width="130">
                  <template #default="{ row }">
                    <Select
                      v-model:value="row.issueOperationCode"
                      :options="operationOptions"
                      class="w-full"
                      allow-clear
                      placeholder="请选择"
                      @change="(value) => handleOperationChange(row, String(value || ''))"
                    />
                  </template>
                </VxeColumn>
                <VxeColumn field="baseQty" title="*基础用量" width="130">
                  <template #default="{ row }">
                    <InputNumber v-model:value="row.baseQty" :min="0" :precision="6" class="w-full" />
                  </template>
                </VxeColumn>
                <VxeColumn field="uom" title="单位" width="90">
                  <template #default="{ row }">
                    <Input :value="row.uom" readonly disabled class="w-full bom-readonly-input" />
                  </template>
                </VxeColumn>
                <VxeColumn field="lossRate" title="损耗率" width="120">
                  <template #default="{ row }">
                    <InputNumber v-model:value="row.lossRate" :min="0" :precision="4" class="w-full" />
                  </template>
                </VxeColumn>
                <VxeColumn field="supplyMode" title="供料方式" width="140">
                  <template #default="{ row }">
                    <Select v-model:value="row.supplyMode" :options="supplyModeOptions" class="w-full" allow-clear placeholder="请选择" />
                  </template>
                </VxeColumn>
                <VxeColumn field="remark" title="备注" min-width="180">
                  <template #default="{ row }">
                    <Input v-model:value="row.remark" placeholder="请输入备注" />
                  </template>
                </VxeColumn>
                <VxeColumn title="操作" width="90" fixed="right" align="center" header-align="center">
                  <template #default="{ $rowIndex }">
                    <Button danger type="link" @click="removeBomItem($rowIndex)">删除</Button>
                  </template>
                </VxeColumn>
              </VxeTable>
            </div>
          </div>
        </Tabs.TabPane>
      </Tabs>
    </div>
  </Modal>
</template>

<style scoped>
.bom-form-modal {
  height: 620px;
  min-height: 620px;
  max-height: 620px;
  overflow: hidden;
}

.bom-form-modal__tabs {
  height: 100%;
}

.bom-form-modal :deep(.ant-tabs) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.bom-form-modal :deep(.ant-tabs-nav) {
  height: 44px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.bom-form-modal :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.bom-form-modal :deep(.ant-tabs-content),
.bom-form-modal :deep(.ant-tabs-tabpane) {
  height: 100%;
}

.bom-form-modal__pane {
  height: 100%;
  min-height: 0;
}

.bom-form-modal__pane--base {
  overflow: auto;
  padding-right: 4px;
}

.bom-product-picker-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
}

.bom-product-picker-label {
  height: 32px;
  color: rgb(0 0 0 / 88%);
  font-size: 14px;
  line-height: 32px;
  text-align: right;
}

.bom-product-picker-label span {
  margin-right: 4px;
  color: #ff4d4f;
}

.bom-product-picker-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
}

.bom-product-picker-main--single {
  grid-template-columns: minmax(0, 1fr);
}

.bom-form-modal :deep(.bom-readonly-input.ant-input[disabled]) {
  color: rgb(0 0 0 / 88%);
  background: #f5f5f5;
  cursor: default;
}

.bom-form-modal__pane--items {
  display: grid;
  height: 100%;
  min-height: 0;
  grid-template-rows: 40px minmax(0, 1fr);
  row-gap: 12px;
}

.bom-items-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bom-items-table-wrap {
  min-height: 0;
  overflow: hidden;
}

.bom-items-table-wrap :deep(.vxe-table) {
  border-radius: 0;
}

.bom-items-table-wrap :deep(.vxe-table--border-wrapper),
.bom-items-table-wrap :deep(.vxe-table--main-wrapper) {
  height: 100%;
}

.bom-items-table-wrap :deep(.is-invalid-row) {
  background-color: #fff1f0;
}

.bom-items-table-wrap :deep(.is-current-row) {
  background-color: #e6f4ff;
}

</style>

<style>
.hc-bom-modal .ant-modal-content {
  overflow: hidden;
}

.hc-bom-modal .ant-modal-body {
  height: 620px;
  min-height: 620px;
  max-height: 620px;
  overflow: hidden;
}
</style>
