<script lang="ts" setup>
import type { UploadFile } from 'ant-design-vue';
import type { UploadRequestOption } from 'ant-design-vue/lib/vc-upload/interface';

import type { SrmPreviewableAttachment } from './attachmentPreview';

import type { SrmAttachmentApi } from '#/api/mes/srm/attachment';

import { computed, ref, watch } from 'vue';

import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Empty,
  Input,
  message,
  Modal,
  Select,
  Tag,
  Upload,
} from 'ant-design-vue';

import { uploadFile } from '#/api/infra/file';
import {
  createAttachment,
  deleteAttachment,
  getAttachmentList,
  updateAttachmentVersion,
} from '#/api/mes/srm/attachment';

import { canPreviewSrmAttachment } from './attachmentPreview';
import SrmAttachmentPreviewModal from './SrmAttachmentPreviewModal.vue';

type AttachmentFile = UploadFile & {
  attachment?: SrmAttachmentApi.Attachment;
};

type CategoryOption = {
  disabled?: boolean;
  label: string;
  value: string;
};

const props = withDefaults(
  defineProps<{
    bizId?: number | string;
    bizType?: string;
    categoryDictType?: string;
    categoryOptions?: CategoryOption[];
    defaultCategory?: string;
    modalZIndex?: number;
    mode?: 'create' | 'detail' | 'edit';
    title?: string;
  }>(),
  {
    bizId: undefined,
    bizType: '',
    categoryDictType: 'mes_srm_attachment_category',
    categoryOptions: undefined,
    defaultCategory: '',
    modalZIndex: undefined,
    mode: 'detail',
    title: '附件材料',
  },
);

const fallbackCategoryOptions: CategoryOption[] = [
  { label: '供应商调查表', value: 'SURVEY_FORM' },
  { label: '营业执照', value: 'BUSINESS_LICENSE' },
  { label: '资质证书', value: 'QUALIFICATION_CERTIFICATE' },
  { label: '财务资料', value: 'FINANCIAL_MATERIAL' },
  { label: '合规资料', value: 'COMPLIANCE_MATERIAL' },
  { label: '产品技术要求说明', value: 'TECH_REQUIREMENT' },
  { label: '采购开发难点', value: 'PURCHASE_DIFFICULTY' },
  { label: '研发样品必要性', value: 'RD_SAMPLE_NECESSITY' },
  { label: '样品需求单线下表单', value: 'SAMPLE_REQUEST_OFFLINE_FORM' },
  { label: '其他附件', value: 'OTHER' },
];

const attachments = ref<AttachmentFile[]>([]);
const expandedGroups = ref<Set<string>>(new Set());
const historyMap = ref<Record<string, SrmAttachmentApi.Attachment[]>>({});
const loading = ref(false);
const pendingDeleteIds = ref<number[]>([]);
const previewModalOpen = ref(false);
const previewTarget = ref<SrmPreviewableAttachment>();

const uploadModalOpen = ref(false);
const uploadUploading = ref(false);
const pendingUploadFile = ref<SrmAttachmentApi.Attachment>();
const uploadFileList = ref<UploadFile[]>([]);
const uploadForm = ref({
  attachmentCategory: '',
});

const versionModalOpen = ref(false);
const versionSaving = ref(false);
const versionUploading = ref(false);
const versionTarget = ref<SrmAttachmentApi.Attachment>();
const versionFile = ref<SrmAttachmentApi.Attachment>();
const versionFileList = ref<UploadFile[]>([]);
const versionForm = ref({
  attachmentCategory: '',
  updateDescription: '',
});

