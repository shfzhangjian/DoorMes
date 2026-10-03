<script lang="ts" setup>
import type { MesCostCenterApi } from '#/api/mes/cost/base/cost-center';
import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createCostCenter, getCostCenter, updateCostCenter } from '#/api/mes/cost/base/cost-center';
import { $t } from '#/locales';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesCostCenterApi.CostCenter>();

const getTitle = computed(() => {
  return formData.value?.id ? '编辑成本中心' : '新增成本中心';
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 100,
  },
  layout: 'horizontal',
  wrapperClass: 'grid grid-cols-2 gap-4', // 使用两列布局
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as MesCostCenterApi.CostCenter;
    try {
      await (formData.value?.id ? updateCostCenter(data) : createCostCenter(data));
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
    const data = modalApi.getData<MesCostCenterApi.CostCenter>();
    if (!data || !data.id) {
      // 传递 parentId 的情况 (新增下级)
      await formApi.setValues(data);
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getCostCenter(data.id);
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[800px]">
    <Form class="mx-4 mt-4" />
  </Modal>
</template>
