<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesWetPoreSelfCheckApi } from '#/api/mes/quality/wet-pore-self-check';
import type { UploadProps } from 'ant-design-vue';

import { nextTick, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Image, message, Modal, Tag, Upload } from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { uploadFile } from '#/api/infra/file';
import {
  getWetPoreSelfCheckPage,
  updateWetPoreSelfCheckPhotos,
} from '#/api/mes/quality/wet-pore-self-check';

import {
  optionMeta,
  PoreSelfCheckImageStatusOptions,
  PoreSelfCheckResultOptions,
  useGridColumns,
  useGridFormSchema,
} from './data';

defineOptions({ name: 'MesPoreSelfCheck' });

const MAX_PORE_PHOTO_COUNT = 10;
const uploadingPhotoCountByRow = ref<Record<string, number>>({});
const rowPhotoOverrides = ref<Record<string, string[]>>({});
const previewOpen = ref(false);
const previewUrls = ref<string[]>([]);
const previewIndex = ref(0);
const rowPhotoSaveQueues = new Map<string, Promise<void>>();

const searchFormFields = [
  'keyword',
  'planNo',
  'productModel',
  'motherBatchNo',
  'productMaterialCode',
  'imageStatus',
  'selfCheckTime',
];

function getCollapsedKeepCount() {
  if (window.innerWidth < 768) return 1;
  if (window.innerWidth < 1024) return 2;
  return 3;
}

function buildSearchFormSchema(collapsed = false) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  return useGridFormSchema().map((item) => ({
    ...item,
    hide: item.hide || (collapsed && !keepFields.has(item.fieldName)),
  }));
}

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    handleCollapsedChange: handleSearchCollapsedChange,
    schema: buildSearchFormSchema(true),
  },
  gridOptions: {
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    rowConfig: { isHover: true, keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getWetPoreSelfCheckPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
          }),
      },
    },
  } as VxeTableGridOptions<MesWetPoreSelfCheckApi.Record>,
});

