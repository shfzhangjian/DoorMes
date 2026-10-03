<script lang="ts" setup>
import type { FileType } from 'ant-design-vue/es/upload/interface';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Checkbox,
  Modal,
  Tag,
  Upload,
  message,
} from 'ant-design-vue';

import {
  confirmStationFormConfigPackageImport,
  previewStationFormConfigPackageImport,
} from '#/api/mes/hc/stationform';

defineOptions({ name: 'HcStationFormConfigPackageImportModal' });

const props = defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  success: [];
  'update:open': [open: boolean];
}>();

const loading = ref(false);
const overwriteExisting = ref(false);
const selectedFile = ref<File>();
const previewResp = ref<MesHcStationFormApi.StationFormConfigPackageResp>();

const uploadFileList = computed(() =>
  selectedFile.value
    ? [
        {
          name: selectedFile.value.name,
          status: 'done' as const,
          uid: 'station-form-config-package',
        },
      ]
    : [],
);

const forms = computed(() => previewResp.value?.forms || []);
const hasUpdate = computed(() => (previewResp.value?.updateCount || 0) > 0);
const canConfirm = computed(
  () =>
    !!previewResp.value &&
    (previewResp.value.failureCount || 0) === 0 &&
    (!hasUpdate.value || overwriteExisting.value),
);

watch(
  () => props.open,
  (open) => {
    if (open) resetState();
  },
);

function resetState() {
  loading.value = false;
  overwriteExisting.value = false;
  selectedFile.value = undefined;
  previewResp.value = undefined;
}

