<script lang="ts" setup>
import type { MesHcRecipeApi } from '#/api/mes/hc/recipe';

import { computed, nextTick, ref, watch } from 'vue';
import dayjs from 'dayjs';

import { useVbenModal } from '@vben/common-ui';
import { Button, Input, InputNumber, Select, Switch, Tabs, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import { createRecipe, getRecipeDetail, updateRecipe } from '#/api/mes/hc/recipe';

import { useFormSchema } from '../data';
import { PickerInline, PickerModal, materialPickerConfig } from '#/components/picker';
import type { PickerOption } from '#/components/picker';

const emit = defineEmits(['success']);

type RecipeRow = MesHcRecipeApi.RecipeItem & { _rowKey: string };
type MaterialLite = { id: number; code?: string; name?: string; label?: string; baseUom?: string };

const formData = ref<MesHcRecipeApi.Recipe>();
const activeKey = ref('base');
const recipeItems = ref<RecipeRow[]>([]);
const rowSeed = ref(1);
const itemTableRef = ref<any>();
const invalidRecipeRowKeys = ref<string[]>([]);
const currentRecipeRowKey = ref('');
const pendingRecipeScrollRowKey = ref('');

const materialTypeOptions = [
  { label: '主料', value: '主料' },
  { label: '辅料', value: '辅料' },
  { label: '溶剂', value: '溶剂' },
  { label: '添加剂', value: '添加剂' },
];
const getTitle = computed(() => (formData.value?.id ? '编辑配方' : '新增配方'));

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
  return `recipe-row-${Date.now()}-${rowSeed.value}`;
}

function normalizeRow(row?: Partial<MesHcRecipeApi.RecipeItem>): RecipeRow {
  return {
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey(),
    lineNo: row?.lineNo || recipeItems.value.length + 1,
    theoryQty: row?.theoryQty ?? 1,
    lossRate: row?.lossRate ?? 0,
    keyControlFlag: row?.keyControlFlag ?? false,
    materialCode: row?.materialCode || '',
    materialName: row?.materialName || '',
  } as RecipeRow;
}

function isEmptyRecipeItem(row: Partial<MesHcRecipeApi.RecipeItem>) {
  return ![row.materialId, row.materialType, row.uom, row.remark, row.materialCode, row.materialName].some(
    (value) => value !== undefined && value !== null && String(value).trim() !== '',
  );
}

function recalculateItemTable() {
  nextTick(() => itemTableRef.value?.recalculate?.(true));
}

function scrollToRecipeTableRow(rowKey?: string) {
  if (!rowKey) return;
  const doScroll = () => {
    const row = recipeItems.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    itemTableRef.value?.scrollToRow?.(row);
  };
  nextTick(() => {
    doScroll();
    setTimeout(doScroll, 60);
  });
}

function focusRecipeRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const holder = document.querySelector(`.hc-recipe-modal [data-focus-key="${rowKey}"]`) as HTMLElement | null;
    if (!holder) return;
    holder.scrollIntoView({ block: 'center', behavior: 'smooth' });
    const target = holder.querySelector('input') as HTMLElement | null;
    target?.focus?.();
  });
}

function validateRecipeItems(items: RecipeRow[]) {
  const invalidIndex = items.findIndex((item) => {
    if (isEmptyRecipeItem(item)) return true;
    return !item.materialId || item.theoryQty === undefined || item.theoryQty === null;
  });
  invalidRecipeRowKeys.value = invalidIndex >= 0 ? [items[invalidIndex]?._rowKey || ''] : [];
  if (invalidIndex >= 0) {
    activeKey.value = 'items';
    const row = items[invalidIndex];
    currentRecipeRowKey.value = row?._rowKey || '';
    if (row && isEmptyRecipeItem(row)) {
      message.warning(`配方明细第 ${invalidIndex + 1} 行为空，请填写后提交或删除空行`);
    } else {
      message.warning(`配方明细第 ${invalidIndex + 1} 行请填写投入物料和理论用量`);
    }
    scrollToRecipeTableRow(row?._rowKey);
    focusRecipeRow(row?._rowKey);
    return false;
  }
  currentRecipeRowKey.value = '';
  return true;
}

