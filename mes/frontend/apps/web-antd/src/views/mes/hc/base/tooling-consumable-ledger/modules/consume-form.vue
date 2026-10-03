<script lang="ts" setup>
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import {
  createToolingConsumableConsume,
  getToolingConsumableConsume,
  updateToolingConsumableConsume,
} from '#/api/mes/hc/tooling-consumable-ledger';

import {
  consumableTypeOptionsByProcess,
  defaultConsumableForProcess,
  defaultProcessForConsumable,
  isAdhesiveProcess,
  isProcessAllowed,
  normalizeProcessCode,
  processText,
  useConsumeFormSchema,
} from '../data';

type ConsumeModalData = MesHcToolingConsumableLedgerApi.Consume & {
  fixedProcessCode?: string;
};

const emit = defineEmits(['success']);
const formData = ref<MesHcToolingConsumableLedgerApi.Consume>();
const fixedProcessCode = ref<string>();

const getTitle = computed(() => {
  const rAndDSample = isAdhesiveProcess(
    fixedProcessCode.value || formData.value?.processCode,
  );
  if (rAndDSample) {
    return formData.value?.id ? '编辑研发样品消耗' : '新增研发样品消耗';
  }
  return formData.value?.id ? '编辑消耗明细' : '新增消耗明细';
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 130,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useConsumeFormSchema(),
  showDefaultActions: false,
});

function defaultValues(data?: Partial<ConsumeModalData>) {
  const processCode = data?.fixedProcessCode || data?.processCode;
  const consumableType = data?.consumableType || defaultConsumableForProcess(processCode) || 'SANDPAPER';
  return {
    batchNo: data?.batchNo || '',
    consumeQty: data?.consumeQty ?? 0,
    consumeSource: data?.consumeSource,
    consumeType: data?.consumeType || 'NORMAL',
    consumeTime: data?.consumeTime || dayjs().format('YYYY-MM-DD HH:mm:ss'),
    consumableType,
    id: data?.id,
    ledgerId: data?.ledgerId,
    glueBoardStockId: data?.glueBoardStockId,
    glueBoardUsageId: data?.glueBoardUsageId,
    model: data?.model || '',
    planNo: data?.planNo || '',
    planOperationId: data?.planOperationId,
    processCode: processCode || defaultProcessForConsumable(consumableType),
    productBatchNo: data?.productBatchNo || '',
    productInputQty: data?.productInputQty,
    productMaterialCode: data?.productMaterialCode || '',
    productModelCode: data?.productModelCode || '',
    productOutputQty: data?.productOutputQty,
    productionBatchNo: data?.productionBatchNo || '',
    remark: data?.remark || '',
  };
}

function applyProcessSchema(processCode?: string) {
  const adhesive2 = normalizeProcessCode(processCode) === 'ADHESIVE2';
  formApi.updateSchema([
    {
      fieldName: 'consumableType',
      componentProps: {
        allowClear: false,
        disabled: true,
        options: consumableTypeOptionsByProcess(processCode),
      },
    },
    {
      fieldName: 'productInputQty',
      label: adhesive2 ? '粘胶2投入(pcs)' : '投入米数(m)',
      componentProps: adhesive2
        ? { min: 1, precision: 0, class: 'w-full' }
        : { min: 0.001, precision: 3, class: 'w-full' },
    },
    {
      fieldName: 'productOutputQty',
      label: adhesive2 ? '粘胶2产出(pcs)' : '产出米数(m)',
      componentProps: adhesive2
        ? { min: 1, precision: 0, class: 'w-full' }
        : { min: 0.001, precision: 3, class: 'w-full' },
    },
  ]);
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[980px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcToolingConsumableLedgerApi.Consume;
    if (fixedProcessCode.value) {
      values.processCode = fixedProcessCode.value;
    }
    if (!isProcessAllowed(values.consumableType, values.processCode)) {
      message.warning(`当前耗材种类不能选择 ${processText(values.processCode)} 工序`);
      return;
    }
    const normalizedProcessCode = normalizeProcessCode(values.processCode);
    if (normalizedProcessCode === 'ADHESIVE1' || normalizedProcessCode === 'ADHESIVE2') {
      const inputLabel = normalizedProcessCode === 'ADHESIVE2' ? '粘胶2投入(pcs)' : '投入米数(m)';
      const outputLabel = normalizedProcessCode === 'ADHESIVE2' ? '粘胶2产出(pcs)' : '产出米数(m)';
      if (!Number.isFinite(Number(values.productInputQty)) || Number(values.productInputQty) <= 0) {
        message.warning(`${inputLabel}必须大于0`);
        return;
      }
      if (!Number.isFinite(Number(values.productOutputQty)) || Number(values.productOutputQty) <= 0) {
        message.warning(`${outputLabel}必须大于0`);
        return;
      }
    }
    modalApi.lock();
    try {
      await (values.id ? updateToolingConsumableConsume(values) : createToolingConsumableConsume(values));
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
      fixedProcessCode.value = undefined;
      await formApi.resetForm();
      return;
    }
    const data = modalApi.getData<ConsumeModalData>();
    fixedProcessCode.value = data?.fixedProcessCode;
    applyProcessSchema(fixedProcessCode.value);
    if (!data?.id) {
      formData.value = undefined;
      await formApi.resetForm();
      await formApi.setValues(defaultValues(data || {}));
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getToolingConsumableConsume(data.id);
      applyProcessSchema(fixedProcessCode.value || formData.value?.processCode);
      await formApi.resetForm();
      await formApi.setValues(defaultValues({ ...formData.value, fixedProcessCode: fixedProcessCode.value }));
    } finally {
      modalApi.unlock();
    }
  },
  showCancelButton: false,
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-4">
      <Form />
    </div>
  </Modal>
</template>
