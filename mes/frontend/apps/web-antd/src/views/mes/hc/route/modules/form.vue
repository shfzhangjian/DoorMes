<script lang="ts" setup>
import type { MesHcRouteApi } from '#/api/mes/hc/route';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { Button, Input, InputNumber, Select, Tooltip, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import { getMaterialPage } from '#/api/mes/hc/material';
import { createRoute, getRouteDetail, updateRoute } from '#/api/mes/hc/route';
import { getWorkCenterSimpleList } from '#/api/mes/hc/workcenter';
import { getUnitSelectOptions } from '#/api/mes/base/unit';

import { useFormSchema } from '../data';
import { PickerInline, PickerModal, materialPickerConfig, workcenterPickerConfig } from '#/components/picker';
import type { PickerOption } from '#/components/picker';
import ParamTemplateModal from './param-template-modal.vue';

const emit = defineEmits(['success']);

type RouteRow = MesHcRouteApi.RouteOperation & {
  _rowKey: string;
  workCenterCode?: string;
  workCenterName?: string;
};

type MaterialLite = {
  id: number;
  code?: string;
  name?: string;
  label?: string;
};

const formData = ref<MesHcRouteApi.Route>();
const routeOperations = ref<RouteRow[]>([]);
const materialList = ref<MaterialLite[]>([]);
const workCenterList = ref<MesHcWorkCenterApi.SimpleItem[]>([]);
const rowSeed = ref(1);
const itemTableRef = ref<any>();
const invalidOperationRowKeys = ref<string[]>([]);
const currentOperationRowKey = ref('');
const pendingOperationScrollRowKey = ref('');
const productMaterialCode = ref('');
const productMaterialName = ref('');
const baseExpanded = ref(true);
const itemsExpanded = ref(true);
const pickerContext = ref<{ source: 'code' | 'name' }>({ source: 'code' });
const workCenterPickerContext = ref<{ rowKey?: string; source: 'code' | 'name' }>({ source: 'code' });
const paramTemplateRowKey = ref('');
const currentApplicableScope = ref('MATERIAL');
const unitOptions = ref<{ label: string; value: number; code?: string; name?: string }[]>([]);

const workCenterMap = computed(
  () =>
    Object.fromEntries(workCenterList.value.map((item) => [item.id, item])) as Record<
      number,
      MesHcWorkCenterApi.SimpleItem
    >,
);
const isGlobalRoute = computed(() => currentApplicableScope.value === 'GLOBAL');

const getTitle = computed(() => (formData.value?.id ? '编辑工艺路线' : '新增工艺路线'));

const materialPickerOpen = ref(false);
const workCenterPickerOpen = ref(false);

const [ParamTemplateModalComp, paramTemplateModalApi] = useVbenModal({
  connectedComponent: ParamTemplateModal,
  destroyOnClose: false,
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useFormSchema(handleApplicableScopeChange),
  showDefaultActions: false,
});

function nextRowKey() {
  rowSeed.value += 1;
  return `route-row-${Date.now()}-${rowSeed.value}`;
}

function normalizeSeqNo(value: unknown, fallback: number) {
  const numericValue = Number(value);
  return Number.isFinite(numericValue) && numericValue > 0 ? numericValue : fallback;
}

function resolveRouteOperationSeqNo(row: Partial<MesHcRouteApi.RouteOperation> | undefined, index: number) {
  return normalizeSeqNo(row?.seqNo, index + 1);
}

function getNextRouteOperationSeqNo() {
  const maxSeqNo = routeOperations.value.reduce((max, item, index) => {
    return Math.max(max, resolveRouteOperationSeqNo(item, index));
  }, 0);
  return maxSeqNo + 1;
}

function sortRouteOperationsBySeqNo(options: { renumber?: boolean } = {}) {
  const originalIndexMap = new Map(routeOperations.value.map((item, index) => [item._rowKey, index]));
  routeOperations.value = [...routeOperations.value].sort((a, b) => {
    const seqDiff = normalizeSeqNo(a.seqNo, Number.MAX_SAFE_INTEGER) - normalizeSeqNo(b.seqNo, Number.MAX_SAFE_INTEGER);
    if (seqDiff !== 0) return seqDiff;
    return (originalIndexMap.get(a._rowKey) ?? 0) - (originalIndexMap.get(b._rowKey) ?? 0);
  });
  if (options.renumber) {
    routeOperations.value.forEach((item, index) => {
      item.seqNo = index + 1;
    });
  }
  recalculateItemTable();
}

function normalizeRow(row?: Partial<MesHcRouteApi.RouteOperation>, fallbackSeqNo = getNextRouteOperationSeqNo()): RouteRow {
  const unitId = (row as any)?.outputUnitId || resolveUnitIdByCode((row as any)?.outputUnitCode || (row as any)?.outputUom);
  return {
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey(),
    seqNo: normalizeSeqNo(row?.seqNo, fallbackSeqNo),
    reportRequired: row?.reportRequired ?? true,
    conversionRate: (row as any)?.conversionRate ?? 1,
    outputUnitId: unitId,
    outputUnitCode: (row as any)?.outputUnitCode || (row as any)?.outputUom || '',
    outputUnitName: (row as any)?.outputUnitName || '',
    outputUom: (row as any)?.outputUom || '',
    qcRequired: row?.qcRequired ?? false,
    workCenterCode: (row as any)?.workCenterCode || '',
    workCenterName: (row as any)?.workCenterName || '',
  } as RouteRow;
}

function resolveUnitIdByCode(code?: string) {
  const text = String(code || '').trim();
  if (!text) return undefined;
  return unitOptions.value.find((item) => item.code === text)?.value;
}

function handleApplicableScopeChange(value?: string) {
  currentApplicableScope.value = value || 'MATERIAL';
  if (currentApplicableScope.value === 'GLOBAL') {
    setProductMaterial(undefined);
  }
}

function isEmptyOperation(row: Partial<RouteRow>) {
  return ![
    row.operationCode,
    row.operationName,
    row.workCenterId,
    row.workCenterCode,
    row.workCenterName,
    (row as any).conversionRate,
    (row as any).outputUnitId,
    (row as any).outputUnitCode,
    (row as any).outputUnitName,
    row.paramTemplateJson,
    row.remark,
  ].some((value) => value !== undefined && value !== null && String(value).trim() !== '');
}

function recalculateItemTable() {
  nextTick(() => itemTableRef.value?.recalculate?.(true));
}

function scrollToOperationTableRow(rowKey?: string) {
  if (!rowKey) return;
  const doScroll = () => {
    const row = routeOperations.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    itemTableRef.value?.scrollToRow?.(row);
  };
  nextTick(() => {
    doScroll();
    setTimeout(doScroll, 60);
  });
}

function focusOperationRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const holder = document.querySelector(`.hc-route-modal [data-focus-key="${rowKey}"]`) as HTMLElement | null;
    if (!holder) return;
    holder.scrollIntoView({ block: 'center', behavior: 'smooth' });
    const target = holder.querySelector('input, .ant-select-selector') as HTMLElement | null;
    target?.focus?.();
  });
}

