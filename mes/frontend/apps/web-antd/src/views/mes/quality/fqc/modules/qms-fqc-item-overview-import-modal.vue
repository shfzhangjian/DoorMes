<script lang="ts" setup>
import type { FileType } from 'ant-design-vue/es/upload/interface';

import type { MesFqcApi } from '#/api/mes/quality/fqc';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Checkbox,
  message,
  Modal,
  Tag,
  Upload,
} from 'ant-design-vue';

import {
  confirmFqcItemImport,
  previewFqcItemImport,
} from '#/api/mes/quality/fqc';

defineOptions({ name: 'QmsFqcItemOverviewImportModal' });

const props = defineProps<{
  fqcId?: number;
  open: boolean;
  record?: MesFqcApi.FqcRecord | null;
}>();

const emit = defineEmits<{
  success: [record?: MesFqcApi.FqcRecord];
  'update:open': [open: boolean];
}>();

const selectedFile = ref<File>();
const previewResp = ref<MesFqcApi.FqcImportResp>();
const loading = ref(false);
const allowOverwrite = ref(false);

const canConfirm = computed(
  () =>
    !!selectedFile.value &&
    !!previewResp.value &&
    (previewResp.value.failureCount || 0) === 0 &&
    (previewResp.value.successCount || 0) > 0 &&
    ((previewResp.value.warningCount || 0) === 0 || allowOverwrite.value),
);

watch(
  () => props.open,
  (open) => {
    if (!open) resetState();
  },
);

function resetState() {
  selectedFile.value = undefined;
  previewResp.value = undefined;
  allowOverwrite.value = false;
  loading.value = false;
}

async function runPreview(file: File) {
  if (!props.fqcId) {
    message.warning('请选择成品检验单');
    return;
  }
  loading.value = true;
  try {
    previewResp.value = await previewFqcItemImport(props.fqcId, file);
    allowOverwrite.value = false;
  } finally {
    loading.value = false;
  }
}

function beforeUpload(file: FileType) {
  selectedFile.value = file as File;
  runPreview(file as File);
  return false;
}

function removeFile() {
  selectedFile.value = undefined;
  previewResp.value = undefined;
  allowOverwrite.value = false;
}

function handleClose() {
  emit('update:open', false);
}

async function handleConfirm() {
  if (!props.fqcId || !selectedFile.value) return;
  loading.value = true;
  try {
    const resp = await confirmFqcItemImport(
      props.fqcId,
      selectedFile.value,
      allowOverwrite.value,
    );
    previewResp.value = resp;
    if ((resp.failureCount || 0) > 0 || !resp.record) {
      message.error('导入未完成，请查看校验结果');
      return;
    }
    message.success(`导入成功，写入 ${resp.successCount || 0} 行`);
    emit('success', resp.record);
    handleClose();
  } finally {
    loading.value = false;
  }
}

function statusColor(status?: string) {
  return status === 'SUCCESS' ? 'success' : 'error';
}
</script>

<template>
  <Modal
    :open="open"
    title="导入检验项明细数据"
    width="760px"
    :confirm-loading="loading"
    :ok-button-props="{ disabled: !canConfirm }"
    ok-text="确认导入"
    @cancel="handleClose"
    @ok="handleConfirm"
  >
    <div class="space-y-4">
      <Alert
        show-icon
        type="info"
        message="请使用当前 Fqc 单下载的模板导入"
        :description="`当前单据：${record?.fqcNo || '-'}`"
      />

      <div class="rounded border border-dashed border-slate-300 p-4">
        <Upload
          :max-count="1"
          accept=".xlsx,.xls"
          :before-upload="beforeUpload"
          @remove="removeFile"
        >
          <Button :loading="loading">
            <IconifyIcon icon="lucide:upload" class="mr-1" />
            选择 Excel 文件
          </Button>
        </Upload>
      </div>

      <div v-if="previewResp" class="space-y-3">
        <div class="grid grid-cols-5 gap-2 text-center text-xs">
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">状态</div>
            <Tag :color="statusColor(previewResp.status)" class="!mt-2">
              {{ previewResp.status === 'SUCCESS' ? '通过' : '未通过' }}
            </Tag>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">总行数</div>
            <div class="mt-2 font-mono text-base font-bold">
              {{ previewResp.totalCount || 0 }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">可导入</div>
            <div class="mt-2 font-mono text-base font-bold text-green-600">
              {{ previewResp.successCount || 0 }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">错误</div>
            <div class="mt-2 font-mono text-base font-bold text-red-600">
              {{ previewResp.failureCount || 0 }}
            </div>
          </div>
          <div class="rounded border bg-slate-50 p-3">
            <div class="text-slate-500">警告</div>
            <div class="mt-2 font-mono text-base font-bold text-amber-600">
              {{ previewResp.warningCount || 0 }}
            </div>
          </div>
        </div>

        <Checkbox
          v-if="(previewResp.warningCount || 0) > 0"
          v-model:checked="allowOverwrite"
        >
          已确认覆盖已有样本值
        </Checkbox>

        <div class="max-h-56 overflow-auto rounded border">
          <div
            v-for="(item, index) in previewResp.messages || []"
            :key="`${index}-${item}`"
            class="border-b px-3 py-2 text-xs last:border-b-0"
            :class="
              item.includes('失败') ||
              item.includes('不匹配') ||
              item.includes('不存在') ||
              item.includes('必须')
                ? 'text-red-600'
                : 'text-slate-600'
            "
          >
            {{ item }}
          </div>
          <div
            v-if="(previewResp.messages || []).length === 0"
            class="px-3 py-6 text-center text-xs text-slate-400"
          >
            暂无校验提示
          </div>
        </div>
      </div>
    </div>
  </Modal>
</template>
