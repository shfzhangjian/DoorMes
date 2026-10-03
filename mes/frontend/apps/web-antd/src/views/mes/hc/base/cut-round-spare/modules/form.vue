<script lang="ts" setup>
import type { MesHcCutRoundSpareApi } from '#/api/mes/hc/cut-round-spare';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import {
  getCutRoundSpare,
  saveCutRoundSpare,
} from '#/api/mes/hc/cut-round-spare';

import { MES_DATETIME_FORMAT, normalizeMesDateTime } from '../../shared/date-time';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const userStore = useUserStore();
const formData = ref<MesHcCutRoundSpareApi.Spare>();

const currentUserId = computed(() => userStore.userInfo?.id as number | undefined);
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '');
const getTitle = computed(() => (formData.value?.id ? '维护裁切备件' : '登记裁切备件'));

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

const defaultValues = (data?: Partial<MesHcCutRoundSpareApi.Spare>) => {
  const spareType = data?.spareType || 'CUTTING_BLADE';
  return {
    correctHistory: false,
    equipmentId: data?.equipmentId,
    id: data?.id,
    lastReplaceTime: normalizeMesDateTime(data?.lastReplaceTime) || dayjs().format(MES_DATETIME_FORMAT),
    limitCount: data?.limitCount ?? (spareType === 'CUTTING_BLADE' ? 50 : 2000),
    limitDays: data?.limitDays ?? (spareType === 'CUTTING_BLADE' ? 0 : 90),
    operatorId: currentUserId.value,
    operatorName: currentUserName.value,
    remark: data?.remark || '',
    replaceReason: '',
    spareType,
    status: data?.status || 'ACTIVE',
    useCount: data?.useCount ?? 0,
  };
};

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[980px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcCutRoundSpareApi.SaveReq;
    values.lastReplaceTime = normalizeMesDateTime(values.lastReplaceTime) || dayjs().format(MES_DATETIME_FORMAT);
    modalApi.lock();
    try {
      await saveCutRoundSpare(values);
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
    const data = modalApi.getData<MesHcCutRoundSpareApi.Spare>();
    if (!data?.id) {
      formData.value = undefined;
      await formApi.resetForm();
      await formApi.setValues(defaultValues(data || {}));
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getCutRoundSpare(data.id);
      await formApi.resetForm();
      await formApi.setValues(defaultValues(formData.value));
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
