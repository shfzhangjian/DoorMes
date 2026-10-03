<script lang="ts" setup>
import type { MesSupplierApi } from '#/api/mes/supplier';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createSupplier,
  getSupplier,
  updateSupplier,
} from '#/api/mes/supplier';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);

const formData = ref<MesSupplierApi.Supplier>();

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['供应商'])
    : $t('ui.actionTitle.create', ['供应商']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 110,
  },
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6',
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[820px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const values = (await formApi.getValues()) as MesSupplierApi.Supplier;
    try {
      if (formData.value?.id) {
        await updateSupplier({
          ...values,
          id: formData.value.id,
          version: formData.value.version,
        });
      } else {
        await createSupplier(values);
      }
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }

    const data = modalApi.getData<MesSupplierApi.Supplier & { __hiddenFields?: string[] }>();
    const hiddenFields = data?.__hiddenFields || [];
    await formApi.updateSchema(useFormSchema(hiddenFields));
    await formApi.resetForm();

    if (!data?.id) {
      formData.value = undefined;
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getSupplier(data.id);
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