function handleSearchCollapsedChange(collapsed: boolean) {
  const keepFields = new Set(
    searchFormFields.slice(0, collapsed ? getCollapsedKeepCount() : undefined),
  );
  gridApi.formApi.updateSchema(
    searchFormFields.map((fieldName) => ({
      fieldName,
      hide: collapsed && !keepFields.has(fieldName),
    })),
  );
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

function resolveUploadUrl(uploadResult: any) {
  return String(
    uploadResult?.url ||
      uploadResult?.data?.url ||
      uploadResult?.path ||
      uploadResult ||
      '',
  );
}

function getErrorMessage(error: unknown) {
  const data = (error as any)?.response?.data || (error as any)?.data || {};
  return String(
    data.msg ||
      data.message ||
      (error as any)?.msg ||
      (error as any)?.message ||
      '',
  );
}

function normalizePhotoUrls(...values: unknown[]): string[] {
  const urls: string[] = [];
  values.forEach((value) => {
    if (Array.isArray(value)) {
      urls.push(...normalizePhotoUrls(...value));
      return;
    }
    const url = String(value || '').trim();
    if (url) urls.push(url);
  });
  return [...new Set(urls)].slice(0, MAX_PORE_PHOTO_COUNT);
}

function getRowPhotos(row: MesWetPoreSelfCheckApi.Record) {
  return (
    rowPhotoOverrides.value[row.id] ||
    normalizePhotoUrls(row.photos, row.photo)
  );
}

function updateRowPhotos(row: MesWetPoreSelfCheckApi.Record, photos: string[]) {
  rowPhotoOverrides.value = {
    ...rowPhotoOverrides.value,
    [row.id]: photos,
  };
}

function getRowPhotoUploadingCount(row: MesWetPoreSelfCheckApi.Record) {
  return uploadingPhotoCountByRow.value[row.id] || 0;
}

function isRowPhotoLimitReached(row: MesWetPoreSelfCheckApi.Record) {
  return (
    getRowPhotos(row).length + getRowPhotoUploadingCount(row) >=
    MAX_PORE_PHOTO_COUNT
  );
}

function changeRowPhotoUploadingCount(
  row: MesWetPoreSelfCheckApi.Record,
  change: number,
) {
  const next = Math.max(0, getRowPhotoUploadingCount(row) + change);
  uploadingPhotoCountByRow.value = {
    ...uploadingPhotoCountByRow.value,
    [row.id]: next,
  };
}

function openPreview(photos?: string[], initialIndex = 0) {
  const urls = normalizePhotoUrls(photos);
  if (urls.length === 0) return;
  previewUrls.value = urls;
  previewIndex.value = Math.min(Math.max(0, initialIndex), urls.length - 1);
  previewOpen.value = true;
}

function showPreviousPhoto() {
  if (previewIndex.value > 0) previewIndex.value -= 1;
}

function showNextPhoto() {
  if (previewIndex.value < previewUrls.value.length - 1) {
    previewIndex.value += 1;
  }
}

function queuePhotoSave(
  row: MesWetPoreSelfCheckApi.Record,
  photos: string[],
) {
  const previous = rowPhotoSaveQueues.get(row.id) || Promise.resolve();
  const next = previous
    .catch(() => undefined)
    .then(async () => {
      await updateWetPoreSelfCheckPhotos({
        clientKey: row.clientKey,
        photos,
        recordIndex: row.recordIndex,
        stationRecordId: row.stationRecordId,
      });
    });
  rowPhotoSaveQueues.set(row.id, next);
  return next;
}

const beforePhotoUpload: (
  row: MesWetPoreSelfCheckApi.Record,
) => UploadProps['beforeUpload'] = (row) => async (file) => {
  const rawFile = file as File;
  const isImage = !rawFile.type || rawFile.type.startsWith('image/');
  if (!isImage) {
    Modal.warning({
      content: '泡孔图片仅支持上传图片文件。',
      title: '文件类型不支持',
    });
    return false;
  }
  if (rawFile.size > 10 * 1024 * 1024) {
    Modal.warning({
      content: '泡孔图片大小不能超过 10MB。',
      title: '文件过大',
    });
    return false;
  }
  if (isRowPhotoLimitReached(row)) {
    Modal.warning({
      content: `每条泡孔自检记录最多上传 ${MAX_PORE_PHOTO_COUNT} 张图片。`,
      title: '已达到图片数量上限',
    });
    return false;
  }

  changeRowPhotoUploadingCount(row, 1);
  try {
    const uploadResult = await uploadFile({
      directory: 'wet-pore-self-check',
      file: rawFile,
    });
    const url = resolveUploadUrl(uploadResult);
    if (!url) {
      throw new Error('上传成功但未返回图片地址');
    }
    const photos = normalizePhotoUrls(getRowPhotos(row), url);
    updateRowPhotos(row, photos);
    await queuePhotoSave(row, photos);
    message.success(`泡孔图片已上传（${photos.length}/${MAX_PORE_PHOTO_COUNT}）`);
    await gridApi.query();
  } catch (error) {
    Modal.warning({
      content: getErrorMessage(error) || '泡孔图片上传失败，请稍后重试。',
      title: '上传失败',
    });
  } finally {
    changeRowPhotoUploadingCount(row, -1);
  }
  return false;
};

function clearPhoto(row: MesWetPoreSelfCheckApi.Record) {
  Modal.confirm({
    content: '清除后该泡孔自检记录将移除全部已上传图片。',
    onOk: async () => {
      updateRowPhotos(row, []);
      await queuePhotoSave(row, []);
      message.success('泡孔图片已全部清除');
      await gridApi.query();
    },
    title: '确认清除全部泡孔图片',
  });
}
</script>

<template>
  <Page auto-content-height>
    <Grid table-title="泡孔自检">
      <template #planNo="{ row }">
        <span class="font-mono font-semibold text-blue-700">
          {{ row.planNo || '-' }}
        </span>
      </template>

      <template #selfCheckResult="{ row }">
        <Tag
          :color="
            optionMeta(PoreSelfCheckResultOptions, row.selfCheckResult).color
          "
          class="!m-0"
        >
          {{ optionMeta(PoreSelfCheckResultOptions, row.selfCheckResult).label }}
        </Tag>
      </template>

      <template #imageStatus="{ row }">
        <Tag
          :color="
            optionMeta(PoreSelfCheckImageStatusOptions, row.imageStatus).color
          "
          class="!m-0"
        >
          {{ optionMeta(PoreSelfCheckImageStatusOptions, row.imageStatus).label }}
        </Tag>
      </template>

      <template #photo="{ row }">
        <button
          v-if="getRowPhotos(row).length"
          class="pore-photo-thumb"
          type="button"
          @click="openPreview(getRowPhotos(row))"
        >
          <img :src="getRowPhotos(row)[0]" alt="泡孔图片" />
          <span>{{ getRowPhotos(row).length }} 张</span>
        </button>
        <span v-else class="text-gray-400">-</span>
      </template>

      <template #actions="{ row }">
        <div class="pore-action-bar">
          <Upload
            accept="image/*"
            multiple
            :before-upload="beforePhotoUpload(row)"
            :show-upload-list="false"
            :disabled="isRowPhotoLimitReached(row)"
          >
            <Button
              size="small"
              type="primary"
              :disabled="isRowPhotoLimitReached(row)"
              :loading="getRowPhotoUploadingCount(row) > 0"
            >
              <IconifyIcon icon="lucide:upload" class="mr-1" />
              {{
                getRowPhotos(row).length
                  ? `补充图片（${getRowPhotos(row).length}/${MAX_PORE_PHOTO_COUNT}）`
                  : '上传图片'
              }}
            </Button>
          </Upload>
          <Button
            v-if="getRowPhotos(row).length"
            size="small"
            @click="openPreview(getRowPhotos(row))"
          >
            <IconifyIcon icon="lucide:eye" class="mr-1" />
            预览
          </Button>
          <Button
            v-if="getRowPhotos(row).length"
            danger
            size="small"
            :disabled="getRowPhotoUploadingCount(row) > 0"
            @click="clearPhoto(row)"
          >
            <IconifyIcon icon="lucide:trash-2" class="mr-1" />
            清除
          </Button>
        </div>
      </template>
    </Grid>

    <Modal
      v-model:open="previewOpen"
      :footer="null"
      title="泡孔图片预览"
      width="860px"
    >
      <div class="pore-preview">
        <Image :src="previewUrls[previewIndex]" />
      </div>
      <div v-if="previewUrls.length > 1" class="pore-preview-toolbar">
        <Button
          size="small"
          :disabled="previewIndex === 0"
          @click="showPreviousPhoto"
        >
          上一张
        </Button>
        <span>{{ previewIndex + 1 }} / {{ previewUrls.length }}</span>
        <Button
          size="small"
          :disabled="previewIndex >= previewUrls.length - 1"
          @click="showNextPhoto"
        >
          下一张
        </Button>
      </div>
      <div v-if="previewUrls.length > 1" class="pore-preview-thumbs">
        <button
          v-for="(photo, index) in previewUrls"
          :key="photo"
          :class="{ 'is-active': index === previewIndex }"
          type="button"
          @click="previewIndex = index"
        >
          <img :src="photo" alt="泡孔图片缩略图" />
        </button>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.pore-action-bar {
  align-items: center;
  display: flex;
  gap: 6px;
  justify-content: center;
}

.pore-photo-thumb {
  align-items: center;
  background: #f8fafc;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  gap: 4px;
  height: 52px;
  overflow: hidden;
  padding: 0;
  width: 88px;
}

.pore-photo-thumb img {
  display: block;
  height: 44px;
  margin-left: 3px;
  object-fit: cover;
  width: 56px;
}

.pore-photo-thumb span {
  color: #2563eb;
  font-size: 12px;
}

.pore-preview {
  align-items: center;
  background: #f8fafc;
  border-radius: 6px;
  display: flex;
  justify-content: center;
  min-height: 420px;
  padding: 12px;
}

.pore-preview-toolbar {
  align-items: center;
  color: #475569;
  display: flex;
  font-size: 13px;
  gap: 12px;
  justify-content: center;
  margin-top: 10px;
}

.pore-preview-thumbs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.pore-preview-thumbs button {
  background: #f8fafc;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  cursor: pointer;
  flex: 0 0 auto;
  height: 48px;
  overflow: hidden;
  padding: 0;
  width: 64px;
}

.pore-preview-thumbs button.is-active {
  border-color: #2563eb;
  box-shadow: 0 0 0 1px #2563eb;
}

.pore-preview-thumbs img {
  display: block;
  height: 100%;
  object-fit: cover;
  width: 100%;
}
</style>