function normalizeDateValue(value: any) {
  if (!value) return undefined;
  if (dayjs.isDayjs(value)) return value;
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    return dayjs(`${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`);
  }
  if (typeof value === 'string' || typeof value === 'number') {
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed : undefined;
  }
  return undefined;
}

function normalizeRecipeDetail(detail: MesHcRecipeApi.Recipe) {
  return {
    ...detail,
    effectiveDate: normalizeDateValue((detail as any).effectiveDate),
    expireDate: normalizeDateValue((detail as any).expireDate),
  } as MesHcRecipeApi.Recipe;
}

function serializeDateValue(value: any) {
  if (!value) return undefined;
  if (dayjs.isDayjs(value)) return value.format('YYYY-MM-DD');
  return value;
}

function setRowMaterial(row: RecipeRow, material?: MaterialLite) {
  row.materialId = material?.id as any;
  row.materialCode = material?.code || '';
  row.materialName = material?.name || material?.label || '';
  row.uom = material?.baseUom || row.uom || '';
}

function clearRowMaterialByInput(row: RecipeRow, field: 'code' | 'name', value: string) {
  const nextValue = String(value || '');
  row.materialId = undefined as any;
  if (field === 'code') {
    row.materialCode = nextValue;
    row.materialName = '';
  } else {
    row.materialName = nextValue;
    row.materialCode = '';
  }
}

function openRowMaterialPicker(row: RecipeRow) {
  pickerRowKey.value = row._rowKey;
  materialPickerOpen.value = true;
}

function handleMaterialPickerSelect(option: PickerOption) {
  const target = recipeItems.value.find((item) => item._rowKey === pickerRowKey.value);
  if (!target) return;
  setRowMaterial(target, {
    id: Number(option.id),
    code: option.code,
    name: option.name,
    label: option.name,
    baseUom: option.extra?.baseUom || '',
  });
  materialPickerOpen.value = false;
}

function addRecipeItem() {
  const row = normalizeRow();
  recipeItems.value.push(row);
  invalidRecipeRowKeys.value = [];
  currentRecipeRowKey.value = row._rowKey;
  activeKey.value = 'items';
  pendingRecipeScrollRowKey.value = row._rowKey;
  recalculateItemTable();
  scrollToRecipeTableRow(row._rowKey);
  focusRecipeRow(row._rowKey);
}

function removeRecipeItem(index: number) {
  const removed = recipeItems.value[index];
  recipeItems.value.splice(index, 1);
  invalidRecipeRowKeys.value = [];
  if (currentRecipeRowKey.value === removed?._rowKey) {
    currentRecipeRowKey.value = recipeItems.value[Math.max(0, index - 1)]?._rowKey || recipeItems.value[0]?._rowKey || '';
  }
  recipeItems.value.forEach((item, itemIndex) => {
    item.lineNo = itemIndex + 1;
  });
  recalculateItemTable();
}

watch(activeKey, async (value) => {
  if (value !== 'items') return;
  await nextTick();
  recalculateItemTable();
  if (pendingRecipeScrollRowKey.value) {
    const rowKey = pendingRecipeScrollRowKey.value;
    pendingRecipeScrollRowKey.value = '';
    scrollToRecipeTableRow(rowKey);
    focusRecipeRow(rowKey);
  }
});

