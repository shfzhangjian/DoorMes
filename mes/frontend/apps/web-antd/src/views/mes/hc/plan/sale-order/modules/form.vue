<script lang="ts" setup>
import type { MesSaleOrderApi } from '#/api/mes/sale-order';

import { computed, ref } from 'vue';

import { useAccess } from '@vben/access';
import { confirm, useVbenModal } from '@vben/common-ui';
import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Select,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { PickerInline, PickerModal, materialPickerConfig } from '#/components/picker';
import type { PickerOption } from '#/components/picker';
import { getMaterialDetail } from '#/api/mes/hc/material';
import {
  auditSaleOrder,
  createSaleOrder,
  deleteSaleOrder,
  getSaleOrder,
  updateSaleOrder,
} from '#/api/mes/sale-order';

import {
  PLAN_FIELD_COPY,
  PLAN_PLACEHOLDER_COPY,
  SIZE_SPEC_OPTIONS,
} from '../../shared/field-copy';

const emit = defineEmits(['success']);

const readOnly = ref(false);
const materialPickerOpen = ref(false);
const formState = ref<MesSaleOrderApi.MesSaleOrder>(buildEmptyOrder());
const { hasAccessByCodes } = useAccess();

const title = computed(() => {
  if (!formState.value.id) return '新增销售订单草稿';
  return readOnly.value ? '查阅销售订单' : '修改销售订单草稿';
});

const canForceDelete = computed(() => hasAccessByCodes(['mes:sale-order:delete-force']));
const canDelete = computed(() => {
  return !!formState.value.id && (formState.value.status === 'DRAFT' || canForceDelete.value);
});

function buildEmptyOrder(): MesSaleOrderApi.MesSaleOrder {
  return {
    orderNo: '',
    erpNo: '',
    customerName: '',
    productId: undefined,
    productCode: '',
    productName: '',
    productSpec: '',
    materialId: undefined,
    materialCode: '',
    materialName: '',
    modelCode: '',
    sizeSpec: undefined,
    sizeName: '',
    quantity: 0,
    plannedQty: 0,
    remainQty: 0,
    unitId: undefined,
    unitCode: '',
    unitName: '',
    unit: '',
    deliveryDate: '',
    status: 'DRAFT',
    statusName: '草稿',
  };
}

function normalizeLocalDate(value?: unknown) {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day] = value;
    if (year && month && day) {
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
    return '';
  }
  const dateValue = dayjs(value as any);
  return dateValue.isValid() ? dateValue.format('YYYY-MM-DD') : String(value);
}

function handleMaterialInput(value: string) {
  formState.value.materialId = undefined;
  formState.value.materialCode = String(value || '');
  formState.value.materialName = '';
  formState.value.productId = undefined;
  formState.value.productCode = String(value || '');
  formState.value.productName = '';
  formState.value.modelCode = '';
  formState.value.unitId = undefined;
  formState.value.unitCode = '';
  formState.value.unitName = '';
  formState.value.unit = '';
}

async function handleMaterialPick(option: PickerOption) {
  materialPickerOpen.value = false;
  const detail = await getMaterialDetail(Number(option.id));
  formState.value.materialId = detail.id;
  formState.value.materialCode = detail.materialCode || option.code || '';
  formState.value.materialName = detail.materialName || option.name || option.label || '';
  // 销售订单兼容旧 product 字段，但文案和交互统一使用生产料号/产品型号/尺寸规格。
  formState.value.productId = detail.id;
  formState.value.productCode = formState.value.materialCode;
  formState.value.productName = formState.value.materialName;
  formState.value.modelCode = detail.modelCode || detail.specModel || '';
  formState.value.unitId = detail.baseUnitId;
  formState.value.unitCode = detail.baseUnitCode || detail.baseUom || '';
  formState.value.unitName = detail.baseUnitName || '';
  formState.value.unit = formState.value.unitCode;
}

