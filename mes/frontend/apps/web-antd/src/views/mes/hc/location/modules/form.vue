<script lang="ts" setup>
import type { MesHcLocationApi } from '#/api/mes/hc/location';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createLocation, getLocationDetail, updateLocation } from '#/api/mes/hc/location';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);

// row id（有值=编辑模式，无值=新增模式）
const formId = ref<number | undefined>();

const getTitle = computed(() => (formId.value ? '编辑库位' : '新增库位'));

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
    const data = (await formApi.getValues()) as MesHcLocationApi.Location;
    try {
      if (formId.value) {
        await updateLocation({ ...data, id: formId.value });
      } else {
        await createLocation(data);
      }
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },

  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formId.value = undefined;
      await formApi.resetForm();
      return;
    }

    const data = modalApi.getData<{
      id?: number;
      warehouseCode?: string;
      warehouseName?: string;
    }>();

    await formApi.resetForm();

    if (data?.id) {
      // ── 编辑模式：从后端拉取完整数据 ──────────────────────────────────────
      formId.value = data.id;
      modalApi.lock();
      try {
        const detail = await getLocationDetail(data.id);
        await formApi.setValues(detail);
      } finally {
        modalApi.unlock();
      }
    } else {
      // ── 新增模式：若从仓库节点触发则预填仓库信息 ───────────────────────────
      formId.value = undefined;
      if (data?.warehouseCode) {
        await formApi.setValues({
          warehouseCode: data.warehouseCode,
          warehouseName: data.warehouseName ?? '',
        });
      }
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
