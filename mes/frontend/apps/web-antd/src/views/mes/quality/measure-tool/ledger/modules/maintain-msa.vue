<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Form, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import { maintainMeasureToolMsa } from '#/api/mes/quality/measure-tool';
import { FileUpload } from '#/components/upload';

import {
  loadPersonnelOptions,
  normalizeEditableSelectValue,
  toEditableSelectValue,
} from '../options';

const emit = defineEmits(['success']);
const ledger = ref<QmsMeasureToolApi.Ledger>();
const reportUrls = ref<string[]>([]);
const title = computed(
  () =>
    `维护MSA分析${ledger.value?.toolName ? ` - ${ledger.value.toolName}` : ''}`,
);

const [VbenForm, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 110,
  },
  layout: 'horizontal',
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6',
  schema: [
    {
      component: 'DatePicker',
      componentProps: { class: 'w-full', valueFormat: 'YYYY-MM-DD' },
      fieldName: 'msaDate',
      label: '分析日期',
      rules: 'required',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: [
          { label: '合格', value: 'QUALIFIED' },
          { label: '不合格', value: 'UNQUALIFIED' },
        ],
      },
      fieldName: 'msaResult',
      label: '分析结果',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: [],
        placeholder: '选择系统用户或直接填写分析人',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'analyst',
      label: '分析人',
    },
    {
      component: 'Textarea',
      componentProps: { rows: 3 },
      fieldName: 'remark',
      formItemClass: 'col-span-2',
      label: '分析说明',
    },
  ],
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[820px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid || !ledger.value?.id) return;
    modalApi.lock();
    try {
      const values = await formApi.getValues();
      await maintainMeasureToolMsa({
        ...values,
        analyst: normalizeEditableSelectValue(values.analyst),
        ledgerId: ledger.value.id,
        msaReport: reportUrls.value.join(','),
      } as any);
      await modalApi.close();
      emit('success');
      message.success('最新MSA分析结果已更新，上一版本已归档到分析历史');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      ledger.value = undefined;
      reportUrls.value = [];
      return;
    }
    ledger.value = modalApi.getData<QmsMeasureToolApi.Ledger>();
    await formApi.updateSchema([
      {
        componentProps: {
          allowClear: true,
          mode: 'combobox',
          optionFilterProp: 'label',
          options: await loadPersonnelOptions(),
          placeholder: '选择系统用户或直接填写分析人',
          showArrow: true,
          showSearch: true,
        },
        fieldName: 'analyst',
      },
    ]);
    reportUrls.value = ledger.value?.msaReport
      ? ledger.value.msaReport.split(',').filter(Boolean)
      : [];
    await formApi.resetForm();
    await formApi.setValues({
      msaDate: dayjs().format('YYYY-MM-DD'),
      msaResult: ledger.value?.msaResult,
      analyst: toEditableSelectValue(ledger.value?.msaAnalyst),
    });
  },
});
</script>

<template>
  <Modal :title="title">
    <div class="px-2 pb-4">
      <div
        class="mb-4 grid grid-cols-2 gap-x-6 rounded border border-gray-200 bg-gray-50 px-4 py-3 text-sm"
      >
        <span>本厂编号：{{ ledger?.toolCode || '-' }}</span>
        <span>设备名称：{{ ledger?.toolName || '-' }}</span>
        <span>当前下次分析：{{ ledger?.nextMsaDate || '-' }}</span>
        <span>MSA周期：{{ ledger?.msaCycleMonths || '-' }}个月</span>
      </div>
      <VbenForm />
      <Form.Item
        class="mx-0 mt-3"
        label="MSA分析报告"
        :label-col="{ style: { width: '110px' } }"
      >
        <FileUpload
          v-model="reportUrls"
          :accept="['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx', 'xls', 'xlsx']"
          directory="mes/qms/measure-tool/msa"
          :max-number="10"
          :max-size="20"
          multiple
        />
      </Form.Item>
    </div>
  </Modal>
</template>
