<script lang="ts" setup>
import type { MesHcCutRoundSpareApi } from '#/api/mes/hc/cut-round-spare';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Input, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import { saveCutRoundSpareRndConsume } from '#/api/mes/hc/cut-round-spare';
import { PAD_TYPE_OPTIONS } from '../../pad-type-options';

import {
  MES_DATETIME_FORMAT,
  normalizeMesDateTime,
} from '../../shared/date-time';

const emit = defineEmits(['success']);
const spare = ref<MesHcCutRoundSpareApi.Spare>();
const remarkValue = ref('');

const title = computed(() => '研发样品裁切备件消耗登记');
const equipmentText = computed(() => {
  const value = spare.value;
  return (
    [value?.equipmentCode, value?.equipmentName].filter(Boolean).join(' - ') ||
    '-'
  );
});

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 130,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: [
    {
      fieldName: 'equipmentId',
      component: 'Input',
      dependencies: { triggerFields: [''], show: () => false },
    },
    {
      fieldName: 'padType',
      label: '垫型',
      component: 'Select',
      componentProps: {
        options: PAD_TYPE_OPTIONS.filter((item) => item.value !== 'COMMON'),
        placeholder: '请选择黑垫或白垫',
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'modelCode',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '请输入研发产品型号' },
      rules: 'required',
    },
    {
      fieldName: 'productionBatchNo',
      label: '产品批次',
      component: 'Input',
      componentProps: { placeholder: '请输入本次研发产品批次' },
      rules: 'required',
    },
    {
      fieldName: 'cutSizeMm',
      label: '裁切尺寸',
      component: 'Select',
      componentProps: {
        options: [
          { label: '775mm', value: '775' },
          { label: '740mm', value: '740' },
        ],
        placeholder: '请选择裁切尺寸',
      },
      rules: 'selectRequired',
    },
    {
      fieldName: 'cutInputPcs',
      label: '裁切投入片数',
      component: 'InputNumber',
      componentProps: { min: 1, precision: 0, class: 'w-full' },
      rules: 'required',
    },
    {
      fieldName: 'cutOutputPcs',
      label: '裁切产出片数',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 0, class: 'w-full' },
      rules: 'required',
    },
    {
      fieldName: 'consumeTime',
      label: '消耗时间',
      component: 'DatePicker',
      componentProps: {
        format: MES_DATETIME_FORMAT,
        showTime: true,
        valueFormat: MES_DATETIME_FORMAT,
      },
      rules: 'required',
    },
  ],
  showDefaultActions: false,
});

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closeOnPressEscape: false,
  class: 'w-[860px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    if (!remarkValue.value.trim()) {
      message.warning('请填写登记说明');
      return;
    }
    const values =
      (await formApi.getValues()) as MesHcCutRoundSpareApi.RndConsumeReq;
    values.remark = remarkValue.value.trim();
    if (Number(values.cutOutputPcs) > Number(values.cutInputPcs)) {
      message.warning('裁切产出片数不能大于投入片数');
      return;
    }
    values.consumeTime =
      normalizeMesDateTime(values.consumeTime) ||
      dayjs().format(MES_DATETIME_FORMAT);
    modalApi.lock();
    try {
      await saveCutRoundSpareRndConsume(values);
      await modalApi.close();
      emit('success');
      message.success('研发样品消耗已登记，并同步刀片、毛毡累计片数和生产记录');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      spare.value = undefined;
      remarkValue.value = '';
      await formApi.resetForm();
      return;
    }
    spare.value = modalApi.getData<MesHcCutRoundSpareApi.Spare>();
    if (!spare.value?.equipmentId) {
      message.warning('当前备件缺少设备信息，无法登记研发消耗');
      await modalApi.close();
      return;
    }
    modalApi.lock();
    try {
      await formApi.resetForm();
      await formApi.setValues({
        consumeTime: dayjs().format(MES_DATETIME_FORMAT),
        cutInputPcs: undefined,
        cutOutputPcs: 0,
        cutSizeMm: undefined,
        equipmentId: spare.value.equipmentId,
        modelCode: '',
        padType: undefined,
        productionBatchNo: '',
      });
      remarkValue.value = '';
    } finally {
      modalApi.unlock();
    }
  },
  showCancelButton: false,
});
</script>

<template>
  <Modal :title="title">
    <div class="px-2 pb-4">
      <div
        class="mb-4 rounded border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-800"
      >
        <div>设备：{{ equipmentText }}</div>
        <div class="mt-1">
          本次将同时累计该设备刀片和毛毡的使用片数；生产记录按本次登记仅汇总一笔裁切投入/产出。
        </div>
      </div>
      <Form />
      <div class="rnd-consume-remark">
        <label for="rnd-consume-remark">
          <span class="required">*</span>登记说明
        </label>
        <Input.TextArea
          id="rnd-consume-remark"
          v-model:value="remarkValue"
          :maxlength="500"
          :rows="3"
          placeholder="例如：研发样品裁切手工登记"
        />
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.rnd-consume-remark {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr);
  column-gap: 12px;
  align-items: start;
  margin-top: 16px;
}

.rnd-consume-remark label {
  padding-top: 6px;
  text-align: right;
}

.rnd-consume-remark .required {
  margin-right: 4px;
  color: #ff4d4f;
}
</style>