const attachmentCategoryOptions = computed<CategoryOption[]>(() => {
  if (props.categoryOptions?.length) {
    return props.categoryOptions;
  }
  const dictOptions = getDictOptions(props.categoryDictType, 'string').map(
    (item) => ({
      label: String(item.label ?? item.value),
      value: String(item.value),
    }),
  );
  if (dictOptions.length > 0) {
    return dictOptions;
  }
  return fallbackCategoryOptions;
});
const attachmentDropdownStyle = computed(() =>
  props.modalZIndex ? { zIndex: props.modalZIndex + 20 } : undefined,
);
const previewModalZIndex = computed(() =>
  props.modalZIndex ? props.modalZIndex + 400 : 6200,
);
const isReadonly = computed(() => props.mode === 'detail');
const canUpload = computed(() => !isReadonly.value && !!props.bizType);
const historyVersionCount = computed(() =>
  Object.values(historyMap.value).reduce((sum, list) => sum + list.length, 0),
);

watch(
  () => [props.bizType, props.bizId, props.mode],
  () => {
    void loadAttachments();
  },
  { immediate: true },
);

async function loadAttachments() {
  pendingDeleteIds.value = [];
  expandedGroups.value = new Set();
  if (!props.bizType || !normalizeId(props.bizId)) {
    attachments.value = [];
    historyMap.value = {};
    return;
  }
  loading.value = true;
  try {
    const list = await getAttachmentList({
      bizId: normalizeId(props.bizId)!,
      bizType: props.bizType,
      includeHistory: true,
    });
    applyAttachmentVersions(list || []);
  } finally {
    loading.value = false;
  }
}

function applyAttachmentVersions(list: SrmAttachmentApi.Attachment[]) {
  const groups = new Map<string, SrmAttachmentApi.Attachment[]>();
  for (const item of list) {
    const key = getAttachmentGroupKey(item);
    groups.set(key, [...(groups.get(key) || []), item]);
  }

  const latestFiles: AttachmentFile[] = [];
  const histories: Record<string, SrmAttachmentApi.Attachment[]> = {};
  for (const [key, group] of groups.entries()) {
    const sorted = group.toSorted(compareAttachmentVersion);
    const latest =
      sorted.find((item) => item.latestVersion !== false) || sorted[0];
    if (!latest) {
      continue;
    }
    latestFiles.push(toUploadFile(latest));
    histories[key] = sorted.filter((item) => item.id !== latest.id);
  }
  attachments.value = latestFiles.toSorted((left, right) =>
    compareAttachmentVersion(left.attachment || {}, right.attachment || {}),
  );
  historyMap.value = histories;
}

function openUploadModal(defaultAttachmentCategory?: string) {
  if (!canUpload.value) {
    message.warning('当前状态不能上传附件');
    return;
  }
  uploadForm.value = {
    attachmentCategory: normalizeCategory(
      defaultAttachmentCategory || resolveDefaultCategory(),
    ),
  };
  pendingUploadFile.value = undefined;
  uploadFileList.value = [];
  uploadModalOpen.value = true;
}

async function handleUploadRequest(info: UploadRequestOption) {
  const file = info.file as File;
  uploadUploading.value = true;
  try {
    const uploaded = await uploadAttachmentFile(file, info);
    pendingUploadFile.value = {
      fileName: file.name,
      fileSize: file.size,
      fileType: resolveAttachmentFileType(file),
      fileUrl: uploaded.fileUrl,
    };
    uploadFileList.value = [
      {
        name: file.name,
        percent: 100,
        size: file.size,
        status: 'done',
        uid: `upload-${Date.now()}`,
        url: uploaded.fileUrl,
      },
    ];
    info.onSuccess?.(uploaded.response);
    message.success('文件上传成功，请确认附件分类');
  } catch (error: any) {
    pendingUploadFile.value = undefined;
    uploadFileList.value = [];
    info.onError?.(error);
    message.error(error?.message || '附件上传失败');
  } finally {
    uploadUploading.value = false;
  }
}

function handleUploadFileRemove() {
  pendingUploadFile.value = undefined;
  uploadFileList.value = [];
  return true;
}

