<script lang="ts" setup>
import type { MesHcEquipmentApi } from '#/api/mes/hc/equipment';
import { getEquipmentSelectOptions } from '#/api/mes/hc/equipment';
import type { MesHcPressSlotSpareApi } from '#/api/mes/hc/press-slot-spare';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Input, message, Select } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import {
  getPressSlotSparePage,
  savePressSlotSpareRndConsume,
} from '#/api/mes/hc/press-slot-spare';

import {
  MES_DATETIME_FORMAT,
  normalizeMesDateTime,
} from '../../../../base/shared/date-time';

const emit = defineEmits(['success']);
const states = ref<MesHcPressSlotSpareApi.Spare[]>([]);
const equipmentDetails = ref<MesHcEquipmentApi.SelectOption[]>([]);
const equipmentId = ref<number>();
const remarkValue = ref('');
const title = computed(() => '压槽研发消耗登记');
const equipmentOptions = computed(() => {
  const options = new Map<number, { value: number; label: string }>();
  for (const state of states.value) {
    if (state.equipmentId)
      options.set(state.equipmentId, {
        value: state.equipmentId,
        label:
          [state.equipmentCode, state.equipmentName]
            .filter(Boolean)
            .join(' - ') || String(state.equipmentId),
      });
  }
  return [...options.values()];
});
const currentSpares = computed(() =>
  [
    { type: 'PRESS_ROLLER', label: '压槽辊' },
    { type: 'BEARING', label: '轴承' },
  ].map((item) => ({
    ...item,
    state: states.value.find(
      (state) =>
        state.equipmentId === equipmentId.value && state.spareType === item.type
    ),
  }))
);
const selectedPadType = computed(() => {
  const equipment = equipmentDetails.value.find(
    (item) => item.value === equipmentId.value
  );
  const type = equipment?.applicablePadType?.trim().toUpperCase();
  const name = equipment?.name || equipment?.label || '';
  const white = name.includes('白垫');
  const black = name.includes('黑垫');
  if (white && black) return undefined;
  if (type !== 'WHITE_PAD' && type !== 'BLACK_PAD') return undefined;
  if ((white && type !== 'WHITE_PAD') || (black && type !== 'BLACK_PAD'))
    return undefined;
  return type;
});
const unavailableReason = computed(() => {
  if (!equipmentId.value) return '请选择压槽设备';
  if (!selectedPadType.value)
    return '请核对设备适用垫型：必须为白垫或黑垫，且不能与设备名称冲突';
  for (const item of currentSpares.value) {
    if (!item.state)
      return `该设备未配置${item.label}，请先到压槽备件管理完成配置`;
    if (item.state.status?.toUpperCase() !== 'ACTIVE')
      return `该设备${item.label}未启用，不能登记研发消耗`;
  }
  return '';
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
      fieldName: 'pressSlotInputPcs',
      label: '压槽投入片数',
      component: 'InputNumber',
      componentProps: { min: 1, precision: 0, class: 'w-full' },
      rules: 'required',
    },
    {
      fieldName: 'pressSlotOutputPcs',
      label: '压槽产出片数',
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
    if (unavailableReason.value) {
      message.warning(unavailableReason.value);
      return;
    }
    const { valid } = await formApi.validate();
    if (!valid) return;
    if (!remarkValue.value.trim()) {
      message.warning('请填写登记说明');
      return;
    }
    const values =
      (await formApi.getValues()) as MesHcPressSlotSpareApi.RndConsumeReq;
    values.equipmentId = equipmentId.value!;
    values.remark = remarkValue.value.trim();
    if (Number(values.pressSlotOutputPcs) > Number(values.pressSlotInputPcs)) {
      message.warning('压槽产出片数不能大于投入片数');
      return;
    }
    values.consumeTime =
      normalizeMesDateTime(values.consumeTime) ||
      dayjs().format(MES_DATETIME_FORMAT);
    modalApi.lock();
    try {
      await savePressSlotSpareRndConsume(values);
      await modalApi.close();
      emit('success');
      message.success(
        '研发样品消耗已登记，并同步压槽辊、轴承累计片数和生产记录'
      );
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      equipmentId.value = undefined;
      states.value = [];
      equipmentDetails.value = [];
      remarkValue.value = '';
      await formApi.resetForm();
      return;
    }
    modalApi.lock();
    try {
      equipmentId.value = undefined;
      states.value = [];
      equipmentDetails.value = [];
      const loaded: MesHcPressSlotSpareApi.Spare[] = [];
      for (let pageNo = 1; ; pageNo++) {
        const page = await getPressSlotSparePage({ pageNo, pageSize: 100 });
        loaded.push(...page.list);
        if (loaded.length >= page.total || !page.list.length) break;
      }
      equipmentDetails.value = await getEquipmentSelectOptions();
      states.value = loaded;
      await formApi.resetForm();
      await formApi.setValues({
        consumeTime: dayjs().format(MES_DATETIME_FORMAT),
        modelCode: '',
        pressSlotInputPcs: undefined,
        pressSlotOutputPcs: 0,
        productionBatchNo: '',
      });
      remarkValue.value = '';
    } catch {
      message.error('压槽备件加载失败，请关闭后重新打开登记');
      await modalApi.close();
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
        <div class="mb-3 flex items-center gap-3">
          <span>压槽设备<span class="text-red-500"> *</span></span>
          <Select
            v-model:value="equipmentId"
            :options="equipmentOptions"
            show-search
            option-filter-prop="label"
            allow-clear
            class="flex-1"
            placeholder="请选择已配置压槽备件的设备"
          />
        </div>
        <div v-if="!equipmentOptions.length">
          暂无已配置备件的设备，请先到压槽备件管理登记。
        </div>
        <div
          v-for="item in currentSpares"
          v-show="equipmentId"
          :key="item.type"
          class="mt-1"
        >
          {{ item.label }}：批号 {{ item.state?.batchNo || '-' }}； 状态
          {{
            !item.state
              ? '未配置'
              : item.state.status?.toUpperCase() === 'ACTIVE'
              ? '使用中'
              : '未使用（停用）'
          }}； 当前累计 {{ item.state?.useCount ?? 0 }} 片
        </div>
        <div v-if="equipmentId && unavailableReason" class="mt-2 text-red-600">
          {{ unavailableReason }}
        </div>
        <div class="mt-1">
          本次将同时累计该设备压槽辊和轴承的使用片数；生产记录按本次登记仅汇总一笔压槽投入/产出。
        </div>
      </div>
      <div v-if="equipmentId" class="mb-4">
        研发类型：{{
          selectedPadType === 'WHITE_PAD'
            ? '白垫'
            : selectedPadType === 'BLACK_PAD'
            ? '黑垫'
            : '设备配置待核对'
        }}（按设备自动确定）
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
          placeholder="例如：研发样品压槽手工登记"
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