function toggleBaseExpanded() {
  baseExpanded.value = !baseExpanded.value;
}

function toggleItemsExpanded() {
  itemsExpanded.value = !itemsExpanded.value;
  if (itemsExpanded.value) {
    nextTick(() => {
      recalculateItemTable();
      if (pendingOperationScrollRowKey.value) {
        const rowKey = pendingOperationScrollRowKey.value;
        pendingOperationScrollRowKey.value = '';
        scrollToOperationTableRow(rowKey);
        focusOperationRow(rowKey);
      }
    });
  }
}

function validateOperations(items: RouteRow[]) {
  const invalidIndex = items.findIndex((item) => {
    if (isEmptyOperation(item)) return true;
    return !item.workCenterCode?.trim() || !item.operationCode?.trim() || !item.operationName?.trim();
  });
  invalidOperationRowKeys.value = invalidIndex >= 0 ? [items[invalidIndex]?._rowKey || ''] : [];
  if (invalidIndex >= 0) {
    itemsExpanded.value = true;
    const row = items[invalidIndex];
    if (row && isEmptyOperation(row)) {
      message.warning(`路线工序第 ${invalidIndex + 1} 行为空，请填写后提交或删除空行`);
    } else if (!row?.workCenterCode?.trim()) {
      message.warning(`路线工序第 ${invalidIndex + 1} 行请选择默认工作中心`);
    } else {
      message.warning(`路线工序第 ${invalidIndex + 1} 行未带出工序编码或工序名称，请重新选择默认工作中心`);
    }
    focusOperationRow(row?._rowKey);
    return false;
  }
  return true;
}

