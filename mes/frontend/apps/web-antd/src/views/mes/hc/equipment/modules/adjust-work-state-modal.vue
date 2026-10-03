<script lang="ts" setup>
import type { MesHcEquipmentApi } from '#/api/mes/hc/equipment';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { adjustEquipmentWorkState } from '#/api/mes/hc/equipment';

const emit = defineEmits(['success']);
const formData = ref<MesHcEquipmentApi.Equipment>();

const getTitle = computed(() => `调整设备状态${formData.value?.equipmentCode ? ` - ${formData.value.equipmentCode}` : ''}`);

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-1',
  layout: 'horizontal',
  schema: [
    {
      fieldName: 'workStatus',
      label: '运行状态',
      component: 'RadioGroup',
      rules: 'required',
      componentProps: {
        options: [
          { label: '待机', value: 'IDLE' },
          { label: '生产中', value: 'PRODUCING' },
          { label: '检修', value: 'MAINTENANCE' },
          { label: '故障', value: 'FAULT' },
        ],
        optionType: 'button',
        buttonStyle: 'solid',
      },
    },
    {
      fieldName: 'clearOperationBinding',
      label: '解除工序挂接',
      component: 'Checkbox',
      defaultValue: false,
      componentProps: {
        children: '同时清空当前计划号、工序编号、工序名称、开工/完工时间',
      },
    },
    {
      fieldName: 'remark',
      label: '调整说明',
      component: 'Textarea',
      componentProps: {
        rows: 3,
        placeholder: '请输入调整说明（选填）',
      },
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[640px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid || !formData.value?.id) {
      return;
    }
    const values = await formApi.getValues();
    modalApi.lock();
    try {
      await adjustEquipmentWorkState({
        id: formData.value.id,
        workStatus: values.workStatus,
        clearOperationBinding: values.clearOperationBinding,
        remark: values.remark,
      });
      await modalApi.close();
      emit('success');
      message.success('调整成功');
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
    formData.value = modalApi.getData<MesHcEquipmentApi.Equipment>();
    await formApi.setValues({
      workStatus: formData.value?.workStatus || 'IDLE',
      clearOperationBinding: false,
      remark: '',
    });
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-4">
      <div class="mb-4 rounded border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-700">
        <div>当前设备：{{ formData?.equipmentCode || '-' }} / {{ formData?.equipmentName || '-' }}</div>
        <div class="mt-1">当前挂接：{{ formData?.currentPlanNo || '-' }} / {{ formData?.currentOperationName || '-' }}</div>
      </div>
      <Form />
    </div>
  </Modal>
</template>