function handleSizeSpecChange(code?: string) {
  const option = SIZE_SPEC_OPTIONS.find((item) => item.code === code);
  formState.value.sizeSpec = option?.code || code;
  formState.value.sizeName = option?.name || '';
  formState.value.productSpec = option?.code || code || '';
}

async function save() {
  const data = formState.value;
  if (!data.customerName || !data.materialId || !data.quantity || !data.deliveryDate) {
    message.warning(`请填写${PLAN_FIELD_COPY.customerName}、${PLAN_FIELD_COPY.productionMaterial}、${PLAN_FIELD_COPY.orderQty}和${PLAN_FIELD_COPY.deliveryDate}`);
    return;
  }
  data.unit = data.unitCode || data.unit || '';
  await (data.id ? updateSaleOrder(data) : createSaleOrder(data));
  message.success('保存成功');
  emit('success');
  await modalApi.close();
}

async function auditCurrent() {
  const id = formState.value.id;
  if (!id || formState.value.status !== 'DRAFT') {
    message.warning('只有草稿订单允许审核');
    return;
  }
  await confirm('确认审核该销售订单？审核后不允许修改。');
  const ok = await auditSaleOrder(id);
  if (!ok) {
    message.warning('审核失败，订单可能已不是草稿状态');
    return;
  }
  message.success('审核成功');
  emit('success');
  const detail = await getSaleOrder(id);
  fillDetail(detail);
}

async function deleteCurrent() {
  const id = formState.value.id;
  if (!id || !canDelete.value) {
    message.warning('只有草稿订单允许删除');
    return;
  }
  const forceDelete = formState.value.status !== 'DRAFT';
  await confirm(
    forceDelete
      ? '风险提示：该销售订单已不是草稿，强制删除可能影响生产计划挂接、欠交量统计和追溯数据。确认继续删除？'
      : '确认删除该销售订单草稿？删除后不可恢复。',
  );
  const ok = await deleteSaleOrder(id);
  if (!ok) {
    message.warning('删除失败，订单状态或权限不满足删除条件');
    return;
  }
  message.success('删除成功');
  emit('success');
  await modalApi.close();
}

function fillDetail(detail: MesSaleOrderApi.MesSaleOrder) {
  formState.value = {
    ...detail,
    materialId: detail.materialId || detail.productId,
    materialCode: detail.materialCode || detail.productCode || '',
    materialName: detail.materialName || detail.productName || '',
    sizeSpec: detail.sizeSpec || detail.productSpec || '',
    sizeName: detail.sizeName || SIZE_SPEC_OPTIONS.find((item) => item.code === (detail.sizeSpec || detail.productSpec))?.name || '',
    unitCode: detail.unitCode || detail.unit || '',
    unitName: detail.unitName || '',
    unit: detail.unitCode || detail.unit || '',
    deliveryDate: normalizeLocalDate(detail.deliveryDate),
  };
}