function setProductMaterial(material?: MaterialLite) {
  productMaterialCode.value = material?.code || '';
  productMaterialName.value = material?.name || material?.label || '';
  formApi.setValues({
    productMaterialId: material?.id,
    productMaterialCode: material?.code || '',
    productMaterialName: material?.name || material?.label || '',
  });
}

function clearProductMaterialByInput(field: 'code' | 'name', value: string) {
  const nextValue = String(value || '');
  if (field === 'code') {
    productMaterialCode.value = nextValue;
    productMaterialName.value = '';
  } else {
    productMaterialName.value = nextValue;
    productMaterialCode.value = '';
  }
  formApi.setValues({
    productMaterialId: undefined,
    productMaterialCode: field === 'code' ? nextValue : '',
    productMaterialName: field === 'name' ? nextValue : '',
  });
}

function openProductMaterialPicker(source: 'code' | 'name') {
  pickerContext.value = { source };
  materialPickerOpen.value = true;
}

function handleMaterialPickerSelect(option: PickerOption) {
  setProductMaterial({
    id: Number(option.id),
    code: option.code,
    name: option.name,
    label: option.name,
  });
  materialPickerOpen.value = false;
}

function setRowWorkCenter(
  row: RouteRow,
  workCenter?: MesHcWorkCenterApi.WorkCenter | MesHcWorkCenterApi.SimpleItem,
) {
  const workCenterCode = (workCenter as any)?.wcCode || (workCenter as any)?.code || '';
  const workCenterName = (workCenter as any)?.wcName || (workCenter as any)?.name || '';
  const processStage = (workCenter as any)?.processStage || '';
  const processName = (workCenter as any)?.processName || '';
  row.workCenterId = workCenter?.id as any;
  row.workCenterCode = workCenterCode;
  row.workCenterName = workCenterName;
  row.operationCode = workCenterCode;
  row.operationName = processName || processStage || workCenterName;
}

function clearRowWorkCenterByInput(row: RouteRow, field: 'code' | 'name', value: string) {
  const nextValue = String(value || '');
  row.workCenterId = undefined as any;
  row.operationCode = '';
  row.operationName = '';
  if (field === 'code') {
    row.workCenterCode = nextValue;
    row.workCenterName = '';
  } else {
    row.workCenterName = nextValue;
    row.workCenterCode = '';
  }
}

function openWorkCenterPicker(row: RouteRow, source: 'code' | 'name') {
  workCenterPickerContext.value = { rowKey: row._rowKey, source };
  workCenterPickerOpen.value = true;
}

function handleWorkCenterPickerSelect(option: PickerOption) {
  const target = routeOperations.value.find((item) => item._rowKey === workCenterPickerContext.value.rowKey);
  if (!target) return;
  setRowWorkCenter(target, {
    id: Number(option.id),
    wcCode: option.code,
    wcName: option.name,
    processStage: option.extra?.processStage || option.raw?.processStage,
    processCode: option.extra?.processCode || option.raw?.processCode,
    processName: option.extra?.processName || option.raw?.processName,
  } as any);
  workCenterPickerOpen.value = false;
}

function getParamTemplateSummary(value?: string) {
  if (!value?.trim()) return '未配置';
  try {
    const parsed = JSON.parse(value);
    if (parsed?.version && Array.isArray(parsed?.sections)) {
      const sectionCount = parsed.sections.length;
      const fieldCount = parsed.sections.reduce((sum: number, section: any) => sum + (section.fields?.length || 0), 0);
      return `${parsed.formStyle || '参数表'} · ${sectionCount}组/${fieldCount}项`;
    }
    const fieldCount = Object.keys(parsed || {}).length;
    return `旧模板 · 1组/${fieldCount}项`;
  } catch {
    return '模板数据异常';
  }
}

