<script lang="ts" setup>
import type { MesHcGuideClothRecordApi } from '#/api/mes/hc/guideclothrecord';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Input, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import {
  getGuideClothRndRuntime,
  saveGuideClothRndConsume,
} from '#/api/mes/hc/guideclothrecord';
import {
  MES_DATETIME_FORMAT,
  normalizeMesDateTime,
} from '#/api/mes/hc/shared/date-time';
import ConsumableLedgerSwitchModal from '#/views/mes/hc/base/tooling-consumable-ledger/components/ConsumableLedgerSwitchModal.vue';

const emit = defineEmits(['success']);
const guideClothRecord = ref<MesHcGuideClothRecordApi.GuideClothRecord>();
const guideClothRuntime = ref<MesHcGuideClothRecordApi.RuntimeInfo>();
const guideClothChanged = ref(false);
const remarkValue = ref('');
type RndConsumableType = 'GUIDE_CLOTH' | 'PET';
const consumableLedgerPickerRef =
  ref<InstanceType<typeof ConsumableLedgerSwitchModal>>();
const selectingConsumableType = ref<RndConsumableType>('PET');

type RndConsumeFormValues = MesHcGuideClothRecordApi.RndConsumeReq & {
  rndPadTypeName?: string;
  wetLineName?: string;
};

const title = computed(() => '湿法导布研发消耗登记');
const guideClothText = computed(() => {
  const record = guideClothRecord.value;
  return (
    [
      record?.lineName,
      guideClothRuntime.value?.guideClothBatchNo || record?.guideClothBatchNo,
    ]
      .filter(Boolean)
      .join(' - ') || '-'
  );
});
const currentUseCountText = computed(
  () => `${guideClothRuntime.value?.currentUseCount ?? 0}/28 次`,
);
const nextUseCountText = computed(() =>
  guideClothChanged.value
    ? '1/28 次（更换后）'
    : `${guideClothRuntime.value?.nextUseCount ?? 1}/28 次`,
);
const runtimeWarning = computed(
  () => Number(guideClothRuntime.value?.warningFlag) === 1,
);

function openConsumableLedgerPicker(consumableType: RndConsumableType) {
  selectingConsumableType.value = consumableType;
  consumableLedgerPickerRef.value?.open({
    consumableType,
    processCode: 'WET',
    selectionMode: 'DIRECT',
    title:
      consumableType === 'PET'
        ? '选择湿法 PET 耗材领用台账'
        : '选择湿法导布耗材领用台账',
  });
}

async function handleConsumableLedgerSelected(
  row: MesHcToolingConsumableLedgerApi.Ledger,
) {
  if (!row.id) {
    message.warning('所选耗材领用台账无效，请重新选择');
    return;
  }
  if (selectingConsumableType.value === 'PET') {
    await formApi.setValues({
      petBatchNo: row.batchNo || '',
      petLedgerId: row.id,
      petModel: row.model || '',
    });
    return;
  }
  await formApi.setValues({
    guideClothLedgerId: row.id,
    guideClothNewBatchNo: row.batchNo || '',
  });
}

