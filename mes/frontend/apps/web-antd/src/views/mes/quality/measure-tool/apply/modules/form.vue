<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createMeasureToolApply,
  getMeasureToolApply,
  getMeasureToolCategoryList,
  updateMeasureToolApply,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import { useFormSchema } from '../data';
import { loadCategoryOptions } from '../../shared';

const emit = defineEmits(['success']);
const formData = ref<QmsMeasureToolApi.Apply>();

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['量检具新增申请'])
    : $t('ui.actionTitle.create', ['量检具新增申请']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 120 },
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6',
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[920px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const values = (await formApi.getValues()) as QmsMeasureToolApi.Apply;
    try {
      if (formData.value?.id) {
        await updateMeasureToolApply({ ...values, id: formData.value.id, version: formData.value.version });
      } else {
        await createMeasureToolApply(values);
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

    const data = modalApi.getData<QmsMeasureToolApi.Apply>();
    await formApi.updateSchema(useFormSchema(await loadCategoryOptions(getMeasureToolCategoryList)));
    await formApi.resetForm();

    if (!data?.id) {
      formData.value = undefined;
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getMeasureToolApply(data.id);
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