function openParamTemplateConfig(row: RouteRow) {
  currentOperationRowKey.value = row._rowKey;
  paramTemplateRowKey.value = row._rowKey;
  paramTemplateModalApi
    .setData({
      value: row.paramTemplateJson || '',
      operationCode: row.operationCode || '',
      operationName: row.operationName || '',
    })
    .open();
}

function handleParamTemplateSave(payload: { value: string }) {
  const row = routeOperations.value.find((item) => item._rowKey === paramTemplateRowKey.value);
  if (!row) return;
  row.paramTemplateJson = payload.value;
}

function addOperation() {
  const row = normalizeRow();
  routeOperations.value.push(row);
  invalidOperationRowKeys.value = [];
  currentOperationRowKey.value = row._rowKey;
  itemsExpanded.value = true;
  pendingOperationScrollRowKey.value = row._rowKey;
  recalculateItemTable();
  scrollToOperationTableRow(row._rowKey);
  focusOperationRow(row._rowKey);
}

function removeOperation(index: number) {
  const removed = routeOperations.value[index];
  routeOperations.value.splice(index, 1);
  invalidOperationRowKeys.value = [];
  if (currentOperationRowKey.value === removed?._rowKey) {
    currentOperationRowKey.value =
      routeOperations.value[Math.max(0, index - 1)]?._rowKey || routeOperations.value[0]?._rowKey || '';
  }
  routeOperations.value.forEach((item, itemIndex) => {
    item.seqNo = itemIndex + 1;
  });
  recalculateItemTable();
}

function copyOperation(index: number) {
  const source = routeOperations.value[index];
  if (!source) return;
  const sourceData = { ...source, id: 0 } as any;
  delete sourceData._rowKey;
  const cloned = normalizeRow(sourceData);
  routeOperations.value.splice(index + 1, 0, cloned);
  routeOperations.value.forEach((item, itemIndex) => {
    item.seqNo = itemIndex + 1;
  });
  invalidOperationRowKeys.value = [];
  currentOperationRowKey.value = cloned._rowKey;
  itemsExpanded.value = true;
  pendingOperationScrollRowKey.value = cloned._rowKey;
  recalculateItemTable();
  scrollToOperationTableRow(cloned._rowKey);
  focusOperationRow(cloned._rowKey);
}

function moveOperation(index: number, direction: 'up' | 'down') {
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= routeOperations.value.length) return;
  const current = routeOperations.value[index];
  routeOperations.value[index] = routeOperations.value[targetIndex]!;
  routeOperations.value[targetIndex] = current!;
  routeOperations.value.forEach((item, itemIndex) => {
    item.seqNo = itemIndex + 1;
  });
  currentOperationRowKey.value = routeOperations.value[targetIndex]?._rowKey || '';
  recalculateItemTable();
}

async function loadOptions() {
  if (materialList.value.length === 0) {
    const res = await getMaterialPage({ pageNo: 1, pageSize: 200 });
    materialList.value = (res.list || []).map((item) => ({
      id: Number(item.id),
      code: item.materialCode || '',
      name: item.materialName || '',
      label: item.materialName || '',
    }));
  }
  if (workCenterList.value.length === 0) {
    workCenterList.value = await getWorkCenterSimpleList();
  }
  if (unitOptions.value.length === 0) {
    unitOptions.value = (await getUnitSelectOptions()).map((item) => ({
      label: item.label,
      value: Number(item.value),
      code: item.code,
      name: item.name,
    }));
  }
}

