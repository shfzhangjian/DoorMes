<script lang="ts" setup>
import type { baseMesMaterialApi } from '#/api/mes/base/material';

import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createMesMaterial, getMesMaterial, updateMesMaterial } from '#/api/mes/base/material';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<baseMesMaterialApi.MesMaterial>();
const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['MES物料主数据'])
    : $t('ui.actionTitle.create', ['MES物料主数据']);
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
  class: 'w-[850px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as baseMesMaterialApi.MesMaterial;

    // 🔥 核心适配逻辑：将 FileUpload 组件产生的文件数组，转换为后端需要的字符串
    if (data.drawingUrl && Array.isArray(data.drawingUrl)) {
      data.drawingUrl = data.drawingUrl.length > 0 ? data.drawingUrl[0] : '';
    }

    try {
      await (formData.value?.id ? updateMesMaterial(data) : createMesMaterial(data));
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) { formData.value = undefined; return; }

    const data = modalApi.getData<baseMesMaterialApi.MesMaterial>();
    if (!data || !data.id) return;

    modalApi.lock();
    try {
      formData.value = await getMesMaterial(data.id);

      // 🔥 核心适配逻辑：如果后端返回的是字符串链接，转换为 FileUpload 组件需要的数组格式进行回显
      const echoData = { ...formData.value };
      if (echoData.drawingUrl && typeof echoData.drawingUrl === 'string') {
        echoData.drawingUrl = [echoData.drawingUrl] as any;
      }

      await formApi.setValues(echoData);
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
