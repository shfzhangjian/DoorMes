<script lang="ts" setup>
import type { UploadProps } from 'ant-design-vue';

import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Alert, message, Modal, Radio, UploadDragger } from 'ant-design-vue';

defineOptions({ name: 'QmsFqcSheetImportModal' });

const props = defineProps<{
  open: boolean;
  record: MesFqcApi.FqcRecord | null;
  template?: MesFqcApi.FqcSheetTemplate;
}>();

const emit = defineEmits<{
  import: [file: File, mode: string];
  'update:open': [value: boolean];
}>();

const importMode = ref('OVERWRITE');

const canImport = computed(() => !!props.record?.id && !!props.template?.id);
const templateTitle = computed(() => {
  if (!props.template) return '-';
  return `${props.template.templateCode} / ${props.template.templateVersion}`;
});

const beforeUpload: UploadProps['beforeUpload'] = async (file) => {
  if (!canImport.value) {
    message.warning('请先选择 Fqc 原始记录表模板');
    return false;
  }
  emit('import', file as File, importMode.value);
  return false;
};

function handleCancel() {
  emit('update:open', false);
}
</script>

<template>
  <Modal
    :open="open"
    title="导入 Fqc 原始记录表"
    width="720px"
    :footer="false"
    centered
    @cancel="handleCancel"
  >
    <div class="flex flex-col gap-4">
      <Alert
        type="info"
        show-icon
        message="导入会按当前表格模板校验 Sheet、区块、字段和样本量，最终计算结果以后端重算为准。"
      />

      <div class="rounded border border-slate-200 bg-slate-50 p-3 text-sm">
        <div class="grid grid-cols-[96px_minmax(0,1fr)] gap-x-3 gap-y-2">
          <span class="font-bold text-slate-500">当前FQC单</span>
          <span class="font-mono text-slate-800">{{
            record?.fqcNo || '-'
          }}</span>
          <span class="font-bold text-slate-500">当前模板</span>
          <span class="font-mono text-indigo-700">{{ templateTitle }}</span>
          <span class="font-bold text-slate-500">Sheet</span>
          <span>{{ template?.sheetName || '-' }}</span>
        </div>
      </div>

      <div>
        <div class="mb-2 text-sm font-bold text-slate-700">导入模式</div>
        <Radio.Group v-model:value="importMode" button-style="solid">
          <Radio.Button value="OVERWRITE">覆盖当前草稿</Radio.Button>
          <Radio.Button value="FILL_EMPTY">仅填空值</Radio.Button>
        </Radio.Group>
      </div>

      <UploadDragger
        accept=".xlsx"
        :before-upload="beforeUpload"
        :disabled="!canImport"
        :show-upload-list="false"
      >
        <p class="ant-upload-drag-icon">
          <IconifyIcon
            icon="lucide:file-spreadsheet"
            class="text-4xl text-green-600"
          />
        </p>
        <p class="ant-upload-text">点击或拖拽 .xlsx 原始记录表到此处</p>
        <p class="ant-upload-hint">
          请确认文件与当前模板编码、版本和样本结构一致。
        </p>
      </UploadDragger>
    </div>
  </Modal>
</template>