async function resetRouteFormState() {
  formData.value = undefined;
  routeOperations.value = [];
  productMaterialCode.value = '';
  productMaterialName.value = '';
  currentApplicableScope.value = 'MATERIAL';
  baseExpanded.value = true;
  itemsExpanded.value = true;
  invalidOperationRowKeys.value = [];
  currentOperationRowKey.value = '';
  pendingOperationScrollRowKey.value = '';
  await formApi.resetForm();
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  fullscreen: true,
  fullscreenButton: false,
  class: 'hc-route-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      baseExpanded.value = true;
      return;
    }

    modalApi.lock();
    const data = (await formApi.getValues()) as MesHcRouteApi.Route;
    try {
      data.applicableScope = data.applicableScope || 'MATERIAL';
      data.productMaterialCode = productMaterialCode.value || data.productMaterialCode;
      data.productMaterialName = productMaterialName.value || data.productMaterialName;
      if (data.applicableScope === 'GLOBAL') {
        data.productMaterialId = undefined;
        data.productMaterialCode = '';
        data.productMaterialName = '';
      }
      if (data.applicableScope !== 'GLOBAL' && !data.productMaterialId) {
        baseExpanded.value = true;
        message.warning('请选择适用物料');
        return;
      }

      sortRouteOperationsBySeqNo({ renumber: true });
      const rawOperations = routeOperations.value.map((row, index) => ({
        ...row,
        seqNo: normalizeSeqNo(row.seqNo, index + 1),
      }));
      if (!validateOperations(rawOperations)) return;
      const validOperations = rawOperations
        .filter((row) => !isEmptyOperation(row))
        .map((row, index) => {
          const workCenter = workCenterMap.value[Number(row.workCenterId)];
          const unit = unitOptions.value.find((item) => Number(item.value) === Number((row as any).outputUnitId));
          const { _rowKey, ...rest } = row as any;
          return {
            ...rest,
            routeCode: data.routeCode,
            seqNo: normalizeSeqNo(row.seqNo, index + 1),
            workCenterCode: workCenter?.code || row.workCenterCode,
            workCenterName: workCenter?.name || row.workCenterName,
            operationCode: workCenter?.code || row.workCenterCode || row.operationCode,
            operationName: workCenter?.processName || workCenter?.processStage || row.operationName || workCenter?.name,
            outputUnitCode: unit?.code || rest.outputUnitCode || rest.outputUom || '',
            outputUnitName: unit?.name || rest.outputUnitName || '',
            outputUom: unit?.code || rest.outputUnitCode || rest.outputUom || '',
          };
        });
      data.routeOperations = validOperations;
      await (formData.value?.id ? updateRoute(data) : createRoute(data));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;

    await loadOptions();
    const data = modalApi.getData<MesHcRouteApi.Route>();
    if (!data?.id) {
      await resetRouteFormState();
      return;
    }

    modalApi.lock();
    try {
      const detail = await getRouteDetail(data.id);
      formData.value = { ...detail, applicableScope: detail.applicableScope || 'MATERIAL' };
      currentApplicableScope.value = formData.value.applicableScope || 'MATERIAL';
      await formApi.setValues(formData.value);
      productMaterialCode.value = (formData.value as any).productMaterialCode || '';
      productMaterialName.value = (formData.value as any).productMaterialName || '';
      routeOperations.value = ((formData.value as any).routeOperations || []).map((item: any, index: number) =>
        normalizeRow(item, index + 1),
      );
      sortRouteOperationsBySeqNo();
      invalidOperationRowKeys.value = [];
      currentOperationRowKey.value = routeOperations.value[0]?._rowKey || '';
      nextTick(() => {
        baseExpanded.value = true;
        itemsExpanded.value = true;
        recalculateItemTable();
      });
    } finally {
      modalApi.unlock();
    }
  },
  async onClosed() {
    await resetRouteFormState();
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="route-form-modal">
      <PickerModal
        :config="materialPickerConfig"
        :open="materialPickerOpen"
        title="选择物料"
        @close="materialPickerOpen = false"
        @pick="handleMaterialPickerSelect"
      />
      <PickerModal
        :config="workcenterPickerConfig"
        :open="workCenterPickerOpen"
        title="选择工作中心"
        @close="workCenterPickerOpen = false"
        @pick="handleWorkCenterPickerSelect"
      />
      <ParamTemplateModalComp @success="handleParamTemplateSave" />

      <div class="route-form-modal__scroll">
        <section class="route-panel">
          <div class="route-panel__header">
            <span class="route-panel__title-chip">基本信息</span>
            <div class="route-panel__header-spacer" />
            <Button type="text" size="small" class="route-panel__toggle-btn" @click="toggleBaseExpanded">
              <IconifyIcon :icon="baseExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="baseExpanded" class="route-panel__body route-panel__body--base">
            <div class="route-base-form-box">
              <Form />
              <div class="route-product-form-row">
                <div class="route-product-form-item">
                  <div class="route-product-form-item__label">适用物料编码</div>
                  <div class="route-product-form-item__content">
                    <PickerInline
                      :model-value="productMaterialCode"
                      :config="materialPickerConfig"
                      :disabled="isGlobalRoute"
                      placeholder="请输入物料编码或快速选择"
                      @update:model-value="(value) => clearProductMaterialByInput('code', value)"
                      @search="openProductMaterialPicker('code')"
                      @pick="handleMaterialPickerSelect"
                    />
                  </div>
                </div>
                <div class="route-product-form-item">
                  <div class="route-product-form-item__label">适用物料名称</div>
                  <div class="route-product-form-item__content">
                    <PickerInline
                      :model-value="productMaterialName"
                      :config="materialPickerConfig"
                      :disabled="isGlobalRoute"
                      placeholder="请输入物料名称或快速选择"
                      @update:model-value="(value) => clearProductMaterialByInput('name', value)"
                      @search="openProductMaterialPicker('name')"
                      @pick="handleMaterialPickerSelect"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="route-panel route-panel--table">
          <div class="route-panel__header">
            <span class="route-panel__title-chip">路线工序</span>
            <div class="route-panel__header-spacer" />
            <div v-show="itemsExpanded" class="route-panel__toolbar-actions route-panel__toolbar-actions--body">
              <Button type="primary" size="small" @click="addOperation">新增工序</Button>
            </div>
            <Button type="text" size="small" class="route-panel__toggle-btn" @click="toggleItemsExpanded">
              <IconifyIcon :icon="itemsExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="itemsExpanded" class="route-panel__body route-panel__body--items">
            <VxeTable
              ref="itemTableRef"
              :data="routeOperations"
              :row-class-name="({ row }) => (invalidOperationRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentOperationRowKey === row._rowKey ? 'is-current-row' : '')"
              auto-resize
              border
              stripe
              :round="false"
              size="small"
              height="100%"
              show-overflow
              row-id="_rowKey"
              @cell-click="({ row }) => (currentOperationRowKey = row._rowKey)"
            >
              <VxeColumn field="seqNo" title="顺序号" width="100" align="center" header-align="center">
                <template #default="{ row }">
                  <InputNumber
                    v-model:value="row.seqNo"
                    :min="1"
                    class="w-full"
                    @blur="sortRouteOperationsBySeqNo({ renumber: true })"
                  />
                </template>
              </VxeColumn>
              <VxeColumn field="operationCode" title="*工序编码" width="160" header-align="center">
                <template #default="{ row }">
                  <Input v-model:value="row.operationCode" readonly placeholder="选择工作中心后带出" />
                </template>
              </VxeColumn>
              <VxeColumn field="operationName" title="*工序名称" width="180" header-align="center">
                <template #default="{ row }">
                  <Input v-model:value="row.operationName" readonly placeholder="选择工作中心后带出" />
                </template>
              </VxeColumn>
              <VxeColumn field="workCenterName" title="默认工作中心" min-width="220" header-align="center">
                <template #default="{ row }">
                  <div :data-focus-key="row._rowKey">
                    <PickerInline
                      :model-value="row.workCenterName"
                      :config="workcenterPickerConfig"
                      placeholder="请输入工作中心或快速选择"
                      @update:model-value="(value) => clearRowWorkCenterByInput(row, 'name', value)"
                      @search="openWorkCenterPicker(row, 'name')"
                      @pick="(opt) => setRowWorkCenter(row, { id: Number(opt.id), wcCode: opt.code, wcName: opt.name, processStage: opt.extra?.processStage || opt.raw?.processStage, processCode: opt.extra?.processCode || opt.raw?.processCode, processName: opt.extra?.processName || opt.raw?.processName } as any)"
                    />
                  </div>
                </template>
              </VxeColumn>
              <VxeColumn field="conversionRate" title="转换率" width="120" header-align="center">
                <template #default="{ row }">
                  <InputNumber v-model:value="row.conversionRate" :min="0.000001" :precision="6" class="w-full" />
                </template>
              </VxeColumn>
              <VxeColumn field="outputUnitId" title="产出计量单位" width="160" header-align="center">
                <template #default="{ row }">
                  <Select v-model:value="row.outputUnitId" :options="unitOptions" class="w-full" allow-clear placeholder="请选择单位" />
                </template>
              </VxeColumn>
              <VxeColumn field="paramTemplateJson" title="参数模板" min-width="220" header-align="center">
                <template #default="{ row }">
                  <div class="route-param-template-cell">
                    <Button size="small" @click="openParamTemplateConfig(row)">配置参数模板</Button>
                    <span class="route-param-template-cell__summary">
                      {{ getParamTemplateSummary(row.paramTemplateJson) }}
                    </span>
                  </div>
                </template>
              </VxeColumn>
              <VxeColumn field="remark" title="备注" min-width="180" header-align="center">
                <template #default="{ row }">
                  <Input v-model:value="row.remark" placeholder="请输入备注" />
                </template>
              </VxeColumn>
              <VxeColumn title="操作" width="160" fixed="right" align="center" header-align="center">
                <template #default="{ $rowIndex }">
                  <div class="route-subtable-actions">
                    <Tooltip title="复制">
                      <Button type="text" @click="copyOperation($rowIndex)">
                        <IconifyIcon icon="lucide:copy" />
                      </Button>
                    </Tooltip>
                    <Tooltip title="上移">
                      <Button type="text" @click="moveOperation($rowIndex, 'up')">
                        <IconifyIcon icon="lucide:arrow-up" />
                      </Button>
                    </Tooltip>
                    <Tooltip title="下移">
                      <Button type="text" @click="moveOperation($rowIndex, 'down')">
                        <IconifyIcon icon="lucide:arrow-down" />
                      </Button>
                    </Tooltip>
                    <Tooltip title="删除">
                      <Button danger type="text" @click="removeOperation($rowIndex)">
                        <IconifyIcon icon="lucide:trash-2" />
                      </Button>
                    </Tooltip>
                  </div>
                </template>
              </VxeColumn>
            </VxeTable>
          </div>
        </section>
      </div>
    </div>
  </Modal>
</template>

<style>
.hc-route-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.route-form-modal {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.route-form-modal__scroll {
  height: 100%;
  overflow: auto;
  padding-right: 4px;
}

.route-panel {
  margin-top: 10px;
  margin-bottom: 12px;
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
  overflow: visible;
  min-width: 0;
  padding: 0;
}

.route-panel__title-chip {
  display: inline-flex;
  align-items: center;
  min-width: 76px;
  height: 24px;
  padding: 0 14px;
  border-radius: 3px;
  background: #8b8b8b;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  line-height: 24px;
}

.route-panel__toolbar-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.route-panel__body {
  padding: 2px 12px 12px;
}

.route-panel__body--base {
  overflow: hidden;
}

.route-base-form-box {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
}

.route-panel__body--items {
  height: 500px;
  min-height: 500px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.route-panel__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px 0;
}

.route-panel__toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  width: 28px;
  height: 28px;
  padding: 0;
  color: #6b7280;
}

.route-panel__header-spacer {
  flex: 1;
}

.route-panel__body--items :deep(.vxe-table) {
  flex: 1;
  min-height: 0;
}

.route-product-form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 8px;
}

.route-product-form-item {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.route-product-form-item__label {
  color: #1f2329;
  text-align: right;
  line-height: 32px;
}

.route-product-form-item__content {
  min-width: 0;
}

.route-panel__body--items :deep(.vxe-table) {
  border-radius: 0;
}

.route-panel__body--items :deep(.vxe-table--border-wrapper),
.route-panel__body--items :deep(.vxe-table--main-wrapper) {
  height: 100%;
}

.route-panel__body--items :deep(.is-invalid-row) {
  background-color: #fff1f0;
}

.route-subtable-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
}

.route-subtable-actions :deep(.ant-btn) {
  min-width: 24px;
  padding: 0 4px;
  font-size: 14px;
}

.route-param-template-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.route-param-template-cell__summary {
  min-width: 0;
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
