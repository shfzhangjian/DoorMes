<script lang="ts" setup>
import type { MesCostOverheadApi } from '#/api/mes/cost/allocation/overhead';
import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createCostOverhead, getCostOverhead, updateCostOverhead } from '#/api/mes/cost/allocation/overhead';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesCostOverheadApi.Overhead>();

const getTitle = computed(() => formData.value?.id ? '编辑制费归集单' : '新增手工归集单');

const [Form, formApi] = useVbenForm({
  commonConfig: { formItemClass: 'col-span-1', labelWidth: 100 },
  layout: 'horizontal',
  wrapperClass: 'grid grid-cols-2 gap-4',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    modalApi.lock();
    const data = (await formApi.getValues()) as MesCostOverheadApi.Overhead;
    data.source = 1; // 前端表单录入的一律标记为手工录入
    data.status = 0; // 新增/编辑默认为草稿状态

    try {
      await (formData.value?.id ? updateCostOverhead(data) : createCostOverhead(data));
      await modalApi.close();
      emit('success');
      message.success('操作成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) { formData.value = undefined; return; }
    const data = modalApi.getData<{ id?: number }>();
    if (!data?.id) {
      // 默认带出当月期间
      const currentMonth = new Date().toISOString().slice(0, 7);
      await formApi.setValues({ period: currentMonth });
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getCostOverhead(data.id) as any;
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[700px]">
    <Form class="mx-4 mt-4" />
  </Modal>
</template>
