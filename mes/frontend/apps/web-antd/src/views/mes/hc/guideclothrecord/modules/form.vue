<script lang="ts" setup>
import type { MesHcGuideClothRecordApi } from '#/api/mes/hc/guideclothrecord';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createGuideClothRecord,
  getGuideClothRecord,
  updateGuideClothRecord,
} from '#/api/mes/hc/guideclothrecord';

import { LINE_OPTIONS, useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<MesHcGuideClothRecordApi.GuideClothRecord>();

const lineNameMap = Object.fromEntries(LINE_OPTIONS.map((item) => [item.value, item.label]));

const getTitle = computed(() => (formData.value?.id ? '编辑导布更换记录' : '新增导布更换记录'));

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 130,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

function setLineFieldsReadonly(readonly: boolean) {
  formApi.updateSchema([
    {
      fieldName: 'lineCode',
      componentProps: {
        options: LINE_OPTIONS,
        allowClear: false,
        disabled: readonly,
      },
    },
    {
      fieldName: 'lineName',
      componentProps: {
        placeholder: '请输入产线名',
        disabled: readonly,
      },
    },
  ]);
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[920px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcGuideClothRecordApi.GuideClothRecord;
    if (!values.lineName && values.lineCode) {
      values.lineName = lineNameMap[values.lineCode] || values.lineCode;
    }
    modalApi.lock();
    try {
      await (values.id ? updateGuideClothRecord(values) : createGuideClothRecord(values));
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
    const data = modalApi.getData<MesHcGuideClothRecordApi.GuideClothRecord>();
    if (!data?.id) {
      formData.value = undefined;
      setLineFieldsReadonly(false);
      await formApi.resetForm();
      await formApi.setValues({ currentFlag: 0, useCount: 1 });
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getGuideClothRecord(data.id);
      setLineFieldsReadonly(true);
      await formApi.setValues(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
  showCancelButton: false,
});

</script>

<template>
  <Modal :title="getTitle">
    <div class="px-2 pb-4">
      <Form />
    </div>
  </Modal>
</template>
