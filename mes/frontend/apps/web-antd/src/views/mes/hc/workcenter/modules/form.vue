<script lang="ts" setup>
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createWorkCenter, getWorkCenterDetail, updateWorkCenter } from '#/api/mes/hc/workcenter';
import { PickerInline, PickerModal, processPickerConfig } from '#/components/picker';
import type { PickerOption } from '#/components/picker';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesHcWorkCenterApi.WorkCenter>();
const processText = ref('');
const processPickerOpen = ref(false);

const getTitle = computed(() => {
  return formData.value?.id ? '编辑工作中心' : '新增工作中心';
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
    const data = (await formApi.getValues()) as MesHcWorkCenterApi.WorkCenter;
    if (!data.processId) {
      modalApi.unlock();
      return message.warning('请选择工序');
    }
    try {
      await (formData.value?.id ? updateWorkCenter(data) : createWorkCenter(data));
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
      processText.value = '';
      processPickerOpen.value = false;
      await formApi.resetForm();
      return;
    }

    const data = modalApi.getData<MesHcWorkCenterApi.WorkCenter>();
    if (!data?.id) {
      formData.value = undefined;
      processText.value = '';
      await formApi.resetForm();
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getWorkCenterDetail(data.id);
      await formApi.setValues(formData.value);
      processText.value = formData.value.processName || formData.value.processStage || formData.value.processCode || '';
    } finally {
      modalApi.unlock();
    }
  },
});

function handleProcessInput(value: string) {
  const nextValue = String(value || '');
  processText.value = nextValue;
  void formApi.setValues({
    processId: undefined,
    processCode: nextValue,
    processName: nextValue,
    processStage: nextValue,
  });
}

async function setProcess(option: PickerOption) {
  processText.value = option.name || option.code || '';
  await formApi.setValues({
    processId: Number(option.id),
    processCode: option.code || '',
    processName: option.name || '',
    processStage: option.name || '',
  });
}

async function handleProcessPickerSelect(option: PickerOption) {
  await setProcess(option);
  processPickerOpen.value = false;
}
</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-4">
      <Form>
        <template #processCode>
          <PickerInline
            :model-value="processText"
            :config="processPickerConfig"
            placeholder="请输入工序名称或点击搜索选择"
            @update:model-value="handleProcessInput"
            @search="processPickerOpen = true"
            @pick="setProcess"
          />
        </template>
      </Form>
    </div>

  </Modal>

  <PickerModal
    :config="processPickerConfig"
    :open="processPickerOpen"
    title="选择标准工序"
    @close="processPickerOpen = false"
    @pick="handleProcessPickerSelect"
  />
</template>

