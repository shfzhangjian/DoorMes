<script lang="ts" setup>
import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Modal as AntModal, Button, Empty } from 'ant-design-vue';

import {
  getSrmAttachmentDisplayName,
  getSrmAttachmentPreviewKind,
  getSrmPdfPreviewUrl,
} from './attachmentPreview';

const props = withDefaults(
  defineProps<{
    fileName?: string;
    fileType?: string;
    fileUrl?: string;
    open: boolean;
    zIndex?: number;
  }>(),
  {
    fileName: '',
    fileType: '',
    fileUrl: '',
    zIndex: 6200,
  },
);

const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const previewKind = computed(() => getSrmAttachmentPreviewKind(props));
const title = computed(() => `附件预览｜${getSrmAttachmentDisplayName(props)}`);
const pdfUrl = computed(() => getSrmPdfPreviewUrl(props.fileUrl));

function close() {
  emit('update:open', false);
}

function openInNewWindow() {
  if (!props.fileUrl) {
    return;
  }
  window.open(props.fileUrl, '_blank', 'noopener,noreferrer');
}
</script>

<template>
  <AntModal
    :destroy-on-close="true"
    :footer="null"
    :open="props.open"
    :title="title"
    width="calc(100vw - 32px)"
    wrap-class-name="srm-attachment-preview-modal"
    :z-index="props.zIndex"
    @cancel="close"
  >
    <div class="srm-attachment-preview-modal__toolbar">
      <span>{{ getSrmAttachmentDisplayName(props) }}</span>
      <div class="srm-attachment-preview-modal__actions">
        <Button
          :disabled="!props.fileUrl"
          size="small"
          @click="openInNewWindow"
        >
          <IconifyIcon icon="lucide:external-link" />
          新窗口打开
        </Button>
        <Button size="small" type="primary" @click="close">关闭</Button>
      </div>
    </div>
    <div class="srm-attachment-preview-modal__body">
      <img
        v-if="previewKind === 'image'"
        :alt="getSrmAttachmentDisplayName(props)"
        class="srm-attachment-preview-modal__image"
        :src="props.fileUrl"
      />
      <iframe
        v-else-if="previewKind === 'pdf'"
        class="srm-attachment-preview-modal__pdf"
        :src="pdfUrl"
        title="PDF预览"
      ></iframe>
      <div v-else class="srm-attachment-preview-modal__empty">
        <Empty description="当前附件格式暂不支持在线预览" />
      </div>
    </div>
  </AntModal>
</template>

<style>
.srm-attachment-preview-modal .ant-modal {
  top: 16px;
  max-width: calc(100vw - 32px);
  padding-bottom: 0;
}

.srm-attachment-preview-modal .ant-modal-content {
  display: flex;
  height: calc(100vh - 32px);
  flex-direction: column;
  overflow: hidden;
}

.srm-attachment-preview-modal .ant-modal-body {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  background: #f8fafc;
}

.srm-attachment-preview-modal__toolbar {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 16px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.srm-attachment-preview-modal__toolbar > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-attachment-preview-modal__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.srm-attachment-preview-modal__body {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  align-items: center;
  justify-content: center;
  overflow: auto;
  background: #111827;
}

.srm-attachment-preview-modal__image {
  display: block;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.srm-attachment-preview-modal__pdf {
  width: 100%;
  height: 100%;
  border: 0;
  background: #fff;
}

.srm-attachment-preview-modal__empty {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  background: #fff;
}
</style>
