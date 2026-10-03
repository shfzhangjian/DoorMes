<script lang="ts" setup>
import type { MesHcEquipmentConsumableApi } from '#/api/mes/hc/equipment-consumable';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';
import dayjs from 'dayjs';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  adjustEquipmentConsumable,
  getEquipmentConsumableState,
} from '#/api/mes/hc/equipment-consumable';

import { MES_DATETIME_FORMAT, normalizeMesDateTime } from '../../shared/date-time';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const userStore = useUserStore();
const formData = ref<MesHcEquipmentConsumableApi.State>();

const currentUserId = computed(() => userStore.userInfo?.id as number | undefined);
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '');
const getTitle = computed(() => (formData.value?.id ? '更换/调整砂纸导布' : '登记砂纸导布'));

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

const defaultValues = (data?: Partial<MesHcEquipmentConsumableApi.State>) => ({
  batchNo: data?.batchNo || '',
  consumableType: data?.consumableType || 'SANDPAPER',
  equipmentId: data?.equipmentId,
  eventTime: dayjs().format(MES_DATETIME_FORMAT),
  eventType: data?.id ? 'ADJUST' : 'REPLACE',
  id: data?.id,
  lastReplacePlanNo: data?.lastReplacePlanNo || '',
  limitCount: data?.limitCount ?? (data?.consumableType === 'GUIDE_CLOTH' ? undefined : 150),
  limitLength: data?.limitLength ?? 300,
  operatorId: currentUserId.value,
  operatorName: currentUserName.value,
  processCode: data?.processCode || 'ROUGH_GRINDING',
  processName: data?.processName || '磨皮',
  replaceReason: '',
  status: data?.status || 'IN_USE',
  useCount: data?.useCount ?? 0,
  usedLength: data?.usedLength ?? 0,
});

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[980px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcEquipmentConsumableApi.AdjustReq;
    if (!values.replaceReason) {
      message.warning('请填写更换/调整原因');
      return;
    }
    if (values.eventType === 'REPLACE') {
      // 更换的起点固定归零，避免编辑旧台账时把历史累计值带入新耗材。
      values.useCount = 0;
      values.usedLength = 0;
    }
    values.eventTime = normalizeMesDateTime(values.eventTime) || dayjs().format(MES_DATETIME_FORMAT);
    modalApi.lock();
    try {
      await adjustEquipmentConsumable(values);
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
    const data = modalApi.getData<MesHcEquipmentConsumableApi.State>();
    if (!data?.id) {
      formData.value = undefined;
      await formApi.resetForm();
      await formApi.setValues(defaultValues(data || {}));
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getEquipmentConsumableState(data.id);
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
