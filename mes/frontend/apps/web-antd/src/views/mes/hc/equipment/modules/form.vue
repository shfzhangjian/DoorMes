<script lang="ts" setup>
import type { MesHcEquipmentApi } from '#/api/mes/hc/equipment';
import type { MesHcWorkCenterApi } from '#/api/mes/hc/workcenter';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { createEquipment, getEquipmentDetail, updateEquipment } from '#/api/mes/hc/equipment';
import { getWorkCenterDetail } from '#/api/mes/hc/workcenter';

import { applicablePadTypeOptions, useFormSchema } from '../data';
import WorkCenterInlinePicker from '../../route/modules/workcenter-inline-picker.vue';
import WorkCenterPickerModal from '../../route/modules/workcenter-picker-modal.vue';

const emit = defineEmits(['success']);
const formData = ref<MesHcEquipmentApi.Equipment>();
const workCenterPickerState = reactive({
  code: '',
  name: '',
});

const getTitle = computed(() => {
  return formData.value?.id ? '编辑设备台账' : '新增设备台账';
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

const [WorkCenterPickerModalComp, workCenterPickerModalApi] = useVbenModal({
  connectedComponent: WorkCenterPickerModal,
  destroyOnClose: true,
});

function clearWorkCenterSelection() {
  workCenterPickerState.code = '';
  workCenterPickerState.name = '';
  formApi.setValues({ workCenterId: undefined });
}

function applyWorkCenter(row: MesHcWorkCenterApi.WorkCenter) {
  workCenterPickerState.code = row.wcCode || '';
  workCenterPickerState.name = row.wcName || '';
  formApi.setValues({ workCenterId: row.id });
}

function handleWorkCenterInput(mode: 'code' | 'name', value: string) {
  if (mode === 'code') {
    workCenterPickerState.code = value;
  } else {
    workCenterPickerState.name = value;
  }
  formApi.setValues({ workCenterId: undefined });
}

function openWorkCenterPicker(source: 'code' | 'name') {
  workCenterPickerModalApi
    .setData({
      keyword: source === 'code' ? workCenterPickerState.code : workCenterPickerState.name,
      source,
    })
    .open();
}

async function loadWorkCenterDisplay(detail?: MesHcEquipmentApi.Equipment) {
  if (!detail?.workCenterId) {
    clearWorkCenterSelection();
    return;
  }
  if (detail.workCenterCode || detail.workCenterName) {
    workCenterPickerState.code = detail.workCenterCode || '';
    workCenterPickerState.name = detail.workCenterName || '';
    return;
  }
  const workCenter = await getWorkCenterDetail(detail.workCenterId);
  workCenterPickerState.code = workCenter?.wcCode || '';
  workCenterPickerState.name = workCenter?.wcName || '';
}

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  class: 'w-[900px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = (await formApi.getValues()) as MesHcEquipmentApi.Equipment;
    if (!values.workCenterId) {
      message.warning('请选择所属工作中心');
      return;
    }
    values.applicablePadTypeName =
      applicablePadTypeOptions.find((item) => item.value === values.applicablePadType)?.label || '';

    modalApi.lock();
    try {
      await (formData.value?.id ? updateEquipment(values) : createEquipment(values));
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
      clearWorkCenterSelection();
      await formApi.resetForm();
      return;
    }

    const data = modalApi.getData<MesHcEquipmentApi.Equipment>();
    if (!data?.id) {
      formData.value = undefined;
      clearWorkCenterSelection();
      await formApi.resetForm();
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getEquipmentDetail(data.id);
      await formApi.setValues(formData.value);
      await loadWorkCenterDisplay(formData.value);
    } finally {
      modalApi.unlock();
    }
  },
});
</script>

<template>
  <Modal :title="getTitle">
    <WorkCenterPickerModalComp @success="applyWorkCenter" />
    <div class="px-2 pb-4">
      <div class="mb-4 grid grid-cols-2 gap-x-6 gap-y-4">
        <div class="equipment-work-center-field">
          <div class="equipment-work-center-field__label">
            <span class="text-red-500">*</span>
            所属工作中心编码
          </div>
          <div class="equipment-work-center-field__content">
            <WorkCenterInlinePicker
              :model-value="workCenterPickerState.code"
              mode="code"
              placeholder="请输入工作中心编码或快速选择"
              @update:model-value="(value) => handleWorkCenterInput('code', value)"
              @pick="applyWorkCenter"
              @search="openWorkCenterPicker('code')"
            />
          </div>
        </div>
        <div class="equipment-work-center-field">
          <div class="equipment-work-center-field__label">
            <span class="text-red-500">*</span>
            所属工作中心名称
          </div>
          <div class="equipment-work-center-field__content">
            <WorkCenterInlinePicker
              :model-value="workCenterPickerState.name"
              mode="name"
              placeholder="请输入工作中心名称或快速选择"
              @update:model-value="(value) => handleWorkCenterInput('name', value)"
              @pick="applyWorkCenter"
              @search="openWorkCenterPicker('name')"
            />
          </div>
        </div>
      </div>
      <Form />
    </div>
  </Modal>
</template>

<style scoped>
.equipment-work-center-field {
  display: flex;
  align-items: center;
  gap: 12px;
}

.equipment-work-center-field__label {
  width: 120px;
  flex-shrink: 0;
  text-align: right;
  color: rgb(0 0 0 / 88%);
}

.equipment-work-center-field__content {
  flex: 1;
}
</style>
