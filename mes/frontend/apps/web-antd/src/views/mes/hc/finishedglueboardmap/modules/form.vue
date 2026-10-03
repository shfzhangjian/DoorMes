<script lang="ts" setup>
import type { MesHcFinishedGlueBoardMapApi } from '#/api/mes/hc/finishedglueboardmap';
import type { PickerOption } from '#/components/picker';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Input, InputNumber, message, Select, Switch, Tooltip } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  createFinishedGlueBoardMap,
  getFinishedGlueBoardMapDetail,
  updateFinishedGlueBoardMap,
} from '#/api/mes/hc/finishedglueboardmap';
import { materialPickerConfig, PickerInline, PickerModal, productModelPickerConfig } from '#/components/picker';

import { glueProcessOptions, useFormSchema } from '../data';

type ItemRow = MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem & { _rowKey: string };

const emit = defineEmits(['success']);

const formData = ref<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap>();
const itemRows = ref<ItemRow[]>([]);
const rowSeed = ref(1);
const itemTableRef = ref<any>();
const productModelText = ref('');
const productModelModalOpen = ref(false);
const materialModalOpen = ref(false);
const activeItemRowKey = ref('');

const title = computed(() => (formData.value?.id ? '编辑成品胶板对照' : '新增成品胶板对照'));

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

const nextRowKey = () => `item-${Date.now()}-${++rowSeed.value}`;

function normalizeItem(row?: Partial<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMapItem>): ItemRow {
  return {
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey(),
    preferredFlag: row?.preferredFlag ?? false,
    sort: row?.sort || itemRows.value.length + 1,
  };
}

function addItem(glueProcess: string) {
  itemRows.value.push(normalizeItem({ glueProcess, glueProcessName: getGlueProcessName(glueProcess) }));
  nextTick(() => itemTableRef.value?.recalculate?.(true));
}

function removeItem(index: number) {
  itemRows.value.splice(index, 1);
  itemRows.value.forEach((row, idx) => {
    row.sort = idx + 1;
  });
}

function getGlueProcessName(glueProcess?: string) {
  return glueProcessOptions.find((item) => item.value === glueProcess)?.label || '';
}

function handleGlueProcessChange(row: ItemRow, value: string) {
  row.glueProcess = value;
  row.glueProcessName = getGlueProcessName(value);
}

async function handleProductModelInput(value: string) {
  productModelText.value = value;
  await formApi.setValues({
    productModelId: undefined,
    productModelCode: value,
    productModelName: value ? '' : undefined,
  });
}

async function handlePickProductModel(option: PickerOption) {
  productModelModalOpen.value = false;
  const currentValues = (await formApi.getValues()) as MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap;
  productModelText.value = option.code || '';
  await formApi.setValues({
    productModelId: Number(option.id) || undefined,
    productModelCode: option.code || '',
    productModelName: option.name || option.code || '',
    productSpec: currentValues.productSpec || option.extra?.sizeSpec || '',
    sizeSpec: option.extra?.sizeSpec || currentValues.sizeSpec || '',
    sizeName: option.extra?.sizeName || currentValues.sizeName || '',
  });
}

function handleMaterialCodeInput(row: ItemRow, value: string) {
  row.glueBoardMaterialCode = value;
  if (!value.trim()) {
    row.glueBoardMaterialId = undefined;
    row.glueBoardMaterialName = '';
    row.glueBoardSpec = '';
  }
}

function openMaterialPicker(row: ItemRow) {
  activeItemRowKey.value = row._rowKey;
  materialModalOpen.value = true;
}

function applyMaterialToRow(row: ItemRow, option: PickerOption) {
  row.glueBoardMaterialId = Number(option.id) || undefined;
  row.glueBoardMaterialCode = option.code || '';
  row.glueBoardMaterialName = option.name || '';
  row.glueBoardSpec = option.extra?.specModel || '';
  if (!row.glueBoardModel) {
    row.glueBoardModel = option.extra?.specModel || '';
  }
}

