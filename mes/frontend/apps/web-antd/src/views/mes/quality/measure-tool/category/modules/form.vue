<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createMeasureToolCategory,
  getMeasureToolCategory,
  getMeasureToolCategoryList,
  updateMeasureToolCategory,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<QmsMeasureToolApi.Category>();
const positionOptions = ref<Array<{ label?: string; value?: number }>>([]);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['量检设备位置/区域'])
    : $t('ui.actionTitle.create', ['量检设备位置/区域']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 100,
  },
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6',
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[720px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const values = (await formApi.getValues()) as QmsMeasureToolApi.Category;
    try {
      await (formData.value?.id
        ? updateMeasureToolCategory({ ...values, id: formData.value.id })
        : createMeasureToolCategory(values));
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

    const data = modalApi.getData<QmsMeasureToolApi.Category>();
    const categoryRows = await getMeasureToolCategoryList();
    positionOptions.value = categoryRows
      .filter(
        (item) =>
          item.status !== 0 &&
          (!item.parentId || item.parentId <= 0) &&
          item.id !== data?.id,
      )
      .map((item) => ({
        label: item.categoryName || item.categoryCode || String(item.id),
        value: item.id,
      }));
    await formApi.updateSchema(useFormSchema(positionOptions.value));
    await formApi.resetForm();

    if (!data?.id) {
      formData.value = undefined;
      await formApi.setValues({ parentId: data?.parentId || 0, status: 1 });
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getMeasureToolCategory(data.id);
      await formApi.setValues({
        ...formData.value,
        parentId: formData.value.parentId || 0,
      });
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
