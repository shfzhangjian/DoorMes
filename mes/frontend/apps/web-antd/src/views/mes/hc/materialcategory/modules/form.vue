<script lang="ts" setup>
import type { MesHcMaterialCategoryApi } from '#/api/mes/hc/materialcategory';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createMaterialCategory, getMaterialCategoryDetail, updateMaterialCategory } from '#/api/mes/hc/materialcategory';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesHcMaterialCategoryApi.MaterialCategory>();

const getTitle = computed(() => (formData.value?.id ? '编辑物料分类' : '新增物料分类'));

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 110,
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
  class: 'w-[920px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as MesHcMaterialCategoryApi.MaterialCategory;
    data.status = 0;
    try {
      await (formData.value?.id ? updateMaterialCategory(data) : createMaterialCategory(data));
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

    const data = modalApi.getData<MesHcMaterialCategoryApi.MaterialCategory & { parentIdOverride?: number }>();
    if (!data?.id) {
      formData.value = undefined;
      await formApi.resetForm();
      await formApi.setValues({ status: 0 });
      if (data?.parentIdOverride !== undefined) {
        await formApi.setValues({ parentId: data.parentIdOverride, status: 0 });
      }
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getMaterialCategoryDetail(data.id);
      await formApi.setValues({ ...formData.value, status: 0 });
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


