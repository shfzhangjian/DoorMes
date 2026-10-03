<script lang="ts" setup>
import type { MesHcOwnerApi } from '#/api/mes/hc/owner';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createOwner, getOwnerDetail, updateOwner } from '#/api/mes/hc/owner';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesHcOwnerApi.Owner>();

const getTitle = computed(() => {
  return formData.value?.id ? '编辑货主' : '新增货主';
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
    const data = (await formApi.getValues()) as MesHcOwnerApi.Owner;
    try {
      await (formData.value?.id ? updateOwner(data) : createOwner(data));
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

    const data = modalApi.getData<MesHcOwnerApi.Owner>();
    if (!data?.id) {
      formData.value = undefined;
      await formApi.resetForm();
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getOwnerDetail(data.id);
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


