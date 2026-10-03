<script lang="ts" setup>
import type { MesHcTerminalApi } from '#/api/mes/hc/terminal';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createTerminal, getTerminalDetail, updateTerminal } from '#/api/mes/hc/terminal';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesHcTerminalApi.Terminal>();

const getTitle = computed(() => {
  return formData.value?.id ? '缂栬緫缁堢宸ヤ綅' : '鏂板缁堢宸ヤ綅';
});

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

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[900px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as MesHcTerminalApi.Terminal;
    try {
      await (formData.value?.id ? updateTerminal(data) : createTerminal(data));
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
      await formApi.resetForm();
      return;
    }

    const data = modalApi.getData<MesHcTerminalApi.Terminal>();
    if (!data?.id) {
      formData.value = undefined;
      await formApi.resetForm();
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getTerminalDetail(data.id);
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-4">
      <Form />
    </div>

  </Modal>
</template>


