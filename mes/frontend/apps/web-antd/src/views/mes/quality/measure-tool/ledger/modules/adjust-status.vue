<script lang="ts" setup>
import type { QmsMeasureToolApi } from '#/api/mes/quality/measure-tool';

import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { Form, message } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenForm } from '#/adapter/form';
import { updateMeasureToolLedgerStatus } from '#/api/mes/quality/measure-tool';
import { FileUpload } from '#/components/upload';

import { optionLabel, TOOL_STATUS_OPTIONS } from '../../shared';
import { loadPersonnelOptions, normalizeEditableSelectValue } from '../options';

const emit = defineEmits(['success']);
const userStore = useUserStore();
const ledger = ref<QmsMeasureToolApi.Ledger>();
const attachmentUrls = ref<string[]>([]);
const adjustableStatusOptions = TOOL_STATUS_OPTIONS.filter((item) =>
  ['CALIBRATING', 'IN_USE', 'REPAIRING', 'SCRAPPED', 'STOPPED'].includes(
    item.value,
  ),
);

const title = computed(() =>
  ledger.value?.toolName
    ? `调整状态 - ${ledger.value.toolName}`
    : '调整量检具状态',
);

const [FormContent, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 96,
  },
  layout: 'horizontal',
  schema: [
    {
      component: 'Select',
      componentProps: {
        options: adjustableStatusOptions,
        placeholder: '请选择调整后的状态',
      },
      fieldName: 'status',
      label: '调整状态',
      rules: 'selectRequired',
    },
    {
      component: 'DatePicker',
      componentProps: {
        class: 'w-full',
        showTime: true,
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
      },
      fieldName: 'handleTime',
      label: '处理时间',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: [],
        placeholder: '选择系统用户或直接填写处理人',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'handler',
      label: '处理人',
      rules: 'required',
    },
    {
      component: 'Input',
      componentProps: { placeholder: '请输入OA处理单号' },
      fieldName: 'oaProcessNo',
      label: 'OA处理单号',
    },
    {
      component: 'Textarea',
      componentProps: { placeholder: '请输入状态调整的处理说明', rows: 3 },
      fieldName: 'handleRemark',
      formItemClass: 'col-span-2',
      label: '处理说明',
      rules: 'required',
    },
  ],
  showDefaultActions: false,
  wrapperClass: 'grid-cols-2 gap-x-6',
});

const [Modal, modalApi] = useVbenModal({
  class: 'w-[760px]',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid || !ledger.value?.id || ledger.value.version === undefined) {
      return;
    }

    modalApi.lock();
    try {
      const values =
        await formApi.getValues<QmsMeasureToolApi.LedgerStatusUpdate>();
      await updateMeasureToolLedgerStatus({
        ...values,
        attachments: attachmentUrls.value.join(','),
        handler: normalizeEditableSelectValue(values.handler) || '',
        id: ledger.value.id,
        version: ledger.value.version,
      });
      await modalApi.close();
      emit('success');
      message.success('状态已调整，处理记录已归档');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen) {
    if (!isOpen) {
      attachmentUrls.value = [];
      ledger.value = undefined;
      return;
    }
    ledger.value = modalApi.getData<QmsMeasureToolApi.Ledger>();
    await formApi.updateSchema({
      componentProps: {
        allowClear: true,
        mode: 'combobox',
        optionFilterProp: 'label',
        options: await loadPersonnelOptions(),
        placeholder: '选择系统用户或直接填写处理人',
        showArrow: true,
        showSearch: true,
      },
      fieldName: 'handler',
    });
    attachmentUrls.value = [];
    await formApi.resetForm();
    await formApi.setValues({
      handleTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      handler: userStore.userInfo?.nickname || undefined,
      status: ledger.value?.status,
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
        <span>
          调整前状态：{{ optionLabel(TOOL_STATUS_OPTIONS, ledger?.status) }}
        </span>
        <span>
          位置/区域：{{ ledger?.storageLocation || '-' }} /
          {{ ledger?.categoryName || '-' }}
        </span>
      </div>
      <FormContent />
      <Form.Item
        class="mx-0 mt-3"
        label="附件"
        :label-col="{ style: { width: '96px' } }"
      >
        <FileUpload
          v-model="attachmentUrls"
          :accept="['pdf', 'jpg', 'jpeg', 'png', 'doc', 'docx', 'xls', 'xlsx']"
          directory="mes/qms/measure-tool/status"
          help-text="支持处理单、现场照片和相关文档；最多 10 个文件，单文件不超过 20MB"
          :max-number="10"
          :max-size="20"
          multiple
          show-description
        />
      </Form.Item>
    </div>
  </Modal>
</template>
