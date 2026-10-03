<script lang="ts" setup>
import type { MesCostWagePerfApi } from '#/api/mes/cost/wage/performance';
import { computed, ref } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';
import { useVbenForm } from '#/adapter/form';
import { createPerfRecord, getPerfRecord, updatePerfRecord } from '#/api/mes/cost/wage/performance';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const isUpdate = ref(false);

const getTitle = computed(() => isUpdate.value ? '编辑奖惩记录' : '新增奖惩登记');

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
    const data = (await formApi.getValues()) as MesCostWagePerfApi.PerfRecord;

    try {
      if (isUpdate.value) {
        await updatePerfRecord(data);
      } else {
        await createPerfRecord(data);
      }
      await modalApi.close();
      emit('success');
      message.success('操作成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) return;
    const data = modalApi.getData<{ id?: number }>();

    isUpdate.value = !!data?.id;

    if (data?.id) {
      modalApi.lock();
      try {
        const res = await getPerfRecord(data.id) as any;
        await formApi.setValues(res);
      } finally {
        modalApi.unlock();
      }
    } else {
      await formApi.resetForm();
      const today = new Date().toISOString().slice(0, 10);
      await formApi.setValues({ recordDate: today });
    }
  },
});
</script>

<template>
  <Modal :title="getTitle" class="w-[700px]">
    <Form class="mx-4 mt-4" />
  </Modal>
</template>
