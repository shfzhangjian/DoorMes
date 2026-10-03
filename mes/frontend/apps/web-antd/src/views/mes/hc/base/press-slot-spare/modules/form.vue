<script lang="ts" setup>
import type { MesHcPressSlotSpareApi } from '#/api/mes/hc/press-slot-spare';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';
import { DatePicker, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import {
  getPressSlotSpare,
  savePressSlotSpare,
} from '#/api/mes/hc/press-slot-spare';

import { MES_DATETIME_FORMAT, normalizeMesDateTime } from '../../shared/date-time';
import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const userStore = useUserStore();
const formData = ref<MesHcPressSlotSpareApi.Spare>();
const lastReplaceTimeValue = ref('');

const currentUserId = computed(() => userStore.userInfo?.id as number | undefined);
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '');
const getTitle = computed(() => (formData.value?.id ? '维护压槽备件' : '登记压槽备件'));

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

const defaultValues = (data?: Partial<MesHcPressSlotSpareApi.Spare>) => {
  const spareType = data?.spareType || 'PRESS_ROLLER';
  return {
    availableQuantity: data?.availableQuantity ?? (spareType === 'PRESS_ROLLER' ? 1 : 0),
    batchNo: data?.batchNo || '',
    equipmentId: data?.equipmentId,
    id: data?.id,
    lastCleanRemark: data?.lastCleanRemark || '',
    limitCount: data?.limitCount ?? (spareType === 'PRESS_ROLLER' ? 2000 : 5000),
    limitDays: data?.limitDays ?? (spareType === 'PRESS_ROLLER' ? 60 : 150),
    materialCode: data?.materialCode || '',
    materialName: data?.materialName || '',
    onlineQuantity: data?.onlineQuantity ?? 1,
    operatorId: currentUserId.value,
    operatorName: currentUserName.value,
    remark: data?.remark || '',
    replaceReason: '',
    spareType,
    status: data?.status || 'ACTIVE',
    useCount: data?.useCount ?? 0,
  };
};

const resetDateValues = (data?: Partial<MesHcPressSlotSpareApi.Spare>) => {
  lastReplaceTimeValue.value = normalizeMesDateTime(data?.lastReplaceTime) || dayjs().format(MES_DATETIME_FORMAT);
};

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[980px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcPressSlotSpareApi.SaveReq;
    if (values.spareType === 'BEARING' && !values.materialCode) {
      message.warning('轴承必须填写料号');
      return;
    }
    values.lastReplaceTime = normalizeMesDateTime(lastReplaceTimeValue.value) || dayjs().format(MES_DATETIME_FORMAT);
    modalApi.lock();
    try {
      await savePressSlotSpare(values);
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
      resetDateValues();
      await formApi.resetForm();
      return;
    }
    const data = modalApi.getData<MesHcPressSlotSpareApi.Spare>();
    if (!data?.id) {
      formData.value = undefined;
      resetDateValues(data || {});
      await formApi.resetForm();
      await formApi.setValues(defaultValues(data || {}));
      return;
    }
    modalApi.lock();
    try {
      formData.value = await getPressSlotSpare(data.id);
      resetDateValues(formData.value);
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
      <div class="press-slot-spare-time-grid">
        <div class="press-slot-spare-time-item">
          <span class="press-slot-spare-time-label">上次更换时间</span>
          <DatePicker
            v-model:value="lastReplaceTimeValue"
            class="w-full"
            show-time
            :value-format="MES_DATETIME_FORMAT"
          />
        </div>
      </div>
      <Form />
    </div>
  </Modal>
</template>

<style scoped>
.press-slot-spare-time-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  column-gap: 24px;
  margin-bottom: 16px;
}

.press-slot-spare-time-item {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr);
  align-items: center;
}

.press-slot-spare-time-label {
  padding-right: 12px;
  color: hsl(var(--foreground));
  font-size: 14px;
  text-align: right;
}
</style>