function confirmUpload() {
  const file = pendingUploadFile.value;
  if (!uploadForm.value.attachmentCategory) {
    message.warning('请选择附件分类');
    return;
  }
  if (!file?.fileName || !file.fileUrl) {
    message.warning('请选择并上传附件文件');
    return;
  }
  const attachment: SrmAttachmentApi.Attachment = {
    ...file,
    attachmentCategory: uploadForm.value.attachmentCategory,
    bizType: props.bizType,
    latestVersion: true,
    updateDescription: '首次上传',
    versionNo: 1,
  };
  attachments.value = [
    {
      attachment,
      name: file.fileName,
      percent: 100,
      size: file.fileSize,
      status: 'done',
      uid: `pending-${Date.now()}-${Math.random()}`,
      url: file.fileUrl,
    },
    ...attachments.value,
  ];
  closeUploadModal();
  message.success('附件已加入当前单据，保存单据后正式生效');
}

function closeUploadModal() {
  uploadModalOpen.value = false;
  pendingUploadFile.value = undefined;
  uploadFileList.value = [];
  uploadForm.value = { attachmentCategory: '' };
}

function handleRemove(file: UploadFile) {
  const attachment = (file as AttachmentFile).attachment;
  if (isReadonly.value) {
    return false;
  }
  if (attachment?.id) {
    pendingDeleteIds.value = [
      ...new Set([attachment.id, ...pendingDeleteIds.value]),
    ];
  }
  attachments.value = attachments.value.filter((item) => item.uid !== file.uid);
  return true;
}

function clearAttachments() {
  if (isReadonly.value) {
    return;
  }
  const attachmentIds = attachments.value
    .map((item) => item.attachment?.id)
    .filter((id): id is number => typeof id === 'number');
  pendingDeleteIds.value = [
    ...new Set([...attachmentIds, ...pendingDeleteIds.value]),
  ];
  attachments.value = [];
}

function handleFileNameClick(
  file: AttachmentFile | SrmAttachmentApi.Attachment,
) {
  if (canPreviewAttachment(file)) {
    openPreview(file);
    return;
  }
  openAttachment(file);
}

function openPreview(file: AttachmentFile | SrmAttachmentApi.Attachment) {
  const attachment = toPreviewAttachment(file);
  if (!canPreviewSrmAttachment(attachment)) {
    message.warning('当前附件格式暂不支持在线预览');
    return;
  }
  previewTarget.value = attachment;
  previewModalOpen.value = true;
}