function handleGuideClothChanged(value: unknown) {
  guideClothChanged.value = Boolean(value);
  if (!guideClothChanged.value) {
    void formApi.setValues({
      guideClothLedgerId: undefined,
      guideClothNewBatchNo: '',
    });
  }
}

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
      fieldName: 'guideClothRecordId',
      component: 'Input',
      dependencies: { show: false, triggerFields: [''] },
    },
    {
      fieldName: 'wetLineName',
      label: '湿法产线',
      component: 'Input',
      componentProps: {
        disabled: true,
        placeholder: '由当前导布记录自动带出',
      },
    },
    {
      fieldName: 'rndPadTypeName',
      label: '研发垫型',
      component: 'Input',
      componentProps: {
        disabled: true,
        placeholder: '由当前导布产线自动确定',
      },
    },
    {
      fieldName: 'guideClothChanged',
      label: '是否更换导布',
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        optionType: 'button',
        onChange: (event: boolean | { target?: { value?: unknown } }) => {
          handleGuideClothChanged(
            typeof event === 'boolean' ? event : event?.target?.value,
          );
        },
        options: [
          { label: '否', value: false },
          { label: '是', value: true },
        ],
      },
    },
    {
      fieldName: 'guideClothLedgerId',
      component: 'Input',
      dependencies: { show: false, triggerFields: [''] },
    },
    {
      fieldName: 'guideClothNewBatchNo',
      label: '新导布批号',
      component: 'Input',
      componentProps: {
        class: 'cursor-pointer',
        placeholder: '点击从湿法导布耗材领用台账选择',
        readonly: true,
        title: '仅可选择未标记完成的湿法导布耗材领用台账',
        onClick: () => openConsumableLedgerPicker('GUIDE_CLOTH'),
      },
      dependencies: {
        triggerFields: ['guideClothChanged'],
        show: (values) => values.guideClothChanged === true,
      },
    },
    {
      fieldName: 'guideClothReplaceReason',
      label: '导布更换原因',
      component: 'Input',
      componentProps: { placeholder: '选择更换时必填' },
      dependencies: {
        triggerFields: ['guideClothChanged'],
        show: (values) => values.guideClothChanged === true,
      },
    },
    {
      fieldName: 'productModelCode',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '请输入研发产品型号' },
      rules: 'required',
    },
    {
      fieldName: 'productMaterialCode',
      label: '产品料号',
      component: 'Input',
      componentProps: { placeholder: '请输入研发产品料号' },
      rules: 'required',
    },
    {
      fieldName: 'productBatchNo',
      label: '产品批次',
      component: 'Input',
      componentProps: { placeholder: '请输入研发产品批次' },
      rules: 'required',
    },
    {
      fieldName: 'petModel',
      label: 'PET型号',
      component: 'Input',
      componentProps: { disabled: true, placeholder: '选择PET台账后自动带出' },
      rules: 'required',
    },
    {
      fieldName: 'petBatchNo',
      label: 'PET批号',
      component: 'Input',
      componentProps: {
        class: 'cursor-pointer',
        placeholder: '点击从湿法PET耗材领用台账选择',
        readonly: true,
        title: '仅可选择未标记完成的湿法PET耗材领用台账',
        onClick: () => openConsumableLedgerPicker('PET'),
      },
      rules: 'required',
    },
    {
      fieldName: 'petLedgerId',
      component: 'Input',
      dependencies: { show: false, triggerFields: [''] },
    },
    {
      fieldName: 'wetInputKg',
      label: '湿法投入(kg)',
      component: 'InputNumber',
      componentProps: { min: 0.001, precision: 3, class: 'w-full' },
      rules: 'required',
    },
    {
      fieldName: 'wetOutputMeter',
      label: '湿法产出(m)',
      component: 'InputNumber',
      componentProps: { min: 0, precision: 3, class: 'w-full' },
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
  class: 'w-[920px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    if (!remarkValue.value.trim()) {
      message.warning('请填写登记说明');
      return;
    }
    const {
      rndPadTypeName: _rndPadTypeName,
      wetLineName: _wetLineName,
      ...values
    } = (await formApi.getValues()) as RndConsumeFormValues;
    values.guideClothChanged = Boolean(values.guideClothChanged);
    guideClothChanged.value = values.guideClothChanged;
    if (!values.petLedgerId) {
      message.warning('请从湿法PET耗材领用台账选择PET批次');
      return;
    }
    if (values.guideClothChanged) {
      if (!values.guideClothLedgerId) {
        message.warning('请从湿法导布耗材领用台账选择新导布批号');
        return;
      }
      values.guideClothNewBatchNo = values.guideClothNewBatchNo?.trim();
      values.guideClothReplaceReason = values.guideClothReplaceReason?.trim();
      if (!values.guideClothNewBatchNo) {
        message.warning('选择更换导布后请填写新导布批号');
        return;
      }
      if (!values.guideClothReplaceReason) {
        message.warning('选择更换导布后请填写导布更换原因');
        return;
      }
    } else {
      values.guideClothLedgerId = undefined;
      values.guideClothNewBatchNo = undefined;
      values.guideClothReplaceReason = undefined;
    }
    if (Number(values.wetInputKg) <= 0) {
      message.warning('湿法投入必须大于 0 kg');
      return;
    }
    if (Number(values.wetOutputMeter) < 0) {
      message.warning('湿法产出不能小于 0 m');
      return;
    }
    values.remark = remarkValue.value.trim();
    values.consumeTime =
      normalizeMesDateTime(values.consumeTime) ||
      dayjs().format(MES_DATETIME_FORMAT);
    modalApi.lock();
    try {
      await saveGuideClothRndConsume(values);
      await modalApi.close();
      emit('success');
      message.success('研发消耗已登记，并已同步导布寿命和湿法生产记录');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      guideClothRecord.value = undefined;
      guideClothRuntime.value = undefined;
      guideClothChanged.value = false;
      remarkValue.value = '';
      await formApi.resetForm();
      return;
    }
    guideClothRecord.value =
      modalApi.getData<MesHcGuideClothRecordApi.GuideClothRecord>();
    if (
      !guideClothRecord.value?.id ||
      Number(guideClothRecord.value.currentFlag) !== 0
    ) {
      message.warning('仅当前导布记录可以登记研发消耗');
      await modalApi.close();
      return;
    }
    const lineCode = String(guideClothRecord.value.lineCode || '')
      .trim()
      .toUpperCase();
    let rndPadTypeName = '';
    if (['B', 'BLACK'].includes(lineCode)) {
      rndPadTypeName = '黑垫';
    } else if (['W', 'WHITE'].includes(lineCode)) {
      rndPadTypeName = '白垫';
    }
    if (!rndPadTypeName) {
      message.warning('当前导布记录未归属黑垫线或白垫线，不能登记研发消耗');
      await modalApi.close();
      return;
    }
    modalApi.lock();
    try {
      await formApi.resetForm();
      guideClothRuntime.value = await getGuideClothRndRuntime(
        guideClothRecord.value.id,
      );
      guideClothChanged.value =
        Number(guideClothRuntime.value?.warningFlag) === 1;
      await formApi.setValues({
        guideClothRecordId: guideClothRecord.value.id,
        rndPadTypeName,
        wetLineName:
          guideClothRecord.value.lineName ||
          guideClothRecord.value.lineCode ||
          '-',
        guideClothChanged: guideClothChanged.value,
        guideClothLedgerId: undefined,
        guideClothNewBatchNo: '',
        guideClothReplaceReason: '',
        productModelCode: '',
        productMaterialCode: '',
        productBatchNo: '',
        petLedgerId: undefined,
        petModel: '',
        petBatchNo: '',
        wetInputKg: undefined,
        wetOutputMeter: undefined,
        consumeTime: dayjs().format(MES_DATETIME_FORMAT),
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
        class="mb-4 rounded border px-4 py-3 text-sm"
        :class="[
          runtimeWarning
            ? 'border-red-300 bg-red-50 text-red-800'
            : 'border-amber-300 bg-amber-50 text-amber-800',
        ]"
      >
        <div>当前导布：{{ guideClothText }}</div>
        <div class="rnd-consume-runtime">
          <span
            >当前使用：<strong>{{ currentUseCountText }}</strong></span
          >
          <span
            >本次登记后：<strong>{{ nextUseCountText }}</strong></span
          >
          <span
            >上次更换：{{ guideClothRuntime?.replaceTime || '未记录' }}</span
          >
        </div>
        <div class="mt-1 font-medium">
          {{ guideClothRuntime?.warningText || '正在读取导布寿命状态' }}
        </div>
        <div class="mt-1">
          研发垫型由当前导布产线自动确定：黑垫线登记为黑垫，白垫线登记为白垫，并同步到湿法生产记录。新导布和
          PET
          必须从湿法耗材领用台账选择，已标记完成的批次不会显示；选择“是”会在本次研发登记中同步更换导布。
        </div>
      </div>
      <Form />
      <div class="rnd-consume-remark">
        <label for="wet-guide-cloth-rnd-remark">
          <span class="required">*</span>登记说明
        </label>
        <Input.TextArea
          id="wet-guide-cloth-rnd-remark"
          v-model:value="remarkValue"
          :maxlength="500"
          :rows="3"
          placeholder="例如：研发样品湿法手工消耗登记"
        />
      </div>
    </div>
  </Modal>
  <ConsumableLedgerSwitchModal
    ref="consumableLedgerPickerRef"
    @selected="handleConsumableLedgerSelected"
  />
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

.rnd-consume-runtime {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin-top: 6px;
}

.rnd-consume-runtime strong {
  font-variant-numeric: tabular-nums;
}
</style>
