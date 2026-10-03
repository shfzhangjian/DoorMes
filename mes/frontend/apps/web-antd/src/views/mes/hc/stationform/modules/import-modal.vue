<script lang="ts" setup>
import type { FileType } from 'ant-design-vue/es/upload/interface';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { computed, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Checkbox,
  Form as AForm,
  FormItem as AFormItem,
  Input,
  InputNumber,
  Modal,
  Select,
  Switch,
  Tag,
  Upload,
  message,
} from 'ant-design-vue';

import {
  confirmStationFormExcelImport,
  previewStationFormExcelImport,
} from '#/api/mes/hc/stationform';

import {
  loadProcessOptions,
  PROCESS_OPTIONS,
  PRESET_TEMPLATE_OPTIONS,
  resolveProcessName,
  TRIGGER_TIMING_NAME_MAP,
  TRIGGER_TIMING_OPTIONS,
} from '../data';

defineOptions({ name: 'HcStationFormImportModal' });

type ImportFormState = MesHcStationFormApi.StationForm & {
  presetTemplate?: string;
};

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
const previewResp = ref<MesHcStationFormApi.StationFormImportResp>();
const processOptions = ref<MesHcStationFormApi.ProcessOption[]>(PROCESS_OPTIONS);

const importForm = ref<ImportFormState>(buildDefaultImportForm());

const uploadFileList = computed(() =>
  selectedFile.value
    ? [
        {
          name: selectedFile.value.name,
          status: 'done' as const,
          uid: 'station-form-import-file',
        },
      ]
    : [],
);

const previewItems = computed(() => importForm.value.items || []);
const needOverwrite = computed(
  () =>
    !!previewResp.value?.existing &&
    !!previewResp.value.form?.formCode &&
    previewResp.value.form.formCode === importForm.value.formCode,
);
const onlyOverwriteFailure = computed(
  () =>
    needOverwrite.value &&
    (previewResp.value?.failures || []).length > 0 &&
    (previewResp.value?.failures || []).every((item) => item.includes('已存在') || item.includes('覆盖')),
);
const canConfirm = computed(
  () =>
    !!previewResp.value &&
    ((previewResp.value.failureCount || 0) === 0 ||
      (onlyOverwriteFailure.value && overwriteExisting.value)) &&
    previewItems.value.length > 0 &&
    !!importForm.value.formCode &&
    !!importForm.value.formName &&
    !!importForm.value.processCode &&
    !!importForm.value.triggerTimingCode &&
    (!needOverwrite.value || overwriteExisting.value),
);

watch(
  () => props.open,
  (open) => {
    if (open) resetState();
  },
);

watch(
  () => [importForm.value.processCode, importForm.value.triggerTimingCode],
  () => syncNames(),
);

function buildDefaultImportForm(): ImportFormState {
  return {
    formCode: '',
    formName: '',
    items: [],
    needConfirm: true,
    presetTemplate: '',
    processCode: 'WET',
    processName: '湿法',
    sortNo: 10,
    status: 1,
    triggerTimingCode: 'IN_PROCESS',
    triggerTimingName: '生产中',
  };
}

function resetState() {
  loading.value = false;
  overwriteExisting.value = false;
  selectedFile.value = undefined;
  previewResp.value = undefined;
  importForm.value = buildDefaultImportForm();
  refreshProcessOptions();
}

function syncNames() {
  importForm.value.processName = resolveProcessName(
    importForm.value.processCode,
    processOptions.value,
  );
  importForm.value.triggerTimingName =
    TRIGGER_TIMING_NAME_MAP[importForm.value.triggerTimingCode || ''] ||
    importForm.value.triggerTimingCode ||
    '';
}

async function refreshProcessOptions() {
  processOptions.value = await loadProcessOptions();
  syncNames();
}

function parseSchemaPreset(schemaJson?: string) {
  if (!schemaJson) return '';
  try {
    const schema = JSON.parse(schemaJson);
    return schema?.presetTemplate || '';
  } catch {
    return '';
  }
}

function syncSchemaPreset(schemaJson?: string, presetTemplate?: string) {
  let schema: Record<string, any> = {};
  try {
    schema = schemaJson ? JSON.parse(schemaJson) : {};
  } catch {
    schema = {};
  }
  if (presetTemplate) {
    schema.presetTemplate = presetTemplate;
    schema.version = presetTemplate;
  }
  return JSON.stringify(schema, null, 2);
}