function handlePickMaterial(option: PickerOption) {
  const target = itemRows.value.find((item) => item._rowKey === activeItemRowKey.value);
  materialModalOpen.value = false;
  if (target) {
    applyMaterialToRow(target, option);
  }
}

function handleInlinePickMaterial(row: ItemRow, option: PickerOption) {
  applyMaterialToRow(row, option);
}

function buildSubmitItems() {
  return itemRows.value
    .filter((row) => row.glueProcess && (row.glueBoardMaterialCode || row.glueBoardMaterialId || row.glueBoardModel))
    .map(({ _rowKey, ...rest }) => ({
      ...rest,
      glueProcessName: getGlueProcessName(rest.glueProcess),
    }));
}

async function resetState() {
  formData.value = undefined;
  productModelText.value = '';
  productModelModalOpen.value = false;
  materialModalOpen.value = false;
  activeItemRowKey.value = '';
  itemRows.value = [
    normalizeItem({ glueProcess: 'ADHESIVE1', glueProcessName: '粘胶1', sort: 1 }),
    normalizeItem({ glueProcess: 'ADHESIVE2', glueProcessName: '粘胶2', sort: 2 }),
  ];
  await formApi.resetForm();
  await formApi.setValues({ status: 'ENABLE' });
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  fullscreen: true,
  fullscreenButton: false,
  class: 'hc-finished-glue-board-map-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap;
    if (!values.productModelCode) {
      message.warning('请选择或输入产品型号');
      return;
    }
    const submitItems = buildSubmitItems();
    if (!submitItems.length) {
      message.warning('请至少维护一条胶板对照明细');
      return;
    }
    modalApi.lock();
    try {
      const submitData = {
        ...values,
        items: submitItems,
      };
      if (values.id) {
        await updateFinishedGlueBoardMap(submitData);
        message.success('更新成功');
      } else {
        await createFinishedGlueBoardMap(submitData);
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
    const data = modalApi.getData<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap>();
    if (!data?.id) {
      await resetState();
      return;
    }
    modalApi.lock();
    try {
      const detail = await getFinishedGlueBoardMapDetail(data.id);
      formData.value = detail;
      productModelText.value = detail.productModelCode || '';
      await formApi.resetForm();
      await formApi.setValues(detail);
      itemRows.value = (detail.items?.length ? detail.items : []).map((item) => normalizeItem(item));
      nextTick(() => itemTableRef.value?.recalculate?.(true));
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
    <div class="hc-finished-glue-board-map">
      <div class="hc-finished-glue-board-map__panel">
        <div class="hc-finished-glue-board-map__panel-title">基本信息</div>
        <div class="hc-finished-glue-board-map__product-row">
          <div class="hc-finished-glue-board-map__product-label">产品型号</div>
          <div class="hc-finished-glue-board-map__product-control">
            <PickerInline
              :config="productModelPickerConfig"
              :model-value="productModelText"
              placeholder="请选择或输入产品型号"
              @update:model-value="handleProductModelInput"
              @search="() => { productModelModalOpen = true; }"
              @pick="handlePickProductModel"
            />
          </div>
        </div>
        <Form />
      </div>

      <div class="hc-finished-glue-board-map__panel hc-finished-glue-board-map__panel--items">
        <div class="hc-finished-glue-board-map__panel-header">
          <div class="hc-finished-glue-board-map__panel-title">胶板对照明细</div>
          <div class="hc-finished-glue-board-map__actions">
            <Button size="small" type="primary" @click="addItem('ADHESIVE1')">新增粘胶1</Button>
            <Button size="small" type="primary" @click="addItem('ADHESIVE2')">新增粘胶2</Button>
          </div>
        </div>
        <div class="hc-finished-glue-board-map__table">
          <VxeTable ref="itemTableRef" :data="itemRows" auto-resize border stripe size="small" height="100%">
            <VxeColumn field="glueProcess" title="粘胶工序" width="130">
              <template #default="{ row }">
                <Select
                  :value="row.glueProcess"
                  :options="glueProcessOptions"
                  class="w-full"
                  placeholder="请选择"
                  @change="(value) => handleGlueProcessChange(row, value as string)"
                />
              </template>
            </VxeColumn>
            <VxeColumn field="glueBoardMaterialCode" title="胶板料号" min-width="190">
              <template #default="{ row }">
                <PickerInline
                  :config="materialPickerConfig"
                  :model-value="row.glueBoardMaterialCode || ''"
                  placeholder="请选择或输入胶板料号"
                  @update:model-value="(value) => handleMaterialCodeInput(row, value)"
                  @search="() => openMaterialPicker(row)"
                  @pick="(option) => handleInlinePickMaterial(row, option)"
                />
              </template>
            </VxeColumn>
            <VxeColumn field="glueBoardMaterialName" title="胶板名称" min-width="150">
              <template #default="{ row }">
                <Input :value="row.glueBoardMaterialName || '-'" disabled />
              </template>
            </VxeColumn>
            <VxeColumn field="glueBoardModel" title="胶板型号" min-width="180">
              <template #default="{ row }">
                <Input v-model:value="row.glueBoardModel" placeholder="请输入胶板型号" />
              </template>
            </VxeColumn>
            <VxeColumn field="glueBoardSpec" title="规格型号快照" min-width="190">
              <template #default="{ row }">
                <Input :value="row.glueBoardSpec || '-'" disabled />
              </template>
            </VxeColumn>
            <VxeColumn field="preferredFlag" title="优先" width="90" align="center">
              <template #default="{ row }">
                <Switch v-model:checked="row.preferredFlag" checked-children="是" un-checked-children="否" />
              </template>
            </VxeColumn>
            <VxeColumn field="sort" title="排序" width="90" align="center">
              <template #default="{ row }">
                <InputNumber v-model:value="row.sort" :min="1" :precision="0" class="w-full" />
              </template>
            </VxeColumn>
            <VxeColumn field="remark" title="备注" min-width="180">
              <template #default="{ row }">
                <Input v-model:value="row.remark" placeholder="请输入备注" />
              </template>
            </VxeColumn>
            <VxeColumn title="操作" width="80" fixed="right" align="center">
              <template #default="{ $rowIndex }">
                <Tooltip title="删除">
                  <Button danger type="link" class="hc-finished-glue-board-map__icon-btn" @click="removeItem($rowIndex)">
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
      :config="productModelPickerConfig"
      :open="productModelModalOpen"
      title="选择产品型号"
      @close="productModelModalOpen = false"
      @pick="handlePickProductModel"
    />
    <PickerModal
      :config="materialPickerConfig"
      :open="materialModalOpen"
      title="选择胶板料号"
      @close="materialModalOpen = false"
      @pick="handlePickMaterial"
    />
  </Modal>
</template>

<style>
.hc-finished-glue-board-map-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.hc-finished-glue-board-map {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
  padding-right: 4px;
}

.hc-finished-glue-board-map__panel {
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.hc-finished-glue-board-map__panel--items {
  min-height: 0;
  flex: 1;
}

.hc-finished-glue-board-map__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.hc-finished-glue-board-map__panel-title {
  margin-bottom: 10px;
  font-weight: 600;
  color: var(--ant-color-text);
}

.hc-finished-glue-board-map__panel-header .hc-finished-glue-board-map__panel-title {
  margin-bottom: 0;
}

.hc-finished-glue-board-map__product-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.hc-finished-glue-board-map__product-label {
  color: var(--ant-color-text);
  text-align: right;
}

.hc-finished-glue-board-map__product-control {
  max-width: 720px;
}

.hc-finished-glue-board-map__actions {
  display: flex;
  gap: 8px;
}

.hc-finished-glue-board-map__table {
  height: 440px;
  min-height: 440px;
  overflow: hidden;
}

.hc-finished-glue-board-map__icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding-inline: 4px;
}
</style>