async function runPreview(file: File) {
  loading.value = true;
  try {
    const resp = await previewStationFormConfigPackageImport(file);
    previewResp.value = resp;
    overwriteExisting.value = false;
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
  overwriteExisting.value = false;
  return true;
}

function handleClose() {
  emit('update:open', false);
}

async function handleConfirm() {
  if (!canConfirm.value || !previewResp.value) return;
  loading.value = true;
  try {
    const resp = await confirmStationFormConfigPackageImport({
      configPackage: previewResp.value,
      overwriteExisting: overwriteExisting.value,
    });
    previewResp.value = resp;
    if ((resp.failureCount || 0) > 0 || resp.status === 'ERROR') {
      message.error('配置包导入未完成，请查看预检结果');
      return;
    }
    message.success('配置包导入成功');
    emit('success');
    handleClose();
  } finally {
    loading.value = false;
  }
}

function actionLabel(action?: string) {
  return (
    {
      INVALID: '异常',
      NEW: '新增',
      SAME: '无变化',
      UPDATE: '覆盖更新',
    } as Record<string, string>
  )[action || ''] || action || '-';
}

function actionColor(action?: string) {
  return (
    {
      INVALID: 'error',
      NEW: 'success',
      SAME: 'default',
      UPDATE: 'warning',
    } as Record<string, string>
  )[action || ''] || 'default';
}

function isErrorMessage(text: string) {
  return ['失败', '错误', '为空', '不正确', '缺少'].some((keyword) => text.includes(keyword));
}
</script>

<template>
  <Modal
    :open="open"
    title="导入动态表单配置包"
    width="1080px"
    :confirm-loading="loading"
    :ok-button-props="{ disabled: !canConfirm }"
    ok-text="确认导入"
    @cancel="handleClose"
    @ok="handleConfirm"
  >
    <div class="station-form-package-import">
      <Alert
        show-icon
        type="info"
        message="配置包用于开发库与客户库之间迁移动态表单配置"
        description="导入只处理动态表单基础信息、设计器布局、业务绑定和明细配置，不会覆盖报工记录。"
      />

      <Upload
        accept=".json,application/json"
        :before-upload="beforeUpload"
        :file-list="uploadFileList"
        :max-count="1"
        @remove="removeFile"
      >
        <Button :loading="loading">
          <IconifyIcon icon="lucide:upload" class="mr-1" />
          选择配置包 JSON
        </Button>
      </Upload>

      <Alert
        v-if="hasUpdate"
        show-icon
        type="warning"
        message="配置包包含同编码且内容不同的表单"
        description="确认导入前必须勾选覆盖，否则不会写入客户库，避免误覆盖现场维护过的配置。"
      />

      <Checkbox v-if="hasUpdate" v-model:checked="overwriteExisting">
        覆盖同编码配置
      </Checkbox>

      <div v-if="previewResp" class="station-form-package-import__summary">
        <div class="station-form-package-import__metric">
          <span>表单总数</span>
          <strong>{{ previewResp.totalCount || 0 }}</strong>
        </div>
        <div class="station-form-package-import__metric">
          <span>新增</span>
          <strong>{{ previewResp.newCount || 0 }}</strong>
        </div>
        <div class="station-form-package-import__metric">
          <span>覆盖更新</span>
          <strong>{{ previewResp.updateCount || 0 }}</strong>
        </div>
        <div class="station-form-package-import__metric">
          <span>无变化</span>
          <strong>{{ previewResp.sameCount || 0 }}</strong>
        </div>
        <div class="station-form-package-import__metric">
          <span>失败</span>
          <strong>{{ previewResp.failureCount || 0 }}</strong>
        </div>
      </div>

      <div v-if="previewResp" class="station-form-package-import__body">
        <div class="station-form-package-import__messages">
          <div
            v-for="(item, index) in previewResp.messages || []"
            :key="`${index}-${item}`"
            class="station-form-package-import__message"
            :class="{ 'station-form-package-import__message--error': isErrorMessage(item) }"
          >
            {{ item }}
          </div>
          <div v-if="!(previewResp.messages || []).length" class="station-form-package-import__empty">
            暂无预检提示
          </div>
        </div>

        <div class="station-form-package-import__table-wrap">
          <table class="station-form-package-import__table">
            <thead>
              <tr>
                <th>动作</th>
                <th>表单编码</th>
                <th>表单名称</th>
                <th>业务工序</th>
                <th>触发时机</th>
                <th>差异字段</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in forms" :key="`${row.form?.formCode || index}-${row.action}`">
                <td>
                  <Tag :color="actionColor(row.action)">
                    {{ actionLabel(row.action) }}
                  </Tag>
                </td>
                <td>{{ row.form?.formCode || '-' }}</td>
                <td>{{ row.form?.formName || '-' }}</td>
                <td>{{ row.form?.processName || row.form?.processCode || '-' }}</td>
                <td>{{ row.form?.triggerTimingName || row.form?.triggerTimingCode || '-' }}</td>
                <td>{{ (row.diffFields || []).join('、') || '-' }}</td>
              </tr>
              <tr v-if="!forms.length">
                <td colspan="6" class="station-form-package-import__empty">暂无配置包明细</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.station-form-package-import {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.station-form-package-import__summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.station-form-package-import__metric {
  display: flex;
  min-height: 56px;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.station-form-package-import__metric span {
  color: #64748b;
  font-size: 12px;
}

.station-form-package-import__metric strong {
  color: #0f172a;
  font-size: 16px;
}

.station-form-package-import__body {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.station-form-package-import__messages {
  max-height: 360px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.station-form-package-import__message {
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.station-form-package-import__message:last-child {
  border-bottom: 0;
}

.station-form-package-import__message--error {
  color: #dc2626;
}

.station-form-package-import__table-wrap {
  max-height: 360px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  scrollbar-gutter: stable;
}

.station-form-package-import__table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  font-size: 12px;
}

.station-form-package-import__table th,
.station-form-package-import__table td {
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
  color: #334155;
  text-align: left;
  vertical-align: top;
}

.station-form-package-import__table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.station-form-package-import__empty {
  padding: 24px 12px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 960px) {
  .station-form-package-import__summary,
  .station-form-package-import__body {
    grid-template-columns: 1fr;
  }
}
</style>