async function runPreview(file: File) {
  if (!importForm.value.processCode || !importForm.value.triggerTimingCode) {
    message.warning('请选择业务工序和触发时机');
    return;
  }

  loading.value = true;
  try {
    const resp = await previewStationFormExcelImport(file, {
      formCode: importForm.value.formCode || undefined,
      formName: importForm.value.formName || undefined,
      presetTemplate: importForm.value.presetTemplate || undefined,
      processCode: importForm.value.processCode,
      triggerTimingCode: importForm.value.triggerTimingCode,
    });
    previewResp.value = resp;
    overwriteExisting.value = false;
    if (resp.form) {
      importForm.value = {
        ...buildDefaultImportForm(),
        ...resp.form,
        presetTemplate: parseSchemaPreset(resp.form.schemaJson) || importForm.value.presetTemplate || '',
      };
      syncNames();
    }
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
  importForm.value.items = [];
  return true;
}

function handleClose() {
  emit('update:open', false);
}

function buildConfirmPayload(): MesHcStationFormApi.StationFormImportConfirmReq {
  syncNames();
  const { presetTemplate, ...form } = importForm.value;
  const items = (form.items || []).map((item, index) => ({
    ...item,
    itemSeq: item.itemSeq || index + 1,
    requiredFlag: item.requiredFlag ?? true,
    valueMode: item.valueMode || 'TEXT',
  }));
  return {
    form: {
      ...form,
      items,
      needConfirm: form.needConfirm ?? true,
      presetItems: items,
      schemaJson: syncSchemaPreset(form.schemaJson, presetTemplate),
      sortNo: form.sortNo || 10,
      status: form.status ?? 1,
    },
    overwriteExisting: overwriteExisting.value,
  };
}

async function handleConfirm() {
  if (!canConfirm.value) return;
  loading.value = true;
  try {
    const resp = await confirmStationFormExcelImport(buildConfirmPayload());
    previewResp.value = resp;
    if ((resp.failureCount || 0) > 0 || resp.status === 'ERROR') {
      message.error('导入未完成，请查看校验结果');
      return;
    }
    message.success(resp.existing ? '动态表单已覆盖更新' : '动态表单已新增');
    emit('success');
    handleClose();
  } finally {
    loading.value = false;
  }
}

function statusColor(status?: string) {
  return status === 'ERROR' ? 'error' : 'success';
}

function isErrorMessage(text: string) {
  return ['失败', '错误', '为空', '不存在', '未识别', '已存在'].some((keyword) =>
    text.includes(keyword),
  );
}
</script>

<template>
  <Modal
    :open="open"
    title="导入动态表单"
    width="1080px"
    :confirm-loading="loading"
    :ok-button-props="{ disabled: !canConfirm }"
    ok-text="确认导入"
    @cancel="handleClose"
    @ok="handleConfirm"
  >
    <div class="station-form-import">
      <AForm layout="vertical" class="station-form-import__form">
        <div class="station-form-import__grid">
          <AFormItem label="业务工序" required>
            <Select
              v-model:value="importForm.processCode"
              :options="processOptions"
              placeholder="请选择业务工序"
            />
          </AFormItem>
          <AFormItem label="触发时机" required>
            <Select
              v-model:value="importForm.triggerTimingCode"
              :options="TRIGGER_TIMING_OPTIONS"
              placeholder="请选择触发时机"
            />
          </AFormItem>
          <AFormItem label="预设模板">
            <Select
              v-model:value="importForm.presetTemplate"
              allow-clear
              :options="PRESET_TEMPLATE_OPTIONS"
              placeholder="请选择预设模板"
            />
          </AFormItem>
          <AFormItem label="表单编码" required>
            <Input v-model:value="importForm.formCode" placeholder="留空时自动生成" />
          </AFormItem>
          <AFormItem label="表单名称" required>
            <Input v-model:value="importForm.formName" placeholder="留空时取 Excel 标题" />
          </AFormItem>
          <AFormItem label="排序">
            <InputNumber
              v-model:value="importForm.sortNo"
              :min="1"
              :max="9999"
              :precision="0"
              class="station-form-import__number"
            />
          </AFormItem>
          <AFormItem label="需确认">
            <Switch v-model:checked="importForm.needConfirm" checked-children="是" un-checked-children="否" />
          </AFormItem>
          <AFormItem label="状态">
            <Select
              v-model:value="importForm.status"
              :options="[
                { label: '启用', value: 1 },
                { label: '停用', value: 0 },
              ]"
            />
          </AFormItem>
          <AFormItem label="Excel 文件" required>
            <Upload
              accept=".xlsx,.xls"
              :before-upload="beforeUpload"
              :file-list="uploadFileList"
              :max-count="1"
              @remove="removeFile"
            >
              <Button :loading="loading">
                <IconifyIcon icon="lucide:upload" class="mr-1" />
                选择 Excel
              </Button>
            </Upload>
          </AFormItem>
        </div>
      </AForm>

      <Alert
        v-if="needOverwrite"
        show-icon
        type="warning"
        message="当前编码已存在"
        description="确认导入将覆盖原动态表单的基础信息、Schema 与明细项。"
      />

      <Checkbox v-if="needOverwrite" v-model:checked="overwriteExisting">
        覆盖已有动态表单
      </Checkbox>

      <div v-if="previewResp" class="station-form-import__summary">
        <div class="station-form-import__metric">
          <span>状态</span>
          <Tag :color="statusColor(previewResp.status)">
            {{ previewResp.status === 'ERROR' ? '未通过' : '通过' }}
          </Tag>
        </div>
        <div class="station-form-import__metric">
          <span>Sheet</span>
          <strong>{{ previewResp.sourceSheetName || '-' }}</strong>
        </div>
        <div class="station-form-import__metric">
          <span>读取行</span>
          <strong>{{ previewResp.totalRows || 0 }}</strong>
        </div>
        <div class="station-form-import__metric">
          <span>明细项</span>
          <strong>{{ previewItems.length }}</strong>
        </div>
        <div class="station-form-import__metric">
          <span>警告</span>
          <strong>{{ previewResp.warningCount || 0 }}</strong>
        </div>
      </div>

      <div v-if="previewResp" class="station-form-import__body">
        <div class="station-form-import__messages">
          <div
            v-for="(item, index) in previewResp.messages || []"
            :key="`${index}-${item}`"
            class="station-form-import__message"
            :class="{ 'station-form-import__message--error': isErrorMessage(item) }"
          >
            {{ item }}
          </div>
          <div v-if="!(previewResp.messages || []).length" class="station-form-import__empty">
            暂无校验提示
          </div>
        </div>

        <div class="station-form-import__table-wrap">
          <table class="station-form-import__table">
            <thead>
              <tr>
                <th>序号</th>
                <th>分类</th>
                <th>节点</th>
                <th>项目名称</th>
                <th>标准</th>
                <th>值模式</th>
                <th>必填</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in previewItems" :key="`${row.itemSeq || index}-${row.itemName}`">
                <td>{{ row.itemSeq || index + 1 }}</td>
                <td>{{ row.itemCategory || '-' }}</td>
                <td>{{ row.stepNode || '-' }}</td>
                <td>{{ row.itemName || '-' }}</td>
                <td>{{ row.standardText || '-' }}</td>
                <td>{{ row.valueMode || 'TEXT' }}</td>
                <td>{{ row.requiredFlag === false ? '否' : '是' }}</td>
                <td>{{ row.remark || '-' }}</td>
              </tr>
              <tr v-if="!previewItems.length">
                <td colspan="8" class="station-form-import__empty">暂无解析明细</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.station-form-import {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.station-form-import__form {
  padding: 12px 12px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.station-form-import__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 12px;
}

.station-form-import__number {
  width: 100%;
}

.station-form-import__summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.station-form-import__metric {
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

.station-form-import__metric span {
  color: #64748b;
  font-size: 12px;
}

.station-form-import__metric strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.station-form-import__body {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.station-form-import__messages {
  max-height: 320px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.station-form-import__message {
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.station-form-import__message:last-child {
  border-bottom: 0;
}

.station-form-import__message--error {
  color: #dc2626;
}

.station-form-import__table-wrap {
  max-height: 320px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  scrollbar-gutter: stable;
}

.station-form-import__table {
  width: 100%;
  min-width: 920px;
  border-collapse: collapse;
  font-size: 12px;
}

.station-form-import__table th,
.station-form-import__table td {
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
  color: #334155;
  text-align: left;
  vertical-align: top;
}

.station-form-import__table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.station-form-import__empty {
  padding: 24px 12px;
  color: #94a3b8;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 960px) {
  .station-form-import__grid,
  .station-form-import__summary,
  .station-form-import__body {
    grid-template-columns: 1fr;
  }
}
</style>
