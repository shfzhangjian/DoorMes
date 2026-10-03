<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import dayjs from 'dayjs';
import { computed, nextTick, ref } from 'vue';

import { confirm, useVbenModal } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createMeasureToolCalibrationRecord,
  getMeasureToolCalibrationRecord,
  getMeasureToolLedger,
  getMeasureToolLedgerPage,
  updateMeasureToolCalibrationRecord,
} from '#/api/mes/quality/measure-tool';
import { $t } from '#/locales';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<QmsMeasureToolApi.CalibrationRecord>();
const ledgerRows = ref<QmsMeasureToolApi.Ledger[]>([]);

const getTitle = computed(() =>
  formData.value?.id
    ? $t('ui.actionTitle.edit', ['校准记录'])
    : $t('ui.actionTitle.create', ['校准记录']),
);

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' }, formItemClass: 'col-span-1', labelWidth: 110 },
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6',
});

async function buildLedgerOptions() {
  const result = await getMeasureToolLedgerPage({ pageNo: 1, pageSize: 200 });
  ledgerRows.value = result.list || [];
  return ledgerRows.value.map((item) => ({
    calibrationCycleMonths: item.calibrationCycleMonths,
    label: item.toolCode ? `${item.toolCode} / ${item.toolName}` : item.toolName,
    value: item.id,
  }));
}

async function getSelectedLedger(ledgerId?: number) {
  if (!ledgerId) {
    return undefined;
  }
  const existed = ledgerRows.value.find((item) => item.id === ledgerId);
  if (existed) {
    return existed;
  }
  const ledger = await getMeasureToolLedger(ledgerId);
  ledgerRows.value = [...ledgerRows.value, ledger];
  return ledger;
}

function calculateNextCalibrationDate(calibrationDate?: string, cycleMonths?: number) {
  if (!calibrationDate) {
    return undefined;
  }
  const baseDate = dayjs(calibrationDate);
  if (!baseDate.isValid()) {
    return undefined;
  }
  const monthCount = cycleMonths == null ? 12 : cycleMonths;
  return baseDate.add(monthCount, 'month').format('YYYY-MM-DD');
}

async function refreshAutoNextCalibrationDate() {
  const values = (await formApi.getValues()) as QmsMeasureToolApi.CalibrationRecord;
  const ledger = await getSelectedLedger(values.ledgerId);
  const nextCalibrationDate = calculateNextCalibrationDate(values.calibrationDate, ledger?.calibrationCycleMonths);
  await formApi.setValues({
    nextCalibrationDate,
    validUntil: values.validUntil || nextCalibrationDate,
  });
}

function scheduleRefreshAutoNextCalibrationDate() {
  void nextTick(() => refreshAutoNextCalibrationDate());
}

const [Modal, modalApi] = useVbenModal({
  class: 'w-[880px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;

    await refreshAutoNextCalibrationDate();
    const values = (await formApi.getValues()) as QmsMeasureToolApi.CalibrationRecord;
    if (values.taskId && !formData.value?.id) {
      await confirm('确认提交本次校准信息并进入待确认吗？');
    }

    modalApi.lock();
    try {
      if (formData.value?.id) {
        await updateMeasureToolCalibrationRecord({ ...values, id: formData.value.id, version: formData.value.version });
      } else {
        await createMeasureToolCalibrationRecord(values);
      }
      await modalApi.close();
      emit('success');
      message.success($t('ui.actionMessage.operationSuccess'));
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      formData.value = undefined;
      return;
    }

    const data = modalApi.getData<QmsMeasureToolApi.CalibrationRecord>();
    await formApi.updateSchema(useFormSchema(await buildLedgerOptions(), {
      onCalibrationDateChange: scheduleRefreshAutoNextCalibrationDate,
      onLedgerChange: scheduleRefreshAutoNextCalibrationDate,
    }));
    await formApi.resetForm();

    if (!data?.id) {
      formData.value = undefined;
      const calibrationDate = dayjs().format('YYYY-MM-DD');
      await formApi.setValues({
        calibrationDate,
        ledgerId: data?.ledgerId,
        sourceType: data?.taskId ? 'TASK' : 'MANUAL',
        taskId: data?.taskId,
      });
      await refreshAutoNextCalibrationDate();
      return;
    }

    modalApi.lock();
    try {
      formData.value = await getMeasureToolCalibrationRecord(data.id);
      await formApi.setValues(formData.value);
      await refreshAutoNextCalibrationDate();
    } finally {
      modalApi.unlock();
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
