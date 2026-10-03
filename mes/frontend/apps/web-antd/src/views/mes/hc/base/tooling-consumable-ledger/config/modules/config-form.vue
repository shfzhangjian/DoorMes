<script lang="ts" setup>
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createToolingProcessConsumable,
  getToolingProcessConsumable,
  updateToolingProcessConsumable,
} from '#/api/mes/hc/tooling-consumable-ledger';

import { useConfigFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesHcToolingConsumableLedgerApi.ProcessConsumable>();
const getTitle = computed(() => (formData.value?.id ? '编辑工序耗材配置' : '新增工序耗材配置'));

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useConfigFormSchema(),
  showDefaultActions: false,
});

async function resetForm() {
  formData.value = undefined;
  await formApi.resetForm();
  await formApi.setValues({ status: 0 });
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[820px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcToolingConsumableLedgerApi.ProcessConsumable;
    modalApi.lock();
    try {
      await (values.id ? updateToolingProcessConsumable(values) : createToolingProcessConsumable(values));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      await resetForm();
      return;
    }
    const data = modalApi.getData<MesHcToolingConsumableLedgerApi.ProcessConsumable>();
    if (!data?.id) {
      await resetForm();
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getToolingProcessConsumable(data.id);
      await formApi.resetForm();
      await formApi.setValues(formData.value);
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
