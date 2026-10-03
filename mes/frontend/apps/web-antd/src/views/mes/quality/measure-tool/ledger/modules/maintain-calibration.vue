<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import { Form, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import { maintainMeasureToolCalibration } from '#/api/mes/quality/measure-tool';
import { FileUpload } from '#/components/upload';

import { CALIBRATION_RESULT_OPTIONS } from '../../shared';
import {
  loadCalibrationMethodOptions,
  loadCalibrationOrgOptions,
  loadPersonnelOptions,
  normalizeEditableSelectValue,
  toEditableSelectValue,
} from '../options';

const emit = defineEmits(['success']);
const ledger = ref<QmsMeasureToolApi.Ledger>();
const reportUrls = ref<string[]>([]);

const title = computed(
  () =>
    `维护校准${ledger.value?.toolName ? ` - ${ledger.value.toolName}` : ''}`,
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
      fieldName: 'calibrationDate',
      label: '校准日期',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        options: [
          { label: '外校', value: 'EXTERNAL' },
          { label: '内校', value: 'INTERNAL' },
        ],
      },
      fieldName: 'calibrationType',
      label: '校准类型',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        options: CALIBRATION_RESULT_OPTIONS.map(({ label, value }) => ({
          label,
          value,
        })),
      },
      fieldName: 'calibrationResult',
      label: '校准结果',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: [],
        placeholder: '选择系统用户或直接填写校准人',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'calibrator',
      label: '校准人',
    },
    {
      component: 'Select',
      componentProps: {
        options: [],
      },
      fieldName: 'calibrationMethod',
      label: '校准方式',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: [],
        placeholder: '选择或输入校准机构，新增值将自动加入字典',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'calibrationOrg',
      label: '校准机构',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入证书编号' },
      fieldName: 'certificateNo',
      label: '证书编号',
    },
    {
      component: 'Textarea',
      componentProps: { rows: 3 },
      fieldName: 'remark',
      formItemClass: 'col-span-2',
      label: '维护说明',
    },
  ],
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[880px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid || !ledger.value?.id) return;
    modalApi.lock();
    try {
      const values = await formApi.getValues();
      await maintainMeasureToolCalibration({
        ...values,
        calibrationMethod: normalizeEditableSelectValue(
          values.calibrationMethod,
        ),
        calibrationOrg: normalizeEditableSelectValue(values.calibrationOrg),
        calibrator: normalizeEditableSelectValue(values.calibrator),
        calibrationReport: reportUrls.value.join(','),
        ledgerId: ledger.value.id,
      } as any);
      await modalApi.close();
      emit('success');
      message.success('最新校准结果已更新，上一版本已归档到校准历史');
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
    const [calibrationMethodOptions, calibrationOrgOptions, personnelOptions] =
      await Promise.all([
        loadCalibrationMethodOptions(),
        loadCalibrationOrgOptions(),
        loadPersonnelOptions(),
      ]);
    await formApi.updateSchema([
      {
        componentProps: {
          options: calibrationMethodOptions,
        },
        fieldName: 'calibrationMethod',
      },
      {
        componentProps: {
          allowClear: true,
          mode: 'combobox',
          optionFilterProp: 'label',
          options: calibrationOrgOptions,
          placeholder: '选择或输入校准机构，新增值将自动加入字典',
          showArrow: true,
          showSearch: true,
        },
        fieldName: 'calibrationOrg',
      },
      {
        componentProps: {
          allowClear: true,
          mode: 'combobox',
          optionFilterProp: 'label',
          options: personnelOptions,
          placeholder: '选择系统用户或直接填写校准人',
          showArrow: true,
          showSearch: true,
        },
        fieldName: 'calibrator',
      },
    ]);
    reportUrls.value = ledger.value?.calibrationReport
      ? ledger.value.calibrationReport.split(',').filter(Boolean)
      : [];
    await formApi.resetForm();
    await formApi.setValues({
      calibrationDate: dayjs().format('YYYY-MM-DD'),
      calibrationType: ledger.value?.calibrationType,
      calibrationMethod: ledger.value?.calibrationMethod,
      calibrationOrg: toEditableSelectValue(ledger.value?.calibrationOrg),
      calibrator: toEditableSelectValue(ledger.value?.calibrator),
      calibrationResult: ledger.value?.calibrationResult,
      certificateNo: ledger.value?.certificateNo,
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
        <span>当前下次校准：{{ ledger?.nextCalibrationDate || '-' }}</span>
        <span>校准周期：{{ ledger?.calibrationCycleMonths || '-' }}个月</span>
      </div>
      <VbenForm />
      <Form.Item
        class="mx-0 mt-3"
        label="校准报告"
        :label-col="{ style: { width: '110px' } }"
      >
        <FileUpload
          v-model="reportUrls"
          :accept="['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx', 'xls', 'xlsx']"
          directory="mes/qms/measure-tool/calibration"
          help-text="支持校准证书、报告和现场照片；最多 10 个文件，单文件不超过 20MB"
          :max-number="10"
          :max-size="20"
          multiple
          show-description
        />
      </Form.Item>
    </div>
  </Modal>
</template>
