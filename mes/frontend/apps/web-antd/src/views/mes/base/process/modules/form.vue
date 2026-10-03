<script lang="ts" setup>
import type { MesProcessApi } from '#/api/mes/base/process';

import { computed, nextTick, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createProcess,
  getProcess,
  updateProcess,
} from '#/api/mes/base/process';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesProcessApi.Process>();

const getTitle = computed(() => {
  return formData.value?.id
    ? $t('ui.actionTitle.edit', ['MES标准工序'])
    : $t('ui.actionTitle.create', ['MES标准工序']);
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 88,
  },
  wrapperClass: 'grid-cols-1 gap-y-4 md:grid-cols-2 md:gap-x-6',
  layout: 'horizontal',
  schema: [],
  showDefaultActions: false,
});

formApi.setState({
  schema: useFormSchema(),
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[720px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    modalApi.lock();
    const data = (await formApi.getValues()) as MesProcessApi.Process;

    try {
      await (formData.value?.id ? updateProcess(data) : createProcess(data));
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
    const data = modalApi.getData<MesProcessApi.Process>();

    formApi.setState({
      schema: useFormSchema(),
    });

    await nextTick();

    if (data?.id) {
      formData.value = data;
      modalApi.lock();
      try {
        const res = await getProcess(data.id);
        formData.value = res;
        await formApi.setValues(res);
      } finally {
        modalApi.unlock();
      }
    } else {
      await formApi.resetForm();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="process-form-panel">
      <div class="process-form-panel__header">
        <div>
          <div class="process-form-panel__eyebrow">MES PROCESS</div>
          <div class="process-form-panel__title">
            {{ formData?.id ? '编辑工序基础信息' : '新增工序基础信息' }}
          </div>
        </div>
        <div class="process-form-panel__badge">
          {{ formData?.id ? '编辑' : '新增' }}
        </div>
      </div>
      <div class="process-form-panel__body">
        <Form />
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.process-form-panel {
  overflow: hidden;
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  background: hsl(var(--background));
}

.process-form-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid hsl(var(--border));
  background: linear-gradient(
    180deg,
    hsl(var(--muted) / 0.55),
    hsl(var(--background))
  );
}

.process-form-panel__eyebrow {
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  line-height: 18px;
}

.process-form-panel__title {
  margin-top: 2px;
  color: hsl(var(--foreground));
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.process-form-panel__badge {
  flex-shrink: 0;
  min-width: 48px;
  padding: 4px 10px;
  border: 1px solid hsl(var(--primary) / 0.22);
  border-radius: 999px;
  background: hsl(var(--primary) / 0.08);
  color: hsl(var(--primary));
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
}

.process-form-panel__body {
  padding: 20px 20px 8px;
}

:deep(.ant-form-item) {
  margin-bottom: 14px;
}

:deep(.ant-input) {
  min-height: 36px;
}

@media (max-width: 768px) {
  .process-form-panel__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .process-form-panel__body {
    padding: 16px 16px 4px;
  }
}
</style>