function openAttachment(file: AttachmentFile | SrmAttachmentApi.Attachment) {
  const url = toPreviewAttachment(file).fileUrl;
  if (url) {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
}

function canPreviewAttachment(
  file: AttachmentFile | SrmAttachmentApi.Attachment,
) {
  return canPreviewSrmAttachment(toPreviewAttachment(file));
}

function toPreviewAttachment(
  file: AttachmentFile | SrmAttachmentApi.Attachment,
): SrmPreviewableAttachment {
  if ('uid' in file) {
    return {
      fileName: file.attachment?.fileName || file.name,
      fileType: file.attachment?.fileType || file.type,
      fileUrl: file.attachment?.fileUrl || file.url,
    };
  }
  return {
    fileName: file.fileName,
    fileType: file.fileType,
    fileUrl: file.fileUrl,
  };
}

function openVersionUpdate(file: AttachmentFile) {
  if (isReadonly.value) {
    message.warning('请进入编辑模式后更新附件');
    return;
  }
  const attachment = file.attachment;
  if (!attachment?.id) {
    message.info('请先保存单据，再更新该附件版本');
    return;
  }
  versionTarget.value = attachment;
  versionForm.value = {
    attachmentCategory: normalizeCategory(
      attachment.attachmentCategory || resolveDefaultCategory(),
    ),
    updateDescription: '',
  };
  versionFile.value = undefined;
  versionFileList.value = [];
  versionModalOpen.value = true;
}

async function handleVersionUploadRequest(info: UploadRequestOption) {
  const file = info.file as File;
  versionUploading.value = true;
  try {
    const uploaded = await uploadAttachmentFile(file, info);
    versionFile.value = {
      fileName: file.name,
      fileSize: file.size,
      fileType: resolveAttachmentFileType(file),
      fileUrl: uploaded.fileUrl,
    };
    versionFileList.value = [
      {
        name: file.name,
        percent: 100,
        size: file.size,
        status: 'done',
        uid: `version-${Date.now()}`,
        url: uploaded.fileUrl,
      },
    ];
    info.onSuccess?.(uploaded.response);
    message.success('新版本文件上传成功');
  } catch (error: any) {
    versionFile.value = undefined;
    versionFileList.value = [];
    info.onError?.(error);
    message.error(error?.message || '新版本文件上传失败');
  } finally {
    versionUploading.value = false;
  }
}

function handleVersionFileRemove() {
  versionFile.value = undefined;
  versionFileList.value = [];
  return true;
}

async function confirmVersionUpdate() {
  const target = versionTarget.value;
  const file = versionFile.value;
  const updateDescription = versionForm.value.updateDescription.trim();
  if (!versionForm.value.attachmentCategory) {
    message.warning('请选择附件分类');
    return;
  }
  if (!updateDescription) {
    message.warning('请填写更新说明');
    return;
  }
  if (!target?.id || !file?.fileName || !file.fileUrl) {
    message.warning('请上传一份新版本附件');
    return;
  }

  versionSaving.value = true;
  try {
    await updateAttachmentVersion({
      attachmentCategory: versionForm.value.attachmentCategory,
      fileName: file.fileName,
      fileSize: file.fileSize,
      fileType: file.fileType,
      fileUrl: file.fileUrl,
      sourceAttachmentId: target.id,
      updateDescription,
    });
    message.success(`附件已更新为 V${(target.versionNo || 1) + 1}`);
    closeVersionModal();
    await loadAttachments();
  } finally {
    versionSaving.value = false;
  }
}

function closeVersionModal() {
  versionModalOpen.value = false;
  versionTarget.value = undefined;
  versionFile.value = undefined;
  versionFileList.value = [];
  versionForm.value = { attachmentCategory: '', updateDescription: '' };
}

function toggleHistory(file: AttachmentFile) {
  const attachment = file.attachment;
  if (!attachment) {
    return;
  }
  const key = getAttachmentGroupKey(attachment);
  const next = new Set(expandedGroups.value);
  if (next.has(key)) {
    next.delete(key);
  } else {
    next.add(key);
  }
  expandedGroups.value = next;
}

function isHistoryExpanded(file: AttachmentFile) {
  return file.attachment
    ? expandedGroups.value.has(getAttachmentGroupKey(file.attachment))
    : false;
}

function getAttachmentHistory(file: AttachmentFile) {
  return file.attachment
    ? historyMap.value[getAttachmentGroupKey(file.attachment)] || []
    : [];
}

function getHistoryCount(file: AttachmentFile) {
  return getAttachmentHistory(file).length;
}

async function syncAttachments(params: {
  bizId?: number | string;
  bizType?: string;
}) {
  const bizType = params.bizType || props.bizType;
  const bizId = normalizeId(params.bizId ?? props.bizId);
  if (!bizType || !bizId) {
    return;
  }
  for (const id of pendingDeleteIds.value) {
    await deleteAttachment(id);
  }
  pendingDeleteIds.value = [];

  for (const file of attachments.value) {
    if (file.attachment?.id) {
      continue;
    }
    const attachment = file.attachment;
    if (
      !attachment?.attachmentCategory ||
      !attachment.fileName ||
      !attachment.fileUrl
    ) {
      continue;
    }
    const id = resolveReturnedId(
      await createAttachment({
        ...attachment,
        bizId,
        bizType,
      }),
    );
    if (!id) {
      throw new Error('附件保存失败：后台未返回附件ID');
    }
    file.attachment = {
      ...attachment,
      bizId,
      bizType,
      id,
    };
    file.uid = String(id);
  }
}

async function uploadAttachmentFile(file: File, info: UploadRequestOption) {
  const response = await uploadFile(
    { directory: 'mes/srm/attachment', file },
    (event) => {
      if (!event.total) {
        return;
      }
      info.onProgress?.({
        percent: Math.round((event.loaded / event.total) * 100),
      });
    },
  );
  const fileUrl = resolveUploadedFileUrl(response);
  if (!fileUrl) {
    throw new Error('上传响应缺少文件地址');
  }
  return { fileUrl, response };
}

function toUploadFile(item: SrmAttachmentApi.Attachment): AttachmentFile {
  return {
    attachment: item,
    name: item.fileName || item.fileUrl || '附件',
    size: item.fileSize,
    status: 'done',
    uid: String(item.id || item.fileUrl || Math.random()),
    url: item.fileUrl,
  };
}

function compareAttachmentVersion(
  left: SrmAttachmentApi.Attachment,
  right: SrmAttachmentApi.Attachment,
) {
  const leftTime = getAttachmentTime(left);
  const rightTime = getAttachmentTime(right);
  return (
    rightTime.localeCompare(leftTime) ||
    (right.versionNo || 1) - (left.versionNo || 1) ||
    (right.id || 0) - (left.id || 0)
  );
}

function getAttachmentGroupKey(item: SrmAttachmentApi.Attachment) {
  return (
    item.versionGroupNo || `legacy-${item.id || item.fileUrl || item.fileName}`
  );
}

function getAttachmentTime(item?: SrmAttachmentApi.Attachment) {
  return (
    item?.versionTime ||
    item?.updateTime ||
    item?.uploadTime ||
    item?.createTime ||
    ''
  );
}

function getAttachmentMeta(file: AttachmentFile) {
  return [
    `更新时间：${getAttachmentTime(file.attachment) || '保存后生成'}`,
    `更新人：${file.attachment?.uploadUserName || '当前登记人'}`,
    `文件大小：${formatFileSize(file.attachment?.fileSize)}`,
    `更新说明：${file.attachment?.updateDescription || '首次上传'}`,
  ];
}

function getCategoryLabel(value?: string) {
  return (
    attachmentCategoryOptions.value.find((item) => item.value === value)
      ?.label ||
    fallbackCategoryOptions.find((item) => item.value === value)?.label ||
    value ||
    '未分类'
  );
}

function formatFileSize(size?: number) {
  if (!size || size <= 0) {
    return '-';
  }
  if (size < 1024) {
    return `${size} B`;
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function resolveDefaultCategory() {
  if (props.defaultCategory) {
    return props.defaultCategory;
  }
  if (props.categoryOptions?.length) {
    return props.categoryOptions[0]?.value || '';
  }
  return props.bizType === 'SURVEY' ? 'SURVEY_FORM' : 'OTHER';
}

function normalizeCategory(value = '') {
  const candidate = value;
  if (
    candidate &&
    attachmentCategoryOptions.value.some((item) => item.value === candidate)
  ) {
    return candidate;
  }
  return attachmentCategoryOptions.value[0]?.value || '';
}

function resolveAttachmentPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}

function resolveUploadedFileUrl(uploaded: unknown) {
  if (!uploaded) {
    return '';
  }
  if (typeof uploaded === 'string') {
    return uploaded;
  }
  const data = uploaded as Record<string, any>;
  return data.url || data.data?.url || data.data || data.fileUrl || '';
}

function resolveReturnedId(result: unknown): number | undefined {
  if (typeof result === 'number') {
    return result;
  }
  if (typeof result === 'string') {
    const id = Number(result);
    return Number.isFinite(id) ? id : undefined;
  }
  if (!result || typeof result !== 'object') {
    return undefined;
  }
  const data = result as Record<string, any>;
  return (
    resolveReturnedId(data.id) ||
    resolveReturnedId(data.data) ||
    resolveReturnedId(data.result)
  );
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const id = Number(value);
  return Number.isFinite(id) ? id : undefined;
}

function getFileExtension(name: string) {
  const index = name.lastIndexOf('.');
  return index === -1 ? '' : name.slice(index + 1);
}

function resolveAttachmentFileType(file: File) {
  const extension = getFileExtension(file.name).trim().toLowerCase();
  return extension || file.type.trim().toLowerCase().slice(0, 64);
}

defineExpose({
  loadAttachments,
  openUploadModal,
  syncAttachments,
});
</script>

<template>
  <section class="srm-attachment-panel">
    <div class="detail-list-head">
      <div class="detail-list-title">
        <strong>{{ props.title }}</strong>
      </div>
      <div class="srm-attachment-panel__head-actions">
        <span class="srm-attachment-panel__summary">
          当前 {{ attachments.length }} 个，历史 {{ historyVersionCount }} 版
        </span>
        <Button
          v-if="!isReadonly"
          :disabled="!canUpload"
          size="small"
          type="primary"
          @click="openUploadModal"
        >
          <IconifyIcon icon="lucide:upload" />
          上传附件
        </Button>
      </div>
    </div>

    <div class="srm-attachment-panel__current-list">
      <div v-if="loading" class="srm-attachment-panel__empty">
        附件加载中...
      </div>
      <Empty v-else-if="attachments.length === 0" description="暂无附件" />
      <article
        v-for="file in attachments"
        v-else
        :key="file.uid"
        class="srm-attachment-panel__item"
      >
        <div class="srm-attachment-panel__item-main">
          <div class="srm-attachment-panel__file-info">
            <IconifyIcon icon="lucide:file-text" />
            <a
              class="srm-attachment-panel__file-name"
              @click="handleFileNameClick(file)"
            >
              {{ file.name }}
            </a>
            <Tag color="blue">
              {{ getCategoryLabel(file.attachment?.attachmentCategory) }}
            </Tag>
            <Tag>V{{ file.attachment?.versionNo || 1 }}</Tag>
          </div>
          <div class="srm-attachment-panel__item-actions">
            <Button
              v-if="canPreviewAttachment(file)"
              size="small"
              type="link"
              @click="openPreview(file)"
            >
              <IconifyIcon icon="lucide:eye" />
              预览
            </Button>
            <Button size="small" type="link" @click="openAttachment(file)">
              <IconifyIcon icon="lucide:external-link" />
              打开
            </Button>
            <Button
              v-if="!isReadonly && file.attachment?.id"
              size="small"
              type="link"
              @click="openVersionUpdate(file)"
            >
              <IconifyIcon icon="lucide:refresh-cw" />
              更新附件
            </Button>
            <Button
              v-if="!isReadonly"
              danger
              size="small"
              type="link"
              @click="handleRemove(file)"
            >
              移除
            </Button>
          </div>
        </div>
        <div class="srm-attachment-panel__meta">
          <span v-for="meta in getAttachmentMeta(file)" :key="meta">
            {{ meta }}
          </span>
        </div>
        <button
          v-if="getHistoryCount(file) > 0"
          class="srm-attachment-panel__history-toggle"
          type="button"
          @click="toggleHistory(file)"
        >
          <IconifyIcon
            :icon="
              isHistoryExpanded(file)
                ? 'lucide:chevron-up'
                : 'lucide:chevron-down'
            "
          />
          {{ isHistoryExpanded(file) ? '收起' : '展开' }}历史附件（{{
            getHistoryCount(file)
          }}）
        </button>
        <div
          v-if="isHistoryExpanded(file)"
          class="srm-attachment-panel__history-list"
        >
          <div
            v-for="history in getAttachmentHistory(file)"
            :key="history.id || history.fileUrl"
            class="srm-attachment-panel__history-item"
          >
            <div class="srm-attachment-panel__history-head">
              <Tag>V{{ history.versionNo || 1 }}</Tag>
              <a @click="handleFileNameClick(history)">
                {{ history.fileName || '历史附件' }}
              </a>
              <span>{{ getCategoryLabel(history.attachmentCategory) }}</span>
              <span>{{ getAttachmentTime(history) || '-' }}</span>
              <span>{{ history.uploadUserName || '-' }}</span>
              <Button
                v-if="canPreviewAttachment(history)"
                size="small"
                type="link"
                @click="openPreview(history)"
              >
                预览
              </Button>
              <Button size="small" type="link" @click="openAttachment(history)">
                打开
              </Button>
            </div>
            <div class="srm-attachment-panel__history-log">
              更新说明：{{ history.updateDescription || '历史附件初始化' }}
            </div>
          </div>
        </div>
      </article>
    </div>

    <div
      v-if="!isReadonly && attachments.length > 0"
      class="srm-attachment-panel__actions"
    >
      <Button size="small" type="link" @click="clearAttachments">
        移除当前全部附件
      </Button>
    </div>
  </section>

  <SrmAttachmentPreviewModal
    v-model:open="previewModalOpen"
    :file-name="previewTarget?.fileName"
    :file-type="previewTarget?.fileType"
    :file-url="previewTarget?.fileUrl"
    :z-index="previewModalZIndex"
  />

  <Modal
    v-model:open="uploadModalOpen"
    :confirm-loading="uploadUploading"
    :mask-closable="false"
    ok-text="确认上传"
    title="上传附件"
    :width="680"
    :z-index="props.modalZIndex"
    @cancel="closeUploadModal"
    @ok="confirmUpload"
  >
    <div class="srm-attachment-upload-form">
      <div class="srm-attachment-upload-form__hint">
        请选择附件分类并上传文件，确认后加入当前单据，保存单据时正式生成 V1。
      </div>
      <label class="srm-attachment-upload-form__field">
        <span>附件分类</span>
        <Select
          v-model:value="uploadForm.attachmentCategory"
          class="srm-attachment-upload-form__control"
          :dropdown-style="attachmentDropdownStyle"
          :get-popup-container="resolveAttachmentPopupContainer"
          :options="attachmentCategoryOptions"
          placeholder="请选择附件分类"
          popup-class-name="srm-attachment-panel__select-popup"
        />
      </label>
      <div class="srm-attachment-upload-form__field">
        <span>附件文件</span>
        <Upload
          class="srm-attachment-upload-form__upload"
          :custom-request="handleUploadRequest"
          :disabled="uploadUploading"
          :file-list="uploadFileList"
          :max-count="1"
          @remove="handleUploadFileRemove"
        >
          <Button block :loading="uploadUploading">
            <IconifyIcon icon="lucide:upload" />
            选择并上传附件文件
          </Button>
        </Upload>
      </div>
    </div>
  </Modal>

  <Modal
    v-model:open="versionModalOpen"
    :confirm-loading="versionSaving"
    :mask-closable="false"
    ok-text="确认更新"
    title="更新附件版本"
    :width="680"
    :z-index="props.modalZIndex"
    @cancel="closeVersionModal"
    @ok="confirmVersionUpdate"
  >
    <div class="srm-attachment-version-form">
      <div class="srm-attachment-version-form__hint">
        当前版本 V{{ versionTarget?.versionNo || 1 }}，确认后生成 V{{
          (versionTarget?.versionNo || 1) + 1
        }}，原版本保留在历史记录中。
      </div>
      <label class="srm-attachment-version-form__field">
        <span>附件分类</span>
        <Select
          v-model:value="versionForm.attachmentCategory"
          class="srm-attachment-version-form__control"
          :dropdown-style="attachmentDropdownStyle"
          :get-popup-container="resolveAttachmentPopupContainer"
          :options="attachmentCategoryOptions"
          placeholder="请选择附件分类"
          popup-class-name="srm-attachment-panel__select-popup"
        />
      </label>
      <label class="srm-attachment-version-form__field">
        <span>更新说明</span>
        <Input.TextArea
          v-model:value="versionForm.updateDescription"
          class="srm-attachment-version-form__control"
          :maxlength="500"
          placeholder="请说明本次附件更新内容"
          :rows="4"
          show-count
        />
      </label>
      <div class="srm-attachment-version-form__field">
        <span>新版本附件</span>
        <Upload
          class="srm-attachment-version-form__upload"
          :custom-request="handleVersionUploadRequest"
          :disabled="versionUploading"
          :file-list="versionFileList"
          :max-count="1"
          @remove="handleVersionFileRemove"
        >
          <Button block :loading="versionUploading">
            <IconifyIcon icon="lucide:upload" />
            选择并上传新版本附件
          </Button>
        </Upload>
      </div>
    </div>
  </Modal>
</template>

<style>
.srm-attachment-panel__head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.srm-attachment-upload-form__control,
.srm-attachment-version-form__control {
  width: 100% !important;
}

.srm-attachment-panel__current-list {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.srm-attachment-panel__item {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #dbe3ee;
  border-radius: 4px;
  background: #fff;
}

.srm-attachment-panel__item-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 9px 12px;
}

.srm-attachment-panel__file-info,
.srm-attachment-panel__item-actions,
.srm-attachment-panel__history-head {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.srm-attachment-panel__file-info > svg {
  flex: 0 0 auto;
  color: #2563eb;
}

.srm-attachment-panel__file-name {
  min-width: 80px;
  overflow: hidden;
  flex: 0 1 auto;
  color: #2563eb;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-attachment-panel__item-actions {
  flex-wrap: nowrap;
}

.srm-attachment-panel__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 20px;
  border-top: 1px dashed #e2e8f0;
  padding: 8px 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 20px;
}

.srm-attachment-panel__history-toggle {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 4px;
  border: 0;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 7px 12px;
  color: #475569;
  cursor: pointer;
  font-size: 12px;
}

.srm-attachment-panel__history-list {
  display: grid;
  gap: 8px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 10px 12px;
}

.srm-attachment-panel__history-item {
  border-left: 3px solid #cbd5e1;
  background: #fff;
  padding: 7px 10px;
}

.srm-attachment-panel__history-head {
  flex-wrap: wrap;
  color: #64748b;
  font-size: 12px;
}

.srm-attachment-panel__history-head a {
  min-width: 100px;
  overflow: hidden;
  flex: 1;
  color: #2563eb;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-attachment-panel__history-log {
  margin-top: 5px;
  color: #475569;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-word;
}

.srm-attachment-upload-form,
.srm-attachment-version-form {
  display: grid;
  gap: 16px;
  padding-top: 4px;
}

.srm-attachment-upload-form__hint,
.srm-attachment-version-form__hint {
  border: 1px solid #bfdbfe;
  border-radius: 4px;
  background: #eff6ff;
  padding: 9px 12px;
  color: #1e40af;
  font-size: 13px;
}

.srm-attachment-upload-form__field,
.srm-attachment-version-form__field {
  display: grid;
  width: 100%;
  gap: 7px;
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.srm-attachment-upload-form__upload,
.srm-attachment-upload-form__upload .ant-upload-select,
.srm-attachment-upload-form__upload .ant-btn,
.srm-attachment-version-form__upload,
.srm-attachment-version-form__upload .ant-upload-select,
.srm-attachment-version-form__upload .ant-btn {
  display: block;
  width: 100%;
}

@media (max-width: 760px) {
  .srm-attachment-panel__item-main {
    grid-template-columns: minmax(0, 1fr);
  }

  .srm-attachment-panel__head-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .srm-attachment-panel__item-actions {
    justify-content: flex-end;
  }
}
</style>