function formatAuditTime(value?: unknown) {
  if (!value) {
    return '-';
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    const dateValue = dayjs(new Date(year, month - 1, day, hour, minute, second));
    return dateValue.isValid() ? dateValue.format('YYYY-MM-DD HH:mm:ss') : String(value);
  }
  const dateValue = typeof value === 'number' ? dayjs(value) : dayjs(String(value).replace('T', ' '));
  return dateValue.isValid() ? dateValue.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

const [Modal, modalApi] = useVbenModal({
  class: 'w-[860px]',
  showConfirmButton: false,
  showCancelButton: false,
  async onOpenChange(open) {
    if (!open) return;
    const data = modalApi.getData<{ id?: number; readonly?: boolean } | null>();
    readOnly.value = !!data?.readonly;
    if (data?.id) {
      const detail = await getSaleOrder(data.id);
      fillDetail(detail);
    } else {
      readOnly.value = false;
      formState.value = buildEmptyOrder();
    }
  },
});
</script>

<template>
  <Modal :title="title">
    <PickerModal
      :config="materialPickerConfig"
      :open="materialPickerOpen"
      title="选择生产料号"
      @close="materialPickerOpen = false"
      @pick="handleMaterialPick"
    />
    <div class="p-4">
      <fieldset class="rounded-md border border-border px-4 pb-1 pt-3">
        <legend class="px-2 text-sm font-semibold text-foreground">基本信息</legend>
        <Form layout="horizontal" :label-col="{ style: { width: '110px' } }">
          <div class="grid grid-cols-2 gap-x-5">
            <Form.Item :label="PLAN_FIELD_COPY.saleOrderNo">
              <Input v-model:value="formState.orderNo" :disabled="readOnly" placeholder="留空自动生成" />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.erpNo">
              <Input v-model:value="formState.erpNo" :disabled="readOnly" placeholder="请输入ERP编号" />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.customerName">
              <Input v-model:value="formState.customerName" :disabled="readOnly" />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.productionMaterial">
              <PickerInline
                :model-value="formState.materialCode"
                :config="materialPickerConfig"
                :disabled="readOnly"
                :placeholder="PLAN_PLACEHOLDER_COPY.productionMaterial"
                @update:model-value="handleMaterialInput"
                @search="materialPickerOpen = true"
                @pick="handleMaterialPick"
              />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.productModel">
              <Input :value="formState.modelCode || '-'" disabled />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.sizeSpec">
              <Select
                v-model:value="formState.sizeSpec"
                allow-clear
                :disabled="readOnly"
                :options="SIZE_SPEC_OPTIONS"
                option-filter-prop="label"
                :placeholder="PLAN_PLACEHOLDER_COPY.sizeSpec"
                @change="handleSizeSpecChange"
              />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.orderQty">
              <InputNumber v-model:value="formState.quantity" :disabled="readOnly" :min="0" class="w-full" />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.plannedQty">
              <InputNumber v-model:value="formState.plannedQty" :disabled="readOnly" :min="0" class="w-full" />
            </Form.Item>
            <Form.Item label="单位">
              <Input :value="[formState.unitCode || formState.unit, formState.unitName].filter(Boolean).join('/') || '-'" disabled />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.deliveryDate">
              <DatePicker
                v-model:value="formState.deliveryDate"
                :disabled="readOnly"
                format="YYYY/MM/DD"
                value-format="YYYY-MM-DD"
                class="w-full"
              />
            </Form.Item>
            <Form.Item :label="PLAN_FIELD_COPY.remark" class="col-span-2">
              <Input.TextArea v-model:value="formState.remark" :disabled="readOnly" :rows="3" />
            </Form.Item>
          </div>
        </Form>
      </fieldset>
      <fieldset class="mt-3 rounded-md border border-border px-4 pb-3 pt-3">
        <legend class="px-2 text-sm font-semibold text-foreground">审核信息</legend>
        <Form layout="horizontal" :label-col="{ style: { width: '86px' } }">
          <div class="grid grid-cols-3 gap-x-4">
            <Form.Item label="状态">
              <Input :value="formState.statusName || formState.status || '-'" disabled />
            </Form.Item>
            <Form.Item label="审核人">
              <Input :value="formState.auditorName || '-'" disabled />
            </Form.Item>
            <Form.Item label="审核时间">
              <Input :value="formatAuditTime(formState.auditTime)" disabled />
            </Form.Item>
          </div>
        </Form>
      </fieldset>
    </div>
    <template #footer>
      <Button @click="modalApi.close()">关闭</Button>
      <Button v-if="canDelete" danger @click="deleteCurrent">删除</Button>
      <Button v-if="formState.id && formState.status === 'DRAFT'" type="primary" @click="auditCurrent">审核</Button>
      <Button v-if="!readOnly" type="primary" @click="save">保存草稿</Button>
    </template>
  </Modal>
</template>
