<script lang="ts" setup>
import type { MesQmsEnvironmentBoardApi } from '#/api/mes/quality/environment-board';

import { reactive, ref, watch } from 'vue';

import dayjs from 'dayjs';
import { Input, InputNumber, Modal, message } from 'ant-design-vue';

import { saveEnvironmentRecord } from '#/api/mes/quality/environment-board';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

interface AuthUser {
  empName?: string;
  empNo?: string;
  userId?: number;
  username?: string;
}

const props = defineProps<{
  open: boolean;
  record?: MesQmsEnvironmentBoardApi.Record;
  workshopCode: string;
  workshopName: string;
}>();

const emit = defineEmits<{
  goConfirm: [];
  saved: [record: MesQmsEnvironmentBoardApi.Record];
  'update:open': [value: boolean];
}>();

const authVisible = ref(false);
const saving = ref(false);
const form = reactive({
  humidityValue: undefined as number | undefined,
  recordDate: dayjs().format('YYYY-MM-DD'),
  remark: '',
  temperatureValue: undefined as number | undefined,
});

watch(
  () => props.open,
  (open) => {
    if (open) {
      resetForm();
    }
  },
);

function resetForm() {
  form.recordDate = dayjs().format('YYYY-MM-DD');
  form.temperatureValue = toNumber(props.record?.temperatureValue);
  form.humidityValue = toNumber(props.record?.humidityValue);
  form.remark = props.record?.remark || '';
}

function requestSave() {
  if (!isValidNumber(form.temperatureValue) || !isValidNumber(form.humidityValue)) {
    message.warning('请填写当天温度和湿度。');
    return;
  }
  authVisible.value = true;
}

async function handleAuthSuccess(user: AuthUser) {
  saving.value = true;
  try {
    const record = await saveEnvironmentRecord({
      humidityValue: Number(form.humidityValue),
      recordDate: form.recordDate,
      recorderId: user.userId,
      recorderName: user.empName || user.username || user.empNo || '',
      recorderUsername: user.username || user.empNo,
      remark: form.remark || undefined,
      temperatureValue: Number(form.temperatureValue),
      workshopCode: props.workshopCode,
      workshopName: props.workshopName,
    });
    emit('saved', record);
    emit('update:open', false);
    Modal.confirm({
      cancelText: '稍后',
      content: '温湿度已保存，还需要在温湿度看板完成确认。',
      okText: '去确认',
      onOk: () => emit('goConfirm'),
      title: '保存成功',
    });
  } finally {
    saving.value = false;
    authVisible.value = false;
  }
}

function handleCancel() {
  emit('update:open', false);
}

function isValidNumber(value?: number) {
  return value !== undefined && Number.isFinite(Number(value));
}

function toNumber(value: null | number | string | undefined) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}
</script>

<template>
  <Modal
    :confirm-loading="saving"
    :open="open"
    cancel-text="取消"
    ok-text="认证保存"
    title="当天温湿度记录"
    @cancel="handleCancel"
    @ok="requestSave"
  >
    <div class="environment-daily-record-form">
      <label>
        日期
        <div class="environment-daily-record-form__date">{{ form.recordDate }}</div>
      </label>
      <label>
        温度℃
        <InputNumber
          v-model:value="form.temperatureValue"
          :precision="2"
          :step="0.1"
          class="environment-daily-record-form__control"
        />
      </label>
      <label>
        湿度%RH
        <InputNumber
          v-model:value="form.humidityValue"
          :precision="2"
          :step="0.1"
          class="environment-daily-record-form__control"
        />
      </label>
      <label>
        备注
        <Input v-model:value="form.remark" allow-clear placeholder="备注" />
      </label>
    </div>

    <AuthModal
      v-model:visible="authVisible"
      action-name="保存当天温湿度记录"
      auth-mode="username"
      title="温湿度记录签核"
      :workstation="workshopName"
      @cancel="authVisible = false"
      @success="handleAuthSuccess"
    />
  </Modal>
</template>

<style scoped>
.environment-daily-record-form {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr;
}

.environment-daily-record-form label {
  display: grid;
  gap: 6px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.environment-daily-record-form__date {
  height: 32px;
  border: 1px solid #d9d9d9;
  background: #f8fafc;
  color: #0f172a;
  line-height: 30px;
  padding: 0 11px;
}

.environment-daily-record-form__control {
  width: 100%;
}
</style>