async function resetRecipeFormState() {
  activeKey.value = 'base';
  formData.value = undefined;
  recipeItems.value = [];
  invalidRecipeRowKeys.value = [];
  currentRecipeRowKey.value = '';
  pendingRecipeScrollRowKey.value = '';
  await formApi.resetForm();
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[1180px] hc-recipe-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      activeKey.value = 'base';
      return;
    }

    modalApi.lock();
    const data = (await formApi.getValues()) as MesHcRecipeApi.Recipe;
    try {
      data.effectiveDate = serializeDateValue((data as any).effectiveDate) as any;
      data.expireDate = serializeDateValue((data as any).expireDate) as any;

      const rawRecipeItems = recipeItems.value.map((row, index) => ({ ...row, lineNo: row.lineNo || index + 1 }));
      if (!validateRecipeItems(rawRecipeItems)) return;
      const validRecipeItems = rawRecipeItems
        .filter((row) => !isEmptyRecipeItem(row))
        .map((row, index) => {
          const { _rowKey, ...rest } = row as any;
          return { ...rest, recipeCode: data.recipeCode, lineNo: index + 1 };
        });
      data.recipeItems = validRecipeItems;
      await (formData.value?.id ? updateRecipe(data) : createRecipe(data));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;
    const data = modalApi.getData<MesHcRecipeApi.Recipe>();
    if (!data?.id) {
      await resetRecipeFormState();
      return;
    }

    modalApi.lock();
    try {
      const detail = await getRecipeDetail(data.id);
      formData.value = normalizeRecipeDetail(detail);
      await formApi.setValues(formData.value);
      recipeItems.value = (formData.value.recipeItems || []).map((item) => normalizeRow(item));
      invalidRecipeRowKeys.value = [];
      currentRecipeRowKey.value = recipeItems.value[0]?._rowKey || '';
      if (recipeItems.value.length > 0) recalculateItemTable();
      activeKey.value = 'base';
    } finally {
      modalApi.unlock();
    }
  },
  async onClosed() {
    await resetRecipeFormState();
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="recipe-form-modal">
      <PickerModal
        :config="materialPickerConfig"
        :open="materialPickerOpen"
        title="选择投入物料"
        @close="materialPickerOpen = false"
        @pick="handleMaterialPickerSelect"
      />
      <Tabs v-model:active-key="activeKey" class="recipe-form-modal__tabs">
        <Tabs.TabPane key="base" tab="基本信息">
          <div class="recipe-form-modal__pane recipe-form-modal__pane--base">
            <Form />
          </div>
        </Tabs.TabPane>
        <Tabs.TabPane key="items" tab="配方明细" force-render>
          <div class="recipe-form-modal__pane recipe-form-modal__pane--items">
            <div class="recipe-items-toolbar">
              <div class="text-base font-medium">配方明细</div>
              <Button type="primary" @click="addRecipeItem">新增明细</Button>
            </div>
            <div class="recipe-items-table-wrap">
              <VxeTable
                ref="itemTableRef"
                :data="recipeItems"
                :row-class-name="({ row }) => invalidRecipeRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentRecipeRowKey === row._rowKey ? 'is-current-row' : ''"
                auto-resize
                border
                stripe
                :round="false"
                size="small"
                height="100%"
                show-overflow
                row-id="_rowKey"
                @cell-click="({ row }) => (currentRecipeRowKey = row._rowKey)"
              >
                <VxeColumn field="lineNo" title="行号" width="70" align="center">
                  <template #default="{ row }">
                    {{ row.lineNo }}
                  </template>
                </VxeColumn>
                <VxeColumn field="materialCode" title="*物料编码" min-width="180">
                  <template #default="{ row }">
                    <div :data-focus-key="row._rowKey">
                      <PickerInline
                        :model-value="row.materialCode"
                        :config="materialPickerConfig"
                        placeholder="输入名称搜索或点击选择"
                        @update:model-value="(value) => clearRowMaterialByInput(row, 'code', value)"
                        @search="openRowMaterialPicker(row)"
                        @pick="(opt) => setRowMaterial(row, { id: Number(opt.id), code: opt.code, name: opt.name, label: opt.name, baseUom: opt.extra?.baseUom || '' })"
                      />
                    </div>
                  </template>
                </VxeColumn>
                <VxeColumn field="materialName" title="*物料名称" min-width="220">
                  <template #default="{ row }">
                    <PickerInline
                      :model-value="row.materialName"
                      :config="materialPickerConfig"
                      placeholder="输入名称搜索或点击选择"
                      @update:model-value="(value) => clearRowMaterialByInput(row, 'name', value)"
                      @search="openRowMaterialPicker(row)"
                      @pick="(opt) => setRowMaterial(row, { id: Number(opt.id), code: opt.code, name: opt.name, label: opt.name, baseUom: opt.extra?.baseUom || '' })"
                    />
                  </template>
                </VxeColumn>
                <VxeColumn field="materialType" title="投入类型" width="140">
                  <template #default="{ row }">
                    <Select v-model:value="row.materialType" :options="materialTypeOptions" class="w-full" allow-clear placeholder="请选择" />
                  </template>
                </VxeColumn>
                <VxeColumn field="theoryQty" title="*理论用量" width="130">
                  <template #default="{ row }">
                    <InputNumber v-model:value="row.theoryQty" :min="0" :precision="6" class="w-full" />
                  </template>
                </VxeColumn>
                <VxeColumn field="uom" title="单位" width="90">
                  <template #default="{ row }">
                    <Input :value="row.uom" readonly disabled class="w-full recipe-readonly-input" />
                  </template>
                </VxeColumn>
                <VxeColumn field="lossRate" title="损耗率" width="120">
                  <template #default="{ row }">
                    <InputNumber v-model:value="row.lossRate" :min="0" :precision="4" class="w-full" />
                  </template>
                </VxeColumn>
                <VxeColumn field="keyControlFlag" title="关键料" width="100">
                  <template #default="{ row }">
                    <Switch v-model:checked="row.keyControlFlag" checked-children="是" un-checked-children="否" />
                  </template>
                </VxeColumn>
                <VxeColumn field="remark" title="备注" min-width="180">
                  <template #default="{ row }">
                    <Input v-model:value="row.remark" placeholder="请输入备注" />
                  </template>
                </VxeColumn>
                <VxeColumn title="操作" width="90" fixed="right" align="center" header-align="center">
                  <template #default="{ $rowIndex }">
                    <Button danger type="link" @click="removeRecipeItem($rowIndex)">删除</Button>
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
.recipe-form-modal {
  height: 620px;
  min-height: 620px;
  max-height: 620px;
  overflow: hidden;
}

.recipe-form-modal__tabs {
  height: 100%;
}

.recipe-form-modal :deep(.ant-tabs) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.recipe-form-modal :deep(.ant-tabs-nav) {
  height: 44px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.recipe-form-modal :deep(.ant-tabs-content-holder) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.recipe-form-modal :deep(.ant-tabs-content),
.recipe-form-modal :deep(.ant-tabs-tabpane) {
  height: 100%;
}

.recipe-form-modal__pane {
  height: 100%;
  min-height: 0;
}

.recipe-form-modal__pane--base {
  overflow: auto;
  padding-right: 4px;
}

.recipe-form-modal__pane--items {
  display: grid;
  height: 100%;
  min-height: 0;
  grid-template-rows: 40px minmax(0, 1fr);
  row-gap: 12px;
}

.recipe-items-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recipe-items-table-wrap {
  min-height: 0;
  overflow: hidden;
}

.recipe-items-table-wrap :deep(.vxe-table) {
  border-radius: 0;
}

.recipe-items-table-wrap :deep(.vxe-table--border-wrapper),
.recipe-items-table-wrap :deep(.vxe-table--main-wrapper) {
  height: 100%;
}

.recipe-items-table-wrap :deep(.is-invalid-row) {
  background-color: #fff1f0;
}

.recipe-items-table-wrap :deep(.is-current-row) {
  background-color: #e6f4ff;
}

.recipe-items-table-wrap :deep(.recipe-readonly-input.ant-input[disabled]) {
  color: rgb(0 0 0 / 88%);
  background: #f5f5f5;
  cursor: default;
}
</style>

<style>
.hc-recipe-modal .ant-modal-content {
  overflow: hidden;
}

.hc-recipe-modal .ant-modal-body {
  height: 620px;
  min-height: 620px;
  max-height: 620px;
  overflow: hidden;
}
</style>
