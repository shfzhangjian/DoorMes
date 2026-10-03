<script lang="ts" setup>
import { reactive, ref, watch } from 'vue';

import {
  DatePicker,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
} from 'ant-design-vue';

import { createProductionRecordRevision } from '#/api/mes/hc/production-record-revision';

export type ProductionRecordRevisionFieldType =
  | 'date'
  | 'datetime'
  | 'integer'
  | 'number'
  | 'select'
  | 'text';

export interface ProductionRecordRevisionField {
  key: string;
  label: string;
  options?: Array<{ label: string; value: string }>;
  type: ProductionRecordRevisionFieldType;
}

const props = defineProps<{
  fields: ProductionRecordRevisionField[];
  moduleCode: string;
  row?: object;
}>();

const emit = defineEmits<{ saved: [] }>();
const open = defineModel<boolean>('open', { default: false });

const formData = reactive<Record<string, any>>({});
const reviseReason = ref('');
const submitting = ref(false);

watch(
  () => [open.value, props.row],
  () => {
    if (!open.value || !props.row) return;
    for (const key of Object.keys(formData)) delete formData[key];
    for (const field of props.fields) formData[field.key] = rowValue(field.key);
    reviseReason.value = '';
  },
  { deep: true },
);

function hasChanged(value: any, original: any) {
  return String(value ?? '') !== String(original ?? '');
}

function rowValue(key: string) {
  return (props.row as Record<string, any> | undefined)?.[key];
}

async function save() {
  const recordId = Number(rowValue('id'));
  if (!recordId) {
    message.error('当前生产记录缺少编号，无法建立展示修订');
    return;
  }
  const reason = reviseReason.value.trim();
  if (!reason) {
    message.warning('请填写修订原因');
    return;
  }
  const revisedData = Object.fromEntries(
    props.fields
      .filter((field) => hasChanged(formData[field.key], rowValue(field.key)))
      .map((field) => [field.key, formData[field.key]]),
  );
  if (!Object.keys(revisedData).length) {
    message.warning('请至少修改一个展示字段');
    return;
  }
  submitting.value = true;
  try {
    await createProductionRecordRevision({
      moduleCode: props.moduleCode,
      originalSnapshot: { ...(props.row as Record<string, any>) },
      recordId,
      revisedData,
      reviseReason: reason,
    });
    message.success('生产记录展示修订已保存，原始报工数据未变更');
    open.value = false;
    emit('saved');
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <Modal
    v-model:open="open"
    :confirm-loading="submitting"
    destroy-on-close
    title="修订生产记录展示"
    width="860px"
    @ok="save"
  >
    <p class="revision-notice">
      保存后仅覆盖生产记录表和导出中的展示值，不会回写报工、批次或耗材原始数据。
    </p>
    <Form class="revision-form" layout="vertical">
      <FormItem v-for="field in fields" :key="field.key" :label="field.label">
        <DatePicker
          v-if="field.type === 'date'"
          v-model:value="formData[field.key]"
          class="form-control"
          value-format="YYYY-MM-DD"
        />
        <DatePicker
          v-else-if="field.type === 'datetime'"
          v-model:value="formData[field.key]"
          class="form-control"
          show-time
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <InputNumber
          v-else-if="field.type === 'number' || field.type === 'integer'"
          v-model:value="formData[field.key]"
          :precision="field.type === 'integer' ? 0 : 3"
          class="form-control"
        />
        <Select
          v-else-if="field.type === 'select'"
          v-model:value="formData[field.key]"
          :options="field.options"
          allow-clear
          class="form-control"
        />
        <Input v-else v-model:value="formData[field.key]" allow-clear />
      </FormItem>
      <FormItem label="修订原因" required>
        <Input.TextArea
          v-model:value="reviseReason"
          :maxlength="500"
          :rows="3"
          placeholder="请说明展示修订原因，原始生产事实不会被修改"
          show-count
        />
      </FormItem>
    </Form>
  </Modal>
</template>

<style scoped>
.revision-notice {
  margin: 0 0 12px;
  padding: 8px 10px;
  color: #8a5a00;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 4px;
}

.revision-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.revision-form :deep(.ant-form-item:last-child) {
  grid-column: 1 / -1;
}

.form-control {
  width: 100%;
}
</style>
